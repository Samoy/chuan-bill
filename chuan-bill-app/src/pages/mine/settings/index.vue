<script setup lang="ts">
definePage({
  name: 'settings',
  layout: 'default',
  style: {
    navigationBarTitleText: '设置',
  },
})

const themeStore = useManualTheme()
const toast = useGlobalToast()
const router = useRouter()

// 主题选项
const themeOptions = [
  { label: '跟随系统', value: 'auto' },
  { label: '浅色模式', value: 'light' },
  { label: '深色模式', value: 'dark' },
]

const themePickerVisible = ref(false)
const currentTheme = computed(() => {
  return themeOptions.find(t => t.value === themeStore.theme.value)?.label || '跟随系统'
})

// 通知设置
const pushEnabled = ref(true)
const billReminderEnabled = ref(true)

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
          <wd-picker
            v-model="themeStore.theme.value"
            v-model:visible="themePickerVisible"
            :columns="themeOptions"
            title="选择主题"
          >
            <view class="flex items-center gap-1">
              <text class="text-sm text-gray-400">
                {{ currentTheme }}
              </text>
              <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
            </view>
          </wd-picker>
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
  </view>
</template>
