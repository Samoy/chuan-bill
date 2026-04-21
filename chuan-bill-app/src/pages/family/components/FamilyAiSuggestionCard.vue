<script setup lang="ts">
const props = defineProps<{
  month: string
  familyId: string
}>()

const suggestion = ref('')
const remainingCount = ref(-1)
const loading = ref(false)
const cached = ref(false)

const AI_DAILY_LIMIT = 5

const remainingLabel = computed(() => {
  if (remainingCount.value < 0)
    return ''
  return `(${remainingCount.value}/${AI_DAILY_LIMIT})`
})

const hasReachedLimit = computed(() => {
  return remainingCount.value === 0
})

// 获取AI建议
async function fetchAiSuggestion() {
  loading.value = true
  try {
    const res = await Apis.familyStatistics.getAiSuggestion({
      params: {
        familyId: props.familyId,
        month: props.month,
      },
    })
    if (res.success && res.data) {
      suggestion.value = res.data.content || ''
      cached.value = res.data.cached || false
      remainingCount.value = res.data.remainingCount ?? -1
    }
  }
  finally {
    loading.value = false
  }
}

// 监听月份变化重新获取
watch(() => props.month, () => {
  fetchAiSuggestion()
}, { immediate: true })
</script>

<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <view class="mb-4 flex items-center justify-between">
      <view class="flex items-center gap-2">
        <view class="i-lucide:sparkles h-5 w-5 text-yellow-500" />
        <text class="text-base font-500">
          AI 家庭账单建议
        </text>
        <text v-if="remainingLabel" class="text-xs text-gray-400">
          {{ remainingLabel }}
        </text>
      </view>
      <view v-if="cached" class="rounded bg-gray-100 px-2 py-0.5 dark:bg-gray-700">
        <text class="text-xs text-gray-500">
          已缓存
        </text>
      </view>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="py-6">
      <wd-skeleton :row="4" animation="gradient" />
    </view>

    <!-- 建议内容 -->
    <view v-else-if="suggestion" class="space-y-3">
      <text class="block whitespace-pre-wrap text-sm text-gray-700 leading-relaxed dark:text-gray-300">
        {{ suggestion }}
      </text>
    </view>

    <!-- 无数据 -->
    <view v-else class="py-6 text-center">
      <view class="i-lucide:message-square h-12 w-12 text-gray-300" />
      <text class="mt-2 block text-sm text-gray-400">
        暂无AI建议
      </text>
    </view>

    <!-- 次数用完提示 -->
    <view v-if="hasReachedLimit" class="mt-3 rounded-lg bg-orange-50 p-3 dark:bg-orange-900/20">
      <text class="block text-center text-xs text-orange-600">
        今日AI建议次数已用完，请明天再试
      </text>
    </view>
  </view>
</template>
