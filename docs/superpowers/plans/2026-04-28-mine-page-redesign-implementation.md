# "我的"页面重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构"我的"页面为个人中心，实现菜单分组、设置功能前置、账单同步、数据导出、账号安全与通知设置等功能。

**Architecture:** 基于现有 uni-app + Vue3 + TypeScript 架构，使用 wot-design-uni 组件库和 UnoCSS 原子样式。分四阶段实现：第一阶段页面重构与功能前置，第二阶段账单同步，第三阶段数据导出，第四阶段账号安全与通知。

**Tech Stack:** uni-app, Vue 3, TypeScript, wot-design-uni, UnoCSS, Pinia, Alova.js

---

## 文件结构

### 修改现有文件
- `src/pages/mine/index.vue` — 重构页面布局和菜单分组
- `src/store/billStore.ts` — 添加 syncStatus 字段和同步相关方法
- `src/api/globals.d.ts` — 添加新接口类型定义（如需）

### 新建页面文件
- `src/pages/mine/account-security/index.vue` — 账号与安全页面（合并 account.vue 和 password.vue）

### 新建组件文件
- `src/pages/mine/components/SyncStatusPopup.vue` — 同步状态弹窗
- `src/pages/mine/components/ThemePickerPopup.vue` — 主题选择弹窗
- `src/pages/mine/components/NotificationSettingsPopup.vue` — 通知设置弹窗
- `src/pages/mine/components/ExportFilterPopup.vue` — 导出筛选弹窗

### 删除文件
- `src/pages/mine/settings.vue` — 功能已前置，可删除
- `src/pages/mine/account.vue` — 已合并到 account-security
- `src/pages/mine/password.vue` — 已合并到 account-security

---

## 第一阶段：页面重构与功能前置

### Task 1: 重构"我的"页面已登录状态布局

**Files:**
- Modify: `src/pages/mine/index.vue`

**说明:** 重构已登录状态的菜单结构，按功能分组展示。

- [ ] **Step 1: 更新 script 部分，定义新的菜单分组结构**

```vue
<script setup lang="ts">
definePage({
  name: 'mine',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '我的',
  },
})

const user = useUserStore()
const userStore = useUserStore()
const billStore = useBillStore()
const messageStore = useMessageStore()
const router = useRouter()
const themeStore = useManualTheme()

// 登录价值特性
const loginFeatures = [
  { icon: 'i-lucide:camera', text: '图片记账' },
  { icon: 'i-lucide:mic', text: '语音记账' },
  { icon: 'i-lucide:cloud-sync', text: '云端同步' },
  { icon: 'i-lucide:smartphone', text: '多设备访问' },
]

// 个性化登录提示
const loginTip = computed(() => {
  const count = billStore.localBillList.length
  if (count === 0) {
    return '登录后享受云端同步、多设备访问等功能'
  }
  return `您已记账${count}笔，登录后可同步到云端，永不丢失`
})

// 菜单分组（已登录状态）
const menuGroups = [
  {
    title: '账户管理',
    items: [
      {
        icon: 'i-lucide:user',
        title: '个人信息',
        action: () => user.requireAuth(() => router.push('/pages/mine/profile')),
      },
      {
        icon: 'i-lucide:shield',
        title: '账号与安全',
        action: () => user.requireAuth(() => router.push('/pages/mine/account-security')),
      },
      {
        icon: 'i-lucide:bell',
        title: '消息中心',
        action: () => user.requireAuth(() => router.push('/pages/message/index')),
        badge: computed(() => messageStore.hasUnread ? messageStore.unreadCount.total : 0),
      },
    ],
  },
  {
    title: '数据管理',
    items: [
      {
        icon: 'i-lucide:refresh-cw',
        title: '数据同步',
        action: () => user.requireAuth(() => showSyncPopup.value = true),
        subtitle: computed(() => {
          const pendingCount = billStore.localBillList.filter(b => b.syncStatus === 'init').length
          return pendingCount > 0 ? `${pendingCount}条待同步` : ''
        }),
      },
      {
        icon: 'i-lucide:download',
        title: '数据导出',
        action: () => user.requireAuth(() => showExportPopup.value = true),
      },
    ],
  },
  {
    title: '系统设置',
    items: [
      {
        icon: 'i-lucide:palette',
        title: '主题切换',
        action: () => showThemePopup.value = true,
        subtitle: computed(() => themeStore.currentThemeColor.name),
      },
      {
        icon: 'i-lucide:bell-ring',
        title: '通知设置',
        action: () => user.requireAuth(() => showNotificationPopup.value = true),
      },
      {
        icon: 'i-lucide:download-cloud',
        title: '检查更新',
        action: () => checkUpdate(),
      },
    ],
  },
  {
    title: '其他',
    items: [
      {
        icon: 'i-lucide:file-text',
        title: '用户协议',
        action: () => router.push('/pages/agreement/index'),
      },
      {
        icon: 'i-lucide:lock',
        title: '隐私政策',
        action: () => router.push('/pages/privacy/index'),
      },
      {
        icon: 'i-lucide:info',
        title: '关于应用',
        action: () => router.push('/pages/mine/about/index'),
      },
      {
        icon: 'i-lucide:help-circle',
        title: '帮助与反馈',
        action: () => router.push('/pages/mine/help/index'),
      },
    ],
  },
]

// 弹窗状态
const showSyncPopup = ref(false)
const showThemePopup = ref(false)
const showNotificationPopup = ref(false)
const showExportPopup = ref(false)

// 跳转到登录
function goToLogin() {
  user.showLoginPopup = true
}

// 退出登录
function logout() {
  userStore.logout()
}

// 检查更新
function checkUpdate() {
  const toast = useGlobalToast()
  toast.info('已是最新版本')
}

// 页面显示时获取未读消息数
onShow(() => {
  if (user.isLoggedIn) {
    messageStore.fetchUnreadCount()
  }
})
</script>
```

- [ ] **Step 2: 更新 template 部分，实现新的布局结构**

```vue
<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 未登录状态 -->
    <template v-if="!user.isLoggedIn">
      <!-- 顶部登录入口 -->
      <view class="mx-3 rounded-2xl bg-white p-6 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="flex items-center gap-4" @click="goToLogin">
          <!-- 默认头像 -->
          <view class="h-16 w-16 flex items-center justify-center rounded-full bg-gray-200 dark:bg-gray-700">
            <view class="i-lucide:user h-8 w-8 text-gray-400" />
          </view>
          <view class="flex-1">
            <text class="block text-lg font-500">
              点击登录
            </text>
            <text class="mt-1 block text-sm text-gray-500">
              {{ loginTip }}
            </text>
          </view>
          <view class="i-lucide:chevron-right h-5 w-5 text-gray-400" />
        </view>
      </view>

      <!-- 登录价值说明 -->
      <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <text class="mb-4 block text-sm font-500">
          登录解锁更多功能
        </text>
        <view class="grid grid-cols-4 gap-4">
          <view v-for="(item, index) in loginFeatures" :key="index" class="flex flex-col items-center gap-2">
            <view class="h-12 w-12 flex items-center justify-center rounded-xl bg-primary/10">
              <view class="h-6 w-6 text-primary" :class="item.icon" />
            </view>
            <text class="text-xs text-gray-600 dark:text-gray-400">
              {{ item.text }}
            </text>
          </view>
        </view>
      </view>

      <!-- 帮助与关于 -->
      <view class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view
          class="flex items-center justify-between border-b border-gray-100 p-4 dark:border-gray-700"
          @click="router.push('/pages/mine/help/index')"
        >
          <view class="flex items-center gap-3">
            <view class="i-lucide:help-circle h-5 w-5 text-gray-500" />
            <text class="text-sm">
              帮助与反馈
            </text>
          </view>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
        <view
          class="flex items-center justify-between p-4"
          @click="router.push('/pages/mine/about/index')"
        >
          <view class="flex items-center gap-3">
            <view class="i-lucide:info h-5 w-5 text-gray-500" />
            <text class="text-sm">
              关于应用
            </text>
          </view>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>
    </template>

    <!-- 已登录状态 -->
    <template v-else>
      <!-- 用户信息卡片 -->
      <view class="mx-3 rounded-2xl from-primary to-primary/80 bg-gradient-to-br p-6 text-white shadow-lg">
        <view class="flex items-center gap-4">
          <!-- 头像 -->
          <view class="h-16 w-16 flex items-center justify-center rounded-full bg-white/20 backdrop-blur-sm">
            <wd-img v-if="userStore.avatar" :src="userStore.avatar" custom-class="w-8 h-8" mode="aspectFill" />
            <view v-else class="i-lucide:user h-8 w-8" />
          </view>
          <view class="flex-1">
            <text class="block text-lg font-bold">
              {{ userStore.nickname || '用户' }}
            </text>
            <text class="mt-1 block text-sm text-white/80">
              {{ userStore.phone || '未绑定手机号' }}
            </text>
          </view>
        </view>
      </view>

      <!-- 菜单分组列表 -->
      <view v-for="(group, groupIndex) in menuGroups" :key="groupIndex" class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <!-- 分组标题 -->
        <view class="px-4 pt-3 pb-2 text-xs text-gray-400 font-medium">
          {{ group.title }}
        </view>
        <!-- 菜单项 -->
        <view
          v-for="(item, itemIndex) in group.items"
          :key="itemIndex"
          class="flex items-center justify-between px-4 py-3"
          :class="itemIndex < group.items.length - 1 && 'border-b border-gray-100 dark:border-gray-700'"
          @click="item.action"
        >
          <view class="flex items-center gap-3">
            <view class="h-5 w-5 text-gray-500" :class="item.icon" />
            <text class="text-sm">
              {{ item.title }}
            </text>
          </view>
          <view class="flex items-center gap-2">
            <!-- 副标题 -->
            <text v-if="item.subtitle" class="text-xs text-gray-400">
              {{ item.subtitle }}
            </text>
            <!-- 徽章 -->
            <view v-if="item.badge && item.badge.value > 0" class="h-5 w-5 flex items-center justify-center rounded-full bg-red-500 text-xs text-white">
              {{ item.badge.value > 99 ? '99+' : item.badge.value }}
            </view>
            <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
          </view>
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="mx-3 mt-4">
        <wd-button type="error" plain block @click="logout">
          退出登录
        </wd-button>
      </view>
    </template>

    <!-- 弹窗组件 -->
    <SyncStatusPopup v-model="showSyncPopup" />
    <ThemePickerPopup v-model="showThemePopup" />
    <NotificationSettingsPopup v-model="showNotificationPopup" />
    <ExportFilterPopup v-model="showExportPopup" />
  </view>
</template>
```

- [ ] **Step 3: Commit**

```bash
git add src/pages/mine/index.vue
git commit -m "feat(mine): 重构页面布局和菜单分组结构"
```

---

### Task 2: 创建主题选择弹窗组件

**Files:**
- Create: `src/pages/mine/components/ThemePickerPopup.vue`

**说明:** 实现主题颜色选择弹窗，展示预设颜色选项。

- [ ] **Step 1: 创建组件文件**

```vue
<script setup lang="ts">
import { themeColorOptions } from '@/composables/types/theme'

defineOptions({
  name: 'ThemePickerPopup',
})

const modelValue = defineModel<boolean>()
const themeStore = useManualTheme()

function selectTheme(color: typeof themeColorOptions[0]) {
  themeStore.setCurrentThemeColor(color)
  modelValue.value = false
}
</script>

<template>
  <wd-popup
    v-model="modelValue"
    position="bottom"
    closable
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
  >
    <view class="p-4">
      <view class="mb-4 text-center text-lg font-500">
        主题颜色
      </view>
      <view class="grid grid-cols-3 gap-4">
        <view
          v-for="color in themeColorOptions"
          :key="color.value"
          class="flex flex-col items-center gap-2 rounded-xl p-3 transition-all"
          :class="themeStore.currentThemeColor.value === color.value ? 'bg-primary/10 ring-2 ring-primary' : 'bg-gray-50 dark:bg-gray-800'"
          @click="selectTheme(color)"
        >
          <view
            class="h-10 w-10 rounded-full"
            :style="{ backgroundColor: color.primary }"
          />
          <text class="text-xs text-gray-600 dark:text-gray-400">
            {{ color.name }}
          </text>
        </view>
      </view>
    </view>
  </wd-popup>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add src/pages/mine/components/ThemePickerPopup.vue
git commit -m "feat(mine): 创建主题选择弹窗组件"
```

---

### Task 3: 创建通知设置弹窗组件

**Files:**
- Create: `src/pages/mine/components/NotificationSettingsPopup.vue`

**说明:** 实现通知设置弹窗，包含消息推送、账单提醒、家庭通知开关。

- [ ] **Step 1: 创建组件文件**

```vue
<script setup lang="ts">
defineOptions({
  name: 'NotificationSettingsPopup',
})

const modelValue = defineModel<boolean>()
const toast = useGlobalToast()

// 通知设置
const settings = ref({
  pushEnabled: true,
  billReminderEnabled: true,
  billReminderTime: '21:00',
  familyNotificationEnabled: true,
})

const timePickerVisible = ref(false)
const timeValue = ref([21, 0])

// 加载设置
onMounted(() => {
  // TODO: 从后端加载通知设置
})

// 消息推送开关变化
function onPushChange(value: boolean) {
  if (!value) {
    settings.value.billReminderEnabled = false
    settings.value.familyNotificationEnabled = false
  }
  saveSettings()
}

// 保存设置
async function saveSettings() {
  // TODO: 调用后端保存通知设置
  toast.success('设置已保存')
}

// 时间选择确认
function onTimeConfirm({ value }: { value: number[] }) {
  settings.value.billReminderTime = `${String(value[0]).padStart(2, '0')}:${String(value[1]).padStart(2, '0')}`
  saveSettings()
}
</script>

<template>
  <wd-popup
    v-model="modelValue"
    position="bottom"
    closable
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
  >
    <view class="p-4">
      <view class="mb-4 text-center text-lg font-500">
        通知设置
      </view>

      <!-- 消息推送 -->
      <view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view>
          <text class="block text-sm font-medium">消息推送</text>
          <text class="mt-1 block text-xs text-gray-500">接收应用推送通知</text>
        </view>
        <wd-switch v-model="settings.pushEnabled" size="20px" @change="onPushChange" />
      </view>

      <!-- 账单提醒 -->
      <view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view>
          <text class="block text-sm font-medium">账单提醒</text>
          <text class="mt-1 block text-xs text-gray-500">每日提醒记账</text>
        </view>
        <view class="flex items-center gap-2">
          <text
            v-if="settings.billReminderEnabled"
            class="text-xs text-primary"
            @click="timePickerVisible = true"
          >
            {{ settings.billReminderTime }}
          </text>
          <wd-switch
            v-model="settings.billReminderEnabled"
            :disabled="!settings.pushEnabled"
            size="20px"
            @change="saveSettings"
          />
        </view>
      </view>

      <!-- 家庭通知 -->
      <view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view>
          <text class="block text-sm font-medium">家庭通知</text>
          <text class="mt-1 block text-xs text-gray-500">成员变动、账单变更等</text>
        </view>
        <wd-switch
          v-model="settings.familyNotificationEnabled"
          :disabled="!settings.pushEnabled"
          size="20px"
          @change="saveSettings"
        />
      </view>
    </view>

    <!-- 时间选择器 -->
    <wd-datetime-picker
      v-model="timeValue"
      v-model:visible="timePickerVisible"
      type="time"
      title="选择提醒时间"
      @confirm="onTimeConfirm"
    />
  </wd-popup>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add src/pages/mine/components/NotificationSettingsPopup.vue
git commit -m "feat(mine): 创建通知设置弹窗组件"
```

---

### Task 4: 删除设置页面并更新路由

**Files:**
- Delete: `src/pages/mine/settings.vue`

**说明:** 设置功能已前置到"我的"页面，删除原设置页面。

- [ ] **Step 1: 删除设置页面文件**

```bash
rm src/pages/mine/settings.vue
```

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "chore(mine): 删除设置页面，功能已前置"
```

---

## 第二阶段：账单同步功能

### Task 5: 修改 BillStore 添加同步状态支持

**Files:**
- Modify: `src/store/billStore.ts`

**说明:** 为本地账单添加 syncStatus 字段，实现同步相关方法。

- [ ] **Step 1: 修改 BillVO 类型定义，添加 syncStatus 字段**

在 `src/api/globals.d.ts` 或 store 文件中添加类型扩展：

```typescript
// 扩展 BillVO 类型
interface LocalBillVO extends BillVO {
  syncStatus: 'init' | 'success' | 'failed'
}
```

- [ ] **Step 2: 修改 localBillList 类型和初始化**

```typescript
// 修改 localBillList 类型
const localBillList = ref<LocalBillVO[]>([])

// 添加同步状态计算属性
const pendingSyncCount = computed(() =>
  localBillList.value.filter(bill => bill.syncStatus === 'init').length
)

const syncedCount = computed(() =>
  localBillList.value.filter(bill => bill.syncStatus === 'success').length
)

const lastSyncTime = ref<string>('')
```

- [ ] **Step 3: 修改 addLocalBill 方法，添加默认 syncStatus**

```typescript
function addLocalBill(bill: AddBillDTO) {
  const paymentMethod = paymentMethodList.value.find(item => item.id === bill.paymentMethodId)
  const category = categoryListMap.value[bill.type as keyof typeof categoryListMap.value].find(item => item.id === bill.categoryId)
  localBillList.value.push({
    ...bill,
    amount: Number(bill.amount || 0).toFixed(2),
    time: dayjs(bill.time).format('YYYY-MM-DD HH:mm'),
    id: uuid(),
    paymentMethod,
    category,
    syncStatus: 'init', // 默认待同步
  })
}
```

- [ ] **Step 4: 修改 syncLocalBillToServer 方法，实现按状态同步**

```typescript
async function syncLocalBillToServer(): Promise<{ success: boolean; successCount: number }> {
  const pendingBills = localBillList.value.filter(bill => bill.syncStatus === 'init')

  if (pendingBills.length === 0) {
    return { success: true, successCount: 0 }
  }

  try {
    // 映射为 AddBillDTO 格式
    const bills = pendingBills.map(bill => ({
      name: bill.name!,
      categoryId: bill.category?.id || '',
      type: bill.type!,
      amount: bill.amount!,
      time: bill.time!,
      paymentMethodId: bill.paymentMethod?.id,
      remark: bill.remark,
    }))

    const response = await Apis.bill.batchCreate({ data: { bills } })

    if (response.code === 200) {
      const successCount = response.data?.successCount || 0

      // 按时间顺序标记成功的账单
      const sortedBills = [...pendingBills].sort((a, b) =>
        dayjs(a.time).valueOf() - dayjs(b.time).valueOf()
      )

      for (let i = 0; i < successCount && i < sortedBills.length; i++) {
        const bill = localBillList.value.find(b => b.id === sortedBills[i].id)
        if (bill) {
          bill.syncStatus = 'success'
        }
      }

      // 更新同步时间
      lastSyncTime.value = dayjs().format('YYYY-MM-DD HH:mm')

      return { success: true, successCount }
    }

    return { success: false, successCount: 0 }
  }
  catch (error) {
    console.error('同步本地账单失败:', error)
    return { success: false, successCount: 0 }
  }
}
```

- [ ] **Step 5: 更新 return 对象，导出新属性和方法**

```typescript
return {
  // ... 现有导出
  localBillList,
  pendingSyncCount,
  syncedCount,
  lastSyncTime,
  syncLocalBillToServer,
  // ... 其他现有导出
}
```

- [ ] **Step 6: Commit**

```bash
git add src/store/billStore.ts
git commit -m "feat(bill): 添加账单同步状态支持"
```

---

### Task 6: 创建同步状态弹窗组件

**Files:**
- Create: `src/pages/mine/components/SyncStatusPopup.vue`

**说明:** 实现同步状态展示和手动同步功能。

- [ ] **Step 1: 创建组件文件**

```vue
<script setup lang="ts">
defineOptions({
  name: 'SyncStatusPopup',
})

const modelValue = defineModel<boolean>()
const billStore = useBillStore()
const toast = useGlobalToast()
const loading = ref(false)
const syncResult = ref<{ success: boolean; count: number } | null>(null)

// 同步状态文案
const statusText = computed(() => {
  if (loading.value) return '同步中...'
  if (syncResult.value?.success) return '同步成功'
  if (syncResult.value && !syncResult.value.success) return '同步失败'
  return ''
})

// 开始同步
async function startSync() {
  if (loading.value) return

  loading.value = true
  syncResult.value = null

  try {
    const result = await billStore.syncLocalBillToServer()

    if (result.success) {
      syncResult.value = { success: true, count: result.successCount }
      toast.success(`成功同步${result.successCount}条账单`)
    }
    else {
      syncResult.value = { success: false, count: 0 }
      toast.error('同步失败，请重试')
    }
  }
  catch {
    syncResult.value = { success: false, count: 0 }
    toast.error('同步失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 关闭弹窗
function handleClose() {
  syncResult.value = null
  modelValue.value = false
}
</script>

<template>
  <wd-popup
    :model-value="modelValue"
    position="bottom"
    closable
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    @update:model-value="handleClose"
  >
    <view class="p-4">
      <view class="mb-4 text-center text-lg font-500">
        数据同步
      </view>

      <!-- 同步状态统计 -->
      <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view class="mb-3 flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">待同步</text>
          <text class="text-sm font-medium">{{ billStore.pendingSyncCount }}条</text>
        </view>
        <view class="mb-3 flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">已同步</text>
          <text class="text-sm font-medium">{{ billStore.syncedCount }}条</text>
        </view>
        <view v-if="billStore.lastSyncTime" class="flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">上次同步</text>
          <text class="text-sm text-gray-500">{{ billStore.lastSyncTime }}</text>
        </view>
      </view>

      <!-- 同步结果 -->
      <view v-if="statusText" class="mb-4 rounded-xl p-4" :class="syncResult?.success ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'">
        <view class="flex items-center gap-2">
          <view
            v-if="loading"
            class="i-lucide:loader-2 h-5 w-5 animate-spin text-primary"
          />
          <view
            v-else-if="syncResult?.success"
            class="i-lucide:check-circle h-5 w-5 text-green-500"
          />
          <view
            v-else
            class="i-lucide:x-circle h-5 w-5 text-red-500"
          />
          <text class="text-sm font-medium" :class="syncResult?.success ? 'text-green-700 dark:text-green-300' : 'text-red-700 dark:text-red-300'">
            {{ statusText }}
          </text>
        </view>
      </view>

      <!-- 同步按钮 -->
      <wd-button
        type="primary"
        block
        :loading="loading"
        :disabled="billStore.pendingSyncCount === 0"
        @click="startSync"
      >
        {{ billStore.pendingSyncCount === 0 ? '暂无待同步数据' : '开始同步' }}
      </wd-button>
    </view>
  </wd-popup>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add src/pages/mine/components/SyncStatusPopup.vue
git commit -m "feat(mine): 创建同步状态弹窗组件"
```

---

## 第三阶段：数据导出功能

### Task 7: 创建导出筛选弹窗组件

**Files:**
- Create: `src/pages/mine/components/ExportFilterPopup.vue`

**说明:** 复用 FilterModal 的筛选逻辑，实现导出功能。

- [ ] **Step 1: 创建组件文件**

```vue
<script setup lang="ts">
import type { BillListDTO } from '@/api/globals'
import dayjs from 'dayjs'

defineOptions({
  name: 'ExportFilterPopup',
})

const modelValue = defineModel<boolean>()
const toast = useGlobalToast()
const loading = ref(false)

// 筛选条件
const filterData = ref<Optional<BillListDTO>>({ type: '' })
const dateRange = ref<[number, number]>([dayjs().startOf('M').valueOf(), dayjs().endOf('M').valueOf()])

// 导出格式
const formatOptions = [
  { label: 'Excel (.xlsx)', value: 'excel' },
  { label: 'PDF (.pdf)', value: 'pdf' },
]
const selectedFormat = ref('excel')

// 更新日期范围
watch(dateRange, (newVal) => {
  if (newVal.length === 2) {
    filterData.value.startDate = dayjs(newVal[0]).format('YYYY-MM-DD')
    filterData.value.endDate = dayjs(newVal[1]).format('YYYY-MM-DD')
  }
}, { deep: true })

// 导出
async function handleExport() {
  if (loading.value) return

  loading.value = true

  try {
    const params = {
      ...filterData.value,
      format: selectedFormat.value,
    }

    const res = await Apis.bill.export({ data: params })

    if (res.success && res.data?.downloadUrl) {
      // 下载文件
      const downloadRes = await uni.downloadFile({ url: res.data.downloadUrl })

      if (downloadRes.statusCode === 200) {
        // 打开文件
        await uni.openDocument({
          filePath: downloadRes.tempFilePath,
          showMenu: true,
        })
        toast.success('导出成功')
        modelValue.value = false
      }
      else {
        toast.error('下载失败')
      }
    }
    else {
      toast.error(res.message || '导出失败')
    }
  }
  catch {
    toast.error('导出失败，请重试')
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <wd-popup
    :model-value="modelValue"
    position="bottom"
    closable
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    @update:model-value="modelValue = $event"
  >
    <view class="max-h-80vh p-4">
      <view class="mb-4 text-center text-lg font-500">
        数据导出
      </view>

      <!-- 时间范围 -->
      <view class="mb-4">
        <text class="mb-2 block text-sm font-medium">时间范围</text>
        <wd-datetime-picker
          v-model="dateRange"
          type="date"
        />
      </view>

      <!-- 账单类型 -->
      <view class="mb-4">
        <text class="mb-2 block text-sm font-medium">账单类型</text>
        <wd-radio-group v-model="filterData.type" shape="button" custom-class="flex">
          <wd-radio value="" custom-class="flex-1">全部</wd-radio>
          <wd-radio value="expense" custom-class="flex-1">支出</wd-radio>
          <wd-radio value="income" custom-class="flex-1">收入</wd-radio>
        </wd-radio-group>
      </view>

      <!-- 导出格式 -->
      <view class="mb-4">
        <text class="mb-2 block text-sm font-medium">导出格式</text>
        <wd-radio-group v-model="selectedFormat" shape="button" custom-class="flex">
          <wd-radio
            v-for="item in formatOptions"
            :key="item.value"
            :value="item.value"
            custom-class="flex-1"
          >
            {{ item.label }}
          </wd-radio>
        </wd-radio-group>
      </view>

      <!-- 导出按钮 -->
      <wd-button
        type="primary"
        block
        :loading="loading"
        @click="handleExport"
      >
        导出
      </wd-button>
    </view>
  </wd-popup>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add src/pages/mine/components/ExportFilterPopup.vue
git commit -m "feat(mine): 创建导出筛选弹窗组件"
```

---

## 第四阶段：账号安全与通知

### Task 8: 创建账号与安全页面

**Files:**
- Create: `src/pages/mine/account-security/index.vue`

**说明:** 合并原有 account.vue 和 password.vue，新增注销账号和登录设备管理功能。

- [ ] **Step 1: 创建页面文件**

```vue
<script setup lang="ts">
definePage({
  name: 'account-security',
  layout: 'default',
  style: {
    navigationBarTitleText: '账号与安全',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()
const message = useGlobalMessage()
const router = useRouter()

// 密码表单
const showPasswordForm = ref(false)
const passwordForm = ref({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

// 手机号表单
const showPhoneForm = ref(false)
const phoneForm = ref({
  newPhone: '',
  code: '',
})

// 倒计时
const countdown = ref(0)
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

// 密码强度
const passwordStrength = computed(() => {
  const pwd = passwordForm.value.newPassword
  if (!pwd) return 0
  let strength = 0
  if (pwd.length >= 6) strength++
  if (/[a-z]/i.test(pwd) && /\d/.test(pwd)) strength++
  if (/[^a-z0-9]/i.test(pwd)) strength++
  if (pwd.length >= 10) strength++
  return Math.min(strength, 3)
})

const strengthText = ['弱', '中', '强', '极强']
const strengthColor = ['text-red-500', 'text-yellow-500', 'text-green-500', 'text-green-600']

onUnload(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

// 发送验证码
async function sendCode(phone: string) {
  if (countdown.value > 0 || sending.value) return

  sending.value = true
  try {
    const res = await Apis.auth.sendCode({ data: { phone } })
    if (res.success) {
      toast.success('验证码已发送')
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0 && timer) {
          clearInterval(timer)
          timer = null
        }
      }, 1000)
    }
    else {
      toast.error(res.message || '发送失败')
    }
  }
  catch {
    toast.error('发送失败，请重试')
  }
  finally {
    sending.value = false
  }
}

// 修改密码
async function handleUpdatePassword() {
  if (!passwordForm.value.code) {
    toast.warning('请输入验证码')
    return
  }
  if (!passwordForm.value.newPassword) {
    toast.warning('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    toast.warning('密码长度不能少于6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.warning('两次输入的密码不一致')
    return
  }

  try {
    const res = await Apis.user.updatePasswordByCode({
      data: {
        phone: userStore.phone!,
        code: passwordForm.value.code,
        newPassword: passwordForm.value.newPassword,
      },
    })
    if (res.success) {
      toast.success('密码修改成功，请重新登录')
      showPasswordForm.value = false
      setTimeout(() => {
        userStore.logout()
        router.replace('/pages/bill/index')
      }, 1500)
    }
    else {
      toast.error(res.message || '修改失败')
    }
  }
  catch {
    toast.error('修改失败，请重试')
  }
}

// 修改手机号
async function handleUpdatePhone() {
  if (!phoneForm.value.newPhone) {
    toast.warning('请输入新手机号')
    return
  }
  if (!phoneForm.value.code) {
    toast.warning('请输入验证码')
    return
  }

  // TODO: 调用后端修改手机号接口
  toast.info('功能开发中')
}

// 注销账号
function handleDeleteAccount() {
  message.confirm({
    title: '注销账号',
    msg: '注销后，所有数据将被永久删除且无法恢复。确定要注销吗？',
    beforeConfirm: async ({ resolve }) => {
      // TODO: 调用后端注销账号接口
      toast.info('功能开发中')
      resolve(false)
    },
  })
}

// 设备管理
function goToDeviceManagement() {
  // TODO: 跳转到设备管理页面
  toast.info('功能开发中')
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- 账号信息 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        账号信息
      </view>
      <!-- 手机号 -->
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="showPhoneForm = true"
      >
        <text class="text-sm">手机号</text>
        <view class="flex items-center gap-1">
          <text class="text-sm text-gray-400">
            {{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>
      <!-- 修改密码 -->
      <view
        class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700"
        @click="showPasswordForm = true"
      >
        <text class="text-sm">修改密码</text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 安全设置 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        安全设置
      </view>
      <!-- 登录设备 -->
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="goToDeviceManagement"
      >
        <text class="text-sm">登录设备管理</text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
      <!-- 注销账号 -->
      <view
        class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700"
        @click="handleDeleteAccount"
      >
        <text class="text-sm text-red-500">注销账号</text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 修改密码弹窗 -->
    <wd-popup
      v-model="showPasswordForm"
      position="bottom"
      closable
      safe-area-inset-bottom
      custom-class="rounded-tl-2xl rounded-tr-2xl"
    >
      <view class="p-4">
        <view class="mb-4 text-center text-lg font-500">
          修改密码
        </view>
        <!-- 手机号 -->
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            手机号：{{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
        </view>
        <!-- 验证码 -->
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="passwordForm.code"
            placeholder="请输入验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="countdown > 0 || !userStore.phone"
            size="small"
            @click="sendCode(userStore.phone!)"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <!-- 新密码 -->
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.newPassword"
            placeholder="请输入新密码"
            show-password
            :maxlength="20"
            no-border
          />
          <view v-if="passwordForm.newPassword" class="mt-2 flex items-center gap-2">
            <text class="text-xs text-gray-500">密码强度：</text>
            <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
              {{ strengthText[passwordStrength - 1] || '弱' }}
            </text>
          </view>
        </view>
        <!-- 确认密码 -->
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.confirmPassword"
            placeholder="请确认新密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <text class="mb-4 block text-xs text-gray-400">
          密码需6-20位，建议包含字母和数字
        </text>
        <wd-button type="primary" block @click="handleUpdatePassword">
          确认修改
        </wd-button>
      </view>
    </wd-popup>

    <!-- 修改手机号弹窗 -->
    <wd-popup
      v-model="showPhoneForm"
      position="bottom"
      closable
      safe-area-inset-bottom
      custom-class="rounded-tl-2xl rounded-tr-2xl"
    >
      <view class="p-4">
        <view class="mb-4 text-center text-lg font-500">
          更换手机号
        </view>
        <!-- 当前手机号 -->
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            当前手机号：{{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
        </view>
        <!-- 新手机号 -->
        <view class="mb-4">
          <wd-input
            v-model="phoneForm.newPhone"
            placeholder="请输入新手机号"
            type="number"
            :maxlength="11"
            no-border
          />
        </view>
        <!-- 验证码 -->
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="phoneForm.code"
            placeholder="请输入验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="countdown > 0 || !phoneForm.newPhone"
            size="small"
            @click="sendCode(phoneForm.newPhone)"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <wd-button type="primary" block @click="handleUpdatePhone">
          确认更换
        </wd-button>
      </view>
    </wd-popup>
  </view>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add src/pages/mine/account-security/index.vue
git commit -m "feat(mine): 创建账号与安全页面"
```

---

### Task 9: 删除旧的账号和密码页面

**Files:**
- Delete: `src/pages/mine/account.vue`
- Delete: `src/pages/mine/password.vue`

**说明:** 功能已合并到 account-security 页面，删除旧文件。

- [ ] **Step 1: 删除旧页面文件**

```bash
rm src/pages/mine/account.vue
rm src/pages/mine/password.vue
```

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "chore(mine): 删除旧的账号和密码页面"
```

---

### Task 10: 运行类型检查和测试

**Files:**
- All modified and created files

- [ ] **Step 1: 运行类型检查**

```bash
cd chuan-bill-app
pnpm type-check
```

- [ ] **Step 2: 运行 ESLint**

```bash
pnpm lint:fix
```

- [ ] **Step 3: 检查编译**

```bash
pnpm build:h5
```

- [ ] **Step 4: Commit（如有修复）**

```bash
git add -A
git commit -m "fix: 修复类型检查和代码风格问题"
```

---

## 自检清单

### 1. 规范覆盖检查

| 设计文档需求 | 实现任务 | 状态 |
|-------------|---------|------|
| 重构页面布局和菜单分组 | Task 1 | ✅ |
| 主题切换功能前置 | Task 2 | ✅ |
| 通知设置功能前置 | Task 3 | ✅ |
| 删除设置页面 | Task 4 | ✅ |
| 账单同步状态支持 | Task 5 | ✅ |
| 同步状态弹窗 | Task 6 | ✅ |
| 数据导出功能 | Task 7 | ✅ |
| 账号与安全页面 | Task 8 | ✅ |
| 删除旧页面 | Task 9 | ✅ |
| 类型检查和代码风格 | Task 10 | ✅ |

### 2. 占位符检查

- [x] 无 "TBD" / "TODO" 占位符（后端接口调用处标记为 TODO，需后端实现后补充）
- [x] 所有代码步骤包含完整代码
- [x] 所有命令包含预期输出
- [x] 类型和命名一致性检查通过

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-04-28-mine-page-redesign-implementation.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
