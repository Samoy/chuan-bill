<script setup lang="ts">
import type { FamilyMemberStatsVO } from '@/api/globals'
import WdProgress from 'wot-design-uni/components/wd-progress/wd-progress.vue'

defineOptions({
  name: 'CategoryChart',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

const props = defineProps<{
  familyId?: string
  month: string
}>()

const activeTab = ref<'expense' | 'income'>('expense')
const memeberData = ref<FamilyMemberStatsVO[]>([])
const loading = ref(false)

const segmentOptions = [
  { value: 'expense', payload: { label: '支出', icon: 'i-icon-park-outline:expenses' } },
  { value: 'income', payload: { label: '收入', icon: 'i-icon-park-outline:income' } },
]

interface RankingItem extends FamilyMemberStatsVO {
  rank: number
  value: number
  percentage: number
}

const rankingData = computed<RankingItem[]>(() => {
  const data = memeberData.value
  if (!data.length)
    return []

  const sorted = [...data]
    .filter((item) => {
      const val = activeTab.value === 'expense' ? Number(item.expense || 0) : Number(item.income || 0)
      return val > 0
    })
    .sort((a, b) => {
      const aVal = activeTab.value === 'expense' ? Number(a.expense || 0) : Number(a.income || 0)
      const bVal = activeTab.value === 'expense' ? Number(b.expense || 0) : Number(b.income || 0)
      return bVal - aVal
    })
    .slice(0, 5)

  if (sorted.length === 0)
    return []

  const maxValue = Math.max(...sorted.map(item =>
    activeTab.value === 'expense' ? Number(item.expense || 0) : Number(item.income || 0),
  ))

  return sorted.map((item, index) => {
    const value = activeTab.value === 'expense' ? Number(item.expense || 0) : Number(item.income || 0)
    return {
      ...item,
      rank: index + 1,
      value,
      percentage: maxValue > 0 ? Math.round((value / maxValue) * 100) : 0,
    }
  })
})

const barColor = computed(() =>
  activeTab.value === 'expense' ? '#ef4444' : '#22c55e',
)

async function fetchMemberStats() {
  loading.value = true
  try {
    const res = await Apis.statistics.getMembersStats({
      params: {
        familyId: props.familyId,
        month: props.month,
      },
    })
    if (res.success) {
      memeberData.value = res.data || []
    }
    else {
      memeberData.value = []
    }
  }
  catch (error) {
    console.error('Failed to fetch member stats:', error)
    memeberData.value = []
  }
  finally {
    loading.value = false
  }
}

watch(() => [props.month, props.familyId], () => {
  console.log('Fetching member stats for month:', props.month, 'familyId:', props.familyId)
  if (props.familyId && props.month) {
    fetchMemberStats()
  }
}, { immediate: true })
</script>

<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <view class="mb-3 flex items-center justify-between">
      <text class="flex-1 text-base font-500">
        收支排行榜
      </text>
      <wd-segmented v-model:value="activeTab" :options="segmentOptions" size="small" custom-class="!rounded-xl dark:!bg-[var(--wot-dark-background3)] w-200px!">
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
    <view v-if="!loading && rankingData.length" class="flex flex-col gap-3">
      <view
        v-for="item in rankingData"
        :key="item.userId"
        class="flex items-center gap-2.5"
      >
        <view class="h-6 w-6 flex flex-shrink-0 items-center justify-center">
          <view
            v-if="item.rank === 1"
            class="i-lucide:crown h-5 w-5"
            style="color: #f59e0b"
          />
          <view
            v-else-if="item.rank === 2"
            class="i-lucide:medal h-5 w-5"
            style="color: #94a3b8"
          />
          <view
            v-else-if="item.rank === 3"
            class="i-lucide:award h-5 w-5"
            style="color: #d97706"
          />
          <view
            v-else
            class="h-5 w-5 flex items-center justify-center rounded-full bg-gray-300 dark:bg-gray-600"
          >
            <text class="text-11px text-white font-600">
              {{ item.rank }}
            </text>
          </view>
        </view>
        <view class="min-w-0 flex flex-1 flex-col gap-0.5">
          <text class="truncate text-11px text-gray-500 dark:text-gray-400">
            {{ item.nickname || '未知' }}
          </text>
          <view
            class="custom-progress w-full"
            style="--wot-progress-bg: transparent; --wot-progress-height: 8px"
          >
            <wd-progress
              :percentage="item.percentage"
              :color="barColor"
              :duration="0"
              hide-text
            />
          </view>
        </view>
        <text class="mt-3 flex-shrink-0 text-sm text-gray-700 font-500 dark:text-gray-300">
          ￥{{ item.value }}
        </text>
      </view>
    </view>
    <view v-else-if="loading" class="flex items-center justify-center py-3">
      <wd-skeleton :row="5" animation="gradient" />
    </view>
    <view v-else class="flex items-center justify-center py-3">
      <text class="text-sm text-gray-400">
        暂无数据
      </text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
:deep(.wd-segmented__item--active) {
  @apply rounded-lg;
}
:deep(.wd-segmented__item-label) {
  @apply h-full! flex! items-center! justify-center!
}
</style>
