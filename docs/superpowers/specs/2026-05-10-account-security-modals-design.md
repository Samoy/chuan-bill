# 账号安全弹框重构设计文档

## 目标

将 `account.vue` 中的修改密码弹框和修改手机号弹框拆分为独立组件，修复验证码发送逻辑混乱问题，并支持多种用户状态下的不同操作流程。

## 当前问题

1. 密码修改和手机号修改共用 `sendCode` 函数，但两者调用的接口不同
2. `UserServiceImpl.getPhoneCode()` 存在 bug（line 248 条件取反），导致已绑定手机的用户无法获取验证码
3. 未考虑不同用户状态（无密码、无手机号）的差异化处理

## 用户状态矩阵

| 用户类型 | 有手机号 | 有密码 | 可用操作 |
|---------|---------|-------|---------|
| 手机验证码注册 | 是 | 否 | 验证码改密、验证码换绑手机 |
| 密码注册 | 是 | 是 | 密码改密、验证码改密、密码换绑手机、验证码换绑手机 |
| 微信登录（未绑手机） | 否 | 否 | 绑定手机号 |
| 微信登录（已绑手机） | 是 | 否 | 验证码改密、验证码换绑手机 |

## 组件设计

### 通用约定

- 所有弹框使用 `wd-action-sheet` 组件（非 `wd-popup`）
- 标题通过 `wd-action-sheet` 的 `title` 属性设置，不再使用独立的标题 DOM
- 组件放置于 `src/pages/mine/components/` 目录，与现有弹框组件保持一致
- 使用 `defineModel<boolean>()` 控制显隐
- Tab 切换使用 `wd-segmented` 组件（非自定义 Tab）

### PasswordChangeModal.vue

**位置**: `src/pages/mine/components/PasswordChangeModal.vue`

**Props**:
- `modelValue`: boolean (v-model)

**内部状态**:
- `activeTab`: string（默认值 '验证码验证'）
- `hasPassword`: boolean（打开弹框时查询 `user.hasPassword` 接口）
- `segmentedRef`: SegmentedInstance（用于微信小程序端更新样式）
- `passwordForm`: { oldPassword, newPassword, confirmPassword }
- `codeForm`: { code, newPassword, confirmPassword }

**Tab 显示逻辑**:
- 有密码：使用 `wd-segmented` 显示「密码验证」和「验证码验证」两个选项
- 无密码：仅显示验证码表单，隐藏 `wd-segmented`
- 微信小程序端：弹框打开后需调用 `segmentedRef.updateActiveStyle()` 更新样式

**密码验证 Tab**:
- 输入：旧密码、新密码、确认密码
- 密码强度指示器
- 提交调用 `user.updatePasswordByOld`

**验证码验证 Tab**:
- 显示当前手机号（脱敏）
- 发送验证码按钮 → 调用 `user.getPhoneCode`（向当前手机发送）
- 输入：验证码、新密码、确认密码
- 提交调用 `user.updatePasswordByCode`

**成功后**: 提示重新登录，调用 `userStore.logout()` 并跳转首页

### PhoneChangeModal.vue

**位置**: `src/pages/mine/components/PhoneChangeModal.vue`

**Props**:
- `modelValue`: boolean (v-model)

**内部状态**:
- `mode`: 'bind' | 'code' | 'password'
- `hasPhone`: boolean（从 `userStore.phone` 判断）
- `hasPassword`: boolean（打开弹框时查询 `user.hasPassword` 接口）
- `bindForm`: { phone, code }
- `codeForm`: { newPhone, oldPhoneCode, newPhoneCode }
- `passwordForm`: { newPhone, password, newPhoneCode }

**模式逻辑**:

1. **bind 模式**（无手机号无密码）:
   - 提示「您还没有绑定手机号，请先绑定手机号」
   - 输入：新手机号、验证码
   - 发送验证码 → 调用 `auth.sendCode`（向新手机发送）
   - 提交调用 `user.bindPhone`

2. **code 模式**（有手机号，默认模式）:
   - 显示当前手机号（脱敏）
   - 输入：新手机号、当前手机验证码、新手机验证码
   - 「发送验证码」按钮（当前手机）→ 调用 `user.getPhoneCode`
   - 「发送验证码」按钮（新手机）→ 调用 `auth.sendCode`
   - 提交调用 `user.updatePhoneByCode`
   - 底部链接：「当前手机不可用？」→ 切换到 password 模式

3. **password 模式**（有手机号且有密码）:
   - 输入：新手机号、登录密码、新手机验证码
   - 发送验证码（新手机）→ 调用 `auth.sendCode`
   - 提交调用 `user.updatePhoneByPassword`
   - 底部链接：「使用验证码修改手机号」→ 切换回 code 模式

**注意**: password 模式仅在用户有密码时可用。如果用户无密码，不显示「当前手机不可用？」入口。

## 接口调用对照

| 操作 | 接口 | 说明 |
|------|------|------|
| 发送验证码到当前手机 | `user.getPhoneCode` | 需修复 bug |
| 发送验证码到指定手机 | `auth.sendCode` | 通用验证码发送 |
| 密码修改密码 | `user.updatePasswordByOld` | 旧密码验证 |
| 验证码修改密码 | `user.updatePasswordByCode` | 验证码验证 |
| 验证码换绑手机 | `user.updatePhoneByCode` | 需要旧手机+新手机验证码 |
| 密码换绑手机 | `user.updatePhoneByPassword` | 需要密码+新手机验证码 |
| 绑定手机号 | `user.bindPhone` | 微信用户绑定手机 |
| 查询是否有密码 | `user.hasPassword` | 用于决定 UI 模式 |

## 后端修复

### UserServiceImpl.getPhoneCode() bug

**文件**: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java`

**Line 248**: 将 `if (!CharSequenceUtil.isEmpty(user.getPhone()))` 改为 `if (CharSequenceUtil.isEmpty(user.getPhone()))`

原因：原代码在用户有手机号时抛出异常，但该接口的目的是向已有手机号发送验证码。

## 账号页面 (account.vue) 改造

- 移除所有弹框相关逻辑
- 引入 `PasswordChangeModal` 和 `PhoneChangeModal` 组件
- 点击「修改密码」→ 打开 `PasswordChangeModal`
- 点击「手机号」行 → 打开 `PhoneChangeModal`
- 页面仅保留展示逻辑和跳转逻辑
