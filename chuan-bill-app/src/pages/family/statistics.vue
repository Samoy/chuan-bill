<script setup lang="ts">
import dayjs from 'dayjs'
import AiSuggestionCard from '@/pages/statistics/components/AiSuggestionCard.vue'
import { setupEcharts } from '@/utils/echarts-setup'
import CategoryChart from '../statistics/components/CategoryChart.vue'
import MemberRankingChart from './components/MemberRankingChart.vue'

definePage({
  name: 'family-statistics',
  layout: 'default',
  style: {
    navigationBarTitleText: '家庭统计',
    enablePullDownRefresh: true,
  },
})

setupEcharts()

const statisticsStore = useFamilyStatisticsStore()

const familyId = ref('')
const currentMonth = ref(dayjs().format('YYYY-MM'))
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

// 是否为当前月（禁用右箭头）
const isCurrentMonth = computed(() => {
  return dayjs(currentMonth.value).isSame(dayjs(), 'month')
})

// 切换到下一个月
function nextMonth() {
  if (isCurrentMonth.value) {
    return
  }
  const next = dayjs(currentMonth.value).add(1, 'month')
  currentMonth.value = next.format('YYYY-MM')
}

// 选择月份
function onMonthSelect({ value }: { value: string }) {
  currentMonth.value = value
  showMonthPicker.value = false
}

onLoad((options) => {
  if (options?.familyId) {
    familyId.value = options.familyId
  }
  if (options?.familyName) {
    uni.setNavigationBarTitle({ title: `${decodeURIComponent(options.familyName)}账单统计` })
  }
  statisticsStore.fetchAll(currentMonth.value, familyId.value)
  statisticsStore.fetchAiSuggestionCached(currentMonth.value, familyId.value)
})

watch(currentMonth, (month) => {
  statisticsStore.fetchAll(month, familyId.value)
  statisticsStore.fetchAiSuggestionCached(month, familyId.value)
})
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 月份选择器 -->
    <wd-sticky :z-index="10">
      <view class="box-border h-50px w-100vw flex items-center justify-center gap-4 bg-[#faf8fc] dark:bg-[var(--wot-dark-background2)]">
        <view
          class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-gray-800"
          @click="prevMonth"
        >
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
        <view
          class="h-8 w-8 flex items-center justify-center rounded-full shadow-sm"
          :class="isCurrentMonth ? 'bg-gray-100 dark:bg-gray-800 opacity-80' : 'bg-white dark:bg-gray-800'"
          @click="nextMonth"
        >
          <view class="i-lucide:chevron-right" :class="isCurrentMonth ? 'text-gray-300 dark:text-gray-600' : 'text-gray-600 dark:text-gray-400'" />
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

    <!-- 类别支出占比饼图 -->
    <view class="mx-3">
      <CategoryChart :month="currentMonth" :family-id="familyId" />
    </view>

    <!-- 收支排行榜 -->
    <view class="mx-3">
      <MemberRankingChart :month="currentMonth" :family-id="familyId" />
    </view>

    <!-- AI建议（仅户主可见） -->
    <view class="mx-3">
      <AiSuggestionCard :month="currentMonth" :family-id="familyId" />
    </view>
  </view>
</template>
