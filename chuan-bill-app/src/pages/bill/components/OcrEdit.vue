<script setup lang="ts">
defineOptions({
  name: 'OcrEdit',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const imageSrc = ref('')

function uploadImage() {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      imageSrc.value = res.tempFilePaths[0]
    },
  })
}
</script>

<template>
  <view class="flex flex-col gap-3">
    <view
      class="relative h-50 w-full flex flex-col items-center justify-center gap-3 border-2 border-gray-300 rounded-xl border-dashed dark:border-gray-600"
      @click="uploadImage"
    >
      <template v-if="imageSrc">
        <image mode="aspectFit" :src="imageSrc" class="h-full w-full" />
      </template>
      <template v-else>
        <view class="h-16 w-16 flex items-center justify-center rounded-full bg-primary/10">
          <view class="i-lucide:camera text-3xl text-primary" />
        </view>
        <text class="text-xs text-gray-500">
          点击此处上传图片，AI将自动解析账单
        </text>
      </template>
      <view v-if="imageSrc" class="absolute inset-0 z-1 rounded-xl bg-black/60">
        <view class="slide absolute left-0 right-0 top-0 h-1 w-full bg-primary" />
      </view>
    </view>
    <view v-if="imageSrc" class="flex items-center justify-center">
      <text class="text-xs text-gray-500">
        AI解析中，请稍候...
      </text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.slide {
  animation: slide 2s linear infinite alternate;
}

@keyframes slide {
  0% {
    top: 4px;
    transform: translateY(4px);
  }
  100% {
    top: calc(100% - 4px);
    transform: translateY(calc(-100% - 4px));
  }
}
</style>
