<script setup lang="ts">
definePage({
  name: 'mine',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '我的',
  },
})

// 鉴权检查
const { isLoggedIn, showLoginPopup } = useAuthCheck()
const userStore = useUserStore()
const localBillStore = useLocalBillStore()

// 登录价值特性
const loginFeatures = [
  { icon: 'i-lucide:camera', text: '图片识别记账' },
  { icon: 'i-lucide:mic', text: '语音记账' },
  { icon: 'i-lucide:cloud-sync', text: '云端同步' },
  { icon: 'i-lucide:smartphone', text: '多设备访问' },
]

// 菜单列表（已登录状态）
const menuList = [
  { icon: 'i-lucide:user', title: '个人信息', action: () => {} },
  { icon: 'i-lucide:settings', title: '设置', action: () => {} },
  { icon: 'i-lucide:help-circle', title: '帮助与反馈', action: () => {} },
  { icon: 'i-lucide:info', title: '关于', action: () => {} },
]

// 跳转到登录
function goToLogin() {
  showLoginPopup.value = true
}

// 退出登录
function logout() {
  userStore.logout()
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 未登录状态 -->
    <template v-if="!isLoggedIn">
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
              登录后享受更多功能
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

      <!-- 本地数据提示 -->
      <view v-if="localBillStore.hasPendingBills" class="mx-3 rounded-2xl bg-orange-50 p-4 dark:bg-orange-900/20">
        <view class="flex items-center gap-3">
          <view class="h-10 w-10 flex items-center justify-center rounded-full bg-orange-100 dark:bg-orange-800">
            <view class="i-lucide:database h-5 w-5 text-orange-600 dark:text-orange-400" />
          </view>
          <view class="flex-1">
            <text class="block text-sm text-orange-800 font-500 dark:text-orange-200">
              您有本地账单数据
            </text>
            <text class="mt-0.5 block text-xs text-orange-600 dark:text-orange-300">
              {{ localBillStore.bills.length }} 笔账单待同步到云端
            </text>
          </view>
          <wd-button type="warning" size="small" @click="goToLogin">
            去同步
          </wd-button>
        </view>
      </view>

      <!-- 关于 -->
      <view class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="flex items-center justify-between border-b border-gray-100 p-4 dark:border-gray-700">
          <view class="flex items-center gap-3">
            <view class="i-lucide:info h-5 w-5 text-gray-500" />
            <text class="text-sm">
              关于小川记账
            </text>
          </view>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
        <view class="flex items-center justify-between p-4">
          <view class="flex items-center gap-3">
            <view class="i-lucide:help-circle h-5 w-5 text-gray-500" />
            <text class="text-sm">
              帮助与反馈
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
            <view class="i-lucide:user h-8 w-8" />
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

      <!-- 菜单列表 -->
      <view class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view
          v-for="(item, index) in menuList"
          :key="index"
          class="flex items-center justify-between p-4"
          :class="index < menuList.length - 1 && 'border-b border-gray-100 dark:border-gray-700'"
          @click="item.action"
        >
          <view class="flex items-center gap-3">
            <view class="h-5 w-5 text-gray-500" :class="item.icon" />
            <text class="text-sm">
              {{ item.title }}
            </text>
          </view>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="mx-3 mt-4">
        <wd-button type="error" plain block @click="logout">
          退出登录
        </wd-button>
      </view>
    </template>
  </view>
</template>
