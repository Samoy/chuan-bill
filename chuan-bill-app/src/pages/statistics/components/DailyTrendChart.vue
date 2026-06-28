<script setup lang="ts">
defineOptions({
  name: 'DailyTrendChart',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

defineProps<{
  month: string
}>()

const statisticsStore = usePersonalStatisticsStore()
const themeStore = useManualThemeStore()

const lineOption = computed(() => {
  const trend = statisticsStore.dailyTrend
  const isDark = themeStore.isDark

  if (!trend)
    return {}

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: isDark ? '#333' : '#fff',
      textStyle: { color: isDark ? '#e0e0e0' : '#333', fontSize: 12 },
    },
    legend: {
      data: ['支出', '收入'],
      textStyle: { color: isDark ? '#a0a0a0' : '#666', fontSize: 12 },
      icon: 'rect',
      bottom: '0%',
    },
    dataZoom: [{
      type: 'inside',
    }],
    grid: {
      left: '3%',
      right: '3%',
      top: '12%',
      bottom: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: trend.days,
      boundaryGap: false,
      axisLabel: {
        color: isDark ? '#a0a0a0' : '#666',
        fontSize: 10,
        interval: 'auto',
      },
      axisLine: { lineStyle: { color: isDark ? '#444' : '#ddd' } },
    },
    yAxis: [
      {
        type: 'value',
        name: '支出',
        position: 'left',
        axisLabel: {
          color: isDark ? '#a0a0a0' : '#666',
          fontSize: 10,
          formatter: '¥{value}',
        },
        splitLine: {
          lineStyle: { color: isDark ? '#a0a0a0' : '#ddd' },
        },
      },
      {
        type: 'value',
        name: '收入',
        position: 'right',
        axisLabel: {
          color: isDark ? '#a0a0a0' : '#666',
          fontSize: 10,
          formatter: '¥{value}',
        },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '支出',
        type: 'line',
        smooth: true,
        yAxisIndex: 0,
        data: trend.expenses,
        lineStyle: { color: '#f87171', width: 2 },
        itemStyle: { color: '#f87171' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(248,113,113,0.5)' },
              { offset: 1, color: 'rgba(248,113,113,0.1)' },
            ],
          },
        },
        symbol: 'none',
      },
      {
        name: '收入',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: trend.incomes,
        lineStyle: { color: '#22c55e', width: 2 },
        itemStyle: { color: '#22c55e' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(34,197,94,0.5)' },
              { offset: 1, color: 'rgba(34,197,94,0.1)' },
            ],
          },
        },
        symbol: 'none',
      },
    ],
  }
})
</script>

<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <text class="mb-3 text-base font-500">
      每日趋势
    </text>

    <view v-if="!statisticsStore.trendLoading && statisticsStore.dailyTrend">
      <uni-echarts :option="lineOption" autoresize custom-style="height: 250px; width: 100%;" />
    </view>
    <view v-else-if="statisticsStore.trendLoading" class="py-4">
      <wd-skeleton :row="5" animation="gradient" />
    </view>
    <view v-else class="flex items-center justify-center py-8">
      <text class="text-sm text-gray-400">
        暂无数据
      </text>
    </view>
  </view>
</template>
