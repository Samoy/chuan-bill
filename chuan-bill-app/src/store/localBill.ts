import { defineStore } from 'pinia'

/**
 * 本地账单接口
 * 用于存储未登录用户创建的账单，待登录后同步到服务器
 */
export interface LocalBill {
  /** 本地 UUID */
  localId: string
  /** 金额 */
  amount: number
  /** 类型：收入/支出 */
  type: 'income' | 'expense'
  /** 分类 ID */
  categoryId?: number
  /** 分类名称 */
  categoryName: string
  /** 支付方式 ID */
  payMethodId?: number
  /** 支付方式名称 */
  payMethodName?: string
  /** 备注 */
  remark?: string
  /** 账单日期 YYYY-MM-DD */
  billDate: string
  /** 创建时间 ISO timestamp */
  createdAt: string
}

/**
 * 本地账单存储
 * 用于离线记账，登录后同步到服务器
 */
export const useLocalBillStore = defineStore('localBill', {
  state: () => ({
    bills: [] as LocalBill[],
  }),

  getters: {
    /**
     * 是否有待同步的账单
     */
    hasPendingBills: state => state.bills.length > 0,

    /**
     * 按年月分组的账单
     * @returns Record<string, LocalBill[]> key 为 YYYY-MM 格式
     */
    billsByMonth: (state): Record<string, LocalBill[]> => {
      const grouped: Record<string, LocalBill[]> = {}
      for (const bill of state.bills) {
        const month = bill.billDate.substring(0, 7) // YYYY-MM
        if (!grouped[month]) {
          grouped[month] = []
        }
        grouped[month].push(bill)
      }
      return grouped
    },
  },

  actions: {
    /**
     * 生成本地 ID
     * 使用时间戳 + 随机数方案
     */
    _generateLocalId(): string {
      return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
    },

    /**
     * 添加本地账单
     * @param bill 账单数据（不包含 localId 和 createdAt）
     */
    addBill(bill: Omit<LocalBill, 'localId' | 'createdAt'>): void {
      const newBill: LocalBill = {
        ...bill,
        localId: this._generateLocalId(),
        createdAt: new Date().toISOString(),
      }
      this.bills.push(newBill)
    },

    /**
     * 更新本地账单
     * @param localId 本地账单 ID
     * @param data 要更新的数据
     */
    updateBill(localId: string, data: Partial<Omit<LocalBill, 'localId' | 'createdAt'>>): void {
      const index = this.bills.findIndex(bill => bill.localId === localId)
      if (index !== -1) {
        this.bills[index] = {
          ...this.bills[index],
          ...data,
        }
      }
    },

    /**
     * 删除本地账单
     * @param localId 本地账单 ID
     */
    deleteBill(localId: string): void {
      const index = this.bills.findIndex(bill => bill.localId === localId)
      if (index !== -1) {
        this.bills.splice(index, 1)
      }
    },

    /**
     * 同步本地账单到服务器
     * 需要在登录后调用
     */
    async syncToServer(): Promise<boolean> {
      if (!this.hasPendingBills) {
        return true
      }

      try {
        // 映射 LocalBill 为 AddBillDTO 格式
        const bills = this.bills.map((bill) => {
          // name：优先用 remark，否则用 categoryName，兜底 "快速记账"
          const name = bill.remark || bill.categoryName || '快速记账'

          return {
            name,
            categoryId: bill.categoryId ? String(bill.categoryId) : '',
            type: bill.type,
            amount: bill.amount,
            time: `${bill.billDate} 12:00`,
            paymentMethodId: bill.payMethodId ? String(bill.payMethodId) : undefined,
            remark: bill.remark,
          }
        })

        // 使用 alovaInstance 直接 Post 到 /bill/batchCreate
        const response = await alovaInstance.Post<{ code: number, message: string, data: number }>(
          '/bill/batchCreate',
          { bills },
        ).send()

        if (response.code === 200) {
          this.clearAll()
          return true
        }

        console.error('同步本地账单失败:', response.message)
        return false
      }
      catch (error) {
        console.error('同步本地账单失败:', error)
        return false
      }
    },

    /**
     * 清空所有本地账单
     */
    clearAll(): void {
      this.bills = []
    },
  },
})
