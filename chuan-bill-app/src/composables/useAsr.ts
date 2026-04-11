const toast = useGlobalToast()

export enum WebSocketStatus {
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  RECOGNIZING = 'recognizing',
  DISCONNECTED = 'disconnected',
  ERROR = 'error',
}

export interface AsrMessage {
  type: string
  message?: string
  data?: {
    text: string
    sentenceEnd: boolean
    beginTime?: number
    endTime?: number
  }
}

export interface AsrConfig {
  format?: string
  sampleRate?: number
  languageHints?: string[]
}

export interface AsrCallbacks {
  onConnected?: () => void
  onDisconnected?: () => void
  onResult?: (result: string, isSentenceEnd: boolean) => void
  onError?: (error: string) => void
  onStatusChange?: (status: WebSocketStatus) => void
}

export class AsrClient {
  private ws: WebSocket | null = null
  private status: WebSocketStatus = WebSocketStatus.DISCONNECTED
  private callbacks: AsrCallbacks = {}
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 3
  private reconnectTimeout: number | null = null
  private isManualDisconnect: boolean = false
  private serviceUrl: string

  constructor(token: string) {
    this.serviceUrl = `${import.meta.env.VITE_WS_BASE_URL}/asr?token=${token}`
  }

  setCallbacks(callbacks: AsrCallbacks) {
    this.callbacks = { ...this.callbacks, ...callbacks }
  }

  connect() {
    return new Promise<void>((resolve, reject) => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        resolve()
        return
      }
      this.status = WebSocketStatus.CONNECTING
      try {
        this.ws = new WebSocket(this.serviceUrl)
        this.ws.onopen = () => {
          console.log('[ASR] WebSocket 连接成功')
          this.reconnectAttempts = 0
          this.isManualDisconnect = false
          this.status = WebSocketStatus.CONNECTED
          this.callbacks.onConnected?.()
          resolve()
        }

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data)
        }

        this.ws.onclose = () => {
          console.log('[ASR] WebSocket 连接关闭')
          this.status = WebSocketStatus.DISCONNECTED
          this.callbacks.onDisconnected?.()
          // 非手动断开才尝试重连
          if (!this.isManualDisconnect) {
            this.attemptReconnect()
          }
        }

        this.ws.onerror = (error) => {
          console.error('[ASR] WebSocket连接错误', error)
          this.status = WebSocketStatus.ERROR
          this.callbacks.onError?.('WebSocket连接错误')
          reject(error)
        }
      }
      catch (error) {
        console.error('[ASR] WebSocket连接失败', error)
        this.status = WebSocketStatus.ERROR
        reject(error)
      }
    })
  }

  private handleMessage(data: string) {
    try {
      const message = JSON.parse(data) as AsrMessage
      console.log('[ASR] 接收到消息', message)
      switch (message.type) {
        case 'connected':
          console.log('[ASR] 服务已连接')
          break
        case 'started':
          this.status = WebSocketStatus.RECOGNIZING
          break
        case 'stopped':
          this.status = WebSocketStatus.CONNECTED
          break
        case 'result':
          if (message.data) {
            console.log('[ASR] 接收到结果', message.data)
            this.callbacks.onResult?.(message.data.text, message.data.sentenceEnd)
          }
          break
        case 'completed':
          console.log('[ASR] 识别完成')
          break
        case 'error':
          console.log('[ASR] 识别错误', message.message)
          this.callbacks.onError?.(message.message || '未知错误')
          break
        default:
          console.log('[ASR] 接收到未知消息类型', message.type)
      }
    }
    catch (error) {
      console.error('[ASR] 解析消息失败', error)
    }
  }

  private attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[ASR] WebSocket重试次数达到上限，放弃重连')
      toast.show('语音服务连接失败，请稍后再试')
      return
    }
    this.reconnectAttempts++
    // 指数退避：1s, 2s, 4s...
    const delay = 2 ** (this.reconnectAttempts - 1) * 1000
    console.log(`[ASR] 尝试重新连接：${this.reconnectAttempts}/${this.maxReconnectAttempts}，延迟 ${delay}ms`)
    this.reconnectTimeout = window.setTimeout(() => {
      this.connect()
    }, delay)
  }

  startRecognition(config?: AsrConfig) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      toast.show('语音服务未连接')
      return
    }
    const command = {
      action: 'start',
      format: config?.format || 'pcm',
      sampleRate: config?.sampleRate || 16000,
      languageHints: config?.languageHints || ['zh'],
    }
    this.ws.send(JSON.stringify(command))
  }

  sendAudioData(audioData: ArrayBuffer) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('[ASR] WebSocket未连接，无法发送音频数据')
      return
    }
    if (this.status !== WebSocketStatus.RECOGNIZING) {
      console.warn('[ASR] 语音识别服务未开始')
      return
    }
    this.ws.send(audioData)
  }

  stopRecognition() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    const command = {
      action: 'stop',
    }
    this.ws.send(JSON.stringify(command))
  }

  disconnect() {
    this.isManualDisconnect = true
    if (this.reconnectTimeout) {
      window.clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.status = WebSocketStatus.DISCONNECTED
  }
}

export function createAsr(token: string) {
  return new AsrClient(token)
}

export function useAsr() {
  let client: AsrClient | null = null
  let recorderManager: UniApp.RecorderManager | null = null
  let isRecording = false

  function init(options: {
    token: string
    onResult?: (result: string, isSentenceEnd: boolean) => void
    onError?: (error: string) => void
  }) {
    return new Promise<void>((resolve, reject) => {
      client = createAsr(options.token)
      client.setCallbacks({
        onConnected: () => {
          console.log('[ASR] 已连接到服务器')
        },
        onDisconnected: () => {
          console.log('[ASR] 已断开连接')
          stopRecording()
        },
        onResult: (text, isSentenceEnd) => {
          options?.onResult?.(text, isSentenceEnd)
        },
        onError: (error) => {
          console.log('[ASR] 发生错误', error)
          options?.onError?.(error)
          stopRecording()
        },
      })

      // #ifndef H5
      recorderManager = uni.getRecorderManager()
      recorderManager.onStart(() => {
        console.log('[ASR] 录音开始')
        isRecording = true
      })
      recorderManager.onPause(() => {
        console.log('[ASR] 录音暂停')
        isRecording = false
      })

      recorderManager.onStop(() => {
        console.log('[ASR] 录音停止')
        isRecording = false
        client?.stopRecognition()
      })

      recorderManager.onError((res) => {
        console.log('[ASR] 录音错误', res)
        isRecording = false
      })

      recorderManager.onFrameRecorded((res) => {
        if (res.frameBuffer) {
          client?.sendAudioData(res.frameBuffer)
        }
      })
      // #endif
      client.connect()
        .then(resolve)
        .catch((error) => {
          console.error('[ASR] 连接失败', error)
          reject(error)
          options?.onError?.('连接语音识别服务失败')
        })
    })
  }

  function startRecording(config?: {
    duration?: number
    sampleRate?: number
    numberOfChannels?: number
    encodeBitRate?: number
    format?: string
  }) {
    if (!client) {
      toast.show('语音识别服务未初始化')
      return
    }
    client.startRecognition({
      format: config?.format || 'pcm',
      languageHints: ['zh'],
      sampleRate: config?.sampleRate || 16000,
    })
    // #ifndef H5
    if (recorderManager) {
      recorderManager.start({
        duration: config?.duration || 60000,
        sampleRate: config?.sampleRate || 16000,
        numberOfChannels: config?.numberOfChannels || 1,
        encodeBitRate: config?.encodeBitRate || 48000,
        format: config?.format || 'pcm',
      })
    }
    // #endif
    // #ifdef H5
    startH5Recording(config)
    // #endif
  }

  async function startH5Recording(config?: { sampleRate?: number }) {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)({
        sampleRate: config?.sampleRate || 16000,
      })
      const source = audioContext.createMediaStreamSource(stream)
      const processor = audioContext.createScriptProcessor(4096, 1, 1)
      source.connect(processor)
      processor.connect(audioContext.destination)
      processor.onaudioprocess = (event) => {
        if (!isRecording) {
          return
        }
        const inputData = event.inputBuffer.getChannelData(0)
        const int16Data = new Int16Array(inputData.length)
        for (let i = 0; i < inputData.length; i++) {
          int16Data[i] = Math.max(-32768, Math.min(32767, inputData[i] * 32768))
        }
        console.log('[ASR] 发送音频数据', int16Data.buffer)
        client?.sendAudioData(int16Data.buffer)
      }
      isRecording = true

      // 保存引用以便停止
      ;(window as any).__AsrAudioContext = audioContext
      ;(window as any).__AsrProcessor = processor
      ;(window as any).__AsrStream = stream
    }
    catch (error) {
      console.error('[ASR] 获取麦克风权限失败', error)
      toast.show('无法访问麦克风')
    }
  }

  function stopRecording() {
    isRecording = false
    // #ifndef H5
    recorderManager?.stop()
    // #endif

    // #ifdef H5
    stopH5Recording()
    // #endif

    client?.stopRecognition()
  }

  function stopH5Recording() {
    const audioContext = (window as any).__AsrAudioContext
    const processor = (window as any).__AsrProcessor
    const stream = (window as any).__AsrStream
    audioContext?.close()
    processor?.disconnect()
    stream?.getTracks().forEach((track: MediaStreamTrack) => track.stop())

    delete (window as any).__AsrAudioContext
    delete (window as any).__AsrProcessor
    delete (window as any).__AsrStream
  }

  function destroy() {
    stopRecording()
    client?.disconnect()
    client = null
    recorderManager = null
  }

  function getIsRecording() {
    return isRecording
  }
  return {
    init,
    startRecording,
    stopRecording,
    destroy,
    getIsRecording,
  }
}
