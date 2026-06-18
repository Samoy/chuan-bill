<script setup lang="ts">
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

// 莫兰迪色系 - 与 echarts 主题一致
const MORANDI_COLORS = [
  '#8D9BA3',
  '#B5C4B1',
  '#C9B1A0',
  '#A3B5C4',
  '#C4B5A3',
  '#B1B5C4',
  '#C4A3B5',
  '#B5C4C4',
  '#C4C4A3',
]

const segmentedOptions = [
  { value: 'expense', payload: { label: '支出', icon: 'i-icon-park-outline:expenses' } },
  { value: 'income', payload: { label: '收入', icon: 'i-icon-park-outline:income' } },
]

const pieOption = computed(() => {
  const data = store.value.categoryData
  const isDark = themeStore.isDark
  if (!data.length) {
    return {}
  }

  return {
    grid: {
      top: '10%',
      bottom: '10%',
      containLabel: true,
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
      backgroundColor: isDark ? '#333' : '#fff',
      borderColor: 'transparent',
      borderWidth: 0,
    },
    color: MORANDI_COLORS,
    series: [{
      type: 'pie',
      radius: ['0%', '90%'],
      center: ['50%', '50%'],
      itemStyle: {
        borderColor: isDark ? '#1a1a1a' : '#fff',
        borderWidth: 2,
      },
      label: { show: false },
      data: data.map(item => ({
        value: item.amount,
        name: item.categoryName,
      })),
    }],
  }
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

    <!-- 饼图 -->
    <view v-if="!store.categoryLoading && store.categoryData.length">
      <uni-echarts :option="pieOption" autoresize custom-style="height: 220px; width: 100%;" />
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

    <!-- 分类列表 -->
    <view v-if="store.categoryData.length" class="mt-3 flex flex-col gap-2">
      <view
        v-for="(item, index) in store.categoryData"
        :key="item.categoryId"
        class="flex items-center gap-2"
      >
        <view
          class="h-3 w-3 shrink-0 rounded-sm"
          :style="{ backgroundColor: MORANDI_COLORS[index % MORANDI_COLORS.length] }"
        />
        <view class="h-4 w-4 flex shrink-0 items-center justify-center text-gray-500 dark:text-gray-300" :class="[item.categoryIcon]" />
        <text class="flex-1 text-xs text-gray-500 dark:text-gray-300">
          {{ item.categoryName }}
        </text>
        <text class="text-sm font-500">
          ¥{{ item.amount.toFixed(2) }}
        </text>
        <text class="w-12 text-right text-xs text-gray-500 dark:text-gray-300">
          {{ item.percentage.toFixed(1) }}%
        </text>
      </view>
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
