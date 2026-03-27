<script setup lang="ts">
import type { AddBillDTO } from '@/api/globals'

interface PickerOption {
  label?: string
  value?: string | number
  disabled?: boolean
}

interface CategoryMap {
  income: PickerOption[]
  expense: PickerOption[]
}

const form = ref()
const isShared = ref(false)

const formData = ref<AddBillDTO>({ name: '', type: 'expense', amount: '', time: '' })
const categoryMap: CategoryMap = { income: [], expense: [] }
const categoryOptions = ref<PickerOption[]>()
const paymentMethodOptions = ref<PickerOption[]>()

function getCategoryList() {
  Apis.bill.getCategories({ params: { } })
    .then((res) => {
      if (res.success) {
        const list = res.data || []
        for (const item of list) {
          const option: PickerOption = { label: item.name, value: item.id }
          if (item.type === 'income') {
            categoryMap.income.push(option)
          }
          if (item.type === 'expense') {
            categoryMap.expense.push(option)
          }
        }
        categoryOptions.value = categoryMap[formData.value.type]
      }
    })
}

function getPayMethodList() {
  Apis.bill.getPaymentMethods().then((res) => {
    if (res.success) {
      paymentMethodOptions.value = res.data?.map(item => ({ label: item.name, value: item.id }))
    }
  })
}

onLoad(() => {
  getCategoryList()
  getPayMethodList()
})

watch(() => formData.value.type, (newValue) => {
  categoryOptions.value = categoryMap[newValue]
  formData.value.categoryId = undefined
})
</script>

<template>
  <wd-form :ref="form" :model="formData" custom-class="flex-1 overflow-auto">
    <wd-radio-group v-model="formData.type" shape="button" custom-class="flex">
      <wd-radio value="expense" custom-class="flex-1 expense-radio">
        支出
      </wd-radio>
      <wd-radio value="income" custom-class="flex-1 income-radio">
        收入
      </wd-radio>
    </wd-radio-group>
    <view class="mt-3 text-xs text-gray-500">
      金额
    </view>
    <wd-input
      v-model="formData.amount" no-border type="digit" placeholder="0.00" custom-class="mt-2"
      custom-input-class="!text-[32px] !font-bold"
    />
    <view class="mt-3 text-xs text-gray-500">
      名称
    </view>
    <wd-input
      v-model="formData.name" type="text" placeholder="账单名称" custom-class="mt-2" no-border
      custom-input-class="!text-4"
      :maxlength="100"
    />
    <wd-divider color="#dddddd" custom-class="!mt-2 !px-0" />
    <view class="mt-3 text-xs text-gray-500">
      时间
    </view>
    <wd-datetime-picker v-model="formData.time" :default-value="Date.now()" custom-class="mt-3" />
    <wd-divider color="#dddddd" custom-class="!mt-2 !px-0" />
    <view class="mt-3 flex gap-3">
      <view class="flex-1">
        <view class="text-xs text-gray-500">
          <text class="i-lucide:tag mr-2" />类目
        </view>
        <wd-picker v-model="formData.categoryId" :columns="categoryOptions" title="请选择账单类目" placeholder="请选择" custom-class="mt-2 " />
      </view>
      <view class="flex-1">
        <view class="text-xs text-gray-500">
          <text class="i-lucide:credit-card mr-2" />支付方式
        </view>
        <wd-picker v-model="formData.paymentMethodId" :columns="paymentMethodOptions" title="请选择支付方式" placeholder="请选择" custom-class="mt-2" />
      </view>
    </view>
    <wd-divider color="#dddddd" custom-class="!mt-2 !px-0" />
    <view class="mt-3 flex items-center justify-between text-xs text-gray-500">
      <text>共享到家庭</text>
      <wd-switch v-model="isShared" size="18px" />
    </view>
    <template v-if="isShared">
      <wd-picker v-model="formData.familyId" custom-class="mt-3" title="请选择所要共享的家庭" />
    </template>
    <view class="mt-3 text-xs text-gray-500">
      备注
    </view>
    <wd-textarea
      v-model="formData.remark" placeholder="可输入关于该账单的更多信息" custom-class="mt-2 !p-0 mb-10" no-border
      custom-input-class="!text-4"
      :maxlength="500"
      show-word-limit
    />
    <wd-button size="large" type="primary" block custom-class="!pos-absolute bottom-[10px] left-4 right-4 z-1">
      保存
    </wd-button>
  </wd-form>
</template>

<style lang="scss" scoped>
:deep(.wd-radio.is-button .wd-radio__label) {
  max-width: none;
  width: 100%;
  @apply h-8 items-center flex justify-center py-0;
}

.expense-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
      @apply important-border-red-100 important-bg-red-100 important-text-red-600;
    }
  }
}

.income-radio {
  margin-right: 0 !important;

  &.is-checked {
    :deep(.wd-radio__label) {
      @apply important-border-green-100 important-bg-green-100 important-text-green-600;
    }
  }
}
:deep(.wd-cell.wd-picker__cell){
  @apply bg-gray-50 bg-opacity-80 rounded-xl;
}
:deep(.wd-cell.wd-datetime-picker__cell){
    @apply bg-gray-50 bg-opacity-80 rounded-xl;
}
:deep(.wd-textarea__inner){
  height: 70px;
}
</style>
