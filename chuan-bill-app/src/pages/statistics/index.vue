<script setup lang="ts">
definePage({
  name: 'statistics',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '统计',
  },
})
</script>

<template>
  <view class="py-3">
    <view class="mx-3 rounded-3 bg-white px-4 py-6 text-center dark:bg-[var(--wot-dark-background2)]">
      <text class="mb-2 block text-5 text-gray-800 font-bold dark:text-[var(--wot-dark-color)]">
        小川记账
      </text>
      <text class="block text-3 text-gray-400 dark:text-[var(--wot-dark-color2)]">
        个人与家庭记账
      </text>
    </view>
  </view>
</template>
