<script setup lang="ts">
import type { AddBillDTO, BillListDTO, BillVO, UpdateBillDTO } from '@/api/globals'
import dayjs from 'dayjs'
import isSameOrAfter from 'dayjs/plugin/isSameOrAfter'
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore'
import { EVENTS } from '@/constant/events'
import { eventBus } from '@/utils/eventBus'
import BillDetailModal from './components/BillDetailModal.vue'
import BillItem from './components/BillItem.vue'
import BillSection from './components/BillSection.vue'
import FilterModal from './components/FilterModal.vue'
import QuickBillModal from './components/QuickBillModal.vue'

dayjs.extend(isSameOrAfter)
dayjs.extend(isSameOrBefore)

definePage({
  name: 'bill',
  layout: 'tabbar',
  type: 'home',
  style: {
    navigationBarTitleText: '我的账单',
    enablePullDownRefresh: true,
  },
})

// 鉴权检查
const user = useUserStore()
const billStore = useBillStore()
const statisStore = usePersonalStatisticsStore()
const message = useGlobalMessage()

const searchValue = ref('')
const filterParams = ref<Optional<BillListDTO>>()
const isFiltered = ref()
const showFilterModal = ref(false)
const showQuickBillModal = ref(false)
const showBillDetailModal = ref(false)
const currentBill = ref<BillVO>()
const isEditBill = ref(false)
const quickBillSource = ref<'manual' | 'ocr' | 'voice'>('manual')

const safeAreaBottomHeight = uni.getWindowInfo().safeAreaInsets.bottom

// 账单列表数据
const billList = ref<BillVO[]>([])
const page = ref(1)
const loadingMoreStatus = ref<'loading' | 'finished' | 'error'>()

// 获取账单列表
async function getBillList() {
  const queryParams = {
    keyword: searchValue.value,
    ...filterParams.value,
  }

  if (!user.isLoggedIn) {
    // 未登录时使用本地数据
    loadingMoreStatus.value = 'finished'
    billList.value = billStore.localBillList.filter(
      (item) => {
        const { keyword, minAmount, maxAmount, categoryId, paymentMethodId, startDate, endDate, type } = queryParams
        let filtered: boolean | undefined = true
        if (keyword) {
          filtered = filtered && (item.name?.includes(keyword) || item.remark?.includes(keyword))
        }
        if (type) {
          filtered = filtered && item.type === type
        }
        if (minAmount) {
          filtered = filtered && Number(item.amount) >= Number(minAmount)
        }
        if (maxAmount) {
          filtered = filtered && Number(item.amount) <= Number(maxAmount)
        }
        if (categoryId) {
          filtered = filtered && item.category?.id === categoryId
        }
        if (paymentMethodId) {
          filtered = filtered && item.paymentMethod?.id === paymentMethodId
        }
        if (startDate) {
          filtered = filtered && dayjs(item.time).isSameOrAfter(dayjs(startDate).startOf('D'))
        }
        if (endDate) {
          filtered = filtered && dayjs(item.time).isSameOrBefore(dayjs(endDate).endOf('D'))
        }
        return filtered
      },
    )
    return
  }

  const res = await Apis.bill.getPageBillList({
    params: {
      page: page.value,
      size: 10,
      ...queryParams,
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
  if (!user.isLoggedIn)
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
  // 判断账目条数是否大于1000条，如果大于，则需要登录
  if (billStore.localBillList.length >= 1000) {
    message.confirm({ title: '提示', msg: '账目条数已达到上限，登录后解锁更多记账条数', confirmButtonText: '去登录', cancelButtonText: '暂不登录', success: (res) => {
      if (res.action === 'confirm') {
        goToLogin()
      }
    } })
    return
  }
  currentBill.value = undefined
  quickBillSource.value = 'manual'
  isEditBill.value = false
  showQuickBillModal.value = true
}

function editBill(bill: BillVO) {
  isEditBill.value = true
  currentBill.value = bill
  showBillDetailModal.value = false
  showQuickBillModal.value = true
}

function submitBill(bill: AddBillDTO | UpdateBillDTO) {
  statisStore.fetchAll(dayjs(bill.time).format('YYYY-MM'))
  showQuickBillModal.value = false
  currentBill.value = undefined
  refresh()
}

function isSameMonth(currentTime?: string, comparedTime?: string) {
  if (!currentTime || !comparedTime) {
    return false
  }
  return dayjs(currentTime).isSame(dayjs(comparedTime), 'month')
}

// 跳转到登录页
function goToLogin() {
  user.showLoginPopup = true
}

async function openBillById(id: string) {
  try {
    const res = await Apis.bill.getBillDetail({ params: { id } })
    if (res.success && res.data) {
      currentBill.value = res.data
      showBillDetailModal.value = true
    }
  }
  catch {
    useGlobalToast().error('无法查看该账单')
  }
}

onLoad((options) => {
  refresh()
  if (options?.id) {
    openBillById(options.id)
  }
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  loadMore()
})

// 监听登录状态变化
watch(() => user.isLoggedIn, () => {
  refresh()
})

// 监听家庭数据变化事件（家庭账单相关）
function handleFamilyUpdated() {
  refresh()
}

onMounted(() => {
  eventBus.on(EVENTS.FAMILY.UPDATED, handleFamilyUpdated)
})

onUnmounted(() => {
  eventBus.off(EVENTS.FAMILY.UPDATED, handleFamilyUpdated)
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
            custom-class="flex-1 rounded-xl border border-solid border-[var(--wot-color-border)] dark:border-none"
            @search="refresh"
            @clear="refresh"
          />
          <view
            class="relative h-5 flex items-center justify-center border border-[var(--wot-color-border)] rounded-xl border-solid bg-white px-2.5 py-2 text-gray-600 transition-all active:scale-95 dark:border-none dark:bg-[var(--wot-dark-background4)]"
            :class="isFiltered && 'text-primary'"
            @click="showFilterModal = true"
          >
            <view v-if="isFiltered" class="absolute right-1 top-1 h-2 w-2 rounded-full bg-primary" />
            <view class="i-lucide:filter" />
          </view>
        </view>
      </view>
    </wd-sticky>

    <!-- 分页列表区域 - 已登录 -->
    <view v-if="billList.length" class="mb-9 box-border flex flex-col gap-3 px-3 pb-3">
      <template v-for="(bill, index) in billList" :key="bill.id">
        <BillSection v-if="!isSameMonth(bill.time, billList?.[index - 1]?.time)" :key="bill.amount" :month="dayjs(bill.time!).format('YYYY-MM')" custom-class="mt-3" />
        <BillItem :bill="bill" @click="onClickBill(bill)" />
      </template>
      <wd-loadmore :state="loadingMoreStatus" finished-text="没有更多数据了" custom-class="h-8!" @reload="refresh" />
    </view>

    <view v-else class="mt-10vh flex flex-col items-center justify-center gap-5">
      <wd-status-tip tip="您还没有任何账单数据，快来记一笔吧！">
        <template #image>
          <image mode="aspectFit" class="h-30 w-80" src="https://chuan-bill-cdn.samoy.site/default/nodata.svg" />
        </template>
      </wd-status-tip>
      <wd-button type="primary" @click="addBill">
        开始记账
      </wd-button>
    </view>

    <!-- 账单详情弹框 -->
    <BillDetailModal v-if="currentBill" v-model="showBillDetailModal" :bill="currentBill!" @delete="refresh" @update="editBill" />
    <!-- 筛选弹框 -->
    <FilterModal v-model="showFilterModal" @submit="submitFilter" />
    <!-- FAB 按钮 - 仅已登录显示 -->
    <wd-fab draggable :expandable="false" :gap="{ bottom: 70 + safeAreaBottomHeight, right: 20 }" @click="addBill" />
    <!-- 快速记账模态框 -->
    <QuickBillModal
      v-model="showQuickBillModal"
      :is-edit="isEditBill"
      :bill="currentBill"
      :source="quickBillSource"
      @success="submitBill"
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
