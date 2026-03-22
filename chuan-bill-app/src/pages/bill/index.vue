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

// 获取金额颜色类
function getAmountColorClass(type?: string) {
  return type === 'income' ? 'text-green-600' : 'text-red-600'
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
    <view class="search-bar px-3 py-3">
      <view class="flex items-center gap-2">
        <wd-search
          v-model="searchValue"
          placeholder="账单名称或备注"
          hide-cancel
          custom-class="flex-1 rounded-xl border border-border dark:border-gray-600"
        />
        <view
          class="filter-btn flex items-center justify-center border border-[var(--wot-border-color)] rounded-xl bg-white p-3 text-gray-600 transition-all active:scale-95 dark:bg-[var(--wot-dark-background2)]"
          @click="showFilterModal = true"
        >
          <view class="i-lucide:filter" />
          <!-- 有筛选条件时显示红点 -->
          <view v-if="Object.keys(filterParams).length > 0" class="filter-dot" />
        </view>
      </view>

      <!-- 筛选条件标签 -->
      <view v-if="Object.keys(filterParams).length > 0" class="filter-tags mt-2 flex flex-wrap gap-2">
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
    <scroll-view scroll-y class="bill-list" @scrolltolower="onReachBottom">
      <!-- 空状态 -->
      <view v-if="!loading && billList.length === 0" class="empty-state">
        <view class="i-lucide:receipt-empty-state-icon" />
        <text class="empty-text">
          暂无账单记录
        </text>
        <wd-button type="primary" size="small" @click="showQuickBillModal = true">
          记一笔
        </wd-button>
      </view>

      <!-- 账单项 -->
      <view v-else class="bill-list-content">
        <view
          v-for="bill in billList"
          :key="bill.id"
          class="bill-item"
        >
          <view class="bill-card">
            <!-- 左侧图标 -->
            <view class="bill-icon-wrapper">
              <view class="bill-icon i-lucide:circle-dollar-sign" />
            </view>

            <!-- 中间信息 -->
            <view class="bill-info">
              <view class="bill-header">
                <text class="bill-name">
                  {{ bill.name }}
                </text>
                <text class="bill-amount" :class="getAmountColorClass(bill.type)">
                  {{ formatAmount(bill.amount, bill.type) }}
                </text>
              </view>

              <view class="bill-details">
                <text v-if="bill.categoryName" class="bill-detail-item">
                  <view class="i-lucide:tag" />
                  {{ bill.categoryName }}
                </text>
                <text v-if="bill.paymentMethodName" class="bill-detail-item">
                  <view class="i-lucide:credit-card" />
                  {{ bill.paymentMethodName }}
                </text>
                <text v-if="bill.time" class="bill-detail-item">
                  <view class="i-lucide:calendar" />
                  {{ bill.time }}
                </text>
              </view>

              <view v-if="bill.remark" class="bill-remark">
                <view class="i-lucide:message-square" />
                <text>{{ bill.remark }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 加载更多提示 -->
        <view v-if="loading && billList.length > 0" class="loading-more">
          <wd-loading size="small" />
          <text class="loading-text">
            加载中...
          </text>
        </view>

        <!-- 没有更多数据 -->
        <view v-if="!loading && !pagination.hasMore && billList.length > 0" class="no-more">
          <text class="no-more-text">
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
.bill-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--wot-background);
}

.search-bar {
  background: var(--wot-background);
  border-bottom: 1px solid var(--wot-border-color);
}

.filter-btn {
  position: relative;

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
}

.bill-list {
  flex: 1;
  overflow-y: hidden;
}

.bill-list-content {
  padding: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;

  .empty-state-icon {
    font-size: 80px;
    color: var(--wot-font-color-placeholder);
    margin-bottom: 16px;
  }

  .empty-text {
    font-size: 16px;
    color: var(--wot-font-color-secondary);
    margin-bottom: 20px;
  }
}

.bill-item {
  margin-bottom: 12px;
}

.bill-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;

  &:active {
    transform: scale(0.98);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  }
}

.bill-icon-wrapper {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;

  .bill-icon {
    font-size: 24px;
    color: #fff;
  }
}

.bill-info {
  flex: 1;
  min-width: 0;
}

.bill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.bill-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--wot-font-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.bill-amount {
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
  margin-left: 8px;
}

.bill-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 6px;
}

.bill-detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--wot-font-color-secondary);

  view {
    font-size: 12px;
  }
}

.bill-remark {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--wot-font-color-placeholder);
  padding-top: 6px;
  border-top: 1px dashed var(--wot-border-color);

  view {
    font-size: 12px;
  }
}

.loading-more,
.no-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
  gap: 8px;

  .loading-text,
  .no-more-text {
    font-size: 12px;
    color: var(--wot-font-color-placeholder);
  }
}

/* 深色模式适配 */
:root.dark {
  .bill-card {
    background: var(--wot-dark-background2);
  }
}
</style>
