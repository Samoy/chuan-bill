<script lang="ts" setup>
import type { TokenVO } from '@/api/globals'

// ========== 状态管理 ==========
const userStore = useUserStore()
const toast = useGlobalToast()

// ========== 登录方式切换 ==========
const activeTab = ref(0) // 0: 验证码登录, 1: 密码登录

// ========== 表单数据 ==========
const phone = ref('')
const code = ref('')
const password = ref('')

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

// ========== 发送验证码 ==========
async function handleSendCode() {
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
  // 1. 保存登录态
  userStore.login({
    token: tokenVO.token || '',
    expireTime: tokenVO.expireTime || 0,
    userId: tokenVO.userId || '',
    nickname: tokenVO.nickname || '',
  })

  // 3. 调用成功回调
  userStore.onLoginSuccess()

  // 4. 显示成功提示
  toast.success('登录成功')

  // 5. 重置表单
  resetForm()
}

// ========== 重置表单 ==========
function resetForm() {
  phone.value = ''
  code.value = ''
  password.value = ''
  activeTab.value = 0
  clearCountdown()
  countdown.value = 0
}

// ========== 验证码登录 ==========
async function handlePhoneLogin() {
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
  isLoading.value = true
  try {
    const loginRes = await uni.login({ provider: 'weixin' })
    if (loginRes.code) {
      // 使用 alovaInstance 直接发送请求（loginByWechat 可能尚未在 apiDefinitions 中生成）
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
    custom-style="border-radius: 24rpx 24rpx 0 0;"
    @close="handleClose"
  >
    <view class="login-popup">
      <!-- 顶部拖拽条 -->
      <view class="login-popup__drag-bar">
        <view class="login-popup__drag-bar-line" />
      </view>

      <!-- 价值说明区域 -->
      <view class="login-popup__header">
        <text class="login-popup__title">
          登录小川记账
        </text>
        <text class="login-popup__subtitle">
          登录后解锁图片识别记账、语音记账、云端同步等更多功能
        </text>
      </view>

      <!-- 微信一键登录（仅小程序） -->
      <!-- #ifdef MP-WEIXIN -->
      <view class="login-popup__wechat-section">
        <button
          class="login-popup__wechat-btn"
          :disabled="isLoading"
          @click="handleWechatLogin"
        >
          <view class="login-popup__wechat-icon i-mingcute-wechat-fill" />
          <text>微信一键登录</text>
        </button>
      </view>

      <!-- 分割线 -->
      <view class="login-popup__divider">
        <view class="login-popup__divider-line" />
        <text class="login-popup__divider-text">
          或使用手机号登录
        </text>
        <view class="login-popup__divider-line" />
      </view>
      <!-- #endif -->

      <!-- Tab 切换区域 -->
      <wd-tabs v-model="activeTab" class="login-popup__tabs">
        <!-- 验证码登录 -->
        <wd-tab title="验证码登录">
          <view class="login-popup__form">
            <wd-input
              v-model="phone"
              type="number"
              placeholder="请输入手机号"
              :maxlength="11"
              custom-class="login-popup__input"
            />
            <view class="login-popup__code-row">
              <wd-input
                v-model="code"
                type="number"
                placeholder="请输入验证码"
                :maxlength="6"
                custom-class="login-popup__input login-popup__code-input"
              />
              <button
                class="login-popup__code-btn"
                :disabled="isCountingDown || !phone"
                @click="handleSendCode"
              >
                {{ isCountingDown ? `${countdown}s后重新获取` : '获取验证码' }}
              </button>
            </view>
            <wd-button
              type="primary"
              size="large"
              round
              block
              :loading="isLoading"
              custom-class="login-popup__submit-btn"
              @click="handlePhoneLogin"
            >
              登录
            </wd-button>
          </view>
        </wd-tab>

        <!-- 密码登录 -->
        <wd-tab title="密码登录">
          <view class="login-popup__form">
            <wd-input
              v-model="phone"
              type="number"
              placeholder="请输入手机号"
              :maxlength="11"
              custom-class="login-popup__input"
            />
            <wd-input
              v-model="password"
              type="safe-password"
              placeholder="请输入密码"
              custom-class="login-popup__input"
            />
            <wd-button
              type="primary"
              size="large"
              round
              block
              :loading="isLoading"
              custom-class="login-popup__submit-btn"
              @click="handlePasswordLogin"
            >
              登录
            </wd-button>
          </view>
        </wd-tab>
      </wd-tabs>
    </view>
  </wd-popup>
</template>

<style lang="scss" scoped>
.login-popup {
  height: 70vh;
  padding: 0 40rpx 40rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;

  &__drag-bar {
    display: flex;
    justify-content: center;
    padding: 20rpx 0;
  }

  &__drag-bar-line {
    width: 80rpx;
    height: 6rpx;
    background-color: #e0e0e0;
    border-radius: 3rpx;
  }

  &__header {
    text-align: center;
    margin-bottom: 40rpx;
  }

  &__title {
    display: block;
    font-size: 40rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 16rpx;
  }

  &__subtitle {
    display: block;
    font-size: 26rpx;
    color: #999;
    line-height: 1.5;
  }

  &__wechat-section {
    margin-bottom: 32rpx;
  }

  &__wechat-btn {
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

    &:disabled {
      opacity: 0.6;
    }
  }

  &__wechat-icon {
    margin-right: 16rpx;
    font-size: 40rpx;
  }

  &__divider {
    display: flex;
    align-items: center;
    margin-bottom: 32rpx;
  }

  &__divider-line {
    flex: 1;
    height: 1rpx;
    background-color: #e8e8e8;
  }

  &__divider-text {
    padding: 0 24rpx;
    font-size: 24rpx;
    color: #999;
  }

  &__tabs {
    flex: 1;
  }

  &__form {
    padding-top: 32rpx;
  }

  &__input {
    margin-bottom: 24rpx;
  }

  &__code-row {
    display: flex;
    align-items: center;
    margin-bottom: 24rpx;
  }

  &__code-input {
    flex: 1;
    margin-bottom: 0;
    margin-right: 20rpx;
  }

  &__code-btn {
    width: 200rpx;
    height: 88rpx;
    line-height: 88rpx;
    background-color: #f5f5f5;
    border-radius: 44rpx;
    border: none;
    font-size: 26rpx;
    color: #333;

    &:active {
      background-color: #ebebeb;
    }

    &:disabled {
      color: #999;
    }
  }

  &__submit-btn {
    margin-top: 40rpx;
  }
}

/* 覆盖 wot-design-uni 的 tab 样式 */
:deep(.wd-tabs__nav) {
  justify-content: center;
}

:deep(.wd-tab__label) {
  font-size: 30rpx;
}
</style>
