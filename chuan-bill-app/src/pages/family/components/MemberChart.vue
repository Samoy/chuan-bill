<script setup lang="ts">
import type { FamilyMemberStatsVO } from '@/api/globals'

const props = defineProps<{
  month: string
  familyId: string
  data: FamilyMemberStatsVO[]
  loading: boolean
}>()

const themeStore = useManualThemeStore()

const activeType = ref<'expense' | 'income'>('expense')

const CHART_COLORS = ['#5B8FF9', '#5AD8A6', '#F6BD16', '#E86452', '#6DC8EC', '#945FB9', '#FF9845', '#1E9493', '#FF99C3']

const segmentedOptions = [
  { value: 'expense', payload: { label: '支出', icon: 'i-icon-park-outline:expenses' } },
  { value: 'income', payload: { label: '收入', icon: 'i-icon-park-outline:income' } },
]

// 饼图数据
const chartData = computed(() => {
  const key = activeType.value
  return props.data
    .filter(item => Number(item[key]) > 0)
    .map((item, index) => ({
      value: item[key],
      name: item.nickname || '未知',
      itemStyle: { color: CHART_COLORS[index % CHART_COLORS.length] },
      percentage: activeType.value === 'expense' ? item.expensePercentage : item.incomePercentage,
      avatar: item.avatar,
      isOwner: item.isOwner,
      rawData: item,
    }))
    .sort((a, b) => Number(b.value) - Number(a.value))
})

// 饼图配置
const pieOption = computed(() => {
  const isDark = themeStore.isDark
  const data = chartData.value

  if (!data.length) {
    return {}
  }

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.name}<br/>${activeType.value === 'expense' ? '支出' : '收入'}: ¥${params.value}<br/>占比: ${params.data.percentage}%`
      },
      backgroundColor: isDark ? '#333' : '#fff',
      borderColor: 'transparent',
      borderWidth: 0,
    },
    color: CHART_COLORS,
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderColor: isDark ? '#1a1a1a' : '#fff',
        borderWidth: 2,
        borderRadius: 8,
      },
      label: { show: false },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold',
        },
      },
      data: data.map(item => ({
        value: item.value,
        name: item.name,
        percentage: item.percentage,
      })),
    }],
  }
})
</script>

<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <view class="mb-4 flex items-center justify-between">
      <text class="text-base font-500">
        成员收支占比
      </text>
      <wd-segmented
        v-model:value="activeType"
        :options="segmentedOptions"
        size="small"
        custom-class="!rounded-xl dark:!bg-[var(--wot-dark-background3)] w-200px!"
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

    <!-- 加载中 -->
    <view v-if="loading" class="py-8">
      <wd-skeleton :row="3" animation="gradient" />
    </view>

    <!-- 饼图 -->
    <view v-else-if="chartData.length > 0">
      <uni-echarts :option="pieOption" autoresize custom-style="height: 200px; width: 100%;" />

      <!-- 图例列表 -->
      <view class="mt-4 space-y-2">
        <view
          v-for="(item, index) in chartData"
          :key="item.rawData.userId"
          class="flex items-center justify-between rounded-lg bg-gray-50 p-2 dark:bg-gray-800"
        >
          <view class="flex items-center gap-2">
            <view
              class="h-6 w-6 flex items-center justify-center rounded-full text-xs text-white font-600"
              :style="{ backgroundColor: CHART_COLORS[index % CHART_COLORS.length] }"
            >
              {{ index + 1 }}
            </view>
            <image v-if="item.avatar" :src="item.avatar" class="h-6 w-6 rounded-full" mode="aspectFill" />
            <view v-else class="i-lucide:user h-5 w-5 text-gray-400" />
            <text class="text-sm">
              {{ item.name }}
            </text>
            <view
              v-if="item.isOwner"
              class="flex items-center rounded bg-primary/10 px-1.5 py-0.5"
            >
              <text class="text-xs text-primary">
                户主
              </text>
            </view>
          </view>
          <view class="text-right">
            <text class="block text-sm font-500">
              ¥{{ item.value }}
            </text>
            <text class="text-xs text-gray-400">
              {{ item.percentage }}%
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 无数据 -->
    <view v-else class="py-12 text-center">
      <view class="i-lucide:pie-chart h-12 w-12 text-gray-300" />
      <text class="mt-2 block text-sm text-gray-400">
        暂无{{ activeType === 'expense' ? '支出' : '收入' }}数据
      </text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
:deep(.wd-segmented__item--active) {
  @apply rounded-lg;
}

:deep(.wd-segmented__item-label) {
  @apply h-full! flex! items-center! justify-center!;
}
</style>
