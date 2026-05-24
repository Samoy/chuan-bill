<script setup lang="ts">
import type { BillVO } from '@/api/globals'
import BillDetail from './BillDetail.vue'

defineOptions({
  name: 'BillDetailModal',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const { bill, type = 'user' } = defineProps<{
  type?: 'user' | 'family'
  bill: BillVO
}>()

const emit = defineEmits<{
  update: [item: BillVO]
  delete: []
}>()

const show = defineModel<boolean>({ default: false })
const message = useGlobalMessage()
const userStore = useUserStore()
const toast = useGlobalToast()

function updateBill() {
  emit('update', bill)
}

function deleteBill() {
  message.confirm({
    title: '提示',
    msg: '是否删除该账单？',
    zIndex: 1000,
    confirmButtonProps: {
      type: 'error',
    },
    beforeConfirm: async ({ resolve }) => {
      try {
        const res = await Apis.bill.deleteBill({ params: { id: bill.id! } })
        if (res.success) {
          resolve(true)
          toast.show('删除成功')
          emit('delete')
          show.value = false
        }
      }
      catch (error) {
        console.error(error)
      }
    },
  })
}

const isFamilyBill = computed(() => {
  return type === 'family'
})

const showBtn = computed(() => {
  return bill.userId === userStore.userId && !isFamilyBill.value
})
</script>

<template>
  <wd-action-sheet v-model="show" title="账单详情" :z-index="100" safe-area-inset-bottom position="bottom" closable custom-class="rounded-tl-2xl rounded-tr-2xl pb-3!" lock-scroll>
    <view class="relative" :class="showBtn ? 'pb-12' : ''">
      <view class="box-border px-4">
        <BillDetail :bill="bill" />
      </view>
      <view v-if="showBtn" class="absolute bottom-0 left-4 right-4 box-border h-8 flex items-center justify-center gap-3">
        <wd-button type="error" custom-class="flex-1" @click="deleteBill">
          删除
        </wd-button>
        <wd-button class="flex-1" type="primary" @click="updateBill">
          编辑
        </wd-button>
      </view>
    </view>
  </wd-action-sheet>
</template>
