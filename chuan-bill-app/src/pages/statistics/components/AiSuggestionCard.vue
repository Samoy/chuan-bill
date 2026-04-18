<script setup lang="ts">
defineOptions({
  name: 'AiSuggestionCard',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

const props = defineProps<{
  month: string
}>()

const user = useUserStore()
const statisticsStore = useStatisticsStore()

function handleLogin() {
  user.requireAuth(() => {
    statisticsStore.fetchAiSuggestion(props.month)
  })
}

function fetchAnalysis() {
  statisticsStore.fetchAiSuggestion(props.month)
}
</script>

<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <view class="mb-3 flex items-center gap-2">
      <text class="text-base font-500">
        AI 消费建议
      </text>
    </view>

    <!-- 未登录状态 -->
    <view v-if="!user.isLoggedIn" class="flex flex-col items-center gap-3 py-6">
      <text class="text-sm text-gray-400">
        登录即可获取AI消费建议
      </text>
      <wd-button type="primary" block @click="handleLogin">
        立即登录
      </wd-button>
    </view>

    <!-- 加载中 -->
    <view v-else-if="statisticsStore.aiLoading" class="py-4">
      <wd-skeleton :row="4" animation="gradient" />
    </view>

    <!-- 已有数据 -->
    <view v-else-if="statisticsStore.aiSuggestion">
      <view class="text-sm text-gray-600 leading-relaxed dark:text-gray-300">
        {{ statisticsStore.aiSuggestion }}
      </view>
      <view class="mt-2 flex items-center gap-1 text-xs text-gray-400 dark:text-gray-500">
        <view class="i-lucide:info text-10px" />
        <text>内容由AI生成，仅供参考</text>
      </view>
    </view>

    <!-- 已登录但无数据 -->
    <view v-else class="flex flex-col items-center gap-3 py-6">
      <text class="text-sm text-gray-400">
        点击按钮获取本月消费分析
      </text>
      <view
        class="ai-btn flex items-center justify-center gap-1.5 rounded-full px-8 py-1.5 text-sm text-white font-500 shadow-md transition-transform active:scale-95"
        @click="fetchAnalysis"
      >
        <view class="i-lucide:sparkles text-base" />
        <text>获取AI分析</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.ai-btn {
  background: linear-gradient(135deg, #6366f5, rgb(var(--color-primary)));

  &:active {
    opacity: 0.85;
  }
}
</style>
