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

const emit = defineEmits<{
  click: [item: BillVO]
}>()

interface BillCardProps {
  bill: BillVO
  customClass?: string
  customStyle?: string
}

const isFriendlyTime = ref(true)

function toggleFriendlyTime(e: UniHelper.TouchEvent, time?: string) {
  const isSame = friendlyTime(time) === time
  if (!isSame) {
    isFriendlyTime.value = !isFriendlyTime.value
  }
  else {
    emit('click', bill)
  }
}
</script>

<template>
  <view
    class="box-border w-full flex items-start gap-4 rounded-xl bg-white p-4 shadow-sm dark:bg-[--wot-dark-background2]"
    :class="customClass"
    :style="customStyle"
    @click="emit('click', bill)"
  >
    <view class="flex flex-1 gap-4">
      <!-- 左侧：图标 -->
      <view
        class="h-5 w-5 flex items-center justify-center border-[1px] border-white/30 rounded-2xl border-solid bg-[rgba(255,255,255,0.2)] p-3 shadow-[0_4px_30px_rgba(0,0,0,0.1)] backdrop-blur-[16px] backdrop-saturate-[180%]"
        :class="bill.type === 'expense' ? 'text-red-400' : 'text-green-500'"
      >
        <text class="h-4 w-4" :class="transformUnoCSS(bill.category?.icon || '')" />
      </view>
      <!-- 中间：名称、时间、支付方式 -->
      <view class="flex flex-col gap-2">
        <view class="font-500">
          {{ bill.name }}
        </view>
        <view class="flex gap-2 text-xs text-gray-500">
          <text v-if="bill.time" @click.stop="toggleFriendlyTime($event, bill.time)">
            {{ isFriendlyTime ? friendlyTime(bill.time) : bill.time }}
          </text>
          <text v-if="bill.paymentMethod">
            {{ bill.paymentMethod.name }}
          </text>
        </view>
      </view>
    </view>
    <!-- 右侧：金额 -->
    <text class="text-lg" :class="bill.type === 'expense' ? 'text-red-400' : 'text-green-500'">
      {{ bill.type === 'expense' ? '-' : '+' }} {{ bill.amount }}
    </text>
  </view>
</template>
