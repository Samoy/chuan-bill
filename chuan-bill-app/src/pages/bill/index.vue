<script setup lang="ts">
definePage({
  name: 'bill',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '账单',
  },
})

const searchValue = ref('')
const source = ref('manual')
const showQuickBillModal = ref(false)
const segmentedOptions = [
  { payload: {
    text: '手动记账',
    icon: 'i-lucide:square-pen',
  }, value: 'manual' },
  { payload: {
    text: '票据识别',
    icon: 'i-lucide:camera',
  }, value: 'ocr' },
  { payload: {
    text: '语音输入',
    icon: 'i-lucide:mic',
  }, value: 'voice' },
]
</script>

<template>
  <view class="py-3">
    <!-- 搜索区域 -->
    <view class="flex items-center gap-2 px-3">
      <wd-search v-model="searchValue" placeholder="账单名称或备注" hide-cancel custom-class="flex-1 rounded-xl border border-border dark:border-gray-600" />
      <view class="flex items-center justify-center border border-[var(--wot-border-color)] rounded-xl bg-white p-3 text-gray-600 transition-all active:scale-95 dark:bg-[var(--wot-dark-background2)]">
        <view class="i-lucide:filter" />
      </view>
    </view>
    <wd-fab draggable :expandable="false" :gap="{ bottom: 70, right: 20 }" @click="showQuickBillModal = true" />
    <wd-action-sheet v-model="showQuickBillModal" title="记一笔" :z-index="100" :closeable="true">
      <view class="flex flex-col gap-4 px-3 pb-3">
        <wd-segmented v-model:value="source" :options="segmentedOptions" custom-class="!rounded-xl">
          <template #label="{ option }">
            <view class="flex items-center justify-center gap-1">
              <view :class="option.payload.icon" class="size-3" />
              <view>{{ option.payload.text }}</view>
            </view>
          </template>
        </wd-segmented>
      </view>
    </wd-action-sheet>
  </view>
</template>

<style lang="scss" scoped>
:deep(.wd-segmented__item--active){
  border-radius: 8px;
}
</style>
