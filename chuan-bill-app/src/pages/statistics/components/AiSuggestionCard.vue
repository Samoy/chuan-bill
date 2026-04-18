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

/**
 * 简易 markdown 转 HTML（处理加粗、换行、列表）
 */
const parsedContent = computed(() => {
  const text = statisticsStore.aiSuggestion
  if (!text)
    return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
    .replace(/^- (.+)/gm, '&bull; $1')
})
</script>

<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <view class="mb-3 flex items-center gap-2">
      <view class="i-lucide:sparkles text-primary" />
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
    <view v-else-if="statisticsStore.aiSuggestion" class="text-sm text-gray-600 leading-relaxed dark:text-gray-300">
      <rich-text :nodes="parsedContent" />
    </view>

    <!-- 已登录但无数据 -->
    <view v-else class="flex flex-col items-center gap-3 py-6">
      <text class="text-sm text-gray-400">
        点击按钮获取本月消费分析
      </text>
      <wd-button type="primary" size="small" plain @click="fetchAnalysis">
        获取AI分析
      </wd-button>
    </view>
  </view>
</template>
