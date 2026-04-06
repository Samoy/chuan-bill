import type { CategoryVO, PaymentMethodVO } from '@/api/globals'

export const useBillStore = defineStore('bill', () => {
  const categoryListMap = ref<{ expense: CategoryVO[], income: CategoryVO[] }>(
    { expense: [], income: [] },
  )
  const paymentMethodList = ref<PaymentMethodVO[]>([])

  const isInitialzed = ref(false)

  async function initBillData() {
    if (isInitialzed.value) {
      return
    }
    try {
      const [categoryRes, paymentMethodRes] = await Promise.all([
        Apis.bill.getCategories({ params: {} }),
        Apis.bill.getPaymentMethods(),
      ])
      if (categoryRes.success) {
        const list = categoryRes.data || []
        categoryListMap.value = {
          expense: list.filter(item => item.type === 'expense'),
          income: list.filter(item => item.type === 'income'),
        }
      }
      if (paymentMethodRes.success) {
        paymentMethodList.value = paymentMethodRes.data || []
      }
      isInitialzed.value = true
    }
    catch (error) {
      console.error('Failed to initialize bill data:', error)
    }
  }

  function getCategoryList(type?: 'expense' | 'income' | '') {
    if (!type) {
      return Object.values(categoryListMap.value).flat()
    }
    return categoryListMap.value[type]
  }

  function getPaymentMethodList() {
    return paymentMethodList.value
  }

  return {
    categoryListMap,
    paymentMethodList,
    isInitialzed,
    initBillData,
    getCategoryList,
    getPaymentMethodList,
  }
})
