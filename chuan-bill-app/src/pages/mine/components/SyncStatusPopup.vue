<script setup lang="ts">
defineOptions({
  name: 'SyncStatusPopup',
})

const modelValue = defineModel<boolean>({ default: false })
const billStore = useBillStore()
const toast = useGlobalToast()
const loading = ref(false)
const syncResult = ref<{ success: boolean, count: number } | null>(null)

// 同步状态文案
const statusText = computed(() => {
  if (loading.value)
    return '同步中...'
  if (syncResult.value?.success)
    return '同步成功'
  if (syncResult.value && !syncResult.value.success)
    return '同步失败'
  return ''
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
      syncResult.value = { success: true, count: result.successCount }
      toast.success(`成功同步${result.successCount}条账单`)
    }
    else {
      syncResult.value = { success: false, count: 0 }
      toast.error('同步失败，请重试')
    }
  }
  catch {
    syncResult.value = { success: false, count: 0 }
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
    @update:model-value="handleClose"
  >
    <view class="p-4">
      <!-- 同步状态统计 -->
      <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view class="mb-3 flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            待同步
          </text>
          <text class="text-sm font-medium">
            {{ billStore.pendingSyncCount }}条
          </text>
        </view>
        <view class="mb-3 flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            已同步
          </text>
          <text class="text-sm font-medium">
            {{ billStore.syncedCount }}条
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
      <view v-if="statusText" class="mb-4 rounded-xl p-4" :class="syncResult?.success ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'">
        <view class="flex items-center gap-2">
          <view
            v-if="loading"
            class="i-lucide:loader-2 h-5 w-5 animate-spin text-primary"
          />
          <view
            v-else-if="syncResult?.success"
            class="i-lucide:check-circle h-5 w-5 text-green-500"
          />
          <view
            v-else
            class="i-lucide:x-circle h-5 w-5 text-red-500"
          />
          <text class="text-sm font-medium" :class="syncResult?.success ? 'text-green-700 dark:text-green-300' : 'text-red-700 dark:text-red-300'">
            {{ statusText }}
          </text>
        </view>
      </view>

      <!-- 同步按钮 -->
      <wd-button
        type="primary"
        block
        :loading="loading"
        :disabled="billStore.pendingSyncCount === 0"
        @click="startSync"
      >
        {{ billStore.pendingSyncCount === 0 ? '暂无待同步数据' : '开始同步' }}
      </wd-button>
    </view>
  </wd-action-sheet>
</template>
