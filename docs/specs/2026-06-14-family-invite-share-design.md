# 家庭邀请分享功能设计

## [S1] 问题

GitHub Issue #23: 实现家庭邀请分享功能

当前家庭系统已有完整的邀请码机制（创建、加入、审批），但缺少便捷的分享方式。用户需要手动复制邀请码并发送给家人，体验不够流畅。

## [S2] 解决方案概述

在家庭详情页添加「分享」按钮，根据平台自动选择最佳分享方式：
- **小程序端**：使用微信原生分享能力（`onShareAppMessage`），直接发送给好友/群聊
- **H5/App端**：复制分享链接到剪贴板，并显示Toast提示

被邀请者通过分享链接进入App后，自动弹出邀请码输入弹框并预填邀请码。

## [S3] 前端实现

### 3.1 家庭详情页改动 (`family/detail.vue`)

**邀请码区域按钮调整：**
- 移除「复制」按钮
- 添加「分享」按钮（primary样式）
- 「刷新」按钮仅户主可见
- 按钮样式统一：分享(primary) + 刷新(plain)

**分享逻辑：**
```vue
<!-- #ifdef MP-WEIXIN -->
<script>
export default {
  onShareAppMessage() {
    const family = this.currentFamily
    return {
      title: `邀请你加入「${family.name}」`,
      path: `/pages/family/index?inviteCode=${family.inviteCode}`,
      imageUrl: family.avatar
    }
  }
}
</script>
<!-- #endif -->
```

```ts
// #ifndef MP-WEIXIN
function handleShare() {
  const family = currentFamily.value
  if (!family) return
  
  const baseUrl = import.meta.env.VITE_SHARE_BASE_URL || window.location.origin
  const link = `${baseUrl}/pages/family/index?inviteCode=${family.inviteCode}`
  
  uni.setClipboardData({
    data: `邀请你加入「${family.name}」，邀请码：${family.inviteCode}\n链接：${link}`,
    success: () => {
      toast.success('分享链接已复制')
    }
  })
}
// #endif
```

### 3.2 家庭首页自动弹框 (`family/index.vue`)

**检测邀请码参数：**
```ts
onLoad((options) => {
  if (options?.inviteCode) {
    joinForm.value.inviteCode = options.inviteCode
    showJoinPopup.value = true  // 自动弹出邀请码弹框
  }
})
```

**弹框已预填邀请码：**
- 用户无需手动输入
- 可选择添加备注后直接提交申请

## [S4] 后端实现

**无需新增接口**，现有API已完全支持：
- `POST /family/join` - 通过邀请码加入家庭
- `GET /family/detail` - 获取家庭详情（含邀请码）
- `POST /family/refresh-invite-code` - 刷新邀请码

## [S5] 分享链接格式

**H5链接：**
```
https://chuan-bill.example.com/pages/family/index?inviteCode=123456
```

**小程序路径：**
```
/pages/family/index?inviteCode=123456
```

**复制文本格式：**
```
邀请你加入「我的家庭」，邀请码：123456
链接：https://chuan-bill.example.com/pages/family/index?inviteCode=123456
```

## [S6] 平台兼容性

| 平台 | 分享方式 | 触发行为 |
|------|----------|----------|
| 微信小程序 | onShareAppMessage | 发送给好友/群聊 |
| H5 | 复制链接 | Toast提示已复制 |
| App | 复制链接 | Toast提示已复制 |

## [S7] 待办项

### P2: 家庭邀请通知推送
- 户主审批完成后，小程序端使用一次性订阅消息通知被邀请人
- 其他端需要使用推送服务（需要大量后端改造）
- 优先级：P2，当前先完成分享功能

## [S8] 测试要点

1. 小程序端分享卡片显示正确的家庭名称和头像
2. H5/App端复制的链接格式正确，可正常打开
3. 被邀请者通过链接进入后，邀请码自动填入
4. 刷新按钮仅户主可见
5. 未登录状态下的分享行为（跳转登录页）

## [S9] 实现状态

- [x] Task 1: 家庭详情页添加分享按钮
- [x] Task 2: 家庭首页自动弹出邀请码弹框
- [x] Task 3: 环境变量配置
- [x] Task 4: 文档更新（当前任务）
