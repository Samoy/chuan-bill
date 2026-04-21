<script setup lang="ts">
import type { FamilyMemberStatsVO } from '@/api/globals'
import dayjs from 'dayjs'
import AiSuggestionCard from './components/FamilyAiSuggestionCard.vue'
import MemberChart from './components/MemberChart.vue'
import MemberRanking from './components/MemberRanking.vue'

definePage({
  name: 'family-statistics',
  style: {
    navigationBarTitleText: '家庭统计',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()

const familyId = ref('')
const currentMonth = ref(dayjs().format('YYYY-MM'))
const showMonthPicker = ref(false)

// 统计数据
const memberStats = ref<FamilyMemberStatsVO[]>([])
const overview = ref({ expense: 0, income: 0, balance: 0 })
const loading = ref(false)

// 月份选项（最近12个月）
const monthOptions = computed(() => {
  const options = []
  for (let i = 0; i < 12; i++) {
    const month = dayjs().subtract(i, 'month').format('YYYY-MM')
    options.push({ label: month, value: month })
  }
  return options
})

// 当前用户信息
const currentUserId = computed(() => userStore.userId)
const isOwner = computed(() => {
  return memberStats.value.some(m => m.userId === currentUserId.value && m.isOwner)
})

onLoad((options) => {
  if (options?.familyId) {
    familyId.value = options.familyId
  }
})

onShow(() => {
  if (familyId.value) {
    fetchStatistics()
  }
})

// 监听月份变化
watch(currentMonth, () => {
  if (familyId.value) {
    fetchStatistics()
  }
})

// 切换到上一个月
function prevMonth() {
  currentMonth.value = dayjs(currentMonth.value).subtract(1, 'month').format('YYYY-MM')
}

// 切换到下一个月
function nextMonth() {
  const next = dayjs(currentMonth.value).add(1, 'month')
  if (next.isAfter(dayjs(), 'month')) {
    toast.info('不能选择未来月份')
    return
  }
  currentMonth.value = next.format('YYYY-MM')
}

// 选择月份
function onMonthSelect({ value }: { value: string }) {
  currentMonth.value = value
  showMonthPicker.value = false
}

// 获取统计数据
async function fetchStatistics() {
  if (!familyId.value)
    return
  loading.value = true
  try {
    const res = await Apis.familyStatistics.getMemberStats({
      params: {
        familyId: familyId.value,
        month: currentMonth.value,
      },
    })
    if (res.success && res.data) {
      memberStats.value = res.data
      // 计算总览数据
      const totalExpense = res.data.reduce((sum, item) => sum + (Number(item.expense) || 0), 0)
      const totalIncome = res.data.reduce((sum, item) => sum + (Number(item.income) || 0), 0)
      overview.value = {
        expense: totalExpense,
        income: totalIncome,
        balance: totalIncome - totalExpense,
      }
    }
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 月份选择器 -->
    <wd-sticky :z-index="10">
      <view class="box-border h-50px w-100vw flex items-center justify-center gap-4 bg-[#faf8fc]">
        <view
          class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]"
          @click="prevMonth"
        >
          <view class="i-lucide:chevron-left text-gray-600 dark:text-gray-400" />
        </view>
        <wd-picker
          v-model="currentMonth"
          v-model:visible="showMonthPicker"
          :columns="monthOptions"
          title="选择月份"
          @confirm="onMonthSelect"
        >
          <view class="flex items-center gap-1 text-lg font-500" @click="showMonthPicker = true">
            <text>{{ currentMonth }}</text>
            <view class="i-lucide:chevron-down h-4 w-4 text-gray-400" />
          </view>
        </wd-picker>
        <view
          class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]"
          @click="nextMonth"
        >
          <view class="i-lucide:chevron-right text-gray-600 dark:text-gray-400" />
        </view>
      </view>
    </wd-sticky>

    <!-- 概览卡片 -->
    <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view v-if="loading" class="py-4">
        <wd-skeleton :row="1" animation="gradient" />
      </view>
      <view v-else class="flex items-center justify-around">
        <view class="flex flex-col items-center">
          <text class="text-xs text-gray-400">
            支出
          </text>
          <text class="text-lg text-red-400 font-600">
            ¥{{ overview.expense.toFixed(2) }}
          </text>
        </view>
        <view class="h-8 w-px bg-gray-200 dark:bg-gray-700" />
        <view class="flex flex-col items-center">
          <text class="text-xs text-gray-400">
            收入
          </text>
          <text class="text-lg text-green-500 font-600">
            ¥{{ overview.income.toFixed(2) }}
          </text>
        </view>
        <view class="h-8 w-px bg-gray-200 dark:bg-gray-700" />
        <view class="flex flex-col items-center">
          <text class="text-xs text-gray-400">
            结余
          </text>
          <text class="text-lg text-primary font-600">
            ¥{{ overview.balance.toFixed(2) }}
          </text>
        </view>
      </view>
    </view>

    <!-- 成员收支饼图 -->
    <view class="mx-3">
      <MemberChart :month="currentMonth" :family-id="familyId" :data="memberStats" :loading="loading" />
    </view>

    <!-- 收支排行榜 -->
    <view class="mx-3">
      <MemberRanking :data="memberStats" :loading="loading" />
    </view>

    <!-- AI建议（仅户主可见） -->
    <view v-if="isOwner" class="mx-3">
      <AiSuggestionCard :month="currentMonth" :family-id="familyId" />
    </view>

    <!-- 底部间距 -->
    <view class="h-10" />
  </view>
</template>
