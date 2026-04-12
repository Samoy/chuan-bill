/**
 * 鉴权检查组合式 API
 *
 * 用于统一处理需要登录权限的操作，提供登录弹框的统一控制
 *
 * 功能特性：
 * - 检查用户登录状态
 * - 统一的登录弹框控制
 * - 待执行回调的暂存与执行
 *
 * 设计说明：
 * - showLoginPopup 和 pendingCallback 定义在模块作用域
 * - 确保所有调用 useAuthCheck() 的地方共享同一份状态
 *
 * @example
 * ```vue
 * <script setup>
 * const { isLoggedIn, requireAuth, showLoginPopup, onLoginSuccess, onLoginCancel } = useAuthCheck()
 *
 * // 需要登录权限的操作
 * function handlePremiumAction() {
 *   requireAuth(() => {
 *     // 执行需要登录的操作
 *     console.log('执行已登录用户的操作')
 *   })
 * }
 * </script>
 *
 * <template>
 *   <wd-button @click="handlePremiumAction">会员功能</wd-button>
 *   <LoginPopup
 *     v-model="showLoginPopup"
 *     @success="onLoginSuccess"
 *     @cancel="onLoginCancel"
 *   />
 * </template>
 * ```
 */

/** 待执行回调的类型 */
type PendingCallback = (() => void) | null

// ========== 模块级状态（全局共享）==========

/**
 * 登录弹框显隐状态
 * 所有调用 useAuthCheck() 的地方共享同一份状态
 */
const showLoginPopup = ref(false)

/**
 * 待执行的回调函数
 * 用于在用户登录成功后执行之前被拦截的操作
 */
const pendingCallback = ref<PendingCallback>(null)

// ========== Composable 定义 ==========

export function useAuthCheck() {
  // 获取用户 store（useUserStore 由 auto-import 自动导入）
  const userStore = useUserStore()

  // ========== 计算属性 ==========

  /**
   * 用户是否已登录
   * 直接从 useUserStore 的 isLoggedIn 计算属性读取
   */
  const isLoggedIn = computed(() => userStore.isLoggedIn)

  // ========== 方法 ==========

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
    // 计算属性
    isLoggedIn,
    // 状态（响应式引用）
    showLoginPopup,
    pendingCallback,
    // 方法
    requireAuth,
    onLoginSuccess,
    onLoginCancel,
  }
}
