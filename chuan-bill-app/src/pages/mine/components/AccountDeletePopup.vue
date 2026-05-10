<script setup lang="ts">
defineOptions({ name: 'AccountDeleteModal' })

const modelValue = defineModel<boolean>({ default: false })
const userStore = useUserStore()
const toast = useGlobalToast()
const router = useRouter()

// 验证码
const code = ref('')
const countdown = ref(0)
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

// 提交状态
const loading = ref(false)

// 监听弹窗打开/关闭
watch(modelValue, (val) => {
  if (val) {
    // 打开时重置
    code.value = ''
    countdown.value = 0
    loading.value = false
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }
})

// 发送验证码
async function sendCode() {
  if (countdown.value > 0 || sending.value)
    return
  sending.value = true
  try {
    const res = await Apis.user.getPhoneCode()
    if (res.success) {
      toast.success('验证码已发送')
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0 && timer) {
          clearInterval(timer)
          timer = null
        }
      }, 1000)
    }
    else {
      toast.error(res.message || '发送失败')
    }
  }
  catch {
    toast.error('发送失败，请重试')
  }
  finally {
    sending.value = false
  }
}

// 提交注销
async function handleSubmit() {
  if (!code.value) {
    toast.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const res = await Apis.user.deleteAccount({ data: { code: code.value } })
    if (res.success) {
      toast.success('账号已注销')
      modelValue.value = false
      userStore.logout()
      router.replaceAll('/')
    }
    else {
      toast.error(res.message || '注销失败')
    }
  }
  catch {
    toast.error('注销失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 清除定时器
onUnload(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    title="注销账号"
    :z-index="999"
    safe-area-inset-bottom
    :close-on-click-modal="false"
  >
    <view class="p-4">
      <!-- 警告文案 -->
      <view class="mb-4 rounded-xl bg-red-50 p-3 dark:bg-red-900/20">
        <view class="flex items-start gap-2">
          <view class="i-lucide:alert-triangle mt-0.5 h-4 w-4 shrink-0 text-red-500" />
          <text class="text-sm text-red-600 dark:text-red-400">
            注销后，您的所有数据将被永久删除且无法恢复，请谨慎操作。
          </text>
        </view>
      </view>

      <!-- 手机号展示 -->
      <view class="mb-4 flex items-center gap-2 text-sm text-gray-500">
        <view class="i-lucide:smartphone h-4 w-4" />
        <text>验证手机号：</text>
        <text>{{ userStore.phone?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') }}</text>
      </view>

      <!-- 验证码输入 -->
      <wd-input
        v-model="code"
        type="number"
        placeholder="请输入验证码"
        :maxlength="6"
        custom-class="login-input"
      >
        <template #prefix>
          <view class="i-lucide:message-square text-gray-400" />
        </template>
        <template #suffix>
          <view
            class="cursor-pointer text-sm"
            :class="countdown > 0 ? 'text-gray-400' : 'text-primary'"
            @click="sendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </view>
        </template>
      </wd-input>

      <!-- 确认按钮 -->
      <wd-button
        type="error"
        round
        block
        :loading="loading"
        custom-class="mt-2"
        @click="handleSubmit"
      >
        确认注销
      </wd-button>
    </view>
  </wd-action-sheet>
</template>

<style lang="scss" scoped>
:deep(.login-input) {
  @apply rounded-2xl bg-gray-100 px-3 py-1 dark:bg-gray-700;

  &::after {
    display: none !important;
  }

  .wd-icon {
    background: none;
  }
}
</style>
