<script setup lang="ts">
import type { BillListDTO, BillVO, FamilyVO } from '@/api/globals'
import dayjs from 'dayjs'

definePage({
  name: 'family-bills',
  style: {
    navigationBarTitleText: '家庭账单',
  },
})

const familyStore = useFamilyStore()

const familyId = ref('')
const family = ref<FamilyVO | null>(null)

// 账单列表数据
const billList = ref<BillVO[]>([])
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const finished = ref(false)

// 筛选条件
const filterParams = ref<Partial<BillListDTO>>({})
const showFilterModal = ref(false)
const isFiltered = ref(false)

onLoad((options) => {
  if (options?.familyId) {
    familyId.value = options.familyId
  }
})

onShow(async () => {
  if (familyId.value) {
    // 获取家庭信息
    family.value = familyStore.familyList.find(f => f.id === familyId.value) || null
    if (!family.value) {
      await familyStore.fetchFamilyDetail(familyId.value)
      family.value = familyStore.currentFamily
    }
    // 刷新账单列表
    await refresh()
  }
})

// 获取账单列表
async function fetchBills() {
  if (loading.value || finished.value)
    return
  loading.value = true
  try {
    const result = await familyStore.fetchFamilyBills(familyId.value, page.value, size.value, filterParams.value)
    if (result) {
      const records = result.records || []
      if (page.value === 1) {
        billList.value = records
      }
      else {
        billList.value = [...billList.value, ...records]
      }
      finished.value = page.value * size.value >= (result.total || 0)
      page.value++
    }
  }
  finally {
    loading.value = false
  }
}

// 刷新列表
async function refresh() {
  page.value = 1
  finished.value = false
  await fetchBills()
}

// 加载更多
function loadMore() {
  if (!finished.value && !loading.value) {
    fetchBills()
  }
}

// 筛选提交
function submitFilter(result: Partial<BillListDTO>, filtered: boolean) {
  filterParams.value = result
  isFiltered.value = filtered
  showFilterModal.value = false
  refresh()
}

// 查看账单详情
function viewBillDetail(bill: BillVO) {
  // 显示账单详情弹窗或跳转
  console.log('查看账单详情', bill)
}

// 判断是否为同一月
function isSameMonth(currentTime?: string, comparedTime?: string) {
  if (!currentTime || !comparedTime)
    return false
  return dayjs(currentTime).isSame(dayjs(comparedTime), 'month')
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 家庭信息头 -->
    <view v-if="family" class="mx-3 rounded-2xl from-primary to-primary/50 bg-gradient-to-br p-4 text-white shadow-lg">
      <view class="flex items-center gap-3">
        <view class="h-12 w-12 flex items-center justify-center rounded-xl bg-white/20 backdrop-blur-sm">
          <image v-if="family.avatar" :src="family.avatar" class="h-12 w-12 rounded-xl" mode="aspectFill" />
          <view v-else class="i-lucide:home h-6 w-6" />
        </view>
        <view class="flex-1">
          <text class="block text-base font-bold">
            {{ family.name }}
          </text>
          <text class="mt-0.5 block text-xs text-white/80">
            家庭共享账单
          </text>
        </view>
      </view>
    </view>

    <!-- 筛选栏 -->
    <view class="mx-3 rounded-2xl bg-white p-3 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="flex items-center gap-2">
        <view
          class="flex items-center gap-1 rounded-lg bg-gray-100 px-3 py-2 dark:bg-gray-700"
          :class="isFiltered && 'text-primary'"
          @click="showFilterModal = true"
        >
          <view class="i-lucide:filter h-4 w-4" />
          <text class="text-sm">
            {{ isFiltered ? '已筛选' : '筛选' }}
          </text>
        </view>
        <view class="flex-1 text-right text-sm text-gray-500">
          共 {{ billList.length }} 条账单
        </view>
      </view>
    </view>

    <!-- 账单列表 -->
    <view v-if="billList.length" class="mx-3 flex flex-col gap-3">
      <template v-for="(bill, index) in billList" :key="bill.id">
        <!-- 月份分割线 -->
        <view v-if="!isSameMonth(bill.time, billList[index - 1]?.time)" class="mt-2 flex items-center gap-2 px-1">
          <text class="text-sm text-gray-700 font-bold dark:text-gray-300">
            {{ dayjs(bill.time).format('YYYY年MM月') }}
          </text>
          <view class="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
        </view>

        <!-- 账单项 -->
        <view
          class="flex items-center gap-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]"
          @click="viewBillDetail(bill)"
        >
          <!-- 分类图标 -->
          <view
            class="h-10 w-10 flex shrink-0 items-center justify-center rounded-full"
            :class="bill.type === 'income' ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'"
          >
            <view :class="bill.type === 'income' ? 'i-lucide:arrow-down-left' : 'i-lucide:arrow-up-right'" class="h-5 w-5" />
          </view>

          <view class="flex-1">
            <view class="flex items-center gap-2">
              <text class="text-sm font-500">
                {{ bill.name }}
              </text>
              <text v-if="bill.category?.name" class="text-xs text-gray-500">
                {{ bill.category.name }}
              </text>
            </view>
            <view class="mt-1 flex items-center gap-2 text-xs text-gray-400">
              <text>{{ dayjs(bill.time).format('MM-DD HH:mm') }}</text>
              <text v-if="bill.userNickname">
                · {{ bill.userNickname }}
              </text>
            </view>
          </view>

          <view class="text-right">
            <text
              class="text-base font-bold"
              :class="bill.type === 'income' ? 'text-green-600' : 'text-red-600'"
            >
              {{ bill.type === 'income' ? '+' : '-' }}{{ bill.amount }}
            </text>
          </view>
        </view>
      </template>

      <!-- 加载状态 -->
      <view v-if="loading" class="py-4 text-center">
        <wd-loading />
      </view>
      <view v-else-if="finished" class="py-4 text-center text-sm text-gray-400">
        没有更多数据了
      </view>
      <view v-else class="py-4 text-center text-sm text-primary" @click="loadMore">
        点击加载更多
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!loading" class="flex flex-col items-center justify-center py-20">
      <wd-status-tip tip="暂无家庭账单">
        <template #image>
          <image mode="aspectFit" class="h-30 w-80" src="https://cdn.chuanbill.samoy.site/default/nodata.svg" />
        </template>
      </wd-status-tip>
    </view>

    <!-- 筛选弹窗 -->
    <BillFilterModal v-model="showFilterModal" @submit="submitFilter" />
  </view>
</template>
