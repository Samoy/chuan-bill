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
  private socketTask: UniApp.SocketTask | null = null
  private isConnected = false
  private status: WebSocketStatus = WebSocketStatus.DISCONNECTED
  private callbacks: AsrCallbacks = {}
  private reconnectAttempts = 0
  private maxReconnectAttempts = 3
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null
  private isManualDisconnect = false
  private serviceUrl: string
  private audioBuffer: ArrayBuffer[] = []
  private maxBufferFrames = 1000 // 30s x ~31fps (frameSize: 1KB, 16kHz, 16bit, mono)
  private isRecordingActive = false
  private lastAsrConfig?: AsrConfig
  private toast: ReturnType<typeof useGlobalToast>

  constructor(token: string, toast: ReturnType<typeof useGlobalToast>) {
    this.serviceUrl = `${import.meta.env.VITE_WS_BASE_URL}/asr?token=${token}`
    this.toast = toast
  }

  setCallbacks(callbacks: AsrCallbacks) {
    this.callbacks = { ...this.callbacks, ...callbacks }
  }

  setRecording(active: boolean) {
    this.isRecordingActive = active
    if (!active) {
      this.audioBuffer = []
    }
  }

  private bufferAudio(data: ArrayBuffer) {
    if (this.audioBuffer.length >= this.maxBufferFrames) {
      this.audioBuffer.shift()
      console.warn('[ASR] 缓冲区已满，丢弃最早帧')
    }
    this.audioBuffer.push(data)
    console.log('[ASR] 音频数据已缓存，缓冲帧数:', this.audioBuffer.length)
  }

  private flushAudioBuffer() {
    const total = this.audioBuffer.length
    let sent = 0
    console.log(`[ASR] 开始刷新缓冲区，共 ${total} 帧`)
    while (this.audioBuffer.length > 0) {
      if (!this.isConnected) {
        console.warn(`[ASR] flush 过程中连接断开，已发送 ${sent}/${total} 帧，剩余帧保留`)
        return
      }
      const frame = this.audioBuffer.shift()!
      this.socketTask!.send({ data: frame })
      sent++
    }
    console.log(`[ASR] 缓冲区已刷新，共发送 ${sent} 帧`)
  }

  connect() {
    return new Promise<void>((resolve, reject) => {
      if (this.isConnected) {
        resolve()
        return
      }
      this.status = WebSocketStatus.CONNECTING

      this.socketTask = uni.connectSocket({
        url: this.serviceUrl,
        success: () => {
          console.log('[ASR] WebSocket 连接请求已发送')
        },
        fail: (err) => {
          console.error('[ASR] WebSocket 连接失败', err)
          this.status = WebSocketStatus.ERROR
          reject(err)
        },
      })

      this.socketTask.onOpen(() => {
        console.log('[ASR] WebSocket 连接成功')
        this.isConnected = true
        this.reconnectAttempts = 0
        this.isManualDisconnect = false
        this.status = WebSocketStatus.CONNECTED
        this.callbacks.onConnected?.()
        if (this.isRecordingActive) {
          this.startRecognition(this.lastAsrConfig)
          if (this.audioBuffer.length > 0) {
            this.flushAudioBuffer()
          }
        }
        resolve()
      })

      this.socketTask.onMessage((res) => {
        this.handleMessage(res.data as string)
      })

      this.socketTask.onClose(() => {
        console.log('[ASR] WebSocket 连接关闭')
        this.isConnected = false
        this.status = WebSocketStatus.DISCONNECTED
        this.callbacks.onDisconnected?.()
        if (!this.isManualDisconnect) {
          this.attemptReconnect()
        }
      })

      this.socketTask.onError((err) => {
        console.error('[ASR] WebSocket 连接错误', err)
        this.isConnected = false
        this.status = WebSocketStatus.ERROR
        this.callbacks.onError?.('WebSocket连接错误')
        reject(err)
      })
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
      this.toast.show('语音服务连接失败，请稍后再试')
      return
    }
    this.reconnectAttempts++
    const delay = 2 ** (this.reconnectAttempts - 1) * 1000
    console.log(`[ASR] 尝试重新连接：${this.reconnectAttempts}/${this.maxReconnectAttempts}，延迟 ${delay}ms`)
    this.reconnectTimeout = setTimeout(() => {
      this.connect()
    }, delay)
  }

  startRecognition(config?: AsrConfig) {
    this.lastAsrConfig = config || this.lastAsrConfig
    if (!this.isConnected) {
      this.toast.show('语音服务未连接')
      return
    }
    const command = {
      action: 'start',
      format: config?.format || 'pcm',
      sampleRate: config?.sampleRate || 16000,
      languageHints: config?.languageHints || ['zh'],
    }
    this.socketTask!.send({ data: JSON.stringify(command) })
  }

  sendAudioData(audioData: ArrayBuffer) {
    if (!this.isConnected) {
      this.bufferAudio(audioData)
      return
    }
    if (this.status !== WebSocketStatus.RECOGNIZING) {
      this.bufferAudio(audioData)
      return
    }
    this.socketTask!.send({ data: audioData })
  }

  stopRecognition() {
    if (!this.isConnected) {
      return
    }
    const command = {
      action: 'stop',
    }
    this.socketTask!.send({ data: JSON.stringify(command) })
  }

  disconnect() {
    this.isManualDisconnect = true
    this.isRecordingActive = false
    this.audioBuffer = []
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }
    if (this.socketTask) {
      this.socketTask.close({})
      this.socketTask = null
    }
    this.isConnected = false
    this.status = WebSocketStatus.DISCONNECTED
  }
}

export function createAsr(token: string, toast: ReturnType<typeof useGlobalToast>) {
  return new AsrClient(token, toast)
}

export function useAsr() {
  const toast = useGlobalToast()
  const message = useGlobalMessage()
  let client: AsrClient | null = null
  let recorderManager: UniApp.RecorderManager | null = null
  let isRecording = false

  function init(options: {
    token: string
    onResult?: (result: string, isSentenceEnd: boolean) => void
    onError?: (error: string) => void
  }) {
    return new Promise<void>((resolve, reject) => {
      client = createAsr(options.token, toast)
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

      // #ifdef MP-WEIXIN
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

  async function checkMicrophonePermission(): Promise<boolean> {
    return new Promise<boolean>((resolve, reject) => {
      // #ifdef H5
      return true
      // #endif
      // #ifdef APP-PLUS
      if (uni.getSystemInfoSync().platform === 'android') {
        plus.android.requestPermissions(['android.permission.RECORD_AUDIO'], (e) => {
          if (e.deniedAlways.length > 0) {
            message.alert({
              title: '提示',
              msg: '麦克风权限被永久拒绝，您需要在应用设置中打开该权限',
            })
            reject(new Error('Always Denied'))
          }
          if (e.deniedPresent.length > 0) {
            message.confirm({
              title: '提示',
              msg: '您需要打开麦克风权限才能进行录音',
              success: (res) => {
                if (res.action === 'confirm') {
                  checkMicrophonePermission()
                }
              },
            })
            reject(new Error('Present Denied'))
          }
          if (e.granted.length > 0) {
            resolve(true)
          }
        }, (e) => {
          console.error(`Request Permissions error:${JSON.stringify(e)}`)
        })
      }
      // #endif
      // #ifdef MP-WEIXIN
      uni.authorize({
        scope: 'scope.record',
        success: () => {
          resolve(true)
        },
        fail: () => {
          message.confirm({
            title: '提示',
            msg: '需要麦克风权限才能录音，请在设置中打开权限',
            success: (res) => {
              if (res.action === 'confirm') {
                uni.openSetting()
              }
            },
            confirmButtonText: '去设置',
          })
          reject(new Error('Permission Denied'))
        },
      })
      // #endif
    })
  }

  async function startRecording(config?: {
    duration?: number
    sampleRate?: number
    numberOfChannels?: number
    encodeBitRate?: number
    format?: string
  }) {
    if (!client) {
      toast.show('语音识别服务未初始化')
      throw new Error('语音识别服务未初始化')
    }
    const hasPermission = await checkMicrophonePermission()
    if (!hasPermission) {
      throw new Error('麦克风权限被拒绝')
    }
    client.setRecording(true)
    client.startRecognition({
      format: config?.format || 'pcm',
      languageHints: ['zh'],
      sampleRate: config?.sampleRate || 16000,
    })
    // #ifdef MP-WEIXIN
    if (recorderManager) {
      recorderManager.start({
        duration: config?.duration || 60000,
        sampleRate: config?.sampleRate || 44100,
        numberOfChannels: config?.numberOfChannels || 1,
        encodeBitRate: config?.encodeBitRate || 48000,
        format: 'PCM',
        frameSize: 1,
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
    client?.setRecording(false)
    // #ifdef MP-WEIXIN
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
