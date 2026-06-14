# 家庭邀请分享功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在家庭详情页添加分享按钮，支持小程序原生分享和H5/App端复制链接分享，被邀请者通过链接进入后自动弹出邀请码弹框。

**Architecture:** 前端改动为主，后端无需新增接口。小程序端使用`onShareAppMessage`原生分享，H5/App端复制分享链接到剪贴板。家庭首页检测URL参数自动弹出邀请码弹框。

**Tech Stack:** Vue 3, TypeScript, uni-app, conditional compilation (`#ifdef MP-WEIXIN`)

---

### Task 1: 家庭详情页添加分享按钮

**Covers:** S3, S5, S6

**Files:**
- Modify: `chuan-bill-app/src/pages/family/detail.vue`

- [ ] **Step 1: 添加分享按钮模板**

在邀请码区域的按钮组中，移除「复制」按钮，添加「分享」按钮。修改 `detail.vue` 的模板部分：

```vue
<!-- 邀请码区域（仅户主可见） -->
<view v-if="isOwner" class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
  <view class="flex items-center justify-between">
    <view>
      <text class="block text-sm font-500">
        邀请码
      </text>
    </view>
    <view class="flex items-center gap-2">
      <text class="mr-2 text-lg font-bold tracking-widest font-mono">
        {{ currentFamily.inviteCode }}
      </text>
      <wd-button size="small" type="primary" plain @click="handleShare">
        📤 分享
      </wd-button>
      <wd-button v-if="isOwner" size="small" plain @click="handleRefreshInviteCode">
        刷新
      </wd-button>
    </view>
  </view>
</view>
```

- [ ] **Step 2: 添加小程序端分享逻辑**

在 `<script setup>` 中添加条件编译的分享逻辑。由于 `onShareAppMessage` 已通过 auto-imports 自动导入，可以直接在 `<script setup>` 中使用：

```vue
<script setup lang="ts">
// ... 现有代码保持不变 ...

// #ifdef MP-WEIXIN
onShareAppMessage(() => {
  const family = currentFamily.value
  if (!family) {
    return {
      title: '邀请你加入家庭',
      path: '/pages/family/index'
    }
  }
  return {
    title: `邀请你加入「${family.name}」`,
    path: `/pages/family/index?inviteCode=${family.inviteCode}`,
    imageUrl: family.avatar || undefined
  }
})
// #endif
</script>
```

- [ ] **Step 3: 添加 H5/App 端分享逻辑**

在 `<script setup>` 中添加非小程序端的分享函数：

```ts
// #ifndef MP-WEIXIN
function handleShare() {
  const family = currentFamily.value
  if (!family) return

  const baseUrl = import.meta.env.VITE_SHARE_BASE_URL || window.location.origin
  const link = `${baseUrl}/pages/family/index?inviteCode=${family.inviteCode}`
  const shareText = `邀请你加入「${family.name}」，邀请码：${family.inviteCode}\n链接：${link}`

  uni.setClipboardData({
    data: shareText,
    success: () => {
      toast.success('分享链接已复制')
    }
  })
}
// #endif
```

- [ ] **Step 4: 添加小程序端空函数占位**

为了保持模板中 `@click="handleShare"` 在所有平台都能正常工作，需要在小程序端也定义一个空函数：

```ts
// #ifdef MP-WEIXIN
function handleShare() {
  // 小程序端使用 onShareAppMessage，此函数不需要执行任何操作
  // 分享由右上角菜单触发
}
// #endif
```

- [ ] **Step 5: 验证构建**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 无类型错误

- [ ] **Step 6: 提交代码**

```bash
git add chuan-bill-app/src/pages/family/detail.vue
git commit -m "feat: 家庭详情页添加分享按钮"
```

---

### Task 2: 家庭首页自动弹出邀请码弹框

**Covers:** S3

**Files:**
- Modify: `chuan-bill-app/src/pages/family/index.vue`

- [ ] **Step 1: 修改 onLoad 处理 inviteCode 参数**

在 `index.vue` 的 `onLoad` 中检测 `inviteCode` 参数：

```ts
onLoad((options) => {
  if (options?.inviteCode) {
    joinForm.value.inviteCode = options.inviteCode
    showJoinPopup.value = true
  }
  handleFamilyUpdated()
})
```

- [ ] **Step 2: 处理未登录状态**

如果用户未登录，需要先登录再弹出邀请码弹框。修改逻辑：

```ts
onLoad((options) => {
  if (options?.inviteCode) {
    // 存储邀请码，登录后自动弹出
    uni.setStorageSync('pendingInviteCode', options.inviteCode)
  }
  handleFamilyUpdated()
})

// 监听登录状态变化
watch(() => user.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    const pendingCode = uni.getStorageSync('pendingInviteCode')
    if (pendingCode) {
      joinForm.value.inviteCode = pendingCode
      showJoinPopup.value = true
      uni.removeStorageSync('pendingInviteCode')
    }
  }
}, { immediate: true })
```

- [ ] **Step 3: 验证构建**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 无类型错误

- [ ] **Step 4: 提交代码**

```bash
git add chuan-bill-app/src/pages/family/index.vue
git commit -m "feat: 家庭首页检测邀请码参数自动弹出弹框"
```

---

### Task 3: 环境变量配置

**Covers:** S5

**Files:**
- Modify: `chuan-bill-app/.env` (or `.env.development`, `.env.production`)

- [ ] **Step 1: 添加分享基础URL环境变量**

在 `.env.development` 中添加：

```
VITE_SHARE_BASE_URL=http://localhost:5173
```

在 `.env.production` 中添加：

```
VITE_SHARE_BASE_URL=https://chuan-bill.example.com
```

注意：实际生产环境URL需要根据部署情况配置。

- [ ] **Step 2: 提交代码**

```bash
git add chuan-bill-app/.env.development chuan-bill-app/.env.production
git commit -m "chore: 添加分享链接基础URL环境变量"
```

---

### Task 4: 文档更新

**Covers:** S7

**Files:**
- Modify: `docs/specs/2026-06-14-family-invite-share-design.md`

- [ ] **Step 1: 更新设计文档状态**

在设计文档末尾添加完成状态：

```markdown
## [S9] 实现状态

- [x] Task 1: 家庭详情页添加分享按钮
- [x] Task 2: 家庭首页自动弹出邀请码弹框
- [x] Task 3: 环境变量配置
- [ ] Task 4: 文档更新（当前任务）
```

- [ ] **Step 2: 提交文档**

```bash
git add docs/specs/2026-06-14-family-invite-share-design.md
git commit -m "docs: 更新家庭邀请分享功能设计文档状态"
```
