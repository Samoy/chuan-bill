<script setup lang="ts">
import type { BillVO } from '@/api/globals'
import BillCard from './BillCard.vue'

defineOptions({
  name: 'VoiceEdit',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const emit = defineEmits<{
  submit: [result: BillVO]
}>()

enum TaskStatus {
  Idle = 'idle',
  Pending = 'pending',
  Success = 'success',
}

const asrText = ref('')
const webSocketError = ref(false)
const isRecording = ref(false)
const startY = ref<number>()
const isCanceled = ref(false)
const tipText = ref('按住按钮开始录音')
const asrFullTextList = ref<string[]>([])
const asrClient = useAsr()
const taskStatus = ref(TaskStatus.Idle)
const billVO = ref<BillVO>()
const toast = useGlobalToast()
const shadowColor = ref('var(--color-primary)')
const user = useUserStore()

async function initAsr() {
  await asrClient.init({
    token: user.token,
    onResult: (text: string, isSentenceEnd: boolean) => {
      webSocketError.value = false
      asrText.value = asrFullTextList.value.join().concat(text)
      if (isSentenceEnd) {
        asrFullTextList.value.push(text)
      }
      if (asrText.value && !isCanceled.value) {
        tipText.value = '松开手指开始识别，上划取消录音'
      }
    },
    onError: (error) => {
      console.error('ASR error', error)
      webSocketError.value = true
      isRecording.value = false
    },
  })
}

async function touchStart(event: UniHelper.TouchEvent) {
  if (taskStatus.value !== TaskStatus.Idle) {
    return
  }
  startY.value = event.touches[0].clientY
  tipText.value = '正在倾听您的声音, 向上滑动取消录音...'
  isRecording.value = true
  try {
    await initAsr()
    await asrClient.startRecording({
      duration: 60000,
      sampleRate: 16000,
      format: 'pcm',
    })
  }
  catch {
    reset()
  }
}

function touchMove(event: UniHelper.TouchEvent) {
  const clientY = event.touches[0].clientY || 0
  const deltaY = clientY - (startY.value || 0)
  if (deltaY < -60) {
    if (!isCanceled.value) {
      isCanceled.value = true
      tipText.value = '松开手指，取消录音'
    }
  }
  else {
    if (isCanceled.value) {
      isCanceled.value = false
      tipText.value = '松开手指开始识别，上划取消录音'
    }
  }
}

async function touchEnd() {
  asrClient.destroy()
  if (isCanceled.value) {
    reset()
    toast.warning('已取消录音')
    return
  }
  isRecording.value = false
  if (webSocketError.value) {
    toast.error('语音识别服务连接失败')
    reset()
    return
  }
  tipText.value = '开始识别账单...'
  taskStatus.value = TaskStatus.Pending
  try {
    const res = await Apis.ai.text({ params: {
      text: asrText.value,
    } })
    if (res.success) {
      taskStatus.value = TaskStatus.Success
      toast.success('识别成功')
      billVO.value = res.data
    }
  }
  catch (error) {
    console.error('语音账单识别失败', error)
    reset()
  }
}

function reset() {
  taskStatus.value = TaskStatus.Idle
  asrText.value = ''
  webSocketError.value = false
  isRecording.value = false
  isCanceled.value = false
  tipText.value = '按住按钮开始录音'
  asrFullTextList.value = []
}

watch(() => isCanceled.value, (newVal) => {
  shadowColor.value = newVal ? '255 0 0' : 'var(--color-primary)'
})
</script>

<template>
  <view class="flex flex-col items-center justify-center gap-3 px-3 pb-3">
    <template v-if="!user.isLoggedIn">
      <text class="mb-2 mt-10 text-xs text-gray-500">
        登录后即可解锁语音输入账单功能
      </text>
      <wd-button block custom-class="mb-7 w-200px" @click="user.showLoginPopup = true">
        立即登录
      </wd-button>
    </template>
    <template v-else>
      <template v-if="taskStatus === TaskStatus.Success && billVO">
        <view class="h-10 w-10 flex items-center justify-center rounded-full bg-green-100">
          <!-- 成功图标 -->
          <wd-icon name="check-circle" size="20px" color="green" />
        </view>
        <bill-card :bill="billVO" custom-class="w-full flex" />
        <view class="box-border w-full flex gap-3">
          <wd-button plain custom-class="flex-1" @click="reset">
            重新识别
          </wd-button>
          <wd-button custom-class="flex-1" @click="emit('submit', billVO)">
            确认入账
          </wd-button>
        </view>
        <text class="text-center text-xs text-gray-300 dark:text-gray-600">
          内容由AI生成，可能出现错误，请仔细辨别。
        </text>
      </template>
      <template v-else>
        <view
          class="box-border h-20 w-full flex items-center justify-center overflow-y-auto rounded-lg bg-primary/10 p-3 shadow-sm"
          :style="{ visibility: (isRecording || asrText) ? 'visible' : 'hidden' }"
        >
          {{ asrText }}
        </view>
        <view class="text-xs text-gray-400">
          {{ tipText }}
        </view>
        <view
          class="relative h-16 w-16 flex select-none items-center justify-center rounded-full bg-primary"
          :class="{ 'mic': isRecording, 'bg-red-500': isCanceled }" @touchmove.prevent.stop="touchMove"
          @touchstart.prevent.stop="touchStart" @touchend.prevent.stop="touchEnd"
        >
          <wd-loading v-if="taskStatus === TaskStatus.Pending" color="#ffffff" />
          <text v-else class="i-lucide:mic z-1 text-2xl text-white" />
        </view>
      </template>
    </template>
  </view>
</template>

<style scoped lang="scss">
.mic {
  --shadow-color: v-bind(shadowColor);
  animation: mic-animation 1.5s ease-in-out infinite;
}
@keyframes mic-animation {
  0% {
    box-shadow: 0 0 0 0 rgb(var(--shadow-color) / 0.5);
  }
  100% {
    box-shadow: 0 0 0 30px rgb(var(--shadow-color) / 0);
  }
}
</style>
