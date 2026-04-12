<script setup lang="ts">
import type { BillMonthlyStatsVO } from '@/api/globals'

defineOptions({
  name: 'BillSection',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const { month, customClass, customStyle } = defineProps<{
  month: string
  customClass?: string
  customStyle?: string
}>()

const statistics = ref<BillMonthlyStatsVO>()
const billStore = useBillStore()

onMounted(async () => {
  const res = await billStore.getMonthlyBillStats(month)
  if (res) {
    statistics.value = res
  }
})
</script>

<template>
  <view class="box-border w-full flex items-center justify-between gap-3" :class="customClass" :style="customStyle">
    <view class="font-500">
      {{ month }}
    </view>
    <view class="flex flex-1 items-center justify-end gap-3">
      <view class="flex items-center gap-1">
        <view class="h-4 w-4 flex items-center justify-center rounded-full bg-red-400 text-center">
          <text class="i-icon-park-outline:expenses h-2 w-2 text-white" />
        </view>
        <text class="text-red-400">
          {{ statistics?.expense }}
        </text>
      </view>
      <view class="flex items-center gap-1">
        <view class="h-4 w-4 flex items-center justify-center rounded-full bg-green-500 text-center">
          <text class="i-icon-park-outline:income h-2 w-2 text-white" />
        </view>
        <text class="text-green-500">
          {{ statistics?.income }}
        </text>
      </view>
      <view class="flex items-center gap-1">
        <view class="h-4 w-4 flex items-center justify-center rounded-full bg-primary text-center">
          <text class="i-icon-park-outline:consume h-2 w-2 text-white" />
        </view>
        <text class="text-primary">
          {{ statistics?.balance }}
        </text>
      </view>
    </view>
  </view>
</template>
