<script setup lang="ts">
import ManualEdit from './ManualEdit.vue'

defineOptions({
  name: 'QuickBillModal',
  virtualHost: true,
  addGlobalClass: true,
  styleIsolation: 'shared',
})

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  'success': []
}>()

interface Props {
  show: boolean
}

// 记账方式选项
const sourceOptions = [
  { payload: { label: '手动添加', icon: 'i-lucide:square-pen' }, value: 'manual' },
  { payload: { label: '图片识别', icon: 'i-lucide:camera' }, value: 'ocr' },
  { payload: { label: '语音识别', icon: 'i-lucide:mic' }, value: 'voice' },
]
const source = ref('manual')

// 使用 computed 处理 v-model
const showPopup = computed({
  get: () => props.show,
  set: val => emit('update:show', val),
})
</script>

<template>
  <wd-action-sheet
    v-model="showPopup"
    position="bottom"
    custom-class="!rounded-3xl !rounded-b-none"
    title="记一笔"
    :close-on-click-modal="false"
    :z-index="100"
  >
    <view class="flex flex-col gap-3 px-3 pb-3">
      <wd-segmented
        v-model:value="source" :options="sourceOptions"
        custom-class="!rounded-xl dark:!bg-[var(--wot-dark-background3)]"
      >
        <template #label="{ option }">
          <view class="flex items-center justify-center gap-1 py-1">
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
    </view>
  </wd-action-sheet>
</template>

<style lang="scss" scoped>
:deep(.wd-segmented__item--active){
  @apply rounded-lg;
}
</style>
