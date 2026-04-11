<script setup lang="ts">
import type { AddBillDTO, BillVO, UpdateBillDTO } from '@/api/globals'
import dayjs from 'dayjs'
import ManualEdit from './ManualEdit.vue'
import OcrEdit from './OcrEdit.vue'

defineOptions({
  name: 'QuickBillModal',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const props = defineProps<{
  isEdit?: boolean
  bill?: BillVO
}>()

const emit = defineEmits<{
  success: [result: AddBillDTO | UpdateBillDTO ]
}>()

// 记账方式选项
const sourceOptions = [
  { payload: { label: '手动添加', icon: 'i-lucide:square-pen' }, value: 'manual' },
  { payload: { label: '图片识别', icon: 'i-lucide:camera' }, value: 'ocr' },
  { payload: { label: '语音识别', icon: 'i-lucide:mic' }, value: 'voice' },
]
const source = ref('manual')

const show = defineModel<boolean>({ default: false })
const segmentedRef = ref()
const toast = useGlobalToast()
const billForm = ref<AddBillDTO>({ name: '', type: 'expense', amount: '', time: '', source: 'manual' })

function convertBillVOToAddBillDTO(billVO: BillVO) {
  billForm.value.name = billVO.name!
  billForm.value.amount = billVO.amount!
  billForm.value.type = billVO.type!
  billForm.value.time = billVO.time!
  if (billVO.category?.id) {
    billForm.value.categoryId = billVO.category!.id!
  }
  if (billVO.paymentMethod?.id) {
    billForm.value.paymentMethodId = billVO.paymentMethod!.id!
  }
  if (billVO.remark) {
    billForm.value.remark = billVO.remark!
  }
  billForm.value.source = billVO.source!
}

function ocrSubmit(result: BillVO) {
  convertBillVOToAddBillDTO(result)
  source.value = 'manual'
}

function voiceSubmit(result: BillVO) {
  convertBillVOToAddBillDTO(result)
  source.value = 'manual'
}

function resetBillForm() {
  billForm.value = {
    name: '',
    type: 'expense',
    amount: '',
    time: '',
    source: 'manual',
    remark: '',
    categoryId: '',
    paymentMethodId: '',
  }
}

async function manualSubmit(billDTO: AddBillDTO | UpdateBillDTO) {
  if (props.isEdit) {
    updateBill({
      id: props.bill!.id!,
      ...billDTO,
    })
    return
  }
  addBill(billDTO as AddBillDTO)
}

async function addBill(billDTO: AddBillDTO) {
  const res = await Apis.bill.addBill({
    data: {
      ...billDTO,
      time: dayjs(billDTO.time).format('YYYY-MM-DD HH:mm'),
    },
  })
  if (res.success) {
    toast.success('添加成功')
    show.value = false
    resetBillForm()
    emit('success', billDTO)
  }
}

async function updateBill(billDTO: UpdateBillDTO) {
  const res = await Apis.bill.updateBill({
    data: {
      ...billDTO,
      time: dayjs(billDTO.time).format('YYYY-MM-DD HH:mm'),
    },
  })
  if (res.success) {
    toast.success('修改成功')
    show.value = false
    resetBillForm()
    emit('success', billDTO)
  }
}

watch(() => props.bill, (newVal) => {
  if (newVal) {
    convertBillVOToAddBillDTO(newVal)
  }
}, { immediate: true, deep: true })
</script>

<template>
  <wd-action-sheet
    v-model="show" position="bottom" custom-class="!rounded-3xl !rounded-b-none" title="记一笔"
    :close-on-click-modal="false" :z-index="100" @opened="segmentedRef?.updateActiveStyle(false)"
  >
    <text v-if="source === 'manual'" class="i-icon-park-outline:clear-format absolute right-10 top-5 h-4 w-4 text-black/65" @click="resetBillForm" />
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
        <ManualEdit v-model="billForm" @submit="manualSubmit" />
      </template>
      <template v-else-if="source === 'ocr'">
        <OcrEdit @submit="ocrSubmit" />
      </template>
      <template v-else-if="source === 'voice'">
        <VoiceEdit @submit="voiceSubmit" />
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
