<script setup lang="ts">
definePage({
  name: 'family',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '家庭',
  },
})

// 鉴权检查
const user = useUserStore()

// 家庭功能特性列表
const features = [
  {
    icon: 'i-lucide:users',
    title: '家庭共享',
    desc: '与家人共同管理家庭财务',
  },
  {
    icon: 'i-lucide:wallet',
    title: '共同记账',
    desc: '家庭成员可共同记录收支',
  },
  {
    icon: 'i-lucide:pie-chart',
    title: '家庭统计',
    desc: '查看家庭整体财务状况',
  },
  {
    icon: 'i-lucide:shield-check',
    title: '权限管理',
    desc: '灵活设置成员权限',
  },
]

// 跳转到登录
function goToLogin() {
  user.showLoginPopup = true
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 未登录状态 -->
    <template v-if="!user.isLoggedIn">
      <!-- 顶部介绍卡片 -->
      <view class="mx-3 rounded-2xl from-primary to-primary/80 bg-gradient-to-br p-6 text-white shadow-lg">
        <view class="mb-4 flex items-center gap-3">
          <view class="h-14 w-14 flex items-center justify-center rounded-2xl bg-white/20 backdrop-blur-sm">
            <view class="i-lucide:home h-7 w-7" />
          </view>
          <view>
            <text class="block text-lg font-bold">
              家庭记账
            </text>
            <text class="mt-0.5 block text-sm text-white/80">
              与家人一起管理财务
            </text>
          </view>
        </view>
        <text class="block text-sm text-white/90 leading-relaxed">
          创建或加入家庭，与家人共同记录和管理家庭收支，让家庭财务更透明、更可控。
        </text>
      </view>

      <!-- 功能特性列表 -->
      <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <text class="mb-4 block text-sm font-500">
          功能特性
        </text>
        <view class="grid grid-cols-2 gap-4">
          <view v-for="(item, index) in features" :key="index" class="flex items-start gap-3">
            <view class="h-10 w-10 flex shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <view class="h-5 w-5 text-primary" :class="item.icon" />
            </view>
            <view>
              <text class="block text-sm font-500">
                {{ item.title }}
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                {{ item.desc }}
              </text>
            </view>
          </view>
        </view>
      </view>

      <!-- 登录提示卡片 -->
      <view class="mx-3 rounded-2xl bg-white p-6 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="mb-3 flex justify-center">
          <view class="h-16 w-16 flex items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
            <view class="i-lucide:lock h-8 w-8 text-gray-400" />
          </view>
        </view>
        <text class="mb-1 block text-base font-500">
          登录后体验家庭共享记账
        </text>
        <text class="mb-6 block text-sm text-gray-500">
          创建家庭、邀请成员、共同管理
        </text>
        <wd-button type="primary" block size="large" @click="goToLogin">
          立即登录
        </wd-button>
      </view>
    </template>

    <!-- 已登录状态（占位） -->
    <template v-else>
      <view class="mx-3 rounded-2xl bg-white px-4 py-12 text-center dark:bg-[var(--wot-dark-background2)]">
        <view class="mb-4 flex justify-center">
          <view class="h-16 w-16 flex items-center justify-center rounded-full bg-primary/10">
            <view class="i-lucide:home h-8 w-8 text-primary" />
          </view>
        </view>
        <text class="mb-2 block text-lg font-500">
          家庭功能开发中
        </text>
        <text class="block text-sm text-gray-500">
          敬请期待...
        </text>
      </view>
    </template>
  </view>
</template>
