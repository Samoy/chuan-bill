<script setup lang="ts">
import type { BillVO } from '@/api/globals'
import BillFilterModal from './components/BillFilterModal.vue'
import QuickBillModal from './components/QuickBillModal.vue'

definePage({
  name: 'bill',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '账单',
    enablePullDownRefresh: true,
    onReachBottomDistance: 50,
  },
})

// 搜索和筛选状态
const toast = useToast('globalToast')
const searchValue = ref('')
const showFilterModal = ref(false)
const showQuickBillModal = ref(false)

// 筛选条件
const filterParams = ref<any>({})

// 账单列表数据
const billList = ref<BillVO[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
  hasMore: true,
})

// 加载状态
const loading = ref(false)

// 搜索防抖
const debouncedSearch = useDebounceFn(() => {
  pagination.page = 1
  loadBills()
}, 500)

watch(searchValue, () => {
  debouncedSearch()
})

// 加载账单列表
async function loadBills(isLoadMore = false) {
  if (loading.value || (isLoadMore && !pagination.hasMore)) {
    return
  }

  try {
    if (!isLoadMore) {
      loading.value = true
    }

    const params: any = {
      page: pagination.page,
      size: pagination.size,
      ...filterParams.value,
    }

    // 添加搜索条件
    if (searchValue.value?.trim()) {
      params.name = searchValue.value.trim()
      params.remark = searchValue.value.trim()
    }

    const res = await Apis.general.getBillList(params)
    const data = res.data

    if (isLoadMore) {
      billList.value.push(...(data.records || []))
    }
    else {
      billList.value = data.records || []
    }

    pagination.total = data.total || 0
    pagination.hasMore = pagination.page * pagination.size < pagination.total
  }
  catch (error: any) {
    console.error('加载账单列表失败:', error)
    if (!isLoadMore) {
      toast.show(error.message || '加载失败')
    }
  }
  finally {
    loading.value = false
  }
}

// 下拉刷新
function refresh() {
  pagination.page = 1
  loadBills()
  uni.stopPullDownRefresh()
}

// 上拉加载更多
function onReachBottom() {
  if (pagination.hasMore && !loading.value) {
    pagination.page++
    loadBills(true)
  }
}

// 筛选确认
function handleFilterConfirm(filters: any) {
  filterParams.value = filters
  pagination.page = 1
  loadBills()
}

// 筛选重置
function handleFilterReset() {
  filterParams.value = {}
  pagination.page = 1
  loadBills()
}

// 记账成功回调
function handleQuickBillSuccess() {
  pagination.page = 1
  loadBills()
}

// 格式化金额显示
function formatAmount(amount: number, type: string) {
  const formatted = Number(amount || 0).toFixed(2)
  return type === 'income' ? `+${formatted}` : `-${formatted}`
}

// 页面生命周期
onPullDownRefresh(() => {
  refresh()
})

onLoad(() => {
  loadBills()
})
</script>

<template>
  <view class="bill-page">
    <!-- 搜索区域 -->
    <view class="border-b border-[var(--wot-border-color)] px-3 py-3">
      <view class="flex items-center gap-2">
        <wd-search
          v-model="searchValue"
          placeholder="账单名称或备注"
          hide-cancel
          custom-class="flex-1 rounded-xl border border-[var(--wot-border-color)] dark:border-gray-600"
        />
        <view
          class="relative flex items-center justify-center border border-[var(--wot-border-color)] rounded-xl bg-white p-3 text-gray-600 transition-all active:scale-95 dark:bg-[var(--wot-dark-background2)]"
          @click="showFilterModal = true"
        >
          <view class="i-lucide:filter" />
          <!-- 有筛选条件时显示红点 -->
          <view v-if="Object.keys(filterParams).length > 0" class="filter-dot" />
        </view>
      </view>

      <!-- 筛选条件标签 -->
      <view v-if="Object.keys(filterParams).length > 0" class="mt-2 flex flex-wrap gap-2">
        <wd-tag v-if="filterParams.type" type="primary" size="small" plain>
          {{ filterParams.type === 'income' ? '收入' : '支出' }}
        </wd-tag>
        <wd-tag v-if="filterParams.startDate || filterParams.endDate" type="primary" size="small" plain>
          {{ filterParams.startDate || '开始' }} ~ {{ filterParams.endDate || '结束' }}
        </wd-tag>
        <wd-tag v-if="filterParams.minAmount || filterParams.maxAmount" type="warning" size="small" plain>
          ¥{{ filterParams.minAmount || 0 }} - ¥{{ filterParams.maxAmount || '∞' }}
        </wd-tag>
      </view>
    </view>

    <!-- 账单列表 -->
    <scroll-view scroll-y class="flex-1" @scrolltolower="onReachBottom">
      <!-- 空状态 -->
      <view v-if="!loading && billList.length === 0" class="flex flex-col items-center justify-center px-5 py-20 text-center">
        <view class="i-lucide:receipt mb-4 text-8xl text-[var(--wot-font-color-placeholder)]" />
        <text class="mb-5 text-base text-[var(--wot-font-color-secondary)]">
          暂无账单记录
        </text>
        <wd-button type="primary" size="small" @click="showQuickBillModal = true">
          记一笔
        </wd-button>
      </view>

      <!-- 账单项 -->
      <view v-else class="p-3">
        <view
          v-for="bill in billList"
          :key="bill.id"
          class="bill-card mb-3"
        >
          <view class="flex gap-3 rounded-xl bg-white p-4 shadow-sm transition-all active:scale-98 dark:bg-[var(--wot-dark-background2)]">
            <!-- 左侧图标 -->
            <view class="bill-icon-wrapper">
              <view class="i-lucide:circle-dollar-sign text-xl text-white" />
            </view>

            <!-- 中间信息 -->
            <view class="min-w-0 flex-1">
              <view class="mb-2 flex items-center justify-between">
                <text class="bill-name mr-2 flex-1 truncate text-base text-[var(--wot-font-color)] font-medium">
                  {{ bill.name }}
                </text>
                <text
                  class="flex-shrink-0 text-lg font-semibold"
                  :class="bill.type === 'income' ? 'text-green-500' : 'text-red-500'"
                >
                  {{ formatAmount(bill.amount, bill.type) }}
                </text>
              </view>

              <view class="mb-1.5 flex flex-wrap gap-3">
                <text v-if="bill.categoryName" class="bill-detail-item">
                  <view class="i-lucide:tag text-xs" />
                  {{ bill.categoryName }}
                </text>
                <text v-if="bill.paymentMethodName" class="bill-detail-item">
                  <view class="i-lucide:credit-card text-xs" />
                  {{ bill.paymentMethodName }}
                </text>
                <text v-if="bill.time" class="bill-detail-item">
                  <view class="i-lucide:calendar text-xs" />
                  {{ bill.time }}
                </text>
              </view>

              <view v-if="bill.remark" class="bill-remark">
                <view class="i-lucide:message-square text-xs" />
                <text class="text-xs">
                  {{ bill.remark }}
                </text>
              </view>
            </view>
          </view>
        </view>

        <!-- 加载更多提示 -->
        <view v-if="loading && billList.length > 0" class="flex items-center justify-center gap-2 py-4">
          <wd-loading size="small" />
          <text class="text-xs text-[var(--wot-font-color-placeholder)]">
            加载中...
          </text>
        </view>

        <!-- 没有更多数据 -->
        <view v-if="!loading && !pagination.hasMore && billList.length > 0" class="flex items-center justify-center py-4">
          <text class="text-xs text-[var(--wot-font-color-placeholder)]">
            - 已经到底了 -
          </text>
        </view>
      </view>
    </scroll-view>

    <!-- FAB 按钮 -->
    <wd-fab
      draggable
      :expandable="false"
      :gap="{ bottom: 70, right: 20 }"
      @click="showQuickBillModal = true"
    />

    <!-- 筛选弹框 -->
    <BillFilterModal
      v-model="showFilterModal"
      :current-filters="filterParams"
      @confirm="handleFilterConfirm"
      @reset="handleFilterReset"
    />

    <!-- 快速记账弹框 -->
    <QuickBillModal
      v-model="showQuickBillModal"
      @success="handleQuickBillSuccess"
    />
  </view>
</template>

<style lang="scss" scoped>
// UnoCSS 无法实现的样式才写在这里

.bill-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--wot-background);
}

// 筛选红点
.filter-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  background: #ff4d4f;
  border-radius: 50%;
  border: 1px solid #fff;
}

// 账单图标
.bill-icon-wrapper {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

// 账单名称
.bill-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 账单详情项
.bill-detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--wot-font-color-secondary);
}

// 账单备注
.bill-remark {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--wot-font-color-placeholder);
  padding-top: 6px;
  border-top: 1px dashed var(--wot-border-color);
}
</style>
