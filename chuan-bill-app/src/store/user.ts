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

  // Getters
  const isLoggedIn = computed(() => {
    return !!token.value && (expireTime.value === -1 || expireTime.value > Date.now())
  })

  // Actions
  /**
   * 用户登录
   * @param data TokenVO 数据
   */
  function login(data: { token: string, expireTime: number, userId: string, nickname: string }) {
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
    token.value = ''
    userId.value = ''
    nickname.value = ''
    phone.value = ''
    expireTime.value = 0
  }

  return {
    token,
    userId,
    nickname,
    phone,
    expireTime,
    isLoggedIn,
    login,
    logout,
  }
})
