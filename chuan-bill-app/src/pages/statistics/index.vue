<script setup lang="ts">
import dayjs from 'dayjs'

definePage({
  name: 'statistics',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '统计',
  },
})

// 鉴权检查
const { isLoggedIn, showLoginPopup } = useAuthCheck()
const localBillStore = useLocalBillStore()

// 当前选中的月份
const currentMonth = ref(dayjs().format('YYYY-MM'))

// 月份选择器显示状态
const showMonthPicker = ref(false)

// 月份选项（最近12个月）
const monthOptions = computed(() => {
  const options = []
  for (let i = 0; i < 12; i++) {
    const month = dayjs().subtract(i, 'month').format('YYYY-MM')
    options.push({ label: month, value: month })
  }
  return options
})

// 本地账单统计
const localStats = computed(() => {
  const monthBills = localBillStore.bills.filter(b => b.billDate.startsWith(currentMonth.value))
  const income = monthBills.filter(b => b.type === 'income').reduce((sum, b) => sum + b.amount, 0)
  const expense = monthBills.filter(b => b.type === 'expense').reduce((sum, b) => sum + b.amount, 0)

  // 按分类统计支出
  const categoryStats: Record<string, { name: string, amount: number, type: 'income' | 'expense' }> = {}
  monthBills.forEach((bill) => {
    const key = `${bill.type}-${bill.categoryName}`
    if (!categoryStats[key]) {
      categoryStats[key] = {
        name: bill.categoryName,
        amount: 0,
        type: bill.type,
      }
    }
    categoryStats[key].amount += bill.amount
  })

  return {
    income,
    expense,
    balance: income - expense,
    totalCount: monthBills.length,
    categoryList: Object.values(categoryStats).sort((a, b) => b.amount - a.amount),
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
    return
  }
  currentMonth.value = next.format('YYYY-MM')
}

// 选择月份
function onMonthSelect({ value }: { value: string }) {
  currentMonth.value = value
  showMonthPicker.value = false
}

// 跳转到登录页
function goToLogin() {
  showLoginPopup.value = true
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 月份选择器 -->
    <view class="mx-3 flex items-center justify-center gap-4">
      <view class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]" @click="prevMonth">
        <view class="i-lucide:chevron-left text-gray-600 dark:text-gray-400" />
      </view>
      <view class="flex items-center gap-1 text-lg font-500" @click="showMonthPicker = true">
        <text>{{ currentMonth }}</text>
        <view class="i-lucide:chevron-down h-4 w-4 text-gray-400" />
      </view>
      <view class="h-8 w-8 flex items-center justify-center rounded-full bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]" @click="nextMonth">
        <view class="i-lucide:chevron-right text-gray-600 dark:text-gray-400" />
      </view>
    </view>

    <!-- 未登录状态 -->
    <template v-if="!isLoggedIn">
      <!-- 统计概览卡片 -->
      <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="mb-4 flex items-center justify-between">
          <text class="text-sm text-gray-500">
            {{ currentMonth }}月收支概览
          </text>
          <view v-if="localBillStore.hasPendingBills" class="rounded-full bg-orange-100 px-2 py-0.5">
            <text class="text-xs text-orange-600">
              本地数据
            </text>
          </view>
        </view>

        <view class="flex justify-around">
          <view class="flex flex-col items-center gap-1">
            <text class="text-xs text-gray-500">
              支出
            </text>
            <text class="text-xl text-red-400 font-bold">
              {{ localStats.expense.toFixed(2) }}
            </text>
          </view>
          <view class="flex flex-col items-center gap-1">
            <text class="text-xs text-gray-500">
              收入
            </text>
            <text class="text-xl text-green-500 font-bold">
              {{ localStats.income.toFixed(2) }}
            </text>
          </view>
          <view class="flex flex-col items-center gap-1">
            <text class="text-xs text-gray-500">
              结余
            </text>
            <text class="text-xl font-bold" :class="localStats.balance >= 0 ? 'text-green-500' : 'text-red-400'">
              {{ localStats.balance.toFixed(2) }}
            </text>
          </view>
        </view>

        <view class="mt-4 flex items-center justify-center gap-2 text-xs text-gray-400">
          <view class="i-lucide:receipt h-3 w-3" />
          <text>共 {{ localStats.totalCount }} 笔账单</text>
        </view>
      </view>

      <!-- 分类统计 -->
      <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <text class="mb-4 block text-sm font-500">
          分类统计
        </text>

        <template v-if="localStats.categoryList.length > 0">
          <view class="flex flex-col gap-3">
            <view v-for="(item, index) in localStats.categoryList" :key="index" class="flex items-center gap-3">
              <view
                class="h-8 w-8 flex items-center justify-center rounded-full"
                :class="item.type === 'expense' ? 'bg-red-100 text-red-400' : 'bg-green-100 text-green-500'"
              >
                <view class="i-lucide:tag h-4 w-4" />
              </view>
              <view class="flex-1">
                <view class="flex items-center justify-between">
                  <text class="text-sm">
                    {{ item.name }}
                  </text>
                  <text class="text-sm font-500" :class="item.type === 'expense' ? 'text-red-400' : 'text-green-500'">
                    {{ item.type === 'expense' ? '-' : '+' }}{{ item.amount.toFixed(2) }}
                  </text>
                </view>
                <view class="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-700">
                  <view
                    class="h-full rounded-full"
                    :class="item.type === 'expense' ? 'bg-red-400' : 'bg-green-500'"
                    :style="{ width: `${Math.min((item.amount / (item.type === 'expense' ? localStats.expense || 1 : localStats.income || 1)) * 100, 100)}%` }"
                  />
                </view>
              </view>
            </view>
          </view>
        </template>

        <!-- 空状态 -->
        <view v-else class="flex flex-col items-center justify-center gap-3 py-8">
          <view class="h-16 w-16 flex items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
            <view class="i-lucide:bar-chart-2 h-8 w-8 text-gray-400" />
          </view>
          <text class="text-sm text-gray-500">
            暂无统计数据
          </text>
          <text class="text-xs text-gray-400">
            {{ currentMonth }}月还没有记账记录
          </text>
        </view>
      </view>

      <!-- 登录提示 -->
      <view class="mx-3 mt-4 rounded-2xl from-primary/10 to-primary/5 bg-gradient-to-r p-4">
        <view class="flex items-center gap-3">
          <view class="h-10 w-10 flex items-center justify-center rounded-full bg-primary/20">
            <view class="i-lucide:cloud h-5 w-5 text-primary" />
          </view>
          <view class="flex-1">
            <text class="block text-sm font-500">
              登录解锁更多功能
            </text>
            <text class="mt-0.5 block text-xs text-gray-500">
              云端同步、多设备访问、数据备份
            </text>
          </view>
          <wd-button type="primary" size="small" @click="goToLogin">
            去登录
          </wd-button>
        </view>
      </view>
    </template>

    <!-- 已登录状态（占位） -->
    <template v-else>
      <view class="mx-3 rounded-2xl bg-white px-4 py-12 text-center dark:bg-[var(--wot-dark-background2)]">
        <view class="mb-4 flex justify-center">
          <view class="h-16 w-16 flex items-center justify-center rounded-full bg-primary/10">
            <view class="i-lucide:bar-chart-2 h-8 w-8 text-primary" />
          </view>
        </view>
        <text class="mb-2 block text-lg font-500">
          统计功能开发中
        </text>
        <text class="block text-sm text-gray-500">
          敬请期待...
        </text>
      </view>
    </template>

    <!-- 月份选择器弹框 -->
    <wd-picker
      v-model="currentMonth"
      v-model:visible="showMonthPicker"
      :columns="monthOptions"
      title="选择月份"
      @confirm="onMonthSelect"
    />
  </view>
</template>
