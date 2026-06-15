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
  const { statusCode, data, header } = response as UniNamespace.RequestSuccessCallbackResult

  function handleError(code: number, message: string) {
    // 处理401/403错误
    if ((code === 401 || code === 403)) {
      // 防止重复弹出
      if (isShowingLoginPopup) {
        throw new ApiError('登录已过期，请重新登录！', code, data)
      }
      isShowingLoginPopup = true

      // 清除过期登录信息并弹出登录弹框
      const userStore = useUserStore()
      userStore.logout()

      // 提示用户
      globalToast.error({ msg: '登录已过期，请重新登录！' })

      // 弹出登录弹框
      userStore.showLoginPopup = true

      // 监听弹框关闭，重置标记
      const unwatch = watch(() => userStore.showLoginPopup, (visible) => {
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

  // Handle arraybuffer responses (e.g., file downloads)
  // When responseType is 'arraybuffer', successful responses are ArrayBuffer
  // but error responses may still be JSON with Content-Type: application/json
  if (data instanceof ArrayBuffer) {
    const contentType = header?.['content-type'] || header?.['Content-Type'] || ''

    // If the response is JSON (error from backend), parse it
    if (contentType.includes('application/json')) {
      try {
        const textDecoder = new TextDecoder('utf-8')
        const jsonString = textDecoder.decode(data)
        const json = JSON.parse(jsonString) as ApiResponse

        if (json.code >= 400) {
          handleError(json.code, json.message || '请求失败')
        }

        // If code is 0 or 200 (success), still return the parsed JSON
        return json
      }
      catch {
        // If parsing fails, treat as regular binary response
        return response
      }
    }

    // For non-JSON arraybuffer responses (successful binary download), return the ArrayBuffer data
    return data
  }

  // The data is already parsed by UniApp adapter
  const json = data as ApiResponse

  if (statusCode >= 400) {
    handleError(statusCode, '系统异常，请稍后再试')
  }
  if (json.code >= 400) {
    handleError(json.code, json.message || '系统异常，请稍后再试')
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

      // 清除过期登录信息并弹出登录弹框
      const userStore = useUserStore()
      userStore.logout()

      // 提示用户
      globalToast.error({ msg: '登录已过期，请重新登录！', duration: 500 })

      // 弹出登录弹框
      userStore.showLoginPopup = true

      // 监听弹框关闭，重置标记
      const unwatch = watch(() => userStore.showLoginPopup, (visible) => {
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
    globalToast.error(error.message || '系统异常，请稍后再试')
  }
  else {
    globalToast.error('发生意外错误')
  }

  throw error
}
