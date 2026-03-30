<script setup lang="ts">
import ManualEdit from './ManualEdit.vue'
import OcrEdit from './OcrEdit.vue'

defineOptions({
  name: 'QuickBillModal',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

// 记账方式选项
const sourceOptions = [
  { payload: { label: '手动添加', icon: 'i-lucide:square-pen' }, value: 'manual' },
  { payload: { label: '图片识别', icon: 'i-lucide:camera' }, value: 'ocr' },
  { payload: { label: '语音识别', icon: 'i-lucide:mic' }, value: 'voice' },
]
const source = ref('manual')

const show = defineModel<boolean>('show', { default: false })
const segmentedRef = ref()
</script>

<template>
  <wd-action-sheet
    v-model="show" position="bottom" custom-class="!rounded-3xl !rounded-b-none" title="记一笔"
    :close-on-click-modal="false" :z-index="100" @opened="segmentedRef?.updateActiveStyle(false)"
  >
    <view class="pos-relative max-h-[80vh] flex flex-col gap-3 px-4 pb-4">
      <wd-segmented
        ref="segmentedRef"
        v-model:value="source" :options="sourceOptions"
        custom-class="!rounded-xl dark:!bg-[var(--wot-dark-background3)]"
      >
        <template #label="{ option }">
          <view class="h-full flex items-center justify-center gap-1">
            <view class="text-xs" :class="[option.payload.icon]" />
            <view class="text-xs">
              {{ option.payload.label }}
            </view>
          </view>
        </template>
      </wd-segmented>
      <template v-if="source === 'manual'">
        <ManualEdit />
      </template>
      <template v-else-if="source === 'ocr'">
        <OcrEdit />
      </template>
    </view>
  </wd-action-sheet>
</template>

<style lang="scss" scoped>
:deep(.wd-segmented__item--active) {
  @apply rounded-lg;
}

:deep(.wd-segmented__item-label) {
  @apply h-full! flex! items-center! justify-center!;
}
</style>
