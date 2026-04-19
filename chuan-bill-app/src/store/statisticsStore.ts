import type { BillMonthlyStatsVO } from '@/api/globals'
import dayjs from 'dayjs'
import { add, divide, multiply } from 'mathjs'

interface CategoryStatItem {
  categoryId: string
  categoryName: string
  categoryIcon: string
  amount: number
  percentage: number
}

export const useStatisticsStore = defineStore('statistics', () => {
  const user = useUserStore()
  const billStore = useBillStore()

  const overview = ref<BillMonthlyStatsVO | null>(null)
  const categoryData = ref<CategoryStatItem[]>([])
  const dailyTrend = ref<{ days: string[], expenses: number[], incomes: number[] } | null>(null)
  const aiSuggestion = ref<string>('')
  const aiCached = ref(false)
  const aiRemainingCount = ref(-1)
  const overviewLoading = ref(false)
  const categoryLoading = ref(false)
  const trendLoading = ref(false)
  const aiLoading = ref(false)

  /**
   * 获取月度概览
   */
  async function fetchOverview(month: string) {
    overviewLoading.value = true
    try {
      overview.value = await billStore.getMonthlyBillStats(month) || null
    }
    finally {
      overviewLoading.value = false
    }
  }

  /**
   * 获取分类统计
   */
  async function fetchCategoryBreakdown(month: string, type: 'expense' | 'income') {
    categoryLoading.value = true
    try {
      if (user.isLoggedIn) {
        const res = await Apis.statistics.getCategoryStats({ params: { month, type } })
        if (res.success && res.data) {
          categoryData.value = res.data.map(item => ({
            categoryId: item.categoryId || '',
            categoryName: item.categoryName || '',
            categoryIcon: item.categoryIcon || '',
            amount: Number(item.amount || 0),
            percentage: Number(item.percentage || 0),
          }))
          return
        }
      }

      // 本地计算
      const filtered = billStore.localBillList.filter(
        bill => bill.type === type && dayjs(bill.time).isSame(dayjs(month), 'month'),
      )

      if (filtered.length === 0) {
        categoryData.value = []
        return
      }

      // 按分类分组求和
      const categoryMap = new Map<string, { categoryId: string, categoryName: string, categoryIcon: string, amount: number }>()
      for (const bill of filtered) {
        const catId = bill.category?.id || ''
        const existing = categoryMap.get(catId)
        const amount = Number(bill.amount || 0)
        if (existing) {
          existing.amount = add(existing.amount, amount) as number
        }
        else {
          categoryMap.set(catId, {
            categoryId: catId,
            categoryName: bill.category?.name || '未知',
            categoryIcon: bill.category?.icon || '',
            amount,
          })
        }
      }

      // 计算总金额
      let total = 0
      for (const item of categoryMap.values()) {
        total = add(total, item.amount) as number
      }

      // 排序并计算百分比
      const sorted = [...categoryMap.values()].sort((a, b) => b.amount - a.amount)
      categoryData.value = sorted.map(item => ({
        ...item,
        percentage: total > 0 ? Number(multiply(divide(item.amount, total), 100).toFixed(1)) : 0,
      }))
    }
    finally {
      categoryLoading.value = false
    }
  }

  /**
   * 获取每日收支趋势
   */
  async function fetchDailyTrend(month: string) {
    trendLoading.value = true
    try {
      const daysInMonth = dayjs(month).daysInMonth()
      const days = Array.from({ length: daysInMonth }, (_, i) => String(i + 1))
      const expenses = Array.from({ length: daysInMonth }, () => 0)
      const incomes = Array.from({ length: daysInMonth }, () => 0)

      if (user.isLoggedIn) {
        const res = await Apis.statistics.getDailyTrend({ params: { month } })
        if (res.success && res.data) {
          for (const item of res.data) {
            if (item.date) {
              const dayIndex = dayjs(item.date).date() - 1
              if (dayIndex >= 0 && dayIndex < daysInMonth) {
                expenses[dayIndex] = Number(item.expense || 0)
                incomes[dayIndex] = Number(item.income || 0)
              }
            }
          }
        }
      }
      else {
        // 本地计算
        const filtered = billStore.localBillList.filter(
          bill => dayjs(bill.time).isSame(dayjs(month), 'month'),
        )
        for (const bill of filtered) {
          const dayIndex = dayjs(bill.time).date() - 1
          if (dayIndex >= 0 && dayIndex < daysInMonth) {
            const amount = Number(bill.amount || 0)
            if (bill.type === 'expense') {
              expenses[dayIndex] = add(expenses[dayIndex], amount) as number
            }
            else if (bill.type === 'income') {
              incomes[dayIndex] = add(incomes[dayIndex], amount) as number
            }
          }
        }
      }

      dailyTrend.value = { days, expenses, incomes }
    }
    finally {
      trendLoading.value = false
    }
  }

  /**
   * 获取AI消费建议（登录后可用）
   * @param month 月份
   * @param regenerate 是否重新生成
   */
  async function fetchAiSuggestion(month: string, regenerate = false) {
    if (!user.isLoggedIn)
      return
    aiLoading.value = true
    try {
      const res = await Apis.ai.analysis({ params: { month, regenerate } })
      if (res.success && res.data) {
        aiSuggestion.value = res.data.content || ''
        aiCached.value = res.data.cached || false
        aiRemainingCount.value = res.data.remainingCount || -1
      }
      else {
        aiSuggestion.value = ''
        aiCached.value = false
      }
    }
    catch {
      aiSuggestion.value = ''
      aiCached.value = false
    }
    finally {
      aiLoading.value = false
    }
  }

  /**
   * 获取缓存的AI建议（不触发AI生成，无缓存时返回空）
   */
  async function fetchAiSuggestionCached(month: string) {
    if (!user.isLoggedIn)
      return
    try {
      const res = await Apis.ai.analysis({ params: { month }, meta: { silent: true } } as any)
      if (res.success && res.data) {
        aiSuggestion.value = res.data.content || ''
        aiCached.value = res.data.cached || false
        aiRemainingCount.value = res.data.remainingCount || -1
      }
      else {
        aiSuggestion.value = ''
        aiCached.value = false
      }
    }
    catch {
      aiSuggestion.value = ''
      aiCached.value = false
    }
  }

  /**
   * 并行获取所有统计数据
   */
  async function fetchAll(month: string) {
    await Promise.all([
      fetchOverview(month),
      fetchCategoryBreakdown(month, 'expense'),
      fetchDailyTrend(month),
    ])
  }

  function reset() {
    overview.value = null
    categoryData.value = []
    dailyTrend.value = null
    aiSuggestion.value = ''
    aiCached.value = false
    aiRemainingCount.value = -1
    overviewLoading.value = false
    categoryLoading.value = false
    trendLoading.value = false
    aiLoading.value = false
  }

  return {
    overview,
    categoryData,
    dailyTrend,
    aiSuggestion,
    aiCached,
    aiRemainingCount,
    overviewLoading,
    categoryLoading,
    trendLoading,
    aiLoading,
    fetchOverview,
    fetchCategoryBreakdown,
    fetchDailyTrend,
    fetchAiSuggestion,
    fetchAiSuggestionCached,
    fetchAll,
    reset,
  }
})
