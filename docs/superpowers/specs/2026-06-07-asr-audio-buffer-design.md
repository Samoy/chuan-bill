# ASR 音频缓冲区设计

## 问题描述

`useAsr.ts` 中的 `AsrClient.sendAudioData()` 在 WebSocket 断连时直接丢弃音频数据（静默返回）。当 WS 通过自动重连恢复连接后，断连期间录音产生的音频帧已经丢失，导致语音识别结果不完整。

## 目标

1. WebSocket 断连期间，录音产生的音频数据缓存在内存中
2. 重连成功后，自动将缓冲区中的数据发送到服务端
3. 缓冲区有 30 秒时长上限，防止内存溢出
4. 仅修改前端 `useAsr.ts`，不涉及后端改动

## 设计方案

### 录音参数调整

- `frameSize` 从 16ms 调整为 **20ms**（业界实时 ASR 标准值）
- 基于 20ms frameSize、16kHz 采样率、16bit 单声道：
  - 每帧字节数 = 16000 x 0.02 x 2 = 640 字节
  - 每秒帧数 = 1000 / 20 = 50 帧/秒
  - 30 秒最大帧数 = 50 x 30 = **1500 帧**
  - 内存占用上限 = 1500 x 640 = **960KB**

### AsrClient 类变更

#### 新增字段

```typescript
private audioBuffer: ArrayBuffer[] = []
private maxBufferFrames = 1500  // 30s x 50fps
private isRecordingActive = false
private lastAsrConfig: AsrConfig | null = null  // 缓存最近一次识别配置，用于重连后重新发送 start 命令
```

#### 新增公开方法：`setRecording(active: boolean)`

- 由 `useAsr` 层在开始/停止录音时调用
- 设置 `isRecordingActive` 状态
- 录音停止时（`active = false`）清空缓冲区

#### 新增私有方法：`flushAudioBuffer()`

- 从缓冲区头部逐帧取出并发送
- 每帧发送前检查 `isConnected`，断连则停止 flush（剩余帧保留）
- flush 完成后打印日志

#### 修改：`startRecognition(config?)`

缓存识别配置，供重连后重新发送 start 命令使用：

```
startRecognition(config?):
  lastAsrConfig = config || lastAsrConfig  // 缓存配置
  // ... 原有逻辑
```

#### 修改：`sendAudioData(audioData: ArrayBuffer)`

当前代码有两个阻止发送的条件（未连接、未开始识别），两种情况都应缓冲：

```
if !connected:
  bufferAudio(audioData)  // 断连时缓存
  return
if status !== RECOGNIZING:
  bufferAudio(audioData)  // 识别未开始时也缓存（重连后 start 命令尚未发出的窗口期）
  return
socketTask.send(audioData)  // 原有逻辑

private bufferAudio(data: ArrayBuffer):
  if audioBuffer.length >= maxBufferFrames:
    audioBuffer.shift()  // 丢弃最早帧（FIFO 滑动窗口）
  audioBuffer.push(data)
  console.warn('[ASR] 音频数据已缓存，缓冲帧数:', audioBuffer.length)
```

#### 修改：`connect()` 的 `onOpen` 回调

重连后需要先重新发送 `start` 命令（服务端会创建新的 AsrSession，`isRecognizing` 为 false），再 flush 缓冲区：

```
onOpen:
  isConnected = true
  reconnectAttempts = 0
  status = CONNECTED
  callbacks.onConnected?.()
  // 新增：如果正在录音，重新发送 start 命令后 flush 缓冲区
  if isRecordingActive:
    startRecognition(lastAsrConfig)
    if audioBuffer.length > 0:
      flushAudioBuffer()
  resolve()
```

> 注意：手动断开时 `isRecordingActive` 已被设为 false，不会触发此逻辑。

#### 修改：`disconnect()`

手动断开时清空缓冲区，确保不会触发 flush：

```
disconnect():
  isManualDisconnect = true
  isRecordingActive = false
  audioBuffer = []  // 新增
  // ... 原有逻辑
```

### useAsr() 函数变更

#### `startRecording()` 改动

1. 调用 `client.setRecording(true)` 同步录音状态
2. `recorderManager.start()` 和 H5 录音的 `frameSize` 默认值改为 20

#### `stopRecording()` 改动

调用 `client.setRecording(false)` 同步录音状态

### 边界情况处理

| 场景 | 行为 |
|------|------|
| flush 过程中再次断连 | 剩余帧保留在缓冲区，等下次重连继续 flush |
| 重连失败（达到最大重试次数） | 缓冲区数据保留，用户重新开始录音时清空 |
| 用户手动取消录音 | `destroy()` -> `stopRecording()` -> `setRecording(false)` -> 清空缓冲区 |
| 缓冲区满时新帧到达 | 丢弃最早帧，推入新帧 |

### 改动文件

| 文件 | 改动内容 |
|------|----------|
| `chuan-bill-app/src/composables/useAsr.ts` | AsrClient 类新增缓冲逻辑，useAsr() 同步录音状态，frameSize 默认值调整 |

不涉及后端改动。
