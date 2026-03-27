<script setup lang="ts">
import QuickBillModal from './components/QuickBillModal.vue'

definePage({
  name: 'bill',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '账单',
    enablePullDownRefresh: true,
    onReachBottomDistance: 50,
  },
})
const searchValue = ref('')
const showFilterModal = ref(false)
const showQuickBillModal = ref(false)

const safeAreaBottomHeight = uni.getWindowInfo().safeAreaInsets.bottom
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 搜索区域 -->
    <view class="border-b border-[var(--wot-border-color)] px-3">
      <view class="flex items-center gap-2">
        <wd-search
          v-model="searchValue" placeholder="账单名称或备注" hide-cancel
          custom-class="flex-1 rounded-xl border border-solid border-[var(--wot-color-border)] dark:border-gray-600"
        />
        <view
          class="relative flex items-center justify-center border border-[var(--wot-color-border)] rounded-xl border-solid bg-white p-2 text-gray-600 transition-all active:scale-95 dark:border-gray-600 dark:bg-[var(--wot-dark-background2)]"
          @click="showFilterModal = true"
        >
          <view class="i-lucide:filter" />
        </view>
      </view>
    </view>

    <!-- FAB 按钮 -->
    <wd-fab draggable :expandable="false" :gap="{ bottom: 70 + safeAreaBottomHeight, right: 20 }" @click="showQuickBillModal = true" />
    <QuickBillModal v-model:show="showQuickBillModal" />
  </view>
</template>

<style lang="scss" scoped>
:deep(.wot-theme-dark .wd-search__cover) {
  background-color: transparent;
}

:deep(.wot-theme-dark .wd-search__block) {
  background-color: transparent;
}
</style>
