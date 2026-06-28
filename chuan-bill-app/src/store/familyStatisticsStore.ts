import type { BillMonthlyStatsVO } from '@/api/globals'
import dayjs from 'dayjs'
import { AiSuggestionType } from '@/constant/ai'

interface CategoryStatItem {
  categoryId: string
  categoryName: string
  categoryIcon: string
  amount: number
  percentage: number
}

export const useFamilyStatisticsStore = defineStore('familyStatistics', () => {
  const user = useUserStore()
  const billStore = useBillStore()

  const overview = ref<BillMonthlyStatsVO | null>(null)
  const categoryData = ref<CategoryStatItem[]>([])
  const dailyTrend = ref<{ days: string[], expenses: number[], incomes: number[] } | null>(null)
  const aiSuggestion = ref('')
  const aiCached = ref(false)
  const aiRemainingCount = ref(-1)
  const overviewLoading = ref(false)
  const categoryLoading = ref(false)
  const trendLoading = ref(false)
  const aiLoading = ref(false)

  async function fetchOverview(month: string, familyId: string) {
    overviewLoading.value = true
    try {
      overview.value = await billStore.getMonthlyBillStats(month, familyId) || null
    }
    finally {
      overviewLoading.value = false
    }
  }

  async function fetchCategoryBreakdown(month: string, type: 'expense' | 'income', familyId: string) {
    categoryLoading.value = true
    try {
      if (user.isLoggedIn) {
        const res = await Apis.statistics.getCategoryStats({ params: { month, type, familyId } })
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

      // 家庭统计不支持本地计算
      categoryData.value = []
    }
    finally {
      categoryLoading.value = false
    }
  }

  async function fetchDailyTrend(month: string, familyId: string) {
    trendLoading.value = true
    try {
      const daysInMonth = dayjs(month).daysInMonth()
      const days = Array.from({ length: daysInMonth }, (_, i) => String(i + 1))
      const expenses = Array.from({ length: daysInMonth }, () => 0)
      const incomes = Array.from({ length: daysInMonth }, () => 0)

      if (user.isLoggedIn) {
        const res = await Apis.statistics.getDailyTrend({ params: { month, familyId } })
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

      dailyTrend.value = { days, expenses, incomes }
    }
    finally {
      trendLoading.value = false
    }
  }

  async function fetchAiSuggestion(month: string, familyId: string, regenerate = false) {
    if (!user.isLoggedIn)
      return
    aiLoading.value = true
    try {
      const res = await Apis.ai.analysis({ params: { month, analysisType: AiSuggestionType.FAMILY, familyId, regenerate } })
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

  async function fetchAiSuggestionCached(month: string, familyId: string) {
    if (!user.isLoggedIn)
      return
    try {
      const res = await Apis.ai.analysis({ params: { analysisType: AiSuggestionType.FAMILY, month, familyId }, meta: { silent: true } } as any)
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

  async function fetchAll(month: string, familyId: string) {
    await Promise.all([
      fetchOverview(month, familyId),
      fetchCategoryBreakdown(month, 'expense', familyId),
      fetchDailyTrend(month, familyId),
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
