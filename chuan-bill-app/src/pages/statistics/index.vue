<script setup lang="ts">
import dayjs from 'dayjs'
import { AI_SUGGESTION_TYPE_USER } from '@/common/constant'
import AiSuggestionCard from './components/AiSuggestionCard.vue'
import CategoryChart from './components/CategoryChart.vue'
import DailyTrendChart from './components/DailyTrendChart.vue'
import { setupEcharts } from './echarts-setup'

definePage({
  name: 'statistics',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '统计',
  },
})

setupEcharts()

const statisticsStore = useStatisticsStore()
const user = useUserStore()

// 当前选中的月份
const currentMonth = ref(dayjs().format('YYYY-MM'))

// 月份选择器显示状态
const showMonthPicker = ref(false)

// 月份选项（最近12个月）
const monthOptions = computed(() => {
  const options = []
  for (let i = 0; i < 12; i++) {
    const month = dayjs().subtract(i, 'month').format('YYYY-MM')
    options.push({ label: month, value: month })
  }
  return options
})

// 切换到上一个月
function prevMonth() {
  currentMonth.value = dayjs(currentMonth.value).subtract(1, 'month').format('YYYY-MM')
}

// 切换到下一个月
function nextMonth() {
  const next = dayjs(currentMonth.value).add(1, 'month')
  if (next.isAfter(dayjs(), 'month')) {
    return
  }
  currentMonth.value = next.format('YYYY-MM')
}

// 选择月份
function onMonthSelect({ value }: { value: string }) {
  currentMonth.value = value
  showMonthPicker.value = false
}

onLoad(() => {
  statisticsStore.setAnalysisContext(AI_SUGGESTION_TYPE_USER)
})

// 监听月份变化，获取统计数据
watch(currentMonth, (month) => {
  statisticsStore.fetchAll(month)
  statisticsStore.fetchAiSuggestionCached(AI_SUGGESTION_TYPE_USER, month)
}, { immediate: true })

// 监听登录状态变化，重新获取
watch(() => user.isLoggedIn, () => {
  statisticsStore.fetchAll(currentMonth.value)
  statisticsStore.fetchAiSuggestionCached(AI_SUGGESTION_TYPE_USER, currentMonth.value)
})
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 月份选择器 -->
    <wd-sticky :z-index="10">
      <view class="box-border h-50px w-100vw flex items-center justify-center gap-4 bg-[#faf8fc]">
        <view class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]" @click="prevMonth">
          <view class="i-lucide:chevron-left text-gray-600 dark:text-gray-400" />
        </view>
        <wd-picker
          v-model="currentMonth"
          v-model:visible="showMonthPicker"
          :columns="monthOptions"
          title="选择月份"
          @confirm="onMonthSelect"
        >
          <view class="flex items-center gap-1 text-lg font-500" @click="showMonthPicker = true">
            <text>{{ currentMonth }}</text>
            <view class="i-lucide:chevron-down h-4 w-4 text-gray-400" />
          </view>
        </wd-picker>
        <view class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]" @click="nextMonth">
          <view class="i-lucide:chevron-right text-gray-600 dark:text-gray-400" />
        </view>
      </view>
    </wd-sticky>

    <!-- 概览卡片 -->
    <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view v-if="statisticsStore.overviewLoading" class="py-4">
        <wd-skeleton :row="1" animation="gradient" />
      </view>
      <view v-else class="flex items-center justify-around">
        <view class="flex flex-col items-center">
          <text class="text-xs text-gray-400">
            支出
          </text>
          <text class="text-lg text-red-400 font-600">
            ¥{{ statisticsStore.overview?.expense || '0.00' }}
          </text>
        </view>
        <view class="h-8 w-px bg-gray-200 dark:bg-gray-700" />
        <view class="flex flex-col items-center">
          <text class="text-xs text-gray-400">
            收入
          </text>
          <text class="text-lg text-green-500 font-600">
            ¥{{ statisticsStore.overview?.income || '0.00' }}
          </text>
        </view>
        <view class="h-8 w-px bg-gray-200 dark:bg-gray-700" />
        <view class="flex flex-col items-center">
          <text class="text-xs text-gray-400">
            结余
          </text>
          <text class="text-lg text-primary font-600">
            ¥{{ statisticsStore.overview?.balance || '0.00' }}
          </text>
        </view>
      </view>
    </view>

    <!-- 分类饼图 -->
    <view class="mx-3">
      <CategoryChart :month="currentMonth" />
    </view>

    <!-- 每日趋势折线图 -->
    <view class="mx-3">
      <DailyTrendChart :month="currentMonth" />
    </view>

    <!-- AI 消费建议 -->
    <view class="mx-3">
      <AiSuggestionCard :month="currentMonth" />
    </view>

    <!-- 底部间距（给 tabbar 留空间） -->
    <view class="h-10" />
  </view>
</template>
