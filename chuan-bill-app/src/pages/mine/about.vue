<script setup lang="ts">
definePage({
  name: 'about',
  layout: 'default',
  style: {
    navigationBarTitleText: '关于',
  },
})

const version = ref<string>('1.0.0')

onLoad(() => {
  // #ifdef APP-PLUS
  version.value = plus.runtime.version || '1.0.0'
  // #endif
  // #ifdef MP-WEIXIN
  try {
    const accountInfo = wx.getAccountInfoSync()
    version.value = accountInfo.miniProgram.version || '1.0.0'
  }
  catch {
    version.value = '1.0.0'
  }
  // #endif
})

// 第三方库列表
const openSourceLibs = [
  { name: 'Vue', version: '3.x', license: 'MIT' },
  { name: 'uni-app', version: '3.x', license: 'Apache-2.0' },
  { name: 'wot-design-uni', version: 'latest', license: 'MIT' },
  { name: 'Pinia', version: '2.x', license: 'MIT' },
  { name: 'Alova', version: 'latest', license: 'MIT' },
]
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- 应用信息 -->
    <view class="flex flex-col items-center py-8">
      <wd-img src="https://chuan-bill-cdn.samoy.site/default/logo.png" custom-class="w-20 h-20 rounded-2xl overflow-hidden" />
      <text class="mt-4 text-lg font-bold">
        小川记账
      </text>
      <text class="mt-1 text-sm text-gray-400">
        版本 {{ version }}
      </text>
    </view>

    <!-- 算法备案信息 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        算法备案信息
      </view>
      <view class="px-4 pb-4">
        <view class="mb-2">
          <text class="block text-xs text-gray-500">
            算法名称
          </text>
          <text class="mt-0.5 block text-sm">
            阿里云百炼大模型服务
          </text>
        </view>
        <view class="mb-2">
          <text class="block text-xs text-gray-500">
            算法备案号
          </text>
          <text class="mt-0.5 block text-sm">
            [待填写]
          </text>
        </view>
        <view>
          <text class="block text-xs text-gray-500">
            服务提供者
          </text>
          <text class="mt-0.5 block text-sm">
            阿里云
          </text>
        </view>
      </view>
    </view>

    <!-- 开源许可 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        开源组件致谢
      </view>
      <view
        v-for="(lib, index) in openSourceLibs"
        :key="lib.name"
        class="flex items-center justify-between px-4 pb-4"
        :class="index < openSourceLibs.length - 1 && 'border-b border-gray-100 dark:border-gray-700'"
      >
        <view>
          <text class="block text-sm">
            {{ lib.name }}
          </text>
          <text class="mt-0.5 block text-xs text-gray-400">
            {{ lib.license }} License
          </text>
        </view>
        <text class="text-xs text-gray-400">
          {{ lib.version }}
        </text>
      </view>
    </view>

    <!-- 联系我们 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        联系我们
      </view>
      <view class="px-4 pb-4">
        <view class="flex items-center gap-2">
          <view class="i-lucide:mail h-4 w-4 text-gray-400" />
          <text class="text-sm">
            feedback@chuanbill.com
          </text>
        </view>
      </view>
    </view>

    <!-- 版权信息 -->
    <view class="py-4 text-center">
      <text class="text-xs text-gray-400">
        © 2025 小川记账 版权所有
      </text>
    </view>
  </view>
</template>
