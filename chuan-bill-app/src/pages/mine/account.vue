<script setup lang="ts">
import AccountDeletePopup from './components/AccountDeletePopup.vue'
import PasswordChangePopup from './components/PasswordChangePopup.vue'
import PhoneChangePopup from './components/PhoneChangePopup.vue'

definePage({
  name: 'account',
  layout: 'default',
  style: {
    navigationBarTitleText: '账号与安全',
  },
})

const userStore = useUserStore()
const message = useGlobalMessage()

onLoad(() => {
  if (!userStore.isLoggedIn) {
    userStore.requireAuth(() => {})
  }
})

// 弹框状态
const showPasswordModal = ref(false)
const showPhoneModal = ref(false)
const showDeleteModal = ref(false)

// 注销账号 - 第一步：确认弹框
function handleDeleteAccount() {
  message.confirm({
    title: '注销账号',
    msg: '注销后，所有数据将被永久删除且无法恢复。确定要注销吗？',
    beforeConfirm: async ({ resolve }) => {
      resolve(true)
    },
    success: (res) => {
      if (res.action === 'confirm') {
        showDeleteModal.value = true
      }
    },
  })
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- 账号信息 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        账号信息
      </view>
      <!-- 手机号 -->
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="showPhoneModal = true"
      >
        <text class="text-sm">
          手机号
        </text>
        <view class="flex items-center gap-1">
          <text class="text-sm text-gray-400">
            {{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>
      <!-- 修改密码 -->
      <view
        class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700"
        @click="showPasswordModal = true"
      >
        <text class="text-sm">
          修改密码
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 安全设置（仅有手机号时显示） -->
    <view v-if="userStore.phone" class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        安全设置
      </view>
      <!-- 注销账号 -->
      <view
        class="flex items-center justify-between px-4 py-4"
        @click="handleDeleteAccount"
      >
        <text class="text-sm text-red-500">
          注销账号
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 修改密码弹框 -->
    <PasswordChangePopup v-model="showPasswordModal" />

    <!-- 修改手机号弹框 -->
    <PhoneChangePopup v-model="showPhoneModal" />

    <!-- 注销账号弹框 -->
    <AccountDeletePopup v-model="showDeleteModal" />
  </view>
</template>
