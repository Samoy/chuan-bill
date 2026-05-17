import type { BudgetVO } from '@/api/globals'

export const useBudgetStore = defineStore('budget', () => {
  const currentBudget = ref<BudgetVO | null>(null)
  const loading = ref(false)

  async function fetchBudget(month?: string) {
    loading.value = true
    try {
      const params: Record<string, string> = {}
      if (month) {
        params.month = month
      }
      const res = await Apis.budget.getCurrentBudget({ params })
      if (res.success) {
        currentBudget.value = res.data ?? null
      }
    }
    catch {
      // 静默失败
    }
    finally {
      loading.value = false
    }
  }

  async function setBudget(amount: string) {
    const res = await Apis.budget.setBudget({
      data: { amount },
    })
    if (res.success) {
      currentBudget.value = res.data ?? null
    }
    return res
  }

  async function deleteBudget() {
    const res = await Apis.budget.deleteBudget()
    if (res.success) {
      currentBudget.value = null
    }
    return res
  }

  return {
    currentBudget,
    loading,
    fetchBudget,
    setBudget,
    deleteBudget,
  }
})
