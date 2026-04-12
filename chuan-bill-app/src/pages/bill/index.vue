<script setup lang="ts">
import type { BillListDTO, BillVO } from '@/api/globals'
import type { LocalBill } from '@/store/localBill'
import dayjs from 'dayjs'

definePage({
  name: 'bill',
  layout: 'tabbar',
  type: 'home',
  style: {
    navigationBarTitleText: '账单',
    enablePullDownRefresh: true,
    onReachBottomDistance: 50,
  },
})

// 鉴权检查
const { isLoggedIn, requireAuth, showLoginPopup } = useAuthCheck()
const localBillStore = useLocalBillStore()
const toast = useGlobalToast()

const searchValue = ref('')
const filterParams = ref<Optional<BillListDTO>>()
const isFiltered = ref()
const showFilterModal = ref(false)
const showQuickBillModal = ref(false)
const showBillDetailModal = ref(false)
const currentBill = ref<BillVO | LocalBill>()
const isEditBill = ref(false)
const quickBillSource = ref<'manual' | 'ocr' | 'voice'>('manual')

const safeAreaBottomHeight = uni.getWindowInfo().safeAreaInsets.bottom

// 账单列表数据
const billList = ref<BillVO[]>([])
const page = ref(1)
const loadingMoreStatus = ref<'loading' | 'finished' | 'error'>()

// 获取账单列表
async function getBillList() {
  if (!isLoggedIn.value) {
    // 未登录时使用本地数据
    loadingMoreStatus.value = 'finished'
    return
  }

  const res = await Apis.bill.getPageBillList({
    params: {
      page: page.value,
      size: 10,
      keyword: searchValue.value,
      ...filterParams.value,
    },
  })
  if (res.success) {
    billList.value = page.value === 1 ? (res.data?.records || []) : [...billList.value || [], ...res.data?.records || []]
    if (page.value * 10 >= (res.data?.total || 0)) {
      loadingMoreStatus.value = 'finished'
    }
    else {
      loadingMoreStatus.value = 'loading'
    }
  }
  else {
    loadingMoreStatus.value = 'error'
  }
}

function refresh() {
  page.value = 1
  return getBillList()
}

function loadMore() {
  if (!isLoggedIn.value)
    return
  page.value++
  getBillList()
}

function submitFilter(result: Optional<BillListDTO>, filtered?: boolean) {
  filterParams.value = result
  isFiltered.value = filtered
  showFilterModal.value = false
  refresh()
}

function onClickBill(bill: BillVO) {
  currentBill.value = bill
  showBillDetailModal.value = true
}

// 添加账单 - 打开快速记账弹框
function addBill() {
  quickBillSource.value = 'manual'
  isEditBill.value = false
  currentBill.value = undefined
  showQuickBillModal.value = true
}

// 添加账单 - OCR/语音需要登录
function addBillWithAuth(source: 'ocr' | 'voice') {
  requireAuth(() => {
    quickBillSource.value = source
    isEditBill.value = false
    currentBill.value = undefined
    showQuickBillModal.value = true
  })
}

function editBill(bill: BillVO) {
  isEditBill.value = true
  currentBill.value = bill
  showQuickBillModal.value = true
}

function submitBill() {
  showQuickBillModal.value = false
  refresh()
}

// 提交本地账单（游客模式）
function submitLocalBill(billData: any) {
  localBillStore.addBill({
    amount: Number(billData.amount),
    type: billData.type as 'income' | 'expense',
    categoryId: billData.categoryId,
    categoryName: billData.categoryName || '未分类',
    payMethodId: billData.paymentMethodId,
    payMethodName: billData.payMethodName,
    remark: billData.remark,
    billDate: dayjs(billData.time).format('YYYY-MM-DD'),
  })
  toast.success('记账成功（本地）')
  showQuickBillModal.value = false
}

function isSameMonth(currentTime?: string, comparedTime?: string) {
  if (!currentTime || !comparedTime) {
    return false
  }
  return dayjs(currentTime).isSame(dayjs(comparedTime), 'month')
}

// 本地账单月份分组显示
function isSameMonthLocal(currentDate?: string, comparedDate?: string) {
  if (!currentDate || !comparedDate) {
    return false
  }
  return dayjs(currentDate).isSame(dayjs(comparedDate), 'month')
}

// 计算本地账单某月统计
function getLocalMonthStats(month: string) {
  const monthBills = localBillStore.bills.filter(b => b.billDate.startsWith(month))
  const income = monthBills.filter(b => b.type === 'income').reduce((sum, b) => sum + b.amount, 0)
  const expense = monthBills.filter(b => b.type === 'expense').reduce((sum, b) => sum + b.amount, 0)
  return {
    income: income.toFixed(2),
    expense: expense.toFixed(2),
    balance: (income - expense).toFixed(2),
  }
}

// 跳转到登录页
function goToLogin() {
  showLoginPopup.value = true
}

onLoad(() => {
  refresh()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  loadMore()
})

// 监听登录状态变化
watch(isLoggedIn, (newVal) => {
  if (newVal) {
    // 登录后刷新数据
    refresh()
  }
})
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 搜索区域 -->
    <wd-sticky>
      <view class="box-border w-100vw border-b border-[var(--wot-border-color)] px-3">
        <view class="flex items-center gap-2">
          <wd-search
            v-model="searchValue" placeholder="账单名称或备注" hide-cancel
            custom-class="flex-1 rounded-xl border border-solid border-[var(--wot-color-border)] dark:border-gray-600"
            @search="refresh"
            @clear="refresh"
          />
          <view
            class="relative flex items-center justify-center border border-[var(--wot-color-border)] rounded-xl border-solid bg-white p-2 text-gray-600 transition-all active:scale-95 dark:border-gray-600 dark:bg-[var(--wot-dark-background2)]"
            :class="isFiltered && 'text-primary'"
            @click="showFilterModal = true"
          >
            <view v-if="isFiltered" class="absolute right-1 top-1 h-2 w-2 rounded-full bg-primary" />
            <view class="i-lucide:filter" />
          </view>
        </view>
      </view>
    </wd-sticky>

    <!-- 未登录时的功能入口 -->
    <view v-if="!isLoggedIn" class="mx-3 flex gap-2">
      <view
        class="flex flex-1 items-center justify-center gap-1 rounded-xl bg-white py-3 shadow-sm transition-all active:scale-95 dark:bg-[var(--wot-dark-background2)]"
        @click="addBill"
      >
        <view class="i-lucide:square-pen text-primary" />
        <text class="text-sm text-gray-700 dark:text-gray-300">
          手动记账
        </text>
      </view>
      <view
        class="relative flex flex-1 items-center justify-center gap-1 rounded-xl bg-white py-3 shadow-sm transition-all active:scale-95 dark:bg-[var(--wot-dark-background2)]"
        @click="addBillWithAuth('ocr')"
      >
        <view class="i-carbon-locked absolute right-2 top-2 h-3 w-3 text-gray-400" />
        <view class="i-lucide:camera text-gray-500" />
        <text class="text-sm text-gray-500">
          图片识别
        </text>
      </view>
      <view
        class="relative flex flex-1 items-center justify-center gap-1 rounded-xl bg-white py-3 shadow-sm transition-all active:scale-95 dark:bg-[var(--wot-dark-background2)]"
        @click="addBillWithAuth('voice')"
      >
        <view class="i-carbon-locked absolute right-2 top-2 h-3 w-3 text-gray-400" />
        <view class="i-lucide:mic text-gray-500" />
        <text class="text-sm text-gray-500">
          语音识别
        </text>
      </view>
    </view>

    <!-- 分页列表区域 - 已登录 -->
    <view v-if="isLoggedIn" class="mb-9 box-border flex flex-col gap-3 px-3 pb-3">
      <template v-for="(bill, index) in billList" :key="bill.id">
        <BillSection v-if="!isSameMonth(bill.time, billList?.[index - 1]?.time)" :month="dayjs(bill.time!).format('YYYY-MM')" custom-class="mt-3" />
        <BillItem :bill="bill" @click="onClickBill(bill)" />
      </template>
      <wd-loadmore :state="loadingMoreStatus" finished-text="没有更多数据了" custom-class="h-8!" @reload="refresh" />
    </view>

    <!-- 本地列表区域 - 未登录 -->
    <view v-else class="mb-9 box-border flex flex-col gap-3 px-3 pb-3">
      <template v-if="localBillStore.bills.length > 0">
        <template v-for="(bill, index) in localBillStore.bills" :key="bill.localId">
          <!-- 月份分隔 -->
          <view v-if="!isSameMonthLocal(bill.billDate, localBillStore.bills[index - 1]?.billDate)" class="mt-3 flex items-center justify-between">
            <text class="font-500">
              {{ dayjs(bill.billDate).format('YYYY-MM') }}
            </text>
            <view class="flex items-center gap-3 text-xs">
              <view class="flex items-center gap-1">
                <view class="h-3 w-3 flex items-center justify-center rounded-full bg-red-400">
                  <text class="i-icon-park-outline:expenses h-2 w-2 text-white" />
                </view>
                <text class="text-red-400">
                  {{ getLocalMonthStats(dayjs(bill.billDate).format('YYYY-MM')).expense }}
                </text>
              </view>
              <view class="flex items-center gap-1">
                <view class="h-3 w-3 flex items-center justify-center rounded-full bg-green-500">
                  <text class="i-icon-park-outline:income h-2 w-2 text-white" />
                </view>
                <text class="text-green-500">
                  {{ getLocalMonthStats(dayjs(bill.billDate).format('YYYY-MM')).income }}
                </text>
              </view>
            </view>
          </view>
          <!-- 本地账单项 -->
          <view
            class="box-border w-full flex items-start gap-4 rounded-xl bg-white p-4 shadow-sm dark:bg-[--wot-dark-background2]"
            @click="toast.show('本地账单：登录后自动同步')"
          >
            <view class="flex flex-1 gap-4">
              <!-- 左侧：图标 -->
              <view
                class="h-5 w-5 flex items-center justify-center rounded-xl p-3"
                :class="bill.type === 'expense' ? 'bg-red-100 text-red-400' : 'bg-green-100 text-green-500'"
              >
                <text class="i-lucide:receipt h-4 w-4" />
              </view>
              <!-- 中间：名称、时间、分类 -->
              <view class="flex flex-col gap-2">
                <view class="font-500">
                  {{ bill.categoryName }}
                </view>
                <view class="flex gap-2 text-xs text-gray-500">
                  <text>{{ bill.billDate }}</text>
                  <text v-if="bill.payMethodName">
                    {{ bill.payMethodName }}
                  </text>
                </view>
              </view>
            </view>
            <!-- 右侧：金额 -->
            <text class="text-lg" :class="bill.type === 'expense' ? 'text-red-400' : 'text-green-500'">
              {{ bill.type === 'expense' ? '-' : '+' }} {{ bill.amount }}
            </text>
          </view>
        </template>
      </template>
      <!-- 空状态 -->
      <view v-else class="mt-20 flex flex-col items-center justify-center gap-4">
        <view class="h-20 w-20 flex items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
          <view class="i-lucide:receipt h-10 w-10 text-gray-400" />
        </view>
        <text class="text-sm text-gray-500">
          暂无账单记录
        </text>
        <text class="text-xs text-gray-400">
          点击上方"手动记账"开始记录第一笔
        </text>
      </view>
      <wd-loadmore state="finished" finished-text="没有更多数据了" custom-class="h-8!" />
    </view>

    <!-- 本地数据提示条 -->
    <view
      v-if="!isLoggedIn && localBillStore.hasPendingBills"
      class="fixed bottom-20 left-3 right-3 z-50 flex items-center justify-between rounded-xl bg-primary/90 px-4 py-3 text-white shadow-lg backdrop-blur-sm"
    >
      <view class="flex items-center gap-2">
        <view class="i-lucide:cloud-upload h-4 w-4" />
        <text class="text-sm">
          登录后数据云端备份，不怕丢失
        </text>
      </view>
      <wd-button type="default" size="small" custom-class="!min-w-[4rem]" @click="goToLogin">
        去登录
      </wd-button>
    </view>

    <!-- 账单详情弹框 -->
    <BillDetailModal v-if="currentBill && isLoggedIn" v-model="showBillDetailModal" :bill="currentBill as BillVO" @delete="refresh" @update="editBill" />
    <!-- 筛选弹框 -->
    <FilterModal v-if="isLoggedIn" v-model="showFilterModal" @submit="submitFilter" />
    <!-- FAB 按钮 - 仅已登录显示 -->
    <wd-fab v-if="isLoggedIn" draggable :expandable="false" :gap="{ bottom: 70 + safeAreaBottomHeight, right: 20 }" @click="addBill" />
    <!-- 快速记账模态框 -->
    <QuickBillModal
      v-model="showQuickBillModal"
      :is-edit="isEditBill"
      :bill="currentBill as BillVO"
      :source="quickBillSource"
      :is-guest-mode="!isLoggedIn"
      @success="submitBill"
      @local-submit="submitLocalBill"
    />
  </view>
</template>

<style lang="scss" scoped>
:deep(.wot-theme-dark .wd-search__cover) {
  background-color: transparent;
}

:deep(.wot-theme-dark .wd-search__block) {
  background-color: transparent;
}
:deep(.wd-loadmore .wd-divider){
  @apply m-0!
}
</style>
