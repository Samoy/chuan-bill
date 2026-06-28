<script setup lang="ts">
import type { BillVO } from '@/api/globals'
import { SOURCE_MAP } from '@/constant/bill'

defineOptions({
  name: 'BillSection',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const { bill, type, customClass, customStyle } = defineProps<{
  bill: BillVO
  type?: 'user' | 'family'
  customClass?: string
  customStyle?: string
}>()
</script>

<template>
  <view class="box-border w-full flex flex-col gap-2" :class="customClass" :style="customStyle">
    <!-- 头部：金额、名称 -->
    <view class="box-border w-full flex flex-col items-center gap-2">
      <view class="h-12 w-12 flex items-center justify-center rounded-2xl" :class="bill.type === 'income' ? 'text-green-500 bg-green-100' : 'text-red-400 bg-red-50'">
        <text class="h-6 w-6 text-2xl" :class="transformUnoCSS(bill.category?.icon || '')" />
      </view>
      <view class="text-2xl font-bold" :class="bill.type === 'income' ? 'text-green-500' : 'text-red-500'">
        {{ bill.type === 'income' ? '+' : '-' }} ￥{{ bill.amount }}
      </view>
    </view>
    <!-- 中间：属性网格 两列布局 -->
    <view class="grid grid-cols-2 mt-2 gap-3">
      <view class="flex flex-col gap-1 border-1 border-gray-100 rounded-xl border-solid bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800">
        <view class="flex items-center text-xs text-gray-400">
          <text class="i-mingcute:bill-2-line mr-1" />
          <text>账单名称</text>
        </view>
        <text class="text-sm">
          {{ bill.name }}
        </text>
      </view>
      <view class="flex flex-col gap-1 border-1 border-gray-100 rounded-xl border-solid bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800">
        <view class="flex items-center text-xs text-gray-400">
          <text class="i-lucide:tag mr-1" />
          <text>类目</text>
        </view>
        <text class="text-sm">
          {{ bill.category?.name }}
        </text>
      </view>
      <view class="flex flex-col gap-1 border-1 border-gray-100 rounded-xl border-solid bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800">
        <view class="flex items-center text-xs text-gray-400">
          <text class="i-lucide:calendar mr-1" />
          <text>时间</text>
        </view>
        <text class="text-sm">
          {{ bill.time }}
        </text>
      </view>
      <view class="flex flex-col gap-1 border-1 border-gray-100 rounded-xl border-solid bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800">
        <view class="flex items-center text-xs text-gray-400">
          <text class="i-lucide:credit-card mr-1" />
          <text>支付方式</text>
        </view>
        <text class="text-sm">
          {{ bill.paymentMethod?.name }}
        </text>
      </view>
      <view class="flex flex-col gap-1 border-1 border-gray-100 rounded-xl border-solid bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800">
        <view class="flex items-center text-xs text-gray-400">
          <text class="i-lucide:brush mr-1" />
          <text>来源</text>
        </view>
        <text class="text-sm">
          {{ SOURCE_MAP[bill.source! as keyof typeof SOURCE_MAP] || '未知' }}
        </text>
      </view>
      <view v-if="type === 'family' && bill.userNickname" class="flex flex-col gap-1 border-1 border-gray-100 rounded-xl border-solid bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800">
        <view class="flex items-center text-xs text-gray-400">
          <text class="i-lucide:user mr-1" />
          <text>添加者</text>
        </view>
        <view class="flex items-center gap-1.5">
          <image v-if="bill.userAvatar" :src="bill.userAvatar" class="h-4 w-4 rounded-full" mode="aspectFill" />
          <text class="text-sm">
            {{ bill.userNickname }}
          </text>
        </view>
      </view>
      <view v-else class="flex flex-col gap-1 border-1 border-gray-100 rounded-xl border-solid bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800">
        <view class="flex items-center text-xs text-gray-400">
          <text class="i-carbon:pedestrian-family mr-1" />
          <text>所属家庭</text>
        </view>
        <text class="text-sm">
          {{ bill.familyName || '无' }}
        </text>
      </view>
      <view v-if="bill.remark" class="col-span-2 flex flex-col gap-1 border-1 border-gray-100 rounded-xl border-solid bg-gray-50 px-3 py-2.5 dark:border-gray-700 dark:bg-gray-800">
        <view class="flex items-center text-xs text-gray-400">
          <text class="i-lucide:message-circle-question-mark mr-1" />
          <text>备注</text>
        </view>
        <text class="max-h-[200px] overflow-auto text-sm">
          {{ bill.remark }}
        </text>
      </view>
    </view>
  </view>
</template>
