<script setup lang="ts">
defineOptions({
  name: 'OcrEdit',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})
</script>

<template>
  <view class="flex flex-col gap-3">
    <view class="h-50 w-full flex flex-col items-center justify-center gap-3 border-2 border-gray-300 rounded-lg border-dashed dark:border-gray-600">
      <view class="h-16 w-16 flex items-center justify-center rounded-full bg-primary/10">
        <view class="i-lucide:camera text-3xl text-primary" />
      </view>
      <text class="text-xs text-gray-500">
        点击此处上传图片，AI将自动解析账单
      </text>
    </view>
  </view>
</template>
