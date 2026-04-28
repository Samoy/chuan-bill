<script setup lang="ts">
definePage({
  name: 'phone-change',
  layout: 'default',
  style: {
    navigationBarTitleText: '更换手机号',
  },
})

const toast = useGlobalToast()

function handleSubmit() {
  toast.info('功能开发中，敬请期待')
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <view class="rounded-2xl bg-white p-8 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="mb-3 flex justify-center">
        <view class="h-16 w-16 flex items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
          <view class="i-lucide:smartphone h-8 w-8 text-gray-400" />
        </view>
      </view>
      <text class="block text-sm text-gray-500">
        更换手机号功能即将上线
      </text>
      <text class="mt-2 block text-xs text-gray-400">
        您可以先使用其他功能
      </text>
    </view>

    <wd-button type="primary" block @click="handleSubmit">
      我知道了
    </wd-button>
  </view>
</template>
