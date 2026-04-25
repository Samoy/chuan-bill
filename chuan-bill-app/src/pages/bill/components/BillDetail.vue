<script setup lang="ts">
import type { BillVO } from '@/api/globals'
import { SOURCE_MAP } from '@/common/constant'

defineOptions({
  name: 'BillSection',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const { bill, customClass, customStyle } = defineProps<{
  bill: BillVO
  customClass?: string
  customStyle?: string
}>()
</script>

<template>
  <view class="box-border w-full flex flex-col gap-2" :class="customClass" :style="customStyle">
    <!-- 头部：金额、名称 -->
    <view class="box-border w-full flex flex-col items-center">
      <view class="text-2xl font-bold" :class="bill.type === 'income' ? 'text-green-500' : 'text-red-500'">
        {{ bill.type === 'income' ? '+' : '-' }} ￥{{ bill.amount }}
      </view>
      <view class="font-medium">
        {{ bill.name }}
      </view>
    </view>
    <!-- 中间：属性列表 -->
    <view class="mt-2 flex flex-col gap-4 border-1 border-gray-100 rounded-2xl border-solid bg-gray-50 px-4 py-3 dark:border-gray-700 dark:bg-gray-800">
      <view class="flex justify-between text-sm">
        <view class="flex items-center text-gray-400">
          <text class="i-lucide:tag mr-1" />
          <text>类目</text>
        </view>
        <text>
          {{ bill.category?.name }}
        </text>
      </view>
      <view class="flex justify-between text-sm">
        <view class="flex items-center text-gray-400">
          <text class="i-lucide:calendar mr-1" />
          <text>时间</text>
        </view>
        <text>
          {{ bill.time }}
        </text>
      </view>
      <view class="flex justify-between text-sm">
        <view class="flex items-center text-gray-400">
          <text class="i-lucide:credit-card mr-1" />
          <text>支付方式</text>
        </view>
        <text>
          {{ bill.paymentMethod?.name }}
        </text>
      </view>
      <view class="flex justify-between text-sm">
        <view class="flex items-center text-gray-400">
          <text class="i-lucide:brush mr-1" />
          <text>来源</text>
        </view>
        <text>
          {{ SOURCE_MAP[bill.source! as keyof typeof SOURCE_MAP] || '未知' }}
        </text>
      </view>
      <view v-if="bill.remark" class="flex flex-col justify-between gap-2 text-sm">
        <view class="flex items-center text-gray-400">
          <text class="i-lucide:message-circle-question-mark mr-1" />
          <text>备注</text>
        </view>
        <text class="max-h-[200px] flex-1 overflow-auto">
          {{ bill.remark }}
        </text>
      </view>
    </view>
  </view>
</template>
