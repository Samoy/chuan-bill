<script setup lang="ts">
definePage({
  name: 'message-list',
  style: {
    navigationBarTitleText: '消息通知',
  },
})

const messageStore = useMessageStore()
const currentPage = ref(1)
const pageSize = 20
const finished = ref(false)
const loading = ref(false)

async function loadMessages(page = 1) {
  loading.value = true
  try {
    const result = await messageStore.fetchMessageList({
      page,
      pageSize,
    })
    if (result) {
      currentPage.value = page
      finished.value = !result.records || result.records.length < pageSize
    }
    else {
      finished.value = true
    }
  }
  finally {
    loading.value = false
  }
}

onLoad(() => {
  loadMessages(1)
})

// 加载更多
function loadMore() {
  if (!finished.value && !loading.value) {
    loadMessages(currentPage.value + 1)
  }
}

// 全部已读
async function markAllRead() {
  uni.showModal({
    title: '全部标记已读',
    content: '确认将所有消息标记为已读？',
    success: async (res) => {
      if (res.confirm) {
        const success = await messageStore.markAllAsRead()
        if (success) {
          const toast = useGlobalToast()
          toast.showToast('已全部标记为已读')
        }
      }
    },
  })
}

// 点击消息标记已读
async function handleMessageClick(msg: { id: string, status: number }) {
  if (msg.status === 0) {
    await messageStore.markAsRead(msg.id)
  }
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 顶部操作 -->
    <view v-if="messageStore.hasUnread" class="mx-3 flex justify-end">
      <text class="text-sm text-primary" @click="markAllRead">
        全部已读
      </text>
    </view>

    <!-- 消息列表 -->
    <view v-if="messageStore.messageList.length > 0" class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view
        v-for="(msg, index) in messageStore.messageList"
        :key="msg.id"
        class="p-4"
        :class="[
          index < messageStore.messageList.length - 1 && 'border-b border-gray-100 dark:border-gray-700',
          msg.status === 0 && 'bg-blue-50/50 dark:bg-blue-900/10',
        ]"
        @click="handleMessageClick(msg)"
      >
        <view class="flex items-start gap-3">
          <!-- 未读标记 -->
          <view v-if="msg.status === 0" class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" />
          <view v-else class="mt-1.5 h-2 w-2 shrink-0" />
          <view class="flex-1">
            <view class="flex items-center justify-between">
              <text class="text-sm font-500" :class="msg.status === 0 ? 'text-gray-900 dark:text-white' : 'text-gray-500'">
                {{ msg.title }}
              </text>
              <text class="text-xs text-gray-400">
                {{ msg.createTime }}
              </text>
            </view>
            <text class="mt-1 block text-xs leading-relaxed" :class="msg.status === 0 ? 'text-gray-600' : 'text-gray-400'">
              {{ msg.content }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!loading" class="mx-3 rounded-2xl bg-white p-8 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="mb-3 flex justify-center">
        <view class="h-16 w-16 flex items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
          <view class="i-lucide:bell h-8 w-8 text-gray-400" />
        </view>
      </view>
      <text class="block text-sm text-gray-500">
        暂无消息
      </text>
    </view>

    <!-- 加载更多 -->
    <view v-if="loading" class="py-4 text-center">
      <wd-loading />
    </view>
    <view v-else-if="finished && messageStore.messageList.length > 0" class="py-4 text-center">
      <text class="text-xs text-gray-400">
        没有更多消息了
      </text>
    </view>
    <view v-else-if="!finished && messageStore.messageList.length > 0" class="py-4 text-center">
      <text class="text-sm text-primary" @click="loadMore">
        加载更多
      </text>
    </view>
  </view>
</template>
