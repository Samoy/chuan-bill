<script setup lang="ts">
definePage({
  name: 'help-feedback',
  layout: 'default',
  style: {
    navigationBarTitleText: '帮助与反馈',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()

// FAQ 数据（客户端内置）
const faqList = [
  {
    question: '如何添加账单？',
    answer: '点击首页底部的"+"按钮，选择记账方式（手动/拍照/语音），填写账单信息后保存即可。',
  },
  {
    question: '如何创建/加入家庭？',
    answer: '在"家庭"页面点击"创建家庭"或"加入家庭"，创建后会自动生成邀请码，分享给家人即可加入。',
  },
  {
    question: '数据安全吗？会丢失吗？',
    answer: '未登录时数据仅存储在本地；登录后数据会同步到云端，换设备登录后可恢复数据。我们采用加密传输和存储，保障数据安全。',
  },
  {
    question: '如何导出账单？',
    answer: '登录后进入"我的"-"设置"-"数据管理"，可选择导出Excel或PDF格式的账单数据。',
  },
]

// 展开状态
const expandedIndex = ref<number | null>(null)

function toggleExpand(index: number) {
  expandedIndex.value = expandedIndex.value === index ? null : index
}

// AI客服入口
function goToAiChat() {
  toast.info('AI客服功能即将上线，敬请期待')
  // TODO: 接入百炼平台AI客服
  // router.push('/pages/mine/ai-chat/index')
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- AI 客服入口（仅登录显示） -->
    <view
      v-if="userStore.isLoggedIn"
      class="rounded-2xl from-primary to-primary/80 bg-gradient-to-br p-5 text-white shadow-lg"
      @click="goToAiChat"
    >
      <view class="flex items-center gap-3">
        <view class="h-12 w-12 flex items-center justify-center rounded-full bg-white/20">
          <view class="i-lucide:bot h-6 w-6" />
        </view>
        <view class="flex-1">
          <text class="block text-base font-bold">
            AI 智能客服
          </text>
          <text class="mt-0.5 block text-xs text-white/80">
            有任何问题都可以问我
          </text>
        </view>
        <view class="i-lucide:chevron-right h-5 w-5" />
      </view>
    </view>

    <!-- 未登录提示 -->
    <view
      v-else
      class="rounded-2xl bg-blue-50 p-5 dark:bg-blue-900/20"
      @click="userStore.showLoginPopup = true"
    >
      <view class="flex items-center gap-3">
        <view class="h-12 w-12 flex items-center justify-center rounded-full bg-blue-100 dark:bg-blue-800">
          <view class="i-lucide:bot h-6 w-6 text-blue-600 dark:text-blue-400" />
        </view>
        <view class="flex-1">
          <text class="block text-sm text-blue-800 font-medium dark:text-blue-200">
            登录后使用 AI 客服
          </text>
          <text class="mt-0.5 block text-xs text-blue-600 dark:text-blue-300">
            智能解答您的所有问题
          </text>
        </view>
        <wd-button type="primary" size="small">
          去登录
        </wd-button>
      </view>
    </view>

    <!-- 常见问题 -->
    <view>
      <text class="mb-3 block text-sm text-gray-600 font-medium dark:text-gray-400">
        常见问题
      </text>
      <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view
          v-for="(item, index) in faqList"
          :key="index"
          class="faq-item"
          :class="[
            index < faqList.length - 1 && 'border-b border-gray-100 dark:border-gray-700',
            expandedIndex === index && 'is-expanded',
          ]"
        >
          <view
            class="flex items-center justify-between p-4"
            @click="toggleExpand(index)"
          >
            <text class="flex-1 text-sm">
              {{ item.question }}
            </text>
            <view
              class="i-lucide:chevron-down h-4 w-4 text-gray-400 transition-transform duration-200"
              :class="expandedIndex === index && 'rotate-180'"
            />
          </view>
          <view
            v-show="expandedIndex === index"
            class="px-4 pb-4"
          >
            <text class="block text-xs text-gray-500 leading-relaxed dark:text-gray-400">
              {{ item.answer }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 联系客服 -->
    <view class="mt-4 rounded-2xl bg-white p-4 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <text class="block text-xs text-gray-400">
        还有其他问题？
      </text>
      <text class="mt-1 block text-sm text-primary">
        反馈邮箱：feedback@chuanbill.com
      </text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.faq-item {
  .is-expanded {
    background-color: rgba(0, 0, 0, 0.02);
  }
}
</style>
