<script setup lang="ts">
import type { AddBillDTO, BillVO, UpdateBillDTO } from '@/api/globals'
import dayjs from 'dayjs'
import { EVENTS } from '@/constant/events'
import { eventBus } from '@/utils/eventBus'
import ManualEdit from './ManualEdit.vue'
import OcrEdit from './OcrEdit.vue'
import VoiceEdit from './VoiceEdit.vue'

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
  source?: 'manual' | 'ocr' | 'voice'
  isGuestMode?: boolean
}>()

const emit = defineEmits<{
  success: [result: AddBillDTO | UpdateBillDTO]
  localSubmit: [data: any]
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
const message = useGlobalMessage()
const billForm = ref<AddBillDTO>({ name: '', type: 'expense', amount: '', time: '', source: 'manual', categoryId: '' })
const user = useUserStore()
const billStore = useBillStore()
const isLoggedIn = computed(() => user.isLoggedIn)

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
  if (!isLoggedIn.value) {
    billStore.addLocalBill(billDTO)
    addBillSuccess(billDTO)
    return
  }
  const res = await Apis.bill.addBill({
    data: {
      ...billDTO,
      time: dayjs(billDTO.time).format('YYYY-MM-DD HH:mm'),
    },
  })
  if (res.success) {
    addBillSuccess(billDTO)
  }
}

function addBillSuccess(billDTO: AddBillDTO) {
  toast.success('添加成功')
  show.value = false
  resetBillForm()
  emit('success', billDTO)
  eventBus.emit(EVENTS.BILL.UPDATED)
}

async function updateBill(billDTO: UpdateBillDTO) {
  if (!isLoggedIn.value) {
    billStore.updateLocalBill(billDTO)
    updateBillSuccess(billDTO)
    return
  }
  const res = await Apis.bill.updateBill({
    data: {
      ...billDTO,
      time: dayjs(billDTO.time).format('YYYY-MM-DD HH:mm'),
    },
  })
  if (res.success) {
    updateBillSuccess(billDTO)
  }
}

function updateBillSuccess(billDTO: UpdateBillDTO) {
  toast.success('修改成功')
  show.value = false
  resetBillForm()
  emit('success', billDTO)
  eventBus.emit(EVENTS.BILL.UPDATED)
}

function showInfo() {
  message.alert({
    title: '提示',
    msg: '暂不支持多笔账单',
    zIndex: 1000,
    confirmButtonText: '我知道了',
  })
}

watch(() => props.bill, (newVal) => {
  if (newVal) {
    convertBillVOToAddBillDTO(newVal)
  }
  else {
    resetBillForm()
  }
}, { immediate: true, deep: true })
</script>

<template>
  <wd-action-sheet
    v-model="show" position="bottom" custom-class="!rounded-3xl !rounded-b-none" title="记一笔"
    :z-index="100" lock-scroll @opened="segmentedRef?.updateActiveStyle(false)"
  >
    <text v-if="source === 'manual'" class="i-icon-park-outline:clear-format absolute right-10 top-[var(--wot-action-sheet-close-top,25px)] box-border h-4 w-4 text-black/65 dark:text-[#e8e6e3cc]" @click="resetBillForm" />
    <text v-else-if="isLoggedIn" class="i-lucide:info absolute right-10 top-[var(--wot-action-sheet-close-top,25px)] box-border h-4 h-4 w-4 w-4 text-black/65 dark:text-[#e8e6e3cc]" @click="showInfo" />

    <view class="pos-relative max-h-[82vh] flex flex-col gap-3 px-4 pb-4">
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
