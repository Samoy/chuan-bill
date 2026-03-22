<script setup lang="ts">
import type { AddBillDTO } from '@/api/globals'

const fromData = ref<AddBillDTO>({
  type: 'expense',
} as AddBillDTO)
const form = ref()
</script>

<template>
  <wd-form :ref="form" :model="fromData">
    <wd-radio-group v-model="fromData.type" shape="button" custom-class="flex">
      <wd-radio value="expense" custom-class="flex-1 expense-radio">
        支出
      </wd-radio>
      <wd-radio value="income" custom-class="flex-1 income-radio">
        收入
      </wd-radio>
    </wd-radio-group>
  </wd-form>
</template>

<style lang="scss" scoped>
:deep(.wd-radio.is-button .wd-radio__label) {
  max-width: none;
  width: 100%;
}

.expense-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
     @apply important-border-red-100 important-bg-red-100 important-text-red-600;
    }
  }
}

.income-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
      @apply important-border-green-100 important-bg-green-100 important-text-green-600;
    }
  }
}
</style>
