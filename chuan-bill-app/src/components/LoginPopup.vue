<script lang="ts" setup>
import type { TokenVO } from '@/api/globals'

// ========== 状态管理 ==========
const userStore = useUserStore()
const toast = useGlobalToast()

// ========== 视图切换 ==========
type LoginView = 'wechat' | 'phone'
const currentView = ref<LoginView>('phone')
// #ifdef MP-WEIXIN
currentView.value = 'wechat'
// #endif

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

// ========== 视图切换 ==========
function switchToPhone() {
  currentView.value = 'phone'
}
// #ifdef MP-WEIXIN
function switchToWechat() {
  currentView.value = 'wechat'
}
// #endif

// ========== 协议链接（占位） ==========
function handleOpenAgreement() {
  toast.info('用户协议页面开发中')
}
function handleOpenPrivacy() {
  toast.info('隐私政策页面开发中')
}

// ========== 发送验证码 ==========
async function handleSendCode() {
  if (isCountingDown.value)
    return
  if (!phone.value) {
    toast.warning('请输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    toast.warning('请输入正确的手机号')
    return
  }

  try {
    const response = await Apis.auth.sendCode({
      data: { phone: phone.value },
    })
    if (response.code === 200) {
      toast.success('验证码已发送')
      startCountdown()
    }
    else {
      toast.error(response.message || '发送失败')
    }
  }
  catch (error) {
    console.error('发送验证码失败:', error)
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
  currentView.value = 'phone'
  // #ifdef MP-WEIXIN
  currentView.value = 'wechat'
  // #endif
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
    else {
      toast.error(response.message || '登录失败')
    }
  }
  catch (error) {
    console.error('登录失败:', error)
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
    else {
      toast.error(response.message || '登录失败')
    }
  }
  catch (error) {
    console.error('登录失败:', error)
  }
  finally {
    isLoading.value = false
  }
}

// ========== 微信登录 ==========
// #ifdef MP-WEIXIN
async function handleWechatLogin() {
  if (!validateAgreement())
    return
  isLoading.value = true
  try {
    const loginRes = await uni.login({ provider: 'weixin' })
    if (loginRes.code) {
      const response = await alovaInstance.Post<any>('/auth/loginByWechat', { code: loginRes.code }).send()
      if (response.code === 200 && response.data) {
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
    isLoading.value = false
  }
}
// #endif

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
    v-model="userStore.showLoginPopup"
    :z-index="9999"
    position="bottom"
    :close-on-click-modal="true"
    lock-scroll
    safe-area-inset-bottom
    custom-style="border-radius: 24rpx 24rpx 0 0;"
    @close="handleClose"
  >
    <view class="px-6 pb-5">
      <!-- 顶部拖拽条 -->
      <view class="flex justify-center py-3">
        <view class="h-1 w-10 rounded-full bg-gray-200" />
      </view>

      <!-- ==================== 微信快速登录视图 ==================== -->
      <!-- #ifdef MP-WEIXIN -->
      <view v-if="currentView === 'wechat'" class="flex flex-col items-center">
        <!-- 应用图标 -->
        <view class="app-icon mt-2">
          <view class="i-mingcute-wallet-4-fill text-3xl text-white" />
        </view>
        <!-- 标题 -->
        <text class="mt-5 text-xl text-gray-800 font-bold">
          欢迎使用小川记账
        </text>
        <text class="mt-2 text-sm text-gray-400">
          登录后解锁云端同步、图片识别等更多功能
        </text>
        <!-- 微信登录按钮 -->
        <button
          class="wechat-btn mt-10"
          :disabled="isLoading"
          @click="handleWechatLogin"
        >
          <view class="i-mingcute-wechat-fill mr-2 text-xl" />
          <text>微信快速登录</text>
        </button>
        <!-- 切换到手机号登录 -->
        <view v-if="!isLoading" class="mt-6 w-full">
          <text class="text-sm text-blue-500" @click="switchToPhone">
            其他方式登录 >
          </text>
        </view>
      </view>
      <!-- #endif -->

      <!-- ==================== 手机号登录视图 ==================== -->
      <view v-if="currentView === 'phone'">
        <!-- 应用图标 -->
        <view class="mt-2 flex justify-center">
          <view class="app-icon">
            <view class="i-mingcute-wallet-4-fill text-3xl text-white" />
          </view>
        </view>
        <!-- 标题 -->
        <view class="mt-5 text-center">
          <text class="text-xl text-gray-800 font-bold">
            欢迎使用小川记账
          </text>
        </view>
        <view class="mt-2 text-center">
          <text class="text-sm text-gray-400">
            随时随地，管理您的家庭财富
          </text>
        </view>

        <!-- Tab 切换 -->
        <wd-tabs v-model="activeTab" class="mt-4">
          <!-- 验证码登录 -->
          <wd-tab title="验证码登录">
            <view class="pt-5">
              <wd-input
                v-model="phone"
                type="number"
                placeholder="手机号"
                :maxlength="11"
                custom-class="login-input"
              >
                <template #prefix>
                  <view class="i-lucide-phone text-lg text-gray-400" />
                </template>
              </wd-input>
              <wd-input
                v-model="code"
                type="number"
                placeholder="验证码"
                :maxlength="6"
                custom-class="login-input"
              >
                <template #prefix>
                  <view class="i-lucide-message-square text-lg text-gray-400" />
                </template>
                <template #suffix>
                  <text
                    class="whitespace-nowrap text-sm"
                    :class="isCountingDown || !phone ? 'text-gray-400' : 'text-blue-500'"
                    @click.stop="handleSendCode"
                  >
                    {{ isCountingDown ? `${countdown}s` : '获取验证码' }}
                  </text>
                </template>
              </wd-input>
              <wd-button
                type="primary"
                size="large"
                round
                block
                :loading="isLoading"
                :disabled="isLoading"
                custom-class="mt-2"
                @click="handlePhoneLogin"
              >
                立即登录
              </wd-button>
            </view>
          </wd-tab>

          <!-- 密码登录 -->
          <wd-tab title="密码登录">
            <view class="pt-5">
              <wd-input
                v-model="phone"
                type="number"
                placeholder="手机号"
                :maxlength="11"
                custom-class="login-input"
              >
                <template #prefix>
                  <view class="i-lucide-phone text-lg text-gray-400" />
                </template>
              </wd-input>
              <wd-input
                v-model="password"
                type="safe-password"
                placeholder="登录密码"
                custom-class="login-input"
              >
                <template #prefix>
                  <view class="i-lucide-lock text-lg text-gray-400" />
                </template>
              </wd-input>
              <wd-button
                type="primary"
                size="large"
                round
                block
                :loading="isLoading"
                :disabled="isLoading"
                custom-class="mt-2"
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

        <!-- 切换到微信登录 -->
        <!-- #ifdef MP-WEIXIN -->
        <view v-if="!isLoading" class="mt-3">
          <text class="text-sm text-blue-500" @click="switchToWechat">
            &lt; 微信快速登录
          </text>
        </view>
        <!-- #endif -->
      </view>

      <!-- ==================== 协议勾选区域（共享） ==================== -->
      <view class="mt-5 flex items-start">
        <wd-checkbox
          v-model="agreedToTerms"
          shape="square"
          custom-class="login-checkbox"
        />
        <view
          class="ml-1 flex-1 text-xs text-gray-500 leading-5"
          @click="agreedToTerms = !agreedToTerms"
        >
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
/* 应用图标 */
.app-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 微信按钮 */
.wechat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 96rpx;
  background-color: #07c160;
  border-radius: 48rpx;
  border: none;
  color: #fff;
  font-size: 32rpx;
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
  background-color: #f5f5f5;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
  overflow: hidden;

  &::after {
    display: none !important;
  }

  .wd-input__prefix {
    margin-right: 12rpx;
  }
}

/* Tab 样式 */
:deep(.wd-tabs__nav) {
  justify-content: center;
}

:deep(.wd-tab__label) {
  font-size: 30rpx;
}

/* Checkbox 样式 */
:deep(.login-checkbox) {
  flex-shrink: 0;
  margin-top: 4rpx;
}
</style>
