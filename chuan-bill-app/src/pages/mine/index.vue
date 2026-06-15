<script setup lang="ts">
import { EVENTS } from '@/constant/events'
import { eventBus } from '@/utils/eventBus'
import ExportFilterPopup from './components/ExportFilterPopup.vue'
import NotificationSettingsPopup from './components/NotificationSettingsPopup.vue'
import SyncStatusPopup from './components/SyncStatusPopup.vue'
import ThemePickerPopup from './components/ThemePickerPopup.vue'

definePage({
  name: 'mine',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '我的',
  },
})

const user = useUserStore()
const billStore = useBillStore()
const messageStore = useMessageStore()
const router = useRouter()
const toast = useGlobalToast()
const message = useGlobalMessage()
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
    return { prefix: '登录后享受云端同步、多设备访问等功能', count: null, suffix: '' }
  }
  return { prefix: '您已记账', count, suffix: '笔，登录后可同步到云端，永不丢失' }
})

// 弹窗状态
const showSyncPopup = ref(false)
const showThemePopup = ref(false)
const showNotificationPopup = ref(false)
const showExportPopup = ref(false)
const showDarkModePopup = ref(false)

// 菜单项类型
interface MenuItem {
  icon: string
  title: string
  action: () => void
  badge?: ComputedRef<number>
  subtitle?: ComputedRef<string>
  needLogin?: boolean
}

// 菜单动态数据
const unreadBadge = computed(() => messageStore.hasUnread ? (messageStore.unreadCount.total || 0) : 0)
const syncSubtitle = computed(() => {
  const pendingCount = billStore.localBillList.filter(b => b.syncStatus === 'init').length
  return pendingCount > 0 ? `${pendingCount}条待同步` : ''
})
const themeSubtitle = computed(() => themeStore.currentThemeColor.value.name)

// 暗黑模式映射
type DarkModeValue = 'system' | 'light' | 'dark'
const darkModeLabelMap: Record<DarkModeValue, string> = {
  system: '跟随系统',
  light: '日间模式',
  dark: '夜间模式',
}
const darkModeLabel = computed(() => {
  const mode: DarkModeValue = themeStore.followSystem.value ? 'system' : (themeStore.isDark.value ? 'dark' : 'light')
  return darkModeLabelMap[mode]
})

// 菜单分组
const menuGroups: { title: string, items: MenuItem[], needLogin?: boolean }[] = [
  {
    title: '账户管理',
    needLogin: true,
    items: [
      {
        icon: 'i-lucide:user',
        title: '个人信息',
        action: () => user.requireAuth(() => router.push('/pages/mine/profile')),
      },
      {
        icon: 'i-lucide:shield',
        title: '账号与安全',
        action: () => user.requireAuth(() => router.push('/pages/mine/account')),
      },
      {
        icon: 'i-lucide:bell',
        title: '消息中心',
        action: () => user.requireAuth(() => router.push('/pages/message/index')),
        badge: unreadBadge,
      },
    ],
  },
  {
    title: '系统设置',
    items: [
      {
        icon: 'i-lucide:moon',
        title: '暗黑模式',
        action: () => showDarkModePopup.value = true,
        subtitle: darkModeLabel,
      },
      {
        icon: 'i-lucide:palette',
        title: '主题切换',
        action: () => showThemePopup.value = true,
        subtitle: themeSubtitle,
      },
      {
        icon: 'i-lucide:bell-ring',
        title: '通知设置',
        needLogin: true,
        action: () => user.requireAuth(() => showNotificationPopup.value = true),
      },
    ],
  },
  {
    title: '数据管理',
    needLogin: true,
    items: [
      {
        icon: 'i-lucide:refresh-cw',
        title: '账单同步',
        action: () => user.requireAuth(() => showSyncPopup.value = true),
        subtitle: syncSubtitle,
      },
      {
        icon: 'i-lucide:share',
        title: '账单导出',
        action: () => user.requireAuth(() => showExportPopup.value = true),
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
        action: () => {
          // #ifdef MP-WEIXIN
          wx.openPrivacyContract({})
          // #endif
          // #ifndef MP-WEIXIN
          router.push('/pages/privacy/index')
          // #endif
        },
      },
      {
        icon: 'i-lucide:help-circle',
        title: '帮助与反馈',
        action: () => router.push('/pages/mine/help'),
      },
      // #ifdef APP-PLUS
      {
        icon: 'i-lucide:download-cloud',
        title: '检查更新',
        action: () => checkUpdate(),
      },
      // #endif
      {
        icon: 'i-lucide:info',
        title: '关于应用',
        action: () => router.push('/pages/mine/about'),
      },
    ],
  },
]

// 跳转到登录
function goToLogin() {
  user.showLoginPopup = true
}

// 退出登录
function logout() {
  message.confirm({
    title: '提示',
    msg: '是否退出登录？',
    confirmButtonProps: {
      type: 'error',
    },
    success: (res) => {
      if (res.action === 'confirm') {
        user.logout()
      }
    },
  })
}

// 检查更新
function checkUpdate() {
  toast.info('已是最新版本')
}

// 暗黑模式选项
const darkModeActions = (Object.entries(darkModeLabelMap) as [DarkModeValue, string][]).map(([value, label]) => ({
  name: label,
  value,
}))

// 选择暗黑模式
function selectDarkMode({ item }: { item: { name: string, value: DarkModeValue } }) {
  if (item.value === 'system') {
    themeStore.setFollowSystem(true)
  }
  else {
    themeStore.toggleTheme(item.value)
  }
}

// 首次加载时获取未读消息数
onLoad(() => {
  if (user.isLoggedIn) {
    messageStore.fetchUnreadCount()
  }
})

// 监听用户和账单数据变化事件
function handleDataUpdated() {
  if (user.isLoggedIn) {
    messageStore.fetchUnreadCount()
  }
}

onMounted(() => {
  eventBus.on(EVENTS.USER.UPDATED, handleDataUpdated)
  eventBus.on(EVENTS.BILL.UPDATED, handleDataUpdated)
})

onUnmounted(() => {
  eventBus.off(EVENTS.USER.UPDATED, handleDataUpdated)
  eventBus.off(EVENTS.BILL.UPDATED, handleDataUpdated)
})
</script>

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
              {{ loginTip.prefix }}
              <text v-if="loginTip.count !== null" class="text-orange-500 font-600">
                {{ loginTip.count }}
              </text>
              {{ loginTip.suffix }}
            </text>
          </view>
          <view class="i-lucide:chevron-right h-4 w-4" />
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
    </template>
    <!-- 用户信息卡片 -->
    <view v-if="user.isLoggedIn" class="mx-3 rounded-2xl from-primary to-primary/80 bg-gradient-to-br p-6 text-white shadow-lg">
      <view class="flex items-center gap-4">
        <!-- 头像 -->
        <wd-img v-if="user.avatar" :src="user.avatar" custom-class="w-16 h-16 rounded-full! overflow-hidden" mode="aspectFill" />
        <view v-else class="i-lucide:user h-12 w-12" />
        <view class="flex-1">
          <text class="block text-lg font-bold">
            {{ user.nickname || '用户' }}
          </text>
          <text class="mt-1 block text-sm text-white/80">
            {{ user.phone || '未绑定手机号' }}
          </text>
        </view>
      </view>
    </view>

    <!-- 菜单分组列表 -->
    <view v-for="(group, groupIndex) in menuGroups" :key="groupIndex" class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <template v-if="!group.needLogin || user.isLoggedIn">
        <!-- 分组标题 -->
        <view class="px-4 pb-2 pt-3 text-xs text-gray-400 font-medium">
          {{ group.title }}
        </view>
        <!-- 菜单项 -->
        <view
          v-for="(item, itemIndex) in group.items"
          v-show="user.isLoggedIn || !item.needLogin"
          :key="itemIndex"
          class="flex items-center justify-between px-4 py-3"
          :class="itemIndex < group.items.length - 1 && 'border-b border-gray-100 dark:border-gray-700'"
          @click="item.action"
        >
          <view class="flex items-center gap-3">
            <view class="h-4 w-4" :class="item.icon" />
            <text class="text-sm">
              {{ item.title }}
            </text>
          </view>
          <view class="flex items-center gap-2">
            <!-- 副标题 -->
            <text v-if="item.subtitle" class="text-xs text-gray-400">
              {{ item.subtitle.value }}
            </text>
            <!-- 徽章 -->
            <view v-if="item.badge && item.badge.value > 0" class="h-4 w-4 flex items-center justify-center rounded-full bg-red-500 text-xs text-white">
              {{ item.badge.value > 99 ? '99+' : item.badge.value }}
            </view>
            <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
          </view>
        </view>
      </template>
    </view>

    <!-- 退出登录 -->
    <view v-if="user.isLoggedIn" class="mx-3 mt-4 -mb-10">
      <wd-button type="error" plain block @click="logout">
        退出登录
      </wd-button>
    </view>

    <!-- 弹窗组件 -->
    <SyncStatusPopup v-if="user.isLoggedIn" v-model="showSyncPopup" />
    <ThemePickerPopup v-model="showThemePopup" />
    <NotificationSettingsPopup v-if="user.isLoggedIn" v-model="showNotificationPopup" />
    <ExportFilterPopup v-if="user.isLoggedIn" v-model="showExportPopup" />
    <wd-action-sheet
      v-model="showDarkModePopup"
      :actions="darkModeActions"
      :z-index="999"
      cancel-text="取消"
      @select="selectDarkMode"
    />

    <!-- 底部间距（给 tabbar 留空间） -->
    <view class="h-10" />
  </view>
</template>
