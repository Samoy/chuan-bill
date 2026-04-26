<script setup lang="ts">
definePage({
  name: 'settings',
  layout: 'default',
  style: {
    navigationBarTitleText: '设置',
  },
})

const userStore = useUserStore()
const themeStore = useThemeStore()
const toast = useGlobalToast()
const message = useGlobalMessage()
const router = useRouter()

// 主题选项
const themeOptions = [
  { label: '跟随系统', value: 'auto' },
  { label: '浅色模式', value: 'light' },
  { label: '深色模式', value: 'dark' },
]

const themePickerVisible = ref(false)
const currentTheme = computed(() => {
  return themeOptions.find(t => t.value === themeStore.theme)?.label || '跟随系统'
})

// 通知设置
const pushEnabled = ref(true)
const billReminderEnabled = ref(true)

// 缓存大小
const cacheSize = ref('0KB')

onShow(() => {
  calcCacheSize()
})

// 计算缓存大小
function calcCacheSize() {
  try {
    const info = uni.getStorageInfoSync()
    let size = 0
    for (const key of info.keys) {
      const value = uni.getStorageSync(key)
      size += JSON.stringify(value).length
    }
    cacheSize.value = formatSize(size)
  }
  catch {
    cacheSize.value = '未知'
  }
}

function formatSize(bytes: number): string {
  if (bytes === 0)
    return '0KB'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / k ** i).toFixed(2)}${sizes[i]}`
}

// 清除缓存
function clearCache() {
  message.confirm({
    title: '清除缓存',
    msg: '确定要清除所有本地缓存数据吗？账单数据不会被清除。',
    beforeConfirm: async ({ resolve }) => {
      try {
        // 保留用户登录信息
        const token = uni.getStorageSync('user-token')
        const userInfo = uni.getStorageSync('user-info')

        uni.clearStorageSync()

        // 恢复登录信息
        if (token)
          uni.setStorageSync('user-token', token)
        if (userInfo)
          uni.setStorageSync('user-info', userInfo)

        calcCacheSize()
        resolve(true)
        toast.success('缓存已清除')
      }
      catch {
        toast.error('清除失败')
        resolve(false)
      }
    },
  })
}

// 退出登录
function handleLogout() {
  message.confirm({
    title: '退出登录',
    msg: '确定要退出登录吗？',
    beforeConfirm: async ({ resolve }) => {
      userStore.logout()
      resolve(true)
      toast.success('已退出登录')
      router.replace('/pages/bill/index')
    },
  })
}

// 检查更新
function checkUpdate() {
  toast.info('已是最新版本')
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- 外观 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        外观
      </view>
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="themePickerVisible = true"
      >
        <text class="text-sm">
          主题模式
        </text>
        <view class="flex items-center gap-1">
          <text class="text-sm text-gray-400">
            {{ currentTheme }}
          </text>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>
    </view>

    <!-- 通知 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        通知
      </view>
      <view class="flex items-center justify-between px-4 pb-4">
        <text class="text-sm">
          消息推送
        </text>
        <wd-switch v-model="pushEnabled" size="20px" />
      </view>
      <view class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700">
        <text class="text-sm">
          账单提醒
        </text>
        <wd-switch v-model="billReminderEnabled" size="20px" />
      </view>
    </view>

    <!-- 关于 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        关于
      </view>
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="checkUpdate"
      >
        <text class="text-sm">
          检查更新
        </text>
        <view class="flex items-center gap-1">
          <text class="text-sm text-gray-400">
            1.0.0
          </text>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>
      <view
        class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700"
        @click="router.push('/pages/agreement/index')"
      >
        <text class="text-sm">
          用户协议
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
      <view
        class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700"
        @click="router.push('/pages/privacy/index')"
      >
        <text class="text-sm">
          隐私政策
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 存储 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        存储
      </view>
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="clearCache"
      >
        <text class="text-sm">
          清除缓存
        </text>
        <view class="flex items-center gap-1">
          <text class="text-sm text-gray-400">
            {{ cacheSize }}
          </text>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>
    </view>

    <!-- 账号 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        账号
      </view>
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="userStore.requireAuth(() => router.push('/pages/mine/password/index'))"
      >
        <text class="text-sm">
          修改密码
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 退出登录 -->
    <view
      v-if="userStore.isLoggedIn"
      class="mt-4 rounded-2xl bg-white p-4 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]"
      @click="handleLogout"
    >
      <text class="text-error text-sm">
        退出登录
      </text>
    </view>

    <!-- 主题选择器 -->
    <wd-picker
      v-model="themeStore.theme"
      v-model:visible="themePickerVisible"
      :columns="themeOptions"
      title="选择主题"
    />
  </view>
</template>
