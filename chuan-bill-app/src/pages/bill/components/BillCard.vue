<script setup lang="ts">
import type { BillVO } from '@/api/globals'

defineOptions({
  name: 'BillCard',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const { bill, customClass, customStyle } = defineProps<BillCardProps>()

interface BillCardProps {
  bill: BillVO
  customClass?: string
  customStyle?: string
}
</script>

<template>
  <view
    class="box-border w-full flex flex-col gap-5 border-1 border-gray-100 rounded-2xl border-solid bg-gray-50 px-4 py-3 dark:border-gray-700 dark:bg-gray-800"
    :class="customClass"
    :style="customStyle"
  >
    <!-- 头部：名称、类型 -->
    <view class="w-full flex justify-between">
      <view class="font-500">
        {{ bill.name }}
      </view>
      <view class="h-5 flex items-center justify-center gap-1 rounded-2.5 bg-primary/10 px-2 text-xs text-primary dark:bg-primary/20">
        <text class="h-3 w-3" :class="transformUnoCSS(bill.category?.icon || '')" />
        {{ bill.category?.name }}
      </view>
    </view>
    <!-- 中间：金额 -->
    <view class="text-2xl font-bold" :class="bill.type === 'income' ? 'text-green-500' : 'text-red-500'">
      {{ bill.type === 'income' ? '+' : '-' }}¥{{ bill.amount }}
    </view>
    <!-- 底部：支付方式：时间 -->
    <view class="flex items-center justify-between text-xs text-gray-400">
      <view class="flex gap-1">
        <text class="h-4 w-4" :class="transformUnoCSS(bill.paymentMethod?.icon || '')" />
        {{ bill.paymentMethod?.name }}
      </view>
      <view class="flex items-baseline justify-center gap-1">
        <text class="i-lucide:calendar-days h-3 w-3" />
        {{ bill.time }}
      </view>
    </view>
  </view>
</template>
