<script setup lang="ts">
import dayjs from 'dayjs'

definePage({
  name: 'statistics',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '统计',
  },
})

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
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 月份选择器 -->
    <view class="mx-3 flex items-center justify-center gap-4">
      <view class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]" @click="prevMonth">
        <view class="i-lucide:chevron-left text-gray-600 dark:text-gray-400" />
      </view>
      <view class="flex items-center gap-1 text-lg font-500" @click="showMonthPicker = true">
        <text>{{ currentMonth }}</text>
        <view class="i-lucide:chevron-down h-4 w-4 text-gray-400" />
      </view>
      <view class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]" @click="nextMonth">
        <view class="i-lucide:chevron-right text-gray-600 dark:text-gray-400" />
      </view>
    </view>

    <!-- 月份选择器弹框 -->
    <wd-picker
      v-model="currentMonth"
      v-model:visible="showMonthPicker"
      :columns="monthOptions"
      title="选择月份"
      @confirm="onMonthSelect"
    />
  </view>
</template>
