<script setup lang="ts">
import { SmsScene } from '@/constant/sms'

defineOptions({
  name: 'PasswordChangeModal',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const modelValue = defineModel<boolean>({ default: false })
const userStore = useUserStore()
const toast = useGlobalToast()
const router = useRouter()

// 状态
const activeTab = ref(0) // 0: 验证码验证, 1: 密码验证
const hasPassword = ref(false)
const loading = ref(false)

// 密码验证表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// 验证码验证表单
const codeForm = ref({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

// 倒计时
const countdown = ref(0)
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

// 密码强度
const passwordStrength = computed(() => {
  const pwd = activeTab.value === 1 ? passwordForm.value.newPassword : codeForm.value.newPassword
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

// 监听弹框打开
watch(modelValue, async (val) => {
  if (val) {
    // 查询是否有密码
    try {
      const res = await Apis.user.hasPassword()
      hasPassword.value = res.data ?? false
      // 无密码时默认选中验证码 Tab
      if (!hasPassword.value) {
        activeTab.value = 0
      }
    }
    catch {
      hasPassword.value = false
    }
  }
  else {
    // 关闭时重置表单
    resetForms()
  }
})

// 重置表单
function resetForms() {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  codeForm.value = { code: '', newPassword: '', confirmPassword: '' }
  activeTab.value = 0
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  countdown.value = 0
}

onUnload(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

// 发送验证码到当前手机
async function sendCodeToCurrentPhone() {
  if (countdown.value > 0 || sending.value)
    return
  if (!userStore.phone) {
    toast.warning('未绑定手机号')
    return
  }

  sending.value = true
  try {
    const res = await Apis.user.getPhoneCode({ params: { scene: SmsScene.RESET_PASSWORD } })
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

// 通过旧密码修改
async function handleUpdateByPassword() {
  if (!passwordForm.value.oldPassword) {
    toast.warning('请输入旧密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    toast.warning('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    toast.warning('密码长度不能少于6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await Apis.user.updatePasswordByOld({
      data: {
        userId: userStore.userId,
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword,
      },
    })
    if (res.success) {
      toast.success('密码修改成功，请重新登录')
      modelValue.value = false
      setTimeout(() => {
        userStore.logout()
        router.pushTab('/pages/mine/index')
      }, 1500)
    }
    else {
      toast.error(res.message || '修改失败')
    }
  }
  catch {
    toast.error('修改失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 通过验证码修改
async function handleUpdateByCode() {
  if (!codeForm.value.code) {
    toast.warning('请输入验证码')
    return
  }
  if (!codeForm.value.newPassword) {
    toast.warning('请输入新密码')
    return
  }
  if (codeForm.value.newPassword.length < 6) {
    toast.warning('密码长度不能少于6位')
    return
  }
  if (codeForm.value.newPassword !== codeForm.value.confirmPassword) {
    toast.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await Apis.user.updatePasswordByCode({
      data: {
        code: codeForm.value.code,
        newPassword: codeForm.value.newPassword,
      },
    })
    if (res.success) {
      toast.success('密码修改成功，请重新登录')
      modelValue.value = false
      setTimeout(() => {
        userStore.logout()
        router.pushTab('/pages/mine/index')
      }, 1500)
    }
    else {
      toast.error(res.message || '修改失败')
    }
  }
  catch {
    toast.error('修改失败，请重试')
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    title="修改密码"
    :z-index="999"
    safe-area-inset-bottom
    :close-on-click-modal="false"
  >
    <view class="p-4 pt-0">
      <!-- Tab 切换（有密码时显示） -->
      <wd-tabs v-if="hasPassword" v-model="activeTab">
        <!-- 验证码验证 Tab -->
        <wd-tab title="验证码验证">
          <view class="pt-5">
            <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
              <text class="text-sm text-gray-600 dark:text-gray-400">
                手机号：{{ userStore.phone || '未绑定' }}
              </text>
            </view>
            <wd-input v-model="codeForm.code" type="number" placeholder="验证码" :maxlength="6" custom-class="login-input">
              <template #prefix>
                <view class="i-lucide:message-square text-gray-400" />
              </template>
              <template #suffix>
                <text
                  class="whitespace-nowrap text-sm"
                  :class="countdown > 0 || !userStore.phone ? 'text-gray-400' : 'text-blue-500'"
                  @click.stop="sendCodeToCurrentPhone"
                >
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </text>
              </template>
            </wd-input>
            <wd-input v-model="codeForm.newPassword" type="safe-password" show-password placeholder="新密码" :maxlength="20" custom-class="login-input">
              <template #prefix>
                <view class="i-lucide:lock-keyhole text-gray-400" />
              </template>
            </wd-input>
            <view v-if="codeForm.newPassword" class="mb-4 flex items-center gap-2 pl-1 -mt-2">
              <text class="text-xs text-gray-500">
                密码强度：
              </text>
              <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
                {{ strengthText[passwordStrength - 1] || '弱' }}
              </text>
            </view>
            <wd-input v-model="codeForm.confirmPassword" type="safe-password" show-password placeholder="确认新密码" :maxlength="20" custom-class="login-input">
              <template #prefix>
                <view class="i-lucide:lock-keyhole text-gray-400" />
              </template>
            </wd-input>
            <text class="mb-4 block pl-1 text-xs text-gray-400">
              密码需6-20位，建议包含字母和数字
            </text>
            <wd-button type="primary" round block :loading="loading" custom-class="mt-2" @click="handleUpdateByCode">
              确认修改
            </wd-button>
          </view>
        </wd-tab>

        <!-- 密码验证 Tab -->
        <wd-tab title="密码验证">
          <view class="pt-5">
            <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
              <view class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                <view class="i-lucide:info" />
                <text class="text-sm">
                  使用旧密码验证身份，设置新密码
                </text>
              </view>
            </view>
            <wd-input v-model="passwordForm.oldPassword" type="safe-password" show-password placeholder="旧密码" :maxlength="20" custom-class="login-input">
              <template #prefix>
                <view class="i-lucide:lock text-gray-400" />
              </template>
            </wd-input>
            <wd-input v-model="passwordForm.newPassword" type="safe-password" show-password placeholder="新密码" :maxlength="20" custom-class="login-input">
              <template #prefix>
                <view class="i-lucide:lock-keyhole text-gray-400" />
              </template>
            </wd-input>
            <view v-if="passwordForm.newPassword" class="mb-4 flex items-center gap-2 pl-1 -mt-2">
              <text class="text-xs text-gray-500">
                密码强度：
              </text>
              <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
                {{ strengthText[passwordStrength - 1] || '弱' }}
              </text>
            </view>
            <wd-input v-model="passwordForm.confirmPassword" type="safe-password" show-password placeholder="确认新密码" :maxlength="20" custom-class="login-input">
              <template #prefix>
                <view class="i-lucide:lock-keyhole text-gray-400" />
              </template>
            </wd-input>
            <text class="mb-4 block pl-1 text-xs text-gray-400">
              密码需6-20位，建议包含字母和数字
            </text>
            <wd-button type="primary" round block :loading="loading" custom-class="mt-2" @click="handleUpdateByPassword">
              确认修改
            </wd-button>
          </view>
        </wd-tab>
      </wd-tabs>

      <!-- 无密码时仅显示验证码验证 -->
      <view v-else class="pt-5">
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            手机号：{{ userStore.phone || '未绑定' }}
          </text>
        </view>
        <wd-input v-model="codeForm.code" type="number" placeholder="验证码" :maxlength="6" custom-class="login-input">
          <template #prefix>
            <view class="i-lucide-message-square text-gray-400" />
          </template>
          <template #suffix>
            <text
              class="whitespace-nowrap text-sm"
              :class="countdown > 0 || !userStore.phone ? 'text-gray-400' : 'text-blue-500'"
              @click.stop="sendCodeToCurrentPhone"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </text>
          </template>
        </wd-input>
        <wd-input v-model="codeForm.newPassword" type="safe-password" show-password placeholder="新密码" :maxlength="20" custom-class="login-input">
          <template #prefix>
            <view class="i-lucide-lock text-gray-400" />
          </template>
        </wd-input>
        <view v-if="codeForm.newPassword" class="mb-4 flex items-center gap-2 pl-1 -mt-2">
          <text class="text-xs text-gray-500">
            密码强度：
          </text>
          <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
            {{ strengthText[passwordStrength - 1] || '弱' }}
          </text>
        </view>
        <wd-input v-model="codeForm.confirmPassword" type="safe-password" show-password placeholder="确认新密码" :maxlength="20" custom-class="login-input">
          <template #prefix>
            <view class="i-lucide-lock text-gray-400" />
          </template>
        </wd-input>
        <text class="mb-4 block pl-1 text-xs text-gray-400">
          密码需6-20位，建议包含字母和数字
        </text>
        <wd-button type="primary" round block :loading="loading" custom-class="mt-2" @click="handleUpdateByCode">
          确认修改
        </wd-button>
      </view>
    </view>
  </wd-action-sheet>
</template>

<style lang="scss" scoped>
/* 输入框样式覆盖 */
:deep(.login-input) {
  @apply px-3 py-1 rounded-2xl mb-4 bg-gray-100 dark:bg-gray-700;

  &::after {
    display: none !important;
  }
  .wd-icon {
    background: none;
  }
}
</style>
