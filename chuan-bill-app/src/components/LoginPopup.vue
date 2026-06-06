<script lang="ts" setup>
import type { TokenVO } from '@/api/globals'

const router = useRouter()

// ========== 状态管理 ==========
const userStore = useUserStore()
const toast = useGlobalToast()
const loading = useGlobalLoading()

// ========== 登录方式切换 ==========
const activeTab = ref(0) // 0: 验证码登录, 1: 密码登录

// ========== 表单数据 ==========
const phone = ref('')
const code = ref('')
const password = ref('')

// ========== 协议勾选 ==========
const agreedToTerms = ref(false)

// ========== 验证码倒计时 ==========
const countdown = ref(0)
const isCountingDown = computed(() => countdown.value > 0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ========== 登录加载状态 ==========
const isLoading = ref(false)
// ========== 验证码发送中 ==========
const isSending = ref(false)

// ========== 清理倒计时 ==========
function clearCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// ========== 启动倒计时 ==========
function startCountdown() {
  countdown.value = 60
  clearCountdown()
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearCountdown()
    }
  }, 1000)
}

// ========== 协议校验 ==========
function validateAgreement(): boolean {
  if (!agreedToTerms.value) {
    toast.warning('请先同意用户协议和隐私政策')
    return false
  }
  return true
}

// ========== 协议链接（占位） ==========
function handleOpenAgreement() {
  router.push('/pages/agreement/index?from=login')
}
function handleOpenPrivacy() {
  // #ifdef MP-WEIXIN
  wx.openPrivacyContract({})
  // #endif
  // #ifndef MP-WEIXIN
  router.push('/pages/privacy/index?from=login')
  // #endif
}

// ========== 发送验证码 ==========
async function handleSendCode() {
  if (isCountingDown.value || isSending.value)
    return
  if (!phone.value) {
    toast.warning('请输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    toast.warning('请输入正确的手机号')
    return
  }

  isSending.value = true
  try {
    const response = await Apis.auth.sendCode({
      data: { phone: phone.value },
    })
    if (response.code === 200) {
      toast.success('验证码已发送')
      startCountdown()
    }
  }
  finally {
    isSending.value = false
  }
}

// ========== 处理登录成功 ==========
async function handleLoginSuccess(tokenVO: TokenVO) {
  userStore.login({
    token: tokenVO.token || '',
    expireTime: tokenVO.expireTime || 0,
    userId: tokenVO.userId || '',
    nickname: tokenVO.nickname || '',
  })
  userStore.onLoginSuccess()
  toast.success('登录成功')
  resetForm()
}

// ========== 重置表单 ==========
function resetForm() {
  phone.value = ''
  code.value = ''
  password.value = ''
  activeTab.value = 0
  agreedToTerms.value = false
  clearCountdown()
  countdown.value = 0
}

// ========== 验证码登录 ==========
async function handlePhoneLogin() {
  if (!validateAgreement())
    return
  if (!phone.value) {
    toast.warning('请输入手机号')
    return
  }
  if (!code.value) {
    toast.warning('请输入验证码')
    return
  }

  isLoading.value = true
  try {
    const response = await Apis.auth.loginByPhone({
      data: {
        phone: phone.value,
        code: code.value,
      },
    })
    if (response.code === 200 && response.data) {
      await handleLoginSuccess(response.data)
    }
  }
  finally {
    isLoading.value = false
  }
}

// ========== 密码登录 ==========
async function handlePasswordLogin() {
  if (!validateAgreement())
    return
  if (!phone.value) {
    toast.warning('请输入手机号')
    return
  }
  if (!password.value) {
    toast.warning('请输入密码')
    return
  }

  isLoading.value = true
  try {
    const response = await Apis.auth.loginByPassword({
      data: {
        phone: phone.value,
        password: password.value,
      },
    })
    if (response.code === 200 && response.data) {
      await handleLoginSuccess(response.data)
    }
  }
  finally {
    isLoading.value = false
  }
}

// ========== 微信登录 ==========
async function handleWechatLogin() {
  if (!validateAgreement())
    return
  if (isLoading.value)
    return
  isLoading.value = true
  try {
    loading.loading('请稍候...')
    const loginRes = await uni.login({ provider: 'weixin' })
    if (loginRes.code) {
      const response = await Apis.auth.loginByWechat({ data: { code: loginRes.code } })
      if (response.success && response.data) {
        await handleLoginSuccess(response.data as TokenVO)
      }
      else {
        toast.error(response.message || '微信登录失败')
      }
    }
    else {
      toast.error('获取微信登录凭证失败')
    }
  }
  catch (error) {
    console.error('微信登录失败:', error)
    toast.error('微信登录失败')
  }
  finally {
    loading.close()
    isLoading.value = false
  }
}

// ========== 弹框关闭处理 ==========
function handleClose() {
  userStore.onLoginCancel()
  resetForm()
}

// ========== 组件卸载时清理 ==========
onUnmounted(() => {
  clearCountdown()
})
</script>

<script lang="ts">
export default {
  options: {
    virtualHost: true,
    addGlobalClass: true,
    styleIsolation: 'shared',
  },
}
</script>

<template>
  <wd-popup
    v-model="userStore.showLoginPopup" :z-index="999" position="bottom" close-on-click-modal lock-scroll
    closable safe-area-inset-bottom custom-class="rounded-tl-2xl rounded-tr-2xl pb-3!" @close="handleClose"
  >
    <view class="px-6 py-2">
      <view>
        <!-- 应用图标 -->
        <view class="mt-2 flex justify-center">
          <wd-img
            src="https://chuan-bill-cdn.samoy.site/default/logo.png"
            custom-class="rounded-2xl mt-4 w-16 h-16 overflow-hidden" image-mode="aspectFill"
          />
        </view>
        <!-- 标题 -->
        <view class="mt-5 text-center">
          <text class="text-xl text-gray-800 font-bold dark:text-primary">
            欢迎使用小川记账
          </text>
        </view>
        <view class="mt-2 text-center">
          <text class="text-sm text-gray-400">
            随时随地，管理您的财富
          </text>
        </view>

        <!-- Tab 切换 -->
        <wd-tabs v-model="activeTab" custom-class="mt-8">
          <!-- 验证码登录 -->
          <wd-tab title="验证码登录">
            <view class="pt-5">
              <wd-input v-model="phone" type="number" placeholder="手机号" :maxlength="11" custom-class="login-input">
                <template #prefix>
                  <view class="i-lucide-phone text-gray-400" />
                </template>
              </wd-input>
              <wd-input v-model="code" type="number" placeholder="验证码" :maxlength="6" custom-class="login-input">
                <template #prefix>
                  <view class="i-lucide-message-square text-gray-400" />
                </template>
                <template #suffix>
                  <text
                    class="whitespace-nowrap text-sm"
                    :class="isCountingDown || isSending || !phone ? 'text-gray-400' : 'text-blue-500'" @click.stop="handleSendCode"
                  >
                    {{ isCountingDown ? `${countdown}s` : isSending ? '发送中...' : '获取验证码' }}
                  </text>
                </template>
              </wd-input>
              <wd-button
                type="primary" round block :disabled="isLoading" custom-class="mt-2"
                @click="handlePhoneLogin"
              >
                登录
              </wd-button>
            </view>
          </wd-tab>

          <!-- 密码登录 -->
          <wd-tab title="密码登录">
            <view class="pt-5">
              <wd-input v-model="phone" type="number" placeholder="手机号" :maxlength="11" custom-class="login-input">
                <template #prefix>
                  <view class="i-lucide-phone text-gray-400" />
                </template>
              </wd-input>
              <wd-input v-model="password" type="safe-password" show-password placeholder="登录密码" custom-class="login-input">
                <template #prefix>
                  <view class="i-lucide-lock text-gray-400" />
                </template>
              </wd-input>
              <wd-button
                type="primary" round block :disabled="isLoading" custom-class="mt-2"
                @click="handlePasswordLogin"
              >
                登录
              </wd-button>
            </view>
          </wd-tab>
        </wd-tabs>

        <!-- 自动注册提示 -->
        <view class="mt-4 text-center">
          <text class="text-xs text-gray-400">
            未注册手机号验证通过后将自动注册
          </text>
        </view>
        <!-- #ifdef MP-WEIXIN -->
        <wd-divider>或</wd-divider>
        <view class="i-mingcute:wechat-fill mx-auto mt-4 h-8 w-8 text-[#07C160]" @click="handleWechatLogin" />
        <!-- #endif -->
      </view>

      <!-- ==================== 协议勾选区域 ==================== -->
      <view class="mt-10 flex items-center justify-center text-center">
        <wd-checkbox v-model="agreedToTerms" shape="square" custom-class="login-checkbox" />
        <view class="text-xs text-gray-500 leading-5" @click="agreedToTerms = !agreedToTerms">
          <text>登录即代表您同意</text>
          <text class="text-blue-500" @click.stop="handleOpenAgreement">
            《用户协议》
          </text>
          <text>和</text>
          <text class="text-blue-500" @click.stop="handleOpenPrivacy">
            《隐私政策》
          </text>
        </view>
      </view>
    </view>
  </wd-popup>
</template>

<style lang="scss" scoped>
/* 微信按钮 */
.wechat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  background-color: #07c160;
  border: none;
  color: #fff;
  font-weight: 500;

  &:active {
    opacity: 0.9;
  }

  &[disabled] {
    opacity: 0.6;
  }
}

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
