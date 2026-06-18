<script setup lang="ts">
import * as echarts from 'echarts/core'
import { CHART_COLORS } from '@/utils/echarts-setup'

defineOptions({
  name: 'CategoryChart',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

const props = defineProps<{
  month: string
  familyId?: string
}>()

const personalStore = usePersonalStatisticsStore()
const familyStore = useFamilyStatisticsStore()
const themeStore = useManualThemeStore()

const store = computed(() => props.familyId ? familyStore : personalStore)

const activeType = ref<'expense' | 'income'>('expense')

const segmentedOptions = [
  { value: 'expense', payload: { label: '支出', icon: 'i-icon-park-outline:expenses' } },
  { value: 'income', payload: { label: '收入', icon: 'i-icon-park-outline:income' } },
]

const chartOption = computed(() => {
  const data = store.value.categoryData
  const isDark = themeStore.isDark
  if (!data.length) {
    return {}
  }

  const reversed = [...data].reverse()
  const maxAmount = Math.max(...data.map(d => d.amount))
  const barHeight = 28
  const chartHeight = reversed.length * (barHeight + 8) + 32

  return {
    height: chartHeight,
    grid: {
      top: 8,
      bottom: 8,
      left: 8,
      right: 60,
      containLabel: true,
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: { name: string, value: number, dataIndex: number }) => {
        const item = data[params.dataIndex]
        return `${params.name}: ¥${params.value.toFixed(2)} (${item.percentage.toFixed(1)}%)`
      },
      backgroundColor: isDark ? '#333' : '#fff',
      borderColor: 'transparent',
      borderWidth: 0,
    },
    xAxis: {
      type: 'value',
      max: maxAmount * 1.15,
      show: false,
    },
    yAxis: {
      type: 'category',
      data: reversed.map(item => item.categoryName),
      show: false,
    },
    series: [{
      type: 'bar',
      data: reversed.map((item, index) => ({
        value: item.amount,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: CHART_COLORS[index % CHART_COLORS.length] },
            { offset: 1, color: `${CHART_COLORS[index % CHART_COLORS.length]}99` },
          ]),
          borderRadius: [0, 4, 4, 0],
        },
      })),
      barWidth: barHeight,
      barGap: '10%',
      label: {
        show: true,
        position: 'right',
        formatter: (params: { value: number }) => `¥${params.value.toFixed(2)}`,
        fontSize: 12,
        color: isDark ? '#ccc' : '#666',
      },
    }],
  }
})

const chartHeight = computed(() => {
  const data = store.value.categoryData
  if (!data.length)
    return 0
  return data.length * 36 + 32
})

function fetchData() {
  if (props.familyId) {
    familyStore.fetchCategoryBreakdown(props.month, activeType.value, props.familyId)
  }
  else {
    personalStore.fetchCategoryBreakdown(props.month, activeType.value)
  }
}

watch(activeType, fetchData)

watch(() => props.month, fetchData)
</script>

<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <!-- 标题和切换 -->
    <view class="mb-3 flex items-center justify-between">
      <text class="text-base font-500">
        分类统计
      </text>
      <wd-segmented
        v-model:value="activeType"
        :options="segmentedOptions"
        size="small"
        custom-class="!rounded-2xl dark:!bg-[var(--wot-dark-background3)] w-200px!"
      >
        <template #label="{ option }">
          <view class="w-200px flex items-center justify-center gap-1">
            <view class="text-xs" :class="[option.payload.icon]" />
            <view class="text-xs">
              {{ option.payload.label }}
            </view>
          </view>
        </template>
      </wd-segmented>
    </view>

    <!-- 条形图 -->
    <view v-if="!store.categoryLoading && store.categoryData.length">
      <uni-echarts :option="chartOption" autoresize :custom-style="`height: ${chartHeight}px; width: 100%;`" />
    </view>
    <view v-else-if="store.categoryLoading" class="flex items-center justify-center py-8">
      <wd-skeleton :row="0" animation="gradient">
        <template #label="{ option }">
          <view class="h-200px flex items-center justify-center gap-1">
            <view class="text-xs dark:text-white" :class="[option.payload.icon]" />
            <view class="text-xs">
              {{ option.payload.label }}
            </view>
          </view>
        </template>
      </wd-skeleton>
    </view>
    <view v-else class="flex items-center justify-center py-8">
      <text class="text-sm text-gray-400">
        暂无数据
      </text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
:deep(.wd-segmented__item--active) {
  @apply rounded-xl;
}

:deep(.wd-segmented__item-label) {
  @apply h-full! flex! items-center! justify-center!;
}
</style>
