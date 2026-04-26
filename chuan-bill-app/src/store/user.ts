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
  const avatar = ref('')
  const gender = ref('0')
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
    // 登录后获取完整资料
    getProfile()
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
    avatar.value = ''
    gender.value = '0'
    expireTime.value = 0
  }

  /**
   * 获取用户资料
   */
  async function getProfile() {
    if (!isLoggedIn.value)
      return
    try {
      const res = await Apis.user.getProfile()
      if (res.success && res.data) {
        nickname.value = res.data.nickname || ''
        phone.value = res.data.phone || ''
        avatar.value = res.data.avatar || ''
        gender.value = res.data.gender || '0'
      }
    }
    catch {
      // 静默失败
    }
  }

  /**
   * 更新用户资料
   */
  async function updateProfile(data: { nickname?: string, avatar?: string, gender?: string }) {
    try {
      const res = await Apis.user.updateProfile({ data })
      if (res.success) {
        if (data.nickname !== undefined)
          nickname.value = data.nickname
        if (data.avatar !== undefined)
          avatar.value = data.avatar
        if (data.gender !== undefined)
          gender.value = data.gender
        return true
      }
    }
    catch {
      // 静默失败
    }
    return false
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
    avatar,
    gender,
    expireTime,
    isLoggedIn,
    showLoginPopup,
    pendingCallback,
    requireAuth,
    onLoginSuccess,
    onLoginCancel,
    login,
    logout,
    getProfile,
    updateProfile,
  }
})
