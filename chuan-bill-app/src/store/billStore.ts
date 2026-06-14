import type { AddBillDTO, BillMonthlyStatsVO, BillVO, CategoryVO, PaymentMethodVO, UpdateBillDTO } from '@/api/globals'
import dayjs from 'dayjs'
import { add, subtract } from 'mathjs'
import { LOCAL_PAY_CATEGORY_LIST, LOCAL_PAYMENT_METHOD_LIST } from '@/constant/bill'
import { EVENTS } from '@/constant/events'

let _localIdCounter = 0
function generateLocalId() {
  return `${Date.now().toString(36)}_${(++_localIdCounter).toString(36)}`
}

// 扩展 BillVO 类型，添加同步状态
export interface LocalBillVO extends BillVO {
  syncStatus: 'init' | 'success' | 'failed'
  local: boolean
}

export const useBillStore = defineStore('bill', () => {
  const categoryListMap = ref<{ expense: CategoryVO[], income: CategoryVO[] }>(
    { expense: [], income: [] },
  )
  const paymentMethodList = ref<PaymentMethodVO[]>([])

  const localBillList = ref<LocalBillVO[]>([])

  const isInitialzed = ref(false)

  const user = useUserStore()

  const hasLocalBills = computed(() => localBillList.value.length > 0)

  const pendingSyncCount = computed(() =>
    localBillList.value.filter(bill => bill.syncStatus === 'init').length,
  )

  const syncedCount = computed(() =>
    localBillList.value.filter(bill => bill.syncStatus === 'success').length,
  )

  const failedSyncCount = computed(() =>
    localBillList.value.filter(bill => bill.syncStatus === 'failed').length,
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

  async function refreshBillData() {
    try {
      await Promise.all([fetchCategoryList(), fetchPaymentMethodList()])
    }
    catch (error) {
      console.error('Failed to refresh bill data:', error)
    }
  }

  /**
   * 清空服务器数据，切换回本地数据
   * 在用户退出登录时调用
   */
  function clearServerData() {
    isInitialzed.value = false
    // 重新获取本地数据
    categoryListMap.value = {
      expense: LOCAL_PAY_CATEGORY_LIST.filter(item => item.type === 'expense'),
      income: LOCAL_PAY_CATEGORY_LIST.filter(item => item.type === 'income'),
    }
    paymentMethodList.value = LOCAL_PAYMENT_METHOD_LIST
  }

  // 监听用户退出登录事件
  eventBus.on(EVENTS.USER.LOGOUT, () => {
    clearServerData()
  })

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

  // ==================== 类目 CRUD ====================

  async function addCategory(data: { name: string, icon: string, type: string }) {
    const res = await Apis.bill.addCategory({ data })
    if (res.success) {
      await fetchCategoryList()
    }
    return res
  }

  async function updateCategory(id: string, data: { name?: string, icon?: string }) {
    const res = await Apis.bill.updateCategory({ pathParams: { id }, data })
    if (res.success) {
      await fetchCategoryList()
    }
    return res
  }

  async function deleteCategory(id: string) {
    const res = await Apis.bill.deleteCategory({ pathParams: { id } })
    if (res.success) {
      await fetchCategoryList()
    }
    return res
  }

  async function sortCategories(ids: string[]) {
    const res = await Apis.bill.sortCategories({ data: { ids } })
    if (res.success) {
      await fetchCategoryList()
    }
    return res
  }

  // ==================== 支付方式 CRUD ====================

  async function addPaymentMethod(data: { name: string, icon: string }) {
    const res = await Apis.bill.addPaymentMethod({ data })
    if (res.success) {
      await fetchPaymentMethodList()
    }
    return res
  }

  async function updatePaymentMethod(id: string, data: { name?: string, icon?: string }) {
    const res = await Apis.bill.updatePaymentMethod({ pathParams: { id }, data })
    if (res.success) {
      await fetchPaymentMethodList()
    }
    return res
  }

  async function deletePaymentMethod(id: string) {
    const res = await Apis.bill.deletePaymentMethod({ pathParams: { id } })
    if (res.success) {
      await fetchPaymentMethodList()
    }
    return res
  }

  async function sortPaymentMethods(ids: string[]) {
    const res = await Apis.bill.sortPaymentMethods({ data: { ids } })
    if (res.success) {
      await fetchPaymentMethodList()
    }
    return res
  }

  // ==================== Filter 排序 Getter ====================

  function isOtherItem(item: { name?: string, isDefault?: boolean }) {
    return item.isDefault && item.name?.includes('其他')
  }

  function getCategoryListForFilter(type?: string) {
    const list = getCategoryList(type)
    const others = list.filter(item => isOtherItem(item))
    const nonOthers = list.filter(item => !isOtherItem(item))
    return [...nonOthers, ...others]
  }

  function getPaymentMethodListForFilter() {
    const list = paymentMethodList.value
    const others = list.filter(item => isOtherItem(item))
    const nonOthers = list.filter(item => !isOtherItem(item))
    return [...nonOthers, ...others]
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
      id: generateLocalId(),
      paymentMethod,
      category,
      syncStatus: 'init',
      local: true,
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
      newBill.syncStatus = 'init'
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
  async function syncLocalBillToServer(): Promise<{ success: boolean, successCount: number, failedCount: number }> {
    const pendingBills = localBillList.value.filter(bill => bill.syncStatus === 'init')
    if (pendingBills.length === 0) {
      return { success: true, successCount: 0, failedCount: 0 }
    }

    try {
      const bills = pendingBills.map(bill => ({
        name: bill.name!,
        categoryId: bill.category?.id || '',
        type: bill.type!,
        amount: bill.amount!,
        time: bill.time!,
        paymentMethodId: bill.paymentMethod?.id,
        remark: bill.remark,
      }))
      const response = await Apis.bill.batchCreate({ data: { bills } })

      if (response.success && response.data) {
        const result = response.data

        // 根据 details 精确标记每条账单状态
        for (const detail of result.details || []) {
          if (detail.index == null)
            continue
          const pendingBill = pendingBills[detail.index]
          if (!pendingBill)
            continue
          const bill = localBillList.value.find(item => item.id === pendingBill.id)
          if (bill) {
            bill.syncStatus = detail.status === 'SUCCESS' ? 'success' : 'failed'
          }
        }

        lastSyncTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
        return {
          success: true,
          successCount: result.successCount || 0,
          failedCount: result.failedCount || 0,
        }
      }

      console.error('同步本地账单失败:', response.message)
      return { success: false, successCount: 0, failedCount: pendingBills.length }
    }
    catch (error) {
      console.error('同步本地账单失败:', error)
      return { success: false, successCount: 0, failedCount: pendingBills.length }
    }
  }

  /**
   * 重试失败的同步账单
   * 将 'failed' 状态重置为 'init'，然后重新同步
   */
  async function retryFailedSync() {
    localBillList.value
      .filter(bill => bill.syncStatus === 'failed')
      .forEach(bill => bill.syncStatus = 'init')
    return syncLocalBillToServer()
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
    hasLocalBills,
    pendingSyncCount,
    syncedCount,
    failedSyncCount,
    lastSyncTime,
    addLocalBill,
    updateLocalBill,
    deleteLocalBill,
    clearLocalBills,
    syncLocalBillToServer,
    retryFailedSync,
    categoryListMap,
    paymentMethodList,
    isInitialzed,
    initBillData,
    refreshBillData,
    clearServerData,
    getCategoryList,
    getPaymentMethodList,
    // 类目 CRUD
    addCategory,
    updateCategory,
    deleteCategory,
    sortCategories,
    // 支付方式 CRUD
    addPaymentMethod,
    updatePaymentMethod,
    deletePaymentMethod,
    sortPaymentMethods,
    // Filter getter
    getCategoryListForFilter,
    getPaymentMethodListForFilter,
  }
})
