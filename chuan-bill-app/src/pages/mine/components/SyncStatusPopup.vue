<script setup lang="ts">
defineOptions({
  name: 'SyncStatusPopup',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const modelValue = defineModel<boolean>({ default: false })
const billStore = useBillStore()
const toast = useGlobalToast()
const loading = ref(false)
const syncResult = ref<{ status: string, successCount: number, failedCount: number, details: Array<{ index: number, status: string, reason?: string }> } | null>(null)

// 是否无待同步数据
const noSyncData = computed(() => billStore.pendingSyncCount === 0 && billStore.failedSyncCount === 0)

// 同步状态文案
const statusText = computed(() => {
  if (loading.value)
    return '同步中...'
  if (syncResult.value?.status === 'ALL_SUCCESS')
    return '同步成功'
  if (syncResult.value?.status === 'PARTIAL_SUCCESS')
    return '部分成功'
  if (syncResult.value?.status === 'ALL_FAILED')
    return '同步失败'
  return ''
})

// 失败账单详情列表
const failedDetails = computed(() => {
  if (!syncResult.value?.details)
    return []
  return syncResult.value.details.filter(d => d.status === 'FAILED')
})

// 开始同步
async function startSync() {
  if (loading.value)
    return

  loading.value = true
  syncResult.value = null

  try {
    const result = await billStore.syncLocalBillToServer()

    if (result.success) {
      syncResult.value = {
        status: result.failedCount === 0 ? 'ALL_SUCCESS' : 'PARTIAL_SUCCESS',
        successCount: result.successCount,
        failedCount: result.failedCount,
        details: [],
      }
      if (result.failedCount === 0) {
        toast.success(`成功同步${result.successCount}条账单`)
      }
      else {
        toast.warning(`${result.successCount}条成功，${result.failedCount}条失败`)
      }
    }
    else {
      syncResult.value = {
        status: 'ALL_FAILED',
        successCount: 0,
        failedCount: result.failedCount,
        details: [],
      }
      toast.error('同步失败，请重试')
    }
  }
  catch {
    syncResult.value = {
      status: 'ALL_FAILED',
      successCount: 0,
      failedCount: billStore.pendingSyncCount,
      details: [],
    }
    toast.error('同步失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 重试失败的账单
async function retryFailed() {
  if (loading.value)
    return

  loading.value = true
  syncResult.value = null

  try {
    const result = await billStore.retryFailedSync()

    if (result.success) {
      syncResult.value = {
        status: result.failedCount === 0 ? 'ALL_SUCCESS' : 'PARTIAL_SUCCESS',
        successCount: result.successCount,
        failedCount: result.failedCount,
        details: [],
      }
      if (result.failedCount === 0) {
        toast.success(`成功同步${result.successCount}条账单`)
      }
      else {
        toast.warning(`${result.successCount}条成功，${result.failedCount}条失败`)
      }
    }
    else {
      syncResult.value = {
        status: 'ALL_FAILED',
        successCount: 0,
        failedCount: result.failedCount,
        details: [],
      }
      toast.error('同步失败，请重试')
    }
  }
  catch {
    syncResult.value = {
      status: 'ALL_FAILED',
      successCount: 0,
      failedCount: billStore.failedSyncCount,
      details: [],
    }
    toast.error('同步失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 关闭弹窗
function handleClose() {
  syncResult.value = null
  modelValue.value = false
}
</script>

<template>
  <wd-action-sheet
    :model-value="modelValue"
    position="bottom"
    closable
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    :z-index="999"
    title="账单同步"
    lock-scroll
    @update:model-value="handleClose"
  >
    <view class="max-h-[70vh] overflow-y-auto p-4">
      <!-- 无待同步数据占位 -->
      <view v-if="noSyncData" class="flex flex-col items-center justify-center py-12">
        <view class="i-lucide:cloud-off mb-3 h-10 w-10 text-gray-300 dark:text-gray-600" />
        <text class="text-xs text-gray-400 dark:text-gray-500">
          暂无待同步数据
        </text>
      </view>

      <template v-else>
        <!-- 同步状态统计 -->
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <view class="mb-3 flex items-center justify-between">
            <text class="text-sm text-gray-600 dark:text-gray-400">
              待同步
            </text>
            <text class="text-sm text-orange-500 font-medium">
              {{ billStore.pendingSyncCount }}条
            </text>
          </view>
          <view v-if="billStore.failedSyncCount > 0" class="mb-3 flex items-center justify-between">
            <text class="text-sm text-gray-600 dark:text-gray-400">
              同步失败
            </text>
            <text class="text-sm text-red-500 font-medium">
              {{ billStore.failedSyncCount }}条
            </text>
          </view>
          <view v-if="billStore.lastSyncTime" class="flex items-center justify-between">
            <text class="text-sm text-gray-600 dark:text-gray-400">
              上次同步
            </text>
            <text class="text-sm text-gray-500">
              {{ billStore.lastSyncTime }}
            </text>
          </view>
        </view>

        <!-- 同步结果 -->
        <view
          v-if="statusText" class="mb-4 rounded-xl p-4" :class="{
            'bg-green-50 dark:bg-green-900/20': syncResult?.status === 'ALL_SUCCESS',
            'bg-orange-50 dark:bg-orange-900/20': syncResult?.status === 'PARTIAL_SUCCESS',
            'bg-red-50 dark:bg-red-900/20': syncResult?.status === 'ALL_FAILED',
          }"
        >
          <view class="flex items-center gap-2">
            <view
              v-if="loading"
              class="i-lucide:loader-2 h-5 w-5 animate-spin text-primary"
            />
            <view
              v-else-if="syncResult?.status === 'ALL_SUCCESS'"
              class="i-lucide:check-circle h-5 w-5 text-green-500"
            />
            <view
              v-else-if="syncResult?.status === 'PARTIAL_SUCCESS'"
              class="i-lucide:alert-circle h-5 w-5 text-orange-500"
            />
            <view
              v-else
              class="i-lucide:x-circle h-5 w-5 text-red-500"
            />
            <text
              class="text-sm font-medium" :class="{
                'text-green-700 dark:text-green-300': syncResult?.status === 'ALL_SUCCESS',
                'text-orange-700 dark:text-orange-300': syncResult?.status === 'PARTIAL_SUCCESS',
                'text-red-700 dark:text-red-300': syncResult?.status === 'ALL_FAILED',
              }"
            >
              {{ statusText }}
              <text v-if="syncResult?.status === 'PARTIAL_SUCCESS'" class="ml-1">
                ，{{ syncResult.successCount }}条成功，{{ syncResult.failedCount }}条失败
              </text>
              <text v-if="syncResult?.status === 'ALL_FAILED' && syncResult?.failedCount" class="ml-1">
                ，{{ syncResult.failedCount }}条失败
              </text>
            </text>
          </view>

          <!-- 失败原因列表 -->
          <view v-if="failedDetails.length > 0" class="mt-3 max-h-32 overflow-y-auto">
            <view
              v-for="detail in failedDetails"
              :key="detail.index"
              class="mb-1 flex items-start gap-2 text-xs text-red-600 dark:text-red-400"
            >
              <text class="i-lucide:alert-triangle mt-0.5 h-3 w-3 flex-shrink-0" />
              <text>{{ detail.reason || '未知错误' }}</text>
            </view>
          </view>
        </view>

        <!-- 同步按钮 -->
        <view class="flex gap-2">
          <wd-button
            v-if="billStore.failedSyncCount > 0 && !loading"
            type="warning"
            block
            @click="retryFailed"
          >
            重试失败（{{ billStore.failedSyncCount }}条）
          </wd-button>
          <wd-button
            type="primary"
            block
            :loading="loading"
            @click="startSync"
          >
            开始同步
          </wd-button>
        </view>
      </template>
    </view>
  </wd-action-sheet>
</template>
