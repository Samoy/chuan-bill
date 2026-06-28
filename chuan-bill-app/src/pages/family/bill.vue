<script setup lang="ts">
import type { BillListDTO, BillVO } from '@/api/globals'
import dayjs from 'dayjs'
import isSameOrAfter from 'dayjs/plugin/isSameOrAfter'
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore'
import BillDetailModal from '@/pages/bill/components/BillDetailModal.vue'
import BillItem from '@/pages/bill/components/BillItem.vue'
import BillSection from '@/pages/bill/components/BillSection.vue'
import FilterModal from '@/pages/bill/components/FilterModal.vue'

dayjs.extend(isSameOrAfter)
dayjs.extend(isSameOrBefore)

definePage({
  name: 'bill',
  layout: 'default',
  style: {
    navigationBarTitleText: '家庭账单',
    enablePullDownRefresh: true,
  },
})

const user = useUserStore()
const billStore = useBillStore()

const searchValue = ref('')
const filterParams = ref<Optional<BillListDTO>>({})
const isFiltered = ref()
const showFilterModal = ref(false)
const showBillDetailModal = ref(false)
const currentBill = ref<BillVO>()
const familyId = ref<string>()

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
  filterParams.value = { familyId: familyId.value, ...result }
  isFiltered.value = filtered
  showFilterModal.value = false
  refresh()
}

function onClickBill(bill: BillVO) {
  currentBill.value = bill
  showBillDetailModal.value = true
}

function isSameMonth(currentTime?: string, comparedTime?: string) {
  if (!currentTime || !comparedTime) {
    return false
  }
  return dayjs(currentTime).isSame(dayjs(comparedTime), 'month')
}

onLoad((options) => {
  if (options?.familyId) {
    filterParams.value.familyId = options.familyId
    familyId.value = options.familyId
  }
  if (options?.familyName) {
    uni.setNavigationBarTitle({ title: `${decodeURIComponent(options.familyName)}的账单` })
  }
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
watch(() => user.isLoggedIn, (newVal) => {
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
            class="relative h-5 flex items-center justify-center border border-[var(--wot-color-border)] rounded-xl border-solid bg-white px-2.5 py-2 text-gray-600 transition-all active:scale-95 dark:border-gray-600 dark:bg-[var(--wot-dark-background2)]"
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
        <BillSection v-if="!isSameMonth(bill.time, billList?.[index - 1]?.time)" :key="bill.amount" :month="dayjs(bill.time!).format('YYYY-MM')" :family-id="familyId" custom-class="mt-3" />
        <BillItem :bill="bill" show-creator @click="onClickBill(bill)" />
      </template>
      <wd-loadmore :state="loadingMoreStatus" finished-text="没有更多数据了" custom-class="h-8!" @reload="refresh" />
    </view>

    <view v-else class="mt-10vh flex flex-col items-center justify-center gap-5">
      <wd-status-tip tip="您的家庭还没有任何账单数据">
        <template #image>
          <image mode="aspectFit" class="h-30 w-80" src="https://chuan-bill-cdn.samoy.site/default/nodata.svg" />
        </template>
      </wd-status-tip>
    </view>

    <!-- 账单详情弹框 -->
    <BillDetailModal v-if="currentBill" v-model="showBillDetailModal" :bill="currentBill!" type="family" />
    <!-- 筛选弹框 -->
    <FilterModal v-model="showFilterModal" :family-id="familyId" @submit="submitFilter" />
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
