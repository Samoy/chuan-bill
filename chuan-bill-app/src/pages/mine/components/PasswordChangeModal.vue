<script setup lang="ts">
import type { SegmentedInstance } from 'wot-design-uni/components/wd-segmented/types'

defineOptions({
  name: 'PasswordChangeModal',
})

const modelValue = defineModel<boolean>({ default: false })
const userStore = useUserStore()
const toast = useGlobalToast()
const router = useRouter()

// 状态
const activeTab = ref('验证码验证')
const hasPassword = ref(false)
const loading = ref(false)
const segmentedRef = ref<SegmentedInstance>()

// Tab 选项
const tabOptions = ['密码验证', '验证码验证']

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
  const pwd = activeTab.value === '密码验证' ? passwordForm.value.newPassword : codeForm.value.newPassword
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
        activeTab.value = '验证码验证'
      }
    }
    catch {
      hasPassword.value = false
    }
    // 微信小程序端需要在弹框打开后更新分段器样式
    nextTick(() => {
      segmentedRef.value?.updateActiveStyle()
    })
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
  activeTab.value = '验证码验证'
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
    <view class="p-4">
      <!-- Tab 切换（有密码时显示） -->
      <view v-if="hasPassword" class="mb-4">
        <wd-segmented
          ref="segmentedRef"
          v-model:value="activeTab"
          :options="tabOptions"
          size="small"
        />
      </view>

      <!-- 密码验证 Tab -->
      <view v-if="activeTab === '密码验证'">
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.oldPassword"
            placeholder="请输入旧密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.newPassword"
            placeholder="请输入新密码"
            show-password
            :maxlength="20"
            no-border
          />
          <view v-if="passwordForm.newPassword" class="mt-2 flex items-center gap-2">
            <text class="text-xs text-gray-500">
              密码强度：
            </text>
            <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
              {{ strengthText[passwordStrength - 1] || '弱' }}
            </text>
          </view>
        </view>
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.confirmPassword"
            placeholder="请确认新密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <text class="mb-4 block text-xs text-gray-400">
          密码需6-20位，建议包含字母和数字
        </text>
        <wd-button type="primary" block :loading="loading" @click="handleUpdateByPassword">
          确认修改
        </wd-button>
      </view>

      <!-- 验证码验证 Tab -->
      <view v-if="activeTab === '验证码验证'">
        <!-- 当前手机号 -->
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            手机号：{{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
        </view>
        <!-- 验证码 -->
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="codeForm.code"
            placeholder="请输入验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="countdown > 0 || !userStore.phone"
            size="small"
            @click="sendCodeToCurrentPhone"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <!-- 新密码 -->
        <view class="mb-4">
          <wd-input
            v-model="codeForm.newPassword"
            placeholder="请输入新密码"
            show-password
            :maxlength="20"
            no-border
          />
          <view v-if="codeForm.newPassword" class="mt-2 flex items-center gap-2">
            <text class="text-xs text-gray-500">
              密码强度：
            </text>
            <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
              {{ strengthText[passwordStrength - 1] || '弱' }}
            </text>
          </view>
        </view>
        <!-- 确认密码 -->
        <view class="mb-4">
          <wd-input
            v-model="codeForm.confirmPassword"
            placeholder="请确认新密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <text class="mb-4 block text-xs text-gray-400">
          密码需6-20位，建议包含字母和数字
        </text>
        <wd-button type="primary" block :loading="loading" @click="handleUpdateByCode">
          确认修改
        </wd-button>
      </view>
    </view>
  </wd-action-sheet>
</template>
