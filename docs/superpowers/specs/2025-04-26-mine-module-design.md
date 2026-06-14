# 个人中心模块设计文档

**日期：** 2025-04-26  
**作者：** Claude Code  
**状态：** 待实现

---

## 1. 概述

### 1.1 目标
完善"小川记账"应用的个人中心功能，提升用户体验和账号管理能力。

### 1.2 设计原则
1. **隐私优先**：未登录状态不发起任何网络请求
2. **渐进增强**：登录后解锁更多功能
3. **一致性**：遵循现有项目架构和UI规范
4. **合规性**：满足中国大陆AI服务算法备案要求

### 1.3 页面清单

| 页面 | 路径 | 功能 |
|------|------|------|
| 个人中心 | `pages/mine/index.vue` | 更新菜单action |
| 个人信息 | `pages/mine/profile/index.vue` | 编辑头像、昵称、性别 |
| 修改密码 | `pages/mine/password/index.vue` | 验证码方式修改密码 |
| 更换手机 | `pages/mine/phone/index.vue` | 预留页面 |
| 设置 | `pages/mine/settings/index.vue` | 主题、通知、存储等 |
| 帮助与反馈 | `pages/mine/help/index.vue` | FAQ + AI客服 |
| 关于 | `pages/mine/about/index.vue` | 应用信息 + 算法备案 |

---

## 2. 页面详细设计

### 2.1 个人中心主页面 (mine/index.vue)

**现有菜单更新：**

```typescript
const menuList = [
  { icon: 'i-lucide:user', title: '个人信息', action: () => router.push('/pages/mine/profile/index') },
  { icon: 'i-lucide:settings', title: '设置', action: () => router.push('/pages/mine/settings/index') },
  { icon: 'i-lucide:help-circle', title: '帮助与反馈', action: () => router.push('/pages/mine/help/index') },
  { icon: 'i-lucide:info', title: '关于', action: () => router.push('/pages/mine/about/index') },
]
```

**交互：**
- 点击菜单项跳转对应子页面
- 未登录状态下点击"个人信息"等需要登录的页面，触发登录弹窗

---

### 2.2 个人信息编辑页 (mine/profile/index.vue)

**布局结构：**

```
┌─────────────────────────────┐
│         [大头像]            │  ← 圆形，居中，点击更换
│        点击更换头像         │
├─────────────────────────────┤
│ 昵称              [输入框]  │  ← wd-input
├─────────────────────────────┤
│ 性别    [男] [女] [保密]    │  ← wd-radio-group button
├─────────────────────────────┤
│ 手机号    138****8888  >    │  ← 只读，脱敏显示
└─────────────────────────────┘
```

**字段定义：**

| 字段 | 组件 | 配置 |
|------|------|------|
| 头像 | wd-upload | limit=1, accept=image, action=/file/upload |
| 昵称 | wd-input | maxlength=20, placeholder="请输入昵称" |
| 性别 | wd-radio-group | shape="button", 选项: 1-男, 2-女, 0-保密 |
| 手机号 | 纯展示 | 脱敏格式：138****8888 |

**API：**
- 获取资料：`Apis.user.getProfile()`
- 更新资料：`Apis.user.updateProfile({ nickname, avatar, gender })`

**交互流程：**
1. 页面加载时调用 `getProfile` 获取最新资料
2. 头像上传成功后更新表单数据
3. 点击保存时校验昵称非空
4. 调用 updateProfile 提交修改
5. 成功后更新 userStore 并返回上一页

---

### 2.3 修改密码页 (mine/password/index.vue)

**布局结构：**

```
┌─────────────────────────────┐
│ 手机号            138****8888 │ ← 只读，当前绑定手机
├─────────────────────────────┤
│ 验证码      [输入框] [发送]  │ ← wd-input + 倒计时按钮
├─────────────────────────────┤
│ 新密码          [输入框*]    │ ← wd-input type=password
├─────────────────────────────┤
│ 确认新密码      [输入框*]    │ ← wd-input type=password
├─────────────────────────────┤
│ [          提交           ]  │ ← wd-button block
└─────────────────────────────┘
```

**表单校验规则：**

| 字段 | 规则 |
|------|------|
| 验证码 | 必填，6位数字 |
| 新密码 | 必填，6-20位，包含字母和数字 |
| 确认密码 | 必填，必须与新密码一致 |

**密码强度提示：**
- 弱：纯数字或纯字母（红色）
- 中：数字+字母（黄色）
- 强：数字+字母+特殊字符（绿色）

**API：**
- 发送验证码：`Apis.auth.sendCode({ phone })`
- 修改密码：`Apis.user.updatePasswordByCode({ phone, code, newPassword })`

**交互流程：**
1. 页面显示当前绑定手机号（脱敏）
2. 点击"发送"按钮：
   - 调用 sendCode 发送验证码
   - 按钮进入60秒倒计时
   - 显示"已发送"提示
3. 输入新密码时实时校验强度
4. 点击提交：
   - 校验所有字段
   - 调用 updatePasswordByCode
   - 成功后显示"密码修改成功"并返回

---

### 2.4 设置页面 (mine/settings/index.vue)

**分组布局：**

```
┌─────────────────────────────┐
│ 外观                        │
├─────────────────────────────┤
│ 主题模式              [跟随系统] │ ← 点击弹出选择器
├─────────────────────────────┤
│ 通知                        │
├─────────────────────────────┤
│ 消息推送                [开关] │ ← wd-switch
├─────────────────────────────┤
│ 账单提醒                [开关] │ ← wd-switch
├─────────────────────────────┤
│ 关于                        │
├─────────────────────────────┤
│ 检查更新                [1.0.0] │
├─────────────────────────────┤
│ 用户协议                   >  │
├─────────────────────────────┤
│ 隐私政策                   >  │
├─────────────────────────────┤
│ 存储                        │
├─────────────────────────────┤
│ 清除缓存               [12MB] │ ← 点击清除
├─────────────────────────────┤
│ 账号                        │
├─────────────────────────────┤
│ 修改密码                   >  │
├─────────────────────────────┤
│ 退出登录                   >  │ ← 红色文字
└─────────────────────────────┘
```

**主题选项：**
- 跟随系统（默认）
- 浅色模式
- 深色模式

**缓存计算：**
```typescript
function getCacheSize(): string {
  // 使用 uni.getStorageInfoSync 计算本地缓存
  const info = uni.getStorageInfoSync()
  const size = info.keys.reduce((total, key) => {
    const value = uni.getStorageSync(key)
    return total + JSON.stringify(value).length
  }, 0)
  return formatSize(size)
}
```

**交互：**
- 主题切换：底部弹出选择器，选择后立即生效
- 开关切换：实时保存到本地存储
- 清除缓存：确认弹窗后清除，显示"已清除"
- 退出登录：确认弹窗后调用 logout，返回首页

---

### 2.5 帮助与反馈页 (mine/help/index.vue)

**未登录状态：**

```
┌─────────────────────────────┐
│ 常见问题                    │
├─────────────────────────────┤
│ [如何添加账单？]            │
├─────────────────────────────┤
│ [如何创建/加入家庭？]       │
├─────────────────────────────┤
│ [数据安全吗？会丢失吗？]    │
├─────────────────────────────┤
│ [如何导出账单？]            │
├─────────────────────────────┤
│ [展开图标]  其他问题        │
├─────────────────────────────┤
│ 登录后可使用AI客服          │
│ [      去登录      ]        │
└─────────────────────────────┘
```

**已登录状态：**

```
┌─────────────────────────────┐
│ [AI客服入口卡片]            │
│ 智能客服，为您解答任何问题   │
│ [    开始对话    ]          │
├─────────────────────────────┤
│ 常见问题                    │
├─────────────────────────────┤
│ [FAQ列表同上]               │
└─────────────────────────────┘
```

**静态FAQ内容（客户端内置）：**

```typescript
const faqList = [
  {
    question: '如何添加账单？',
    answer: '点击首页底部的"+"按钮，选择记账方式（手动/拍照/语音），填写账单信息后保存即可。'
  },
  {
    question: '如何创建/加入家庭？',
    answer: '在"家庭"页面点击"创建家庭"或"加入家庭"，创建后会自动生成邀请码，分享给家人即可加入。'
  },
  {
    question: '数据安全吗？会丢失吗？',
    answer: '未登录时数据仅存储在本地；登录后数据会同步到云端，换设备登录后可恢复数据。我们采用加密传输和存储，保障数据安全。'
  },
  {
    question: '如何导出账单？',
    answer: '登录后进入"我的"-"设置"-"数据管理"，可选择导出Excel或PDF格式的账单数据。'
  }
]
```

**AI客服预留：**
- 点击"开始对话"进入AI客服页面（预留）
- 调用百炼平台API（需先在平台搭建Agent）
- 支持上下文对话

---

### 2.6 关于页面 (mine/about/index.vue)

**布局结构：**

```
┌─────────────────────────────┐
│                             │
│         [应用Logo]          │
│         小川记账            │
│         版本 1.0.0          │
│                             │
├─────────────────────────────┤
│ 算法备案信息                │
├─────────────────────────────┤
│ 算法名称：阿里云百炼大模型  │
│ 算法备案号：[待填写]        │
│ 服务提供者：阿里云          │
├─────────────────────────────┤
│ 开源许可                    │
├─────────────────────────────┤
│ 第三方开源组件致谢         >  │
├─────────────────────────────┤
│ 联系我们                    │
├─────────────────────────────┤
│ 反馈邮箱：feedback@chu...  │
└─────────────────────────────┘
```

**算法备案信息（合规要求）：**

根据《互联网信息服务算法推荐管理规定》和《生成式人工智能服务管理暂行办法》，使用大模型服务需在关于页面展示：

- 算法名称
- 算法备案号
- 服务提供者信息

**版本号获取：**
```typescript
const version = ref('')
onLoad(() => {
  // #ifdef APP-PLUS
  version.value = plus.runtime.version
  // #endif
  // #ifdef MP-WEIXIN
  const accountInfo = wx.getAccountInfoSync()
  version.value = accountInfo.miniProgram.version || '1.0.0'
  // #endif
})
```

---

## 3. 数据流设计

### 3.1 用户资料同步

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  个人信息页  │ ───> │  userStore   │ ───> │   后端API    │
└──────────────┘      └──────────────┘      └──────────────┘
       │                     │                     │
       │                     ▼                     │
       │              ┌──────────────┐             │
       └─────────────>│   本地缓存   │<────────────┘
                      └──────────────┘
```

**更新流程：**
1. 用户修改资料并提交
2. 调用 `Apis.user.updateProfile()`
3. 成功后更新 `userStore` 中的对应字段
4. 本地缓存自动同步（通过 persist 插件）

### 3.2 登录状态检查

```typescript
// 页面级别的登录检查
onShow(() => {
  if (!userStore.isLoggedIn) {
    userStore.requireAuth(() => {
      // 登录成功后的回调
      initPageData()
    })
  }
})
```

---

## 4. API 清单

### 4.1 已有API（无需改动）

| API | 用途 |
|-----|------|
| `Apis.user.getProfile()` | 获取用户资料 |
| `Apis.user.updateProfile()` | 更新用户资料 |
| `Apis.user.updatePasswordByCode()` | 验证码修改密码 |
| `Apis.user.hasPassword()` | 检查是否设置密码 |
| `Apis.auth.sendCode()` | 发送验证码 |
| `Apis.file.uploadTempFileToR2()` | 上传头像 |

### 4.2 预留API（后续实现）

| API | 用途 |
|-----|------|
| `Apis.user.updatePhone()` | 更换手机号 |
| `Apis.ai.chat()` | AI客服对话 |

---

## 5. 路由配置

在 `pages.config.ts` 中添加：

```typescript
easycom: {
  autoscan: true,
  custom: {
    // ... 现有配置
  }
}
// pages 数组由 vite-plugin-uni-pages 自动生成
// 页面文件创建后会自动注册路由
```

页面文件路径遵循约定：
- `src/pages/mine/profile/index.vue` → 路由 `/pages/mine/profile/index`
- `src/pages/mine/password/index.vue` → 路由 `/pages/mine/password/index`

---

## 6. 组件清单

### 6.1 使用现有组件

| 组件 | 用途 |
|------|------|
| wd-upload | 头像上传 |
| wd-input | 文本输入 |
| wd-radio-group | 性别选择 |
| wd-switch | 开关设置 |
| wd-button | 操作按钮 |
| wd-popup | 主题选择器 |
| wd-picker | 底部选择器 |
| wd-divider | 分隔线 |
| wd-loading | 加载状态 |

### 6.2 可能需要的自定义组件

| 组件 | 用途 |
|------|------|
| CountdownButton | 验证码发送按钮（带倒计时） |
| PasswordStrength | 密码强度指示器 |
| CacheSize | 缓存大小显示（带计算逻辑） |

---

## 7. 样式规范

### 7.1 颜色变量

使用项目已有CSS变量：
- `--wot-primary` - 主色调
- `--wot-dark-background` - 深色模式背景
- `--wot-dark-background2` - 深色模式卡片背景

### 7.2 间距规范

```
页面内边距：p-4 (16px)
卡片间距：gap-3 (12px)
列表项间距：py-4 (16px纵向)
分组间距：mt-4 (16px)
```

### 7.3 文字规范

```
标题：text-lg font-500
正文：text-sm
辅助文字：text-xs text-gray-500
链接文字：text-sm text-primary
警告文字：text-error
```

---

## 8. 错误处理

### 8.1 网络错误

```typescript
try {
  await Apis.user.updateProfile({ ... })
} catch (error) {
  const toast = useGlobalToast()
  toast.error('网络异常，请稍后重试')
}
```

### 8.2 表单校验错误

```typescript
function validateForm(): boolean {
  if (!formData.nickname.trim()) {
    toast.warning('请输入昵称')
    return false
  }
  if (formData.newPassword !== formData.confirmPassword) {
    toast.warning('两次输入的密码不一致')
    return false
  }
  return true
}
```

---

## 9. 测试要点

### 9.1 功能测试

- [ ] 未登录时点击菜单触发登录弹窗
- [ ] 头像上传成功并正确显示
- [ ] 昵称修改后同步到个人中心首页
- [ ] 性别选择正确保存
- [ ] 验证码发送倒计时正常
- [ ] 密码修改成功后可用新密码登录
- [ ] 主题切换立即生效
- [ ] 缓存清除后大小显示为0
- [ ] 退出登录后回到未登录状态

### 9.2 边界测试

- [ ] 网络断开时显示错误提示
- [ ] 上传大图片时显示加载状态
- [ ] 验证码连续点击只发送一次
- [ ] 密码强度实时计算正确

---

## 10. 后续扩展

### 10.1 预留功能

| 功能 | 说明 |
|------|------|
| 更换手机号 | 页面已预留，等待后端API就绪 |
| AI客服 | 等待百炼平台Agent搭建完成 |
| 数据导出 | 可在设置页面添加入口 |
| 账号注销 | 可在账号安全分组中添加 |

### 10.2 可能的优化

- 添加手势密码/指纹解锁
- 多设备登录管理
- 操作日志查看

---

## 11. 参考资料

- [wot-design-uni 文档](https://wot-ui.cn)
- [百炼平台大模型服务](https://bailian.console.aliyun.com)
- [算法备案要求](https://www.cac.gov.cn/)
- 项目现有代码：`src/pages/family/edit.vue`（头像上传参考）
