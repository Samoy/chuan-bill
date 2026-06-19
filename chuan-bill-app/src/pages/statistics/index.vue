<script setup lang="ts">
import dayjs from 'dayjs'
import { EVENTS } from '@/constant/events'
import { setupEcharts } from '@/utils/echarts-setup'
import { eventBus } from '@/utils/eventBus'
import AiSuggestionCard from './components/AiSuggestionCard.vue'
import BudgetCard from './components/BudgetCard.vue'
import BudgetSettingPopup from './components/BudgetSettingPopup.vue'
import CategoryChart from './components/CategoryChart.vue'
import DailyTrendChart from './components/DailyTrendChart.vue'

definePage({
  name: 'statistics',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '统计',
  },
})

setupEcharts()

const statisticsStore = usePersonalStatisticsStore()
const budgetStore = useBudgetStore()
const user = useUserStore()
const showSettingPopup = ref(false)

// 当前选中的月份
const currentMonth = ref(dayjs().format('YYYY-MM'))

// 页面可见性（用于避免后台更新导致 echarts 渲染空白）
const pageVisible = ref(true)
const needsRefresh = ref(false)

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

// 首次加载时获取数据
onLoad(() => {
  statisticsStore.fetchAll(currentMonth.value)
  statisticsStore.fetchAiSuggestionCached(currentMonth.value)
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(currentMonth.value)
  }
})

onPullDownRefresh(() => {
  handleDataUpdated()
    .finally(() => uni.stopPullDownRefresh())
})

// 页面可见性管理：切回前台时，如果有待刷新的数据则立即获取
onShow(() => {
  pageVisible.value = true
  if (needsRefresh.value) {
    needsRefresh.value = false
    handleDataUpdated()
  }
})

onHide(() => {
  pageVisible.value = false
})

// 监听月份变化，获取统计数据
watch(currentMonth, (month) => {
  statisticsStore.fetchAll(month)
  statisticsStore.fetchAiSuggestionCached(month)
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(month)
  }
})

// 监听登录状态变化，重新获取
watch(() => user.isLoggedIn, () => {
  statisticsStore.fetchAll(currentMonth.value)
  statisticsStore.fetchAiSuggestionCached(currentMonth.value)
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(currentMonth.value)
  }
})

// 监听账单和家庭数据变化事件（页面不可见时延迟到 onShow 再刷新，避免 echarts 渲染空白）
async function handleDataUpdated() {
  if (!pageVisible.value) {
    needsRefresh.value = true
    return
  }
  await statisticsStore.fetchAll(currentMonth.value)
  await statisticsStore.fetchAiSuggestionCached(currentMonth.value)
  if (user.isLoggedIn) {
    await budgetStore.fetchBudget(currentMonth.value)
  }
}

onMounted(() => {
  eventBus.on(EVENTS.BILL.UPDATED, handleDataUpdated)
  eventBus.on(EVENTS.FAMILY.UPDATED, handleDataUpdated)
})

onUnmounted(() => {
  eventBus.off(EVENTS.BILL.UPDATED, handleDataUpdated)
  eventBus.off(EVENTS.FAMILY.UPDATED, handleDataUpdated)
})
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 月份选择器 -->
    <wd-sticky :z-index="10">
      <view class="box-border h-50px w-100vw flex items-center justify-center gap-4 bg-[#faf8fc] dark:bg-[var(--wot-dark-background2)]">
        <view class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-gray-800" @click="prevMonth">
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
          :class="isCurrentMonth ? 'bg-gray-100 dark:bg-gray-700 opacity-80' : 'bg-white dark:bg-gray-800'"
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

    <!-- 预算卡片 -->
    <BudgetCard v-if="user.isLoggedIn" :month="currentMonth" @open-setting="showSettingPopup = true" />

    <!-- 分类饼图 -->
    <view class="mx-3">
      <CategoryChart :month="currentMonth" />
    </view>

    <!-- 每日趋势折线图 -->
    <view class="mx-3">
      <DailyTrendChart :month="currentMonth" />
    </view>

    <!-- AI 消费建议 -->
    <view class="mx-3 mb-3">
      <AiSuggestionCard :month="currentMonth" />
    </view>

    <!-- 预算弹窗 -->
    <BudgetSettingPopup v-model="showSettingPopup" />

    <!-- 底部间距（给 tabbar 留空间） -->
    <view class="h-10" />
  </view>
</template>
