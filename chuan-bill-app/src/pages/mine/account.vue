<script setup lang="ts">
definePage({
  name: 'account',
  layout: 'default',
  style: {
    navigationBarTitleText: '账号与安全',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()
const message = useGlobalMessage()

onLoad(() => {
  if (!userStore.isLoggedIn) {
    userStore.requireAuth(() => {})
  }
})

// 弹框状态
const showPasswordModal = ref(false)
const showPhoneModal = ref(false)

// 注销账号
function handleDeleteAccount() {
  message.confirm({
    title: '注销账号',
    msg: '注销后，所有数据将被永久删除且无法恢复。确定要注销吗？',
    beforeConfirm: async ({ resolve }) => {
      // TODO: 调用后端注销账号接口
      toast.info('功能开发中')
      resolve(false)
    },
  })
}

// 设备管理
function goToDeviceManagement() {
  // TODO: 跳转到设备管理页面
  toast.info('功能开发中')
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

    <!-- 安全设置 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        安全设置
      </view>
      <!-- 登录设备 -->
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="goToDeviceManagement"
      >
        <text class="text-sm">
          登录设备管理
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
      <!-- 注销账号 -->
      <view
        class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700"
        @click="handleDeleteAccount"
      >
        <text class="text-sm text-red-500">
          注销账号
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 修改密码弹框 -->
    <PasswordChangeModal v-model="showPasswordModal" />

    <!-- 修改手机号弹框 -->
    <PhoneChangeModal v-model="showPhoneModal" />
  </view>
</template>
