<script setup lang="ts">
import type { AddBillDTO } from '@/api/globals'

defineOptions({
  name: 'ManualEdit',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const emit = defineEmits<{
  submit: [data: AddBillDTO]
}>()

interface PickerOption {
  label?: string
  value?: string | number
  disabled?: boolean
}

const form = ref()
const isShared = ref(false)
const billStore = useBillStore()
const user = useUserStore()
const familyStore = useFamilyStore()

const formData = defineModel<AddBillDTO>({ required: true, default: { source: 'manual', type: 'expense' } })
const categoryOptions = computed<PickerOption[]>(() => billStore.getCategoryList(formData.value.type).map(category => ({ label: category.name, value: category.id })))
const paymentMethodOptions = computed<PickerOption[]>(() => billStore.getPaymentMethodList().map(paymentMethod => ({ label: paymentMethod.name, value: paymentMethod.id })))
const familyOptions = computed<PickerOption[]>(() => familyStore.familyList.map(f => ({ label: f.name, value: f.id })))

onLoad(async () => {
  await billStore.initBillData()
  // 已登录时加载家庭列表
  if (user.isLoggedIn && !familyStore.hasFamily) {
    await familyStore.fetchFamilyList()
  }
})

watch(() => formData.value.type, () => {
  formData.value.categoryId = undefined
  formData.value.paymentMethodId = undefined
})

// 关闭家庭共享时清除 familyId
watch(isShared, (val) => {
  if (!val) {
    formData.value.familyId = undefined
  }
})

function sumbit() {
  form.value?.validate().then(({ valid }: { valid: boolean }) => {
    if (valid) {
      emit('submit', formData.value)
    }
  }).catch((error: Error) => {
    console.error(error)
  })
}
</script>

<template>
  <wd-form ref="form" :model="formData" custom-class="flex-1 overflow-auto" error-type="toast">
    <wd-radio-group
      v-model="formData.type" shape="button" custom-class="flex mt-1" prop="type"
      :rules="[{ required: true, message: '请选择账单类型' }]"
    >
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
      v-model="formData.amount" :rules="[{ required: true, message: '请输入账单金额' }]" no-border type="digit" placeholder="0.00" custom-class="mt-2"
      prop="amount"
      custom-input-class="!text-[32px] !font-bold"
    />
    <view class="mt-3 text-xs text-gray-500">
      名称
    </view>
    <wd-input
      v-model="formData.name" type="text" placeholder="账单名称" custom-class="mt-2" no-border
      prop="name"
      :rules="[{ required: true, message: '请输入账单名称' }]"
      custom-input-class="!text-4"
      :maxlength="100"
    />
    <wd-divider custom-class="!mt-2 !px-0" />
    <view class="mt-3 text-xs text-gray-500">
      时间
    </view>
    <wd-datetime-picker
      v-model="formData.time" :default-value="Date.now()" custom-class="mt-3"
      prop="time"
      :rules="[{ required: true, message: '请选择账单时间' }]"
    />
    <wd-divider custom-class="!mt-2 !px-0 dark:hidden!" />
    <view class="mt-3 flex gap-3">
      <view class="flex-1">
        <view class="text-xs text-gray-500">
          <text class="i-lucide:tag mr-2" />类目
        </view>
        <wd-picker
          v-model="formData.categoryId" :columns="categoryOptions" title="请选择账单类目" placeholder="请选择" custom-class="mt-2" prop="categoryId"
          :rules="[{ required: true, message: '请选择账单类目' }]"
        />
      </view>
      <view class="flex-1">
        <view class="text-xs text-gray-500">
          <text class="i-lucide:credit-card mr-2" />支付方式
        </view>
        <wd-picker
          v-model="formData.paymentMethodId" :columns="paymentMethodOptions" title="请选择支付方式" placeholder="请选择" custom-class="mt-2" prop="paymentMethodId"
          :rules="[{ required: true, message: '请选择支付方式' }]"
        />
      </view>
    </view>
    <wd-divider custom-class="!mt-2 !px-0 dark:hidden!" />
    <!-- 游客模式下不显示家庭共享 -->
    <template v-if="user.isLoggedIn">
      <view class="mt-3 flex items-center justify-between text-xs text-gray-500">
        <text>共享到家庭</text>
        <wd-switch v-model="isShared" size="18px" />
      </view>
      <template v-if="isShared">
        <wd-picker
          v-model="formData.familyId" custom-class="mt-3" title="请选择所要共享的家庭"
          :columns="familyOptions" prop="familyId"
          :rules="[{ required: true, message: '请选择所要共享的家庭' }]"
        />
      </template>
    </template>
    <view class="mt-3 text-xs text-gray-500">
      备注(可选)
    </view>
    <wd-textarea
      v-model="formData.remark" placeholder="可输入关于该账单的更多信息" custom-class="mt-2 !p-0 mb-10" no-border
      custom-input-class="!text-4"
      :maxlength="500"
      show-word-limit
    />
    <wd-button type="primary" block custom-class="!pos-absolute bottom-[10px] left-4 right-4 z-1" @click="sumbit">
      保存
    </wd-button>
  </wd-form>
</template>

<style lang="scss" scoped>
:deep(.wd-radio.is-button .wd-radio__label) {
  max-width: none !important;
  width: 100%;
  @apply h-8 items-center flex justify-center py-0 dark:border-black/30!;
}

.expense-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
      @apply border-red-100! bg-red-100! text-red-600! shadow-red-100/20 shadow-lg;
    }
  }
}

.income-radio {
  margin-right: 0 !important;

  &.is-checked {
    :deep(.wd-radio__label) {
      @apply border-green-100! bg-green-100! text-green-600! shadow-green-100/20 shadow-lg;
    }
  }
}
:deep(.wd-cell.wd-picker__cell) {
  @apply bg-gray-50/80 rounded-xl dark:bg-black/30;
}
:deep(.wd-cell.wd-datetime-picker__cell) {
    @apply bg-gray-50/80 rounded-xl dark:bg-black/30;
}
:deep(.wd-textarea__inner){
  height: 70px;
}
:deep(.wd-icon-arrow-right) {
  @apply dark:text-white/50!;
}
</style>
