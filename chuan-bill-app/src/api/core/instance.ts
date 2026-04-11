import type { Method } from 'alova'
import AdapterUniapp from '@alova/adapter-uniapp'
import { createAlova } from 'alova'
import vueHook from 'alova/vue'
import mockAdapter from '../mock/mockAdapter'
import { handleAlovaError, handleAlovaResponse } from './handlers'

function createLoadingManager() {
  let activeCount = 0
  let isShowing = false
  const loading = useGlobalLoading()
  const pendingTimers = new Map<string, ReturnType<typeof setTimeout> | null>()

  const generateId = (() => {
    let counter = 0
    return (method: Method) => `${method.type}-${method.url}-${Date.now()}-${++counter}`
  })()

  return {
    start(method: Method) {
      const shouldShow = !method.config.meta?.silent
      const delay = method.config.meta?.loadingDelay ?? 300
      const text = method.config.meta?.loadingText || '请稍候...'
      if (!shouldShow) {
        return null
      }
      activeCount++
      const requestId = generateId(method)

      // 如果已经在显示，不再需要再定时
      if (isShowing) {
        pendingTimers.set(requestId, null)
      }
      // 延迟显示
      const timer = setTimeout(() => {
        isShowing = true
        loading.loading(text)
        // 显示后清除timer引用
        pendingTimers.set(requestId, null)
      }, delay)
      // 储存定时器
      pendingTimers.set(requestId, timer)
      return requestId
    },

    finish(requestId: string) {
      if (!requestId || !pendingTimers.has(requestId)) {
        return
      }
      const timer = pendingTimers.get(requestId)

      // 清除定时器
      if (timer) {
        clearTimeout(timer)
      }
      pendingTimers.delete(requestId)
      activeCount--

      // 只有当所有请求都完成，且正在显示时，才关闭
      if (activeCount <= 0 && isShowing) {
        isShowing = false
        loading.close()
        activeCount = 0 // 防止负数
      }
    },
  }
}

const loadingManager = createLoadingManager()

export const alovaInstance = createAlova({
  // #ifndef H5
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // #endif
  ...AdapterUniapp({
    mockRequest: mockAdapter,
  }),
  statesHook: vueHook,
  beforeRequest: (method) => {
    // FIXME: 临时使用
    method.config.headers.token = 'LKr82GJOAIwZAN2uPQzls2y2DOzZ05dzzlqikZvMRdlPgdHOpoRNmOUDpfsX3oOX'
    // Add content type for POST/PUT/PATCH requests
    if (['POST', 'PUT', 'PATCH'].includes(method.type)) {
      method.config.headers['Content-Type'] = 'application/json'
    }

    // Add timestamp to prevent caching for GET requests
    if (method.type === 'GET' && CommonUtil.isObj(method.config.params)) {
      method.config.params._t = Date.now()
    }

    // Log request in development
    if (import.meta.env.MODE === 'development') {
      // #ifdef H5
      method.baseURL = `${method.baseURL}/api`
      // #endif
      console.log(`[Alova Request] ${method.type} ${method.url}`, method.data || method.config.params)
      console.log(`[API Base URL] ${import.meta.env.VITE_API_BASE_URL}`)
      console.log(`[Environment] ${import.meta.env.VITE_ENV_NAME}`)
    }

    // Start loading
    const requestId = loadingManager.start(method)
    if (requestId) {
      method.config.meta = {
        ...method.config.meta,
        __requestId: requestId,
      }
    }
  },

  // Response handlers
  responded: {
    // Success handler
    onSuccess: handleAlovaResponse,

    // Error handler
    onError: handleAlovaError,

    // Complete handler - runs after success or error
    onComplete: async (method) => {
      // Any cleanup or logging can be done here
      loadingManager.finish(method.config.meta?.__requestId)
    },
  },

  // We'll use the middleware in the hooks
  // middleware is not directly supported in createAlova options

  // Default request timeout (30 seconds)
  timeout: 1000 * 30,
  // 设置为null即可全局关闭全部请求缓存
  cacheFor: null,
})

export default alovaInstance
