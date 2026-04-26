<script setup lang="ts">
definePage({
  name: 'password-edit',
  layout: 'default',
  style: {
    navigationBarTitleText: '修改密码',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()
const router = useRouter()

// 表单数据
const formData = ref({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

// 倒计时
const countdown = ref(0)
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

// 页面加载
onLoad(() => {
  if (!userStore.isLoggedIn) {
    userStore.requireAuth(() => {})
  }
})

onUnload(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

// 发送验证码
async function sendCode() {
  if (countdown.value > 0 || sending.value)
    return
  if (!userStore.phone) {
    toast.warning('未绑定手机号')
    return
  }

  sending.value = true
  try {
    const res = await Apis.auth.sendCode({ data: { phone: userStore.phone } })
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

// 密码强度
const passwordStrength = computed(() => {
  const pwd = formData.value.newPassword
  if (!pwd)
    return 0
  let strength = 0
  if (pwd.length >= 6)
    strength++
  if (/[a-z]/i.test(pwd) && /\d/.test(pwd))
    strength++
  if (/[^a-z0-9]/i.test(pwd))
    strength++
  if (pwd.length >= 10)
    strength++
  return Math.min(strength, 3)
})

const strengthText = ['弱', '中', '强', '极强']
const strengthColor = ['text-red-500', 'text-yellow-500', 'text-green-500', 'text-green-600']

// 提交
async function handleSubmit() {
  if (!formData.value.code) {
    toast.warning('请输入验证码')
    return
  }
  if (!formData.value.newPassword) {
    toast.warning('请输入新密码')
    return
  }
  if (formData.value.newPassword.length < 6) {
    toast.warning('密码长度不能少于6位')
    return
  }
  if (formData.value.newPassword !== formData.value.confirmPassword) {
    toast.warning('两次输入的密码不一致')
    return
  }

  try {
    const res = await Apis.user.updatePasswordByCode({
      data: {
        phone: userStore.phone!,
        code: formData.value.code,
        newPassword: formData.value.newPassword,
      },
    })
    if (res.success) {
      toast.success('密码修改成功，请重新登录')
      setTimeout(() => {
        userStore.logout()
        router.replace('/pages/bill/index')
      }, 1500)
    }
    else {
      toast.error(res.message || '修改失败')
    }
  }
  catch {
    toast.error('修改失败，请重试')
  }
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- 手机号 -->
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="flex items-center justify-between py-2">
        <text class="text-sm text-gray-600">
          手机号
        </text>
        <text class="text-sm text-gray-900 dark:text-white">
          {{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
        </text>
      </view>
    </view>

    <!-- 验证码 -->
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="flex items-center gap-3">
        <wd-input
          v-model="formData.code"
          placeholder="请输入验证码"
          :maxlength="6"
          type="number"
          no-border
          custom-class="flex-1"
        />
        <wd-button
          :disabled="countdown > 0 || !userStore.phone"
          size="small"
          @click="sendCode"
        >
          {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
        </wd-button>
      </view>
    </view>

    <!-- 新密码 -->
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <wd-input
        v-model="formData.newPassword"
        placeholder="请输入新密码"
        type="password"
        :maxlength="20"
        no-border
      />
      <view v-if="formData.newPassword" class="mt-2 flex items-center gap-2">
        <text class="text-xs text-gray-500">
          密码强度：
        </text>
        <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
          {{ strengthText[passwordStrength - 1] || '弱' }}
        </text>
      </view>
    </view>

    <!-- 确认密码 -->
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <wd-input
        v-model="formData.confirmPassword"
        placeholder="请确认新密码"
        type="password"
        :maxlength="20"
        no-border
      />
    </view>

    <!-- 提示 -->
    <text class="px-2 text-xs text-gray-400">
      密码需6-20位，建议包含字母和数字
    </text>

    <!-- 提交按钮 -->
    <wd-button type="primary" block @click="handleSubmit">
      确认修改
    </wd-button>
  </view>
</template>
