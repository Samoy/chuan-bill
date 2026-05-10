# 账号注销功能设计

## 概述

在"账号与安全"页面实现账号注销功能，移除设备管理入口。注销为危险操作，需通过手机短信验证码验证身份。微信小程序登录且未绑定手机号的用户不显示注销入口（由微信官方通道处理数据清理）。

## 前端设计

### account.vue 修改

1. **移除**：`goToDeviceManagement` 函数及"登录设备管理"入口
2. **条件显示**：整个"安全设置"组仅在 `userStore.phone` 存在时显示（无手机号时整个组隐藏，避免空组）
3. **交互流程**：
   - 点击"注销账号" → `message.confirm` 警告（"注销后，所有数据将被永久删除且无法恢复。确定要注销吗？"）
   - 用户确认 → 打开 `AccountDeletePopup` 弹窗

### 新建 AccountDeletePopup.vue

位置：`src/pages/mine/components/AccountDeletePopup.vue`

遵循现有弹窗模式（参考 `PasswordChangePopup.vue`、`PhoneChangePopup.vue`）：

- **容器**：`wd-action-sheet`，通过 `defineModel<boolean>` 控制显隐
- **内容**：
  - 标题："注销账号"
  - 红色警告文案："注销后，您的所有数据将被永久删除且无法恢复"
  - 脱敏手机号展示（`138****1234`）
  - 验证码输入框 + "获取验证码"按钮（60s 倒计时）
  - "确认注销"按钮（验证码填写后才可点击）
- **API 调用**：
  - 获取验证码：`Apis.user_getPhoneCode`（向绑定手机发送验证码）
  - 提交注销：`Apis.user_accountDelete`（传递验证码）
- **成功处理**：toast 提示 → `userStore.logout()` → 跳转首页

## 后端设计

### 新增接口

`POST /user/account/delete`

**请求体**：`DeleteAccountDTO`
```java
@Data
public class DeleteAccountDTO {
    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

**处理流程**：
1. `StpUtil.getLoginIdAsString()` 获取当前用户 ID
2. `VerificationCodeService` 校验验证码
3. 软删除用户：设置 `deleted = true`
4. `StpUtil.logout(userId)` 登出
5. 返回 `Result.success()`

**新增文件**：
- `controller/UserController.java`：新增 `deleteAccount` 方法
- `service/UserService.java` + `service/impl/UserServiceImpl.java`：新增 `deleteAccount` 方法
- `dto/DeleteAccountDTO.java`：请求参数

## 风险：软删除后重复记录

当前 `loginByPhone` 和 `loginByWechat` 在用户查不到时直接 `save()` 创建新用户，不检查是否存在软删除记录。注销后重新登录会产生重复行（同手机号/openid）。

**处理方案**：在注销接口中，除了设置 `deleted = true`，同时清除 `phone` 和 `openid` 字段（置空或加后缀），确保重新登录时不会与旧记录冲突，且新用户从零开始。

## API 定义

运行 `pnpm alova-gen` 重新生成 API 定义，然后执行 `alova-api-fix` 修复类型。

## 边界情况

| 场景 | 处理 |
|------|------|
| 微信登录，无手机号 | 不显示注销入口 |
| 微信登录，已绑定手机号 | 正常显示，走短信验证 |
| 手机号登录 | 正常显示，走短信验证 |
| 验证码过期/错误 | 后端返回错误，前端 toast 提示 |
| 注销后重新登录（手机/微信） | 用户已软删除，登录查不到用户，系统会创建新账号（旧数据不可恢复） |
| 注销后用密码登录 | 抛出 USER_NOT_FOUND 错误 |
