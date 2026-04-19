/*
 * @Author: weisheng
 * @Date: 2025-04-17 15:58:11
 * @LastEditTime: 2025-06-15 21:47:22
 * @LastEditors: weisheng
 * @Description: Alova response and error handlers
 * @FilePath: /wot-starter/src/api/core/handlers.ts
 */
import type { Method } from 'alova'

// 防止重复弹出登录弹框的标记
let isShowingLoginPopup = false

// 获取 useAuthCheck 的引用（延迟获取以避免循环依赖）
let authCheckRef: ReturnType<typeof useAuthCheck> | null = null
function getAuthCheck() {
  if (!authCheckRef) {
    authCheckRef = useAuthCheck()
  }
  return authCheckRef
}

// Custom error class for API errors
export class ApiError extends Error {
  code: number
  data?: any

  constructor(message: string, code: number, data?: any) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
  }
}

// Define a type for the expected API response structure
interface ApiResponse {
  code: number
  message?: string
  data?: any
  timestamp?: number
}

// Handle successful responses
export async function handleAlovaResponse(
  response: UniApp.RequestSuccessCallbackResult | UniApp.UploadFileSuccessCallbackResult | UniApp.DownloadSuccessData,
) {
  const globalToast = useGlobalToast()
  // Extract status code and data from UniApp response
  const { statusCode, data } = response as UniNamespace.RequestSuccessCallbackResult

  function handleError(code: number, message: string) {
    // 处理401/403错误
    if ((code === 401 || code === 403)) {
      // 防止重复弹出
      if (isShowingLoginPopup) {
        throw new ApiError('登录已过期，请重新登录！', code, data)
      }
      isShowingLoginPopup = true

      // 清除过期登录信息
      useUserStore().logout()

      // 提示用户
      globalToast.error({ msg: '登录已过期，请重新登录！', duration: 500 })

      // 接入 useAuthCheck 弹出登录弹框
      const authCheck = getAuthCheck()
      authCheck.showLoginPopup.value = true

      // 监听弹框关闭，重置标记
      const unwatch = watch(() => authCheck.showLoginPopup.value, (visible) => {
        if (!visible) {
          isShowingLoginPopup = false
          unwatch()
        }
      })

      throw new ApiError('登录已过期，请重新登录！', code, data)
    }

    // Handle HTTP error status codes
    if (code >= 400) {
      globalToast.error(message)
      throw new ApiError(message, code, data)
    }
  }

  // The data is already parsed by UniApp adapter
  const json = data as ApiResponse

  if (statusCode >= 400) {
    handleError(statusCode, response.errMsg || '请求失败')
  }
  if (json.code >= 400) {
    handleError(json.code, json.message || '请求失败')
  }

  // Log response in development
  if (import.meta.env.MODE === 'development') {
    console.log('[Alova Response]', json)
  }

  // Return data for successful responses
  return json
}

// Handle request errors
export function handleAlovaError(error: any, method: Method) {
  const globalToast = useGlobalToast()
  // Log error in development
  if (import.meta.env.MODE === 'development') {
    console.error('[Alova Error]', error, method)
  }

  // 处理401/403错误（如果不是在handleAlovaResponse中处理的）
  if (error instanceof ApiError && (error.code === 401 || error.code === 403)) {
    // 防止重复弹出
    if (!isShowingLoginPopup) {
      isShowingLoginPopup = true

      // 清除过期登录信息
      useUserStore().logout()

      // 提示用户
      globalToast.error({ msg: '登录已过期，请重新登录！', duration: 500 })

      // 接入 useAuthCheck 弹出登录弹框
      const authCheck = getAuthCheck()
      authCheck.showLoginPopup.value = true

      // 监听弹框关闭，重置标记
      const unwatch = watch(() => authCheck.showLoginPopup.value, (visible) => {
        if (!visible) {
          isShowingLoginPopup = false
          unwatch()
        }
      })
    }
    throw new ApiError('登录已过期，请重新登录！', error.code, error.data)
  }

  // Handle different types of errors
  if (error.name === 'NetworkError') {
    globalToast.error('网络错误，请检查您的网络连接')
  }
  else if (error.name === 'TimeoutError') {
    globalToast.error('请求超时，请重试')
  }
  else if (error instanceof ApiError) {
    globalToast.error(error.message || '请求失败')
  }
  else {
    globalToast.error('发生意外错误')
  }

  throw error
}
