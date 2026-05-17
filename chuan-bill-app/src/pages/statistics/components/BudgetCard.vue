<script setup lang="ts">
import dayjs from 'dayjs'
import BudgetSettingPopup from './BudgetSettingPopup.vue'

defineOptions({
  name: 'BudgetCard',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

const props = defineProps<{
  month: string
}>()

const user = useUserStore()
const budgetStore = useBudgetStore()
const showSettingPopup = ref(false)

const budget = computed(() => budgetStore.currentBudget)

// 进度条颜色
const progressColor = computed(() => {
  if (!budget.value)
    return 'var(--color-primary)'
  const percent = Number(budget.value.usagePercent)
  if (percent > 100)
    return '#ef4444'
  if (percent >= 80)
    return '#f97316'
  if (percent >= 60)
    return '#eab308'
  return '#22c55e'
})

// 进度条宽度（限制最大 100%）
const progressWidth = computed(() => {
  if (!budget.value)
    return '0%'
  const percent = Math.min(Number(budget.value.usagePercent), 100)
  return `${percent}%`
})

// 是否超预算
const isOverBudget = computed(() => {
  if (!budget.value)
    return false
  return Number(budget.value.usagePercent) > 100
})

// 超支金额
const overAmount = computed(() => {
  if (!budget.value || !isOverBudget.value)
    return '0.00'
  return (Number(budget.value.useAmount) - Number(budget.value.amount)).toFixed(2)
})

// 是否当月
const isCurrentMonth = computed(() => {
  return props.month === dayjs().format('YYYY-MM')
})

function openSetting() {
  if (!isCurrentMonth.value)
    return
  showSettingPopup.value = true
}

// 月份变化时获取预算
watch(() => props.month, (newMonth) => {
  if (user.isLoggedIn && newMonth) {
    budgetStore.fetchBudget(newMonth)
  }
}, { immediate: true })

// 登录状态变化时获取预算
watch(() => user.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    budgetStore.fetchBudget(props.month)
  }
})
</script>

<template>
  <view v-if="user.isLoggedIn" class="mx-3">
    <!-- 有预算 -->
    <view
      v-if="budget"
      class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]"
      :class="isCurrentMonth && 'active:scale-98 transition-transform'"
      @click="openSetting"
    >
      <view class="mb-3 flex items-center justify-between">
        <text class="text-sm text-gray-500 font-medium">
          本月预算
        </text>
        <text v-if="isCurrentMonth" class="text-xs text-gray-400">
          点击修改
        </text>
      </view>

      <!-- 进度条 -->
      <view class="mb-3 h-2 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-700">
        <view
          class="h-full rounded-full transition-all duration-500"
          :style="{ width: progressWidth, backgroundColor: progressColor }"
        />
      </view>

      <!-- 金额信息 -->
      <view class="flex items-center justify-between">
        <view>
          <text class="text-xs text-gray-400">
            已用
          </text>
          <text class="ml-1 text-sm font-bold" :style="{ color: progressColor }">
            ¥{{ budget.useAmount }}
          </text>
        </view>
        <view>
          <text class="text-xs text-gray-400">
            预算
          </text>
          <text class="ml-1 text-sm font-bold">
            ¥{{ budget.amount }}
          </text>
        </view>
      </view>

      <!-- 剩余/超支提示 -->
      <view class="mt-2 flex items-center justify-between">
        <text v-if="isOverBudget" class="text-xs text-red-500 font-medium">
          超支 ¥{{ overAmount }}
        </text>
        <text v-else class="text-xs text-gray-400">
          剩余 ¥{{ budget.remainingAmount }}
        </text>
        <text class="text-xs font-medium" :style="{ color: progressColor }">
          {{ budget.usagePercent }}%
        </text>
      </view>
    </view>

    <!-- 无预算 -->
    <view
      v-else
      class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]"
      :class="isCurrentMonth && 'active:scale-98 transition-transform cursor-pointer'"
      @click="openSetting"
    >
      <view class="flex items-center justify-center gap-2 py-2">
        <view class="i-lucide:wallet h-4 w-4 text-gray-400" />
        <text class="text-sm text-gray-400">
          {{ isCurrentMonth ? '尚未设置预算，点击设置' : '该月未设置预算' }}
        </text>
      </view>
    </view>

    <!-- 设置弹窗 -->
    <BudgetSettingPopup v-model="showSettingPopup" />
  </view>
</template>
