import type { AddBillDTO, BillMonthlyStatsVO, BillVO, CategoryVO, PaymentMethodVO, UpdateBillDTO } from '@/api/globals'
import dayjs from 'dayjs'
import { add, subtract } from 'mathjs'
import { v4 as uuid } from 'uuid'
import { LOCAL_PAY_CATEGORY_LIST, LOCAL_PAYMENT_METHOD_LIST } from '@/common/constant'

// 扩展 BillVO 类型，添加同步状态
interface LocalBillVO extends BillVO {
  syncStatus: 'init' | 'success' | 'failed'
}

export const useBillStore = defineStore('bill', () => {
  const categoryListMap = ref<{ expense: CategoryVO[], income: CategoryVO[] }>(
    { expense: [], income: [] },
  )
  const paymentMethodList = ref<PaymentMethodVO[]>([])

  const localBillList = ref<LocalBillVO[]>([])

  const isInitialzed = ref(false)

  const user = useUserStore()

  const hasPendingBills = computed(() => localBillList.value.length > 0)

  const pendingSyncCount = computed(() =>
    localBillList.value.filter(bill => bill.syncStatus === 'init').length,
  )

  const syncedCount = computed(() =>
    localBillList.value.filter(bill => bill.syncStatus === 'success').length,
  )

  const lastSyncTime = ref<string>('')

  async function initBillData() {
    if (isInitialzed.value) {
      return
    }
    try {
      await Promise.all([fetchCategoryList(), fetchPaymentMethodList()])
      isInitialzed.value = true
    }
    catch (error) {
      console.error('Failed to initialize bill data:', error)
    }
  }

  async function fetchPaymentMethodList() {
    if (user.isLoggedIn) {
      const res = await Apis.bill.getPaymentMethods()
      if (res.success) {
        paymentMethodList.value = res.data || []
      }
      return
    }
    paymentMethodList.value = LOCAL_PAYMENT_METHOD_LIST
  }

  async function fetchCategoryList() {
    if (user.isLoggedIn) {
      const res = await Apis.bill.getCategories({ params: {} })
      if (res.success) {
        categoryListMap.value = {
          expense: res.data?.filter(item => item.type === 'expense') || [],
          income: res.data?.filter(item => item.type === 'income') || [],
        }
      }
      return
    }
    categoryListMap.value = {
      expense: LOCAL_PAY_CATEGORY_LIST.filter(item => item.type === 'expense'),
      income: LOCAL_PAY_CATEGORY_LIST.filter(item => item.type === 'income'),
    }
  }

  function getCategoryList(type?: string) {
    if (!type) {
      return Object.values(categoryListMap.value).flat()
    }
    return categoryListMap.value[type as keyof typeof categoryListMap.value]
  }

  function getPaymentMethodList() {
    return paymentMethodList.value
  }

  /**
   * 添加本地账单
   * @param bill
   */
  function addLocalBill(bill: AddBillDTO) {
    const paymentMethod = paymentMethodList.value.find(item => item.id === bill.paymentMethodId)
    const category = categoryListMap.value[bill.type as keyof typeof categoryListMap.value].find(item => item.id === bill.categoryId)
    localBillList.value.push({
      ...bill,
      amount: Number(bill.amount || 0).toFixed(2),
      time: dayjs(bill.time).format('YYYY-MM-DD HH:mm'),
      id: uuid(),
      paymentMethod,
      category,
      syncStatus: 'init',
    })
  }

  /**
   * 更新本地账单
   * @param bill
   */
  function updateLocalBill(bill: UpdateBillDTO) {
    const index = localBillList.value.findIndex(item => item.id === bill.id)
    if (index !== -1) {
      const paymentMethod = paymentMethodList.value.find(item => item.id === bill.paymentMethodId)
      const category = categoryListMap.value[bill.type as keyof typeof categoryListMap.value].find(item => item.id === bill.categoryId)
      const newBill = {
        ...localBillList.value[index],
        ...bill,
      }
      newBill.amount = Number(bill.amount || 0).toFixed(2)
      newBill.paymentMethod = paymentMethod
      newBill.category = category
      newBill.time = dayjs(newBill.time).format('YYYY-MM-DD HH:mm')
      localBillList.value[index] = newBill
    }
  }

  /**
   * 删除本地账单
   * @param id
   */
  function deleteLocalBill(id: string) {
    const index = localBillList.value.findIndex(item => item.id === id)
    if (index !== -1) {
      localBillList.value.splice(index, 1)
    }
  }

  /**
   * 清空本地账单
   */
  function clearLocalBills() {
    localBillList.value = []
  }

  /**
   * 同步本地账单到服务器
   * 仅同步 syncStatus 为 'init' 的账单
   */
  async function syncLocalBillToServer(): Promise<{ success: boolean, successCount: number }> {
    const pendingBills = localBillList.value.filter(bill => bill.syncStatus === 'init')
    if (pendingBills.length === 0) {
      return { success: true, successCount: 0 }
    }

    try {
      // 映射 LocalBill 为 AddBillDTO 格式
      const bills = pendingBills.map((bill) => {
        const name = bill.name!

        return {
          name,
          categoryId: bill.category?.id || '',
          type: bill.type!,
          amount: bill.amount!,
          time: bill.time!,
          paymentMethodId: bill.paymentMethod?.id,
          remark: bill.remark,
        }
      })
      const response = await Apis.bill.batchCreate({ data: { bills } })

      if (response.code === 200) {
        const successCount = response.data || 0

        // 按时间顺序标记成功同步的账单
        const sortedPendingBills = [...pendingBills].sort((a, b) =>
          dayjs(a.time).valueOf() - dayjs(b.time).valueOf(),
        )
        for (let i = 0; i < successCount && i < sortedPendingBills.length; i++) {
          const bill = localBillList.value.find(item => item.id === sortedPendingBills[i].id)
          if (bill) {
            bill.syncStatus = 'success'
          }
        }

        lastSyncTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
        return { success: true, successCount }
      }

      console.error('同步本地账单失败:', response.message)
      return { success: false, successCount: 0 }
    }
    catch (error) {
      console.error('同步本地账单失败:', error)
      return { success: false, successCount: 0 }
    }
  }

  async function getMonthlyBillStats(month: string, familyId?: string): Promise<BillMonthlyStatsVO | undefined> {
    if (user.isLoggedIn) {
      const res = await Apis.bill.getMonthlyStats({ params: { month, familyId } })
      return res.data
    }

    const expense = localBillList.value
      .filter(bill => bill.type === 'expense' && dayjs(bill.time).isSame(dayjs(month), 'month'))
      .reduce((acc, bill) => add(acc, Number(bill.amount)), 0)
    const income = localBillList.value
      .filter(bill => bill.type === 'income' && dayjs(bill.time).isSame(dayjs(month), 'month'))
      .reduce((acc, bill) => add(acc, Number(bill.amount)), 0)
    const balance = subtract(income, expense)

    return {
      expense: expense.toFixed(2),
      income: income.toFixed(2),
      balance: balance.toFixed(2),
    }
  }

  return {
    getMonthlyBillStats,
    localBillList,
    hasPendingBills,
    pendingSyncCount,
    syncedCount,
    lastSyncTime,
    addLocalBill,
    updateLocalBill,
    deleteLocalBill,
    clearLocalBills,
    syncLocalBillToServer,
    categoryListMap,
    paymentMethodList,
    isInitialzed,
    initBillData,
    getCategoryList,
    getPaymentMethodList,
  }
})
