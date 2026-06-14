# ASR 音频缓冲区 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 WebSocket 断连时缓存音频数据，重连后自动 flush，避免语音识别数据丢失。

**Architecture:** 在 `AsrClient` 类中添加环形缓冲区，断连时音频帧入队（FIFO 滑动窗口，最大 1500 帧 ≈ 30 秒），重连后先重发 `start` 命令再 flush 缓冲区。`useAsr()` 层同步录音状态并调整 frameSize 默认值为 20ms。

**Tech Stack:** TypeScript, uni-app WebSocket API, Vue 3 Composition API

---

### Task 1: 添加缓冲区字段和新方法到 AsrClient

**Files:**
- Modify: `chuan-bill-app/src/composables/useAsr.ts:36-212`

- [ ] **Step 1: 添加缓冲区相关字段**

在 `AsrClient` 类中，在 `private serviceUrl: string`（第 45 行）之后添加以下字段：

```typescript
  private serviceUrl: string
  private audioBuffer: ArrayBuffer[] = []
  private maxBufferFrames = 1500 // 30s x 50fps (20ms frameSize, 16kHz, 16bit, mono)
  private isRecordingActive = false
  private lastAsrConfig: AsrConfig | null = null
```

- [ ] **Step 2: 添加 `setRecording` 公开方法**

在 `setCallbacks` 方法（第 51 行）之后添加：

```typescript
  setRecording(active: boolean) {
    this.isRecordingActive = active
    if (!active) {
      this.audioBuffer = []
    }
  }
```

- [ ] **Step 3: 添加 `bufferAudio` 私有方法**

在 `setRecording` 方法之后添加：

```typescript
  private bufferAudio(data: ArrayBuffer) {
    if (this.audioBuffer.length >= this.maxBufferFrames) {
      this.audioBuffer.shift()
      console.warn('[ASR] 缓冲区已满，丢弃最早帧')
    }
    this.audioBuffer.push(data)
    console.warn('[ASR] 音频数据已缓存，缓冲帧数:', this.audioBuffer.length)
  }
```

- [ ] **Step 4: 添加 `flushAudioBuffer` 私有方法**

在 `bufferAudio` 方法之后添加：

```typescript
  private flushAudioBuffer() {
    const count = this.audioBuffer.length
    console.log(`[ASR] 开始刷新缓冲区，共 ${count} 帧`)
    while (this.audioBuffer.length > 0) {
      if (!this.isConnected) {
        console.warn('[ASR] flush 过程中连接断开，剩余帧保留')
        return
      }
      const frame = this.audioBuffer.shift()!
      this.socketTask!.send({ data: frame })
    }
    console.log(`[ASR] 缓冲区已刷新，共发送 ${count} 帧`)
  }
```

- [ ] **Step 5: 运行类型检查确认无报错**

```bash
cd chuan-bill-app && pnpm type-check
```

Expected: 无错误

- [ ] **Step 6: Commit**

```bash
git add chuan-bill-app/src/composables/useAsr.ts
git commit -m "feat(asr): 添加音频缓冲区字段和辅助方法"
```

---

### Task 2: 修改现有 AsrClient 方法以支持缓冲

**Files:**
- Modify: `chuan-bill-app/src/composables/useAsr.ts`

- [ ] **Step 1: 修改 `startRecognition` 方法缓存配置**

将 `startRecognition` 方法（第 159-171 行）替换为：

```typescript
  startRecognition(config?: AsrConfig) {
    this.lastAsrConfig = config || this.lastAsrConfig
    if (!this.isConnected) {
      toast.show('语音服务未连接')
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
```

- [ ] **Step 2: 修改 `sendAudioData` 方法支持缓冲**

将 `sendAudioData` 方法（第 173-187 行）替换为：

```typescript
  sendAudioData(audioData: ArrayBuffer) {
    if (!this.isConnected) {
      this.bufferAudio(audioData)
      return
    }
    if (this.status !== WebSocketStatus.RECOGNIZING) {
      this.bufferAudio(audioData)
      return
    }
    this.socketTask!.send({
      data: audioData,
      success: () => {
        console.log('[ASR] 音频数据发送成功')
      },
      fail: (error) => {
        console.log('[ASR] 音频数据发送失败', error)
      },
    })
  }
```

- [ ] **Step 3: 修改 `connect` 方法的 `onOpen` 回调**

将 `this.socketTask.onOpen` 回调（第 75-83 行）替换为：

```typescript
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
```

- [ ] **Step 4: 修改 `disconnect` 方法清空缓冲区**

将 `disconnect` 方法（第 199-211 行）替换为：

```typescript
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
```

- [ ] **Step 5: 运行类型检查确认无报错**

```bash
cd chuan-bill-app && pnpm type-check
```

Expected: 无错误

- [ ] **Step 6: Commit**

```bash
git add chuan-bill-app/src/composables/useAsr.ts
git commit -m "feat(asr): 实现断连缓冲和重连 flush 逻辑"
```

---

### Task 3: 更新 useAsr() 集成

**Files:**
- Modify: `chuan-bill-app/src/composables/useAsr.ts:218-398`

- [ ] **Step 1: 修改 `startRecording` 方法**

将 `startRecording` 方法（第 288-319 行）替换为：

```typescript
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
    client.setRecording(true)
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
        frameSize: 20,
      })
    }
    // #endif
    // #ifdef H5
    startH5Recording(config)
    // #endif
  }
```

关键变更：
- 添加 `client.setRecording(true)` 调用
- `frameSize` 从 16 改为 20

- [ ] **Step 2: 修改 `stopRecording` 方法**

将 `stopRecording` 方法（第 355-366 行）替换为：

```typescript
  function stopRecording() {
    isRecording = false
    client?.setRecording(false)
    // #ifndef H5
    recorderManager?.stop()
    // #endif

    // #ifdef H5
    stopH5Recording()
    // #endif

    client?.stopRecognition()
  }
```

关键变更：添加 `client?.setRecording(false)` 调用

- [ ] **Step 3: 运行类型检查和 lint**

```bash
cd chuan-bill-app && pnpm type-check && pnpm lint:fix
```

Expected: 无错误

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-app/src/composables/useAsr.ts
git commit -m "feat(asr): useAsr 集成录音状态同步和 frameSize 调整"
```

---

### Task 4: 端到端验证

- [ ] **Step 1: 启动 H5 开发服务器**

```bash
cd chuan-bill-app && pnpm dev
```

- [ ] **Step 2: 手动测试录音功能**

在浏览器中打开页面，测试以下场景：
1. 正常录音 → 松开 → 识别成功（回归测试）
2. 录音过程中断开网络 → 恢复网络 → 验证识别结果是否完整
3. 录音过程中取消录音 → 验证缓冲区被清空

- [ ] **Step 3: 检查控制台日志**

确认以下日志正常输出：
- `[ASR] 音频数据已缓存，缓冲帧数: N`（断连时）
- `[ASR] 开始刷新缓冲区，共 N 帧`（重连时）
- `[ASR] 缓冲区已刷新，共发送 N 帧`（flush 完成时）

- [ ] **Step 4: 最终 Commit（如有修复）**

```bash
git add -A
git commit -m "fix(asr): 修复音频缓冲区验证中发现的问题"
```
