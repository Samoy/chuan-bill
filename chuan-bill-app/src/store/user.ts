import type { TokenVO } from '@/api/globals'

type PendingCallback = (() => void) | null

/**
 * 用户状态管理
 * 管理用户登录状态、token 和用户信息
 */
export const useUserStore = defineStore('user', () => {
  // State
  const token = ref('')
  const userId = ref('')
  const nickname = ref('')
  const phone = ref('')
  const expireTime = ref(0)
  const showLoginPopup = ref(false)
  const pendingCallback = ref<PendingCallback>(null)

  // Getters
  const isLoggedIn = computed(() => {
    const loggedIn = !!token.value && (expireTime.value === -1 || expireTime.value > Date.now())
    return loggedIn
  })

  // Actions
  /**
   * 用户登录
   * @param data TokenVO 数据
   *
   */
  function login(data: Required<TokenVO>): void {
    token.value = data.token
    expireTime.value = data.expireTime
    userId.value = data.userId
    nickname.value = data.nickname
  }

  /**
   * 用户登出
   * 重置所有字段为默认空值
   */
  function logout() {
    Apis.auth.logout()
    token.value = ''
    userId.value = ''
    nickname.value = ''
    phone.value = ''
    expireTime.value = 0
  }

  /**
   * 要求登录权限
   * 检查登录状态，根据结果执行不同操作
   *
   * @param callback - 需要在登录后执行的回调函数
   *
   * @example
   * ```ts
   * requireAuth(() => {
   *   // 执行需要登录权限的操作
   *   navigateTo('/premium-page')
   * })
   * ```
   */
  function requireAuth(callback: () => void) {
    if (isLoggedIn.value) {
      // 已登录，直接执行回调
      callback()
    }
    else {
      // 未登录，保存回调并显示登录弹框
      pendingCallback.value = callback
      showLoginPopup.value = true
    }
  }

  /**
   * 登录成功回调
   * 执行待执行的回调，并关闭弹框
   * 由 LoginPopup 组件在登录成功后调用
   */
  function onLoginSuccess() {
    // 执行待执行的回调
    if (pendingCallback.value) {
      pendingCallback.value()
      pendingCallback.value = null
    }
    // 关闭弹框
    showLoginPopup.value = false
  }

  /**
   * 取消登录回调
   * 清除待执行的回调，并关闭弹框
   * 由 LoginPopup 组件在用户取消登录时调用
   */
  function onLoginCancel() {
    // 清除待执行的回调
    pendingCallback.value = null
    // 关闭弹框
    showLoginPopup.value = false
  }

  return {
    token,
    userId,
    nickname,
    phone,
    expireTime,
    isLoggedIn,
    showLoginPopup,
    pendingCallback,
    requireAuth,
    onLoginSuccess,
    onLoginCancel,
    login,
    logout,
  }
})
