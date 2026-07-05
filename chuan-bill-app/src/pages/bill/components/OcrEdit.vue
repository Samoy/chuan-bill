<script setup lang="ts">
import type { UploadFile, UploadSuccessEvent } from 'wot-design-uni/components/wd-upload/types'
import type { BillVO, ResultTempFileVO, TempFileVO } from '@/api/globals'
import BillCard from './BillCard.vue'

defineOptions({
  name: 'OcrEdit',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const emit = defineEmits<{
  submit: [result: BillVO]
}>()

enum TaskStatus {
  Init = 'init',
  UploadFailed = 'uploadFailed',
  Pending = 'pending',
  Success = 'success',
  Failed = 'failed',
}

const actionUrl = ref('/api/file/temp/upload')

const fileList = ref<UploadFile[]>([])
const tempFileInfo = ref<TempFileVO>()
const taskResult = ref<BillVO>()
const taskStatus = ref(TaskStatus.Init)
const toast = useGlobalToast()
const user = useUserStore()

async function startTask() {
  const fileId = tempFileInfo.value?.fileId
  const fileExt = tempFileInfo.value?.fileExt
  if (fileId && fileExt) {
    taskStatus.value = TaskStatus.Pending
    try {
      const res = await Apis.ai.ocr({ params: { fileId, fileExt } })
      if (res.code === 200) {
        toast.success('识别成功')
        taskResult.value = res.data
        taskStatus.value = TaskStatus.Success
      }
      else {
        taskStatus.value = TaskStatus.Failed
      }
    }
    catch (error) {
      console.error(error)
      taskStatus.value = TaskStatus.Failed
    }
  }
  else {
    taskStatus.value = TaskStatus.Failed
  }
}

function uploadSuccess(e: UploadSuccessEvent) {
  const res = e.file.response
  if (res) {
    let tempFileInfoRes: ResultTempFileVO
    if (typeof res === 'string') {
      tempFileInfoRes = JSON.parse(res)
    }
    else {
      tempFileInfoRes = res as ResultTempFileVO
    }
    if (tempFileInfoRes.success) {
      tempFileInfo.value = tempFileInfoRes.data
      if (tempFileInfo.value) {
        startTask()
        return
      }
    }
  }
  toast.error('图片上传失败')
  taskStatus.value = TaskStatus.UploadFailed
}

function reset() {
  fileList.value = []
  tempFileInfo.value = undefined
  taskResult.value = undefined
  taskStatus.value = TaskStatus.Init
}

// #ifndef H5
actionUrl.value = `${import.meta.env.VITE_API_BASE_URL}${actionUrl.value}`
// #endif
</script>

<template>
  <view class="flex flex-col items-center justify-center gap-3">
    <template v-if="!user.isLoggedIn">
      <text class="mb-2 mt-10 text-xs text-gray-500">
        登录后即可解锁图片识别账单功能
      </text>
      <wd-button block custom-class="mb-10 w-200px" @click="user.showLoginPopup = true">
        立即登录
      </wd-button>
    </template>
    <template v-else>
      <view v-if="taskStatus !== TaskStatus.Success" class="relative h-50 w-full">
        <wd-upload
          v-model:file-list="fileList"
          :action="actionUrl"
          accept="image"
          :limit="1"
          reupload
          :header="{
            token: user.token,
          }"
          :custom-class="`w-full! h-full! ${taskStatus !== TaskStatus.UploadFailed ? 'wd-upload-success' : ''}`"
          :custom-preview-class="`${taskStatus !== TaskStatus.Pending ? 'border-2 border-dashed rounded-xl border-gray-200 dark:border-gray-600' : ''}`"
          @success="uploadSuccess"
          @fail="taskStatus = TaskStatus.UploadFailed"
        >
          <view
            class="h-50 w-full flex flex-col items-center justify-center gap-3 border-2 border-gray-200 rounded-lg border-dashed dark:border-gray-600"
          >
            <view class="h-16 w-16 flex items-center justify-center rounded-full bg-primary/10">
              <view class="i-lucide:camera text-3xl text-primary" />
            </view>
            <text class="text-xs text-gray-500">
              点击此处上传图片，AI将自动识别账单
            </text>
          </view>
        </wd-upload>
        <view
          v-if="taskStatus === TaskStatus.Pending"
          class="absolute bottom-0 left-0 right-0 top-0 box-border h-full w-full flex flex-col items-center justify-center gap-3 rounded-xl bg-black/60"
        >
          <!-- 左上角 -->
          <view class="absolute left-0 top-0 z-20 h-8 w-8 border-b-0 border-l-4 border-r-0 border-t-4 border-primary rounded-tl-xl border-solid" />
          <!-- 右上角 -->
          <view class="absolute right-0 top-0 z-20 h-8 w-8 border-b-0 border-l-0 border-r-4 border-t-4 border-primary rounded-tr-xl border-solid" />
          <!-- 左下角 -->
          <view class="absolute bottom-0 left-0 z-20 h-8 w-8 border-b-4 border-l-4 border-r-0 border-t-0 border-primary rounded-bl-xl border-solid" />
          <!-- 右下角 -->
          <view class="absolute bottom-0 right-0 z-20 h-8 w-8 border-b-4 border-l-0 border-r-4 border-t-0 border-primary rounded-br-xl border-solid" />
          <!-- 扫描线 -->
          <view class="scan-line absolute left-0 right-0 z-10 h-1 shadow-[0_0_40px_rgb(var(--color-primary)/0.8)]" />
        </view>
      </view>

      <text v-if="taskStatus === TaskStatus.Pending" class="text-center text-xs text-gray-400">
        AI识别中，请稍候...
      </text>
      <template v-if="taskStatus === TaskStatus.Failed">
        <image class="h-10 w-10" src="@/static/cross.svg" mode="aspectFit" />
        <text class="text-center text-xs text-gray-400">
          识别失败，请重试或者手动输入
        </text>
        <view class="w-full flex gap-3">
          <wd-button custom-class="flex-1" @click="startTask">
            重试
          </wd-button>
          <wd-button plain custom-class="flex-1">
            手动输入
          </wd-button>
        </view>
      </template>
      <template v-if="taskResult">
        <image class="h-10 w-10" src="@/static/checkmark.svg" mode="aspectFit" />
        <BillCard :bill="taskResult" custom-class="w-full flex" />
        <view class="box-border w-full flex gap-3">
          <wd-button plain custom-class="flex-1" @click="reset">
            重新识别
          </wd-button>
          <wd-button custom-class="flex-1" @click="emit('submit', taskResult)">
            确认入账
          </wd-button>
        </view>
        <text class="text-center text-xs text-gray-300 dark:text-gray-600">
          内容由AI生成，可能出现错误，请仔细辨别。
        </text>
      </template>
    </template>
  </view>
</template>

<style scoped lang="scss">
:deep(.wd-upload__evoke-slot){
  @apply w-full! h-full!
}
:deep(.wd-upload__preview){
  @apply w-full! h-full!
}
:deep(.wd-upload-success .wd-icon.wd-upload__close){
  @apply hidden!
}
.scan-line {
  background: linear-gradient(90deg,
  transparent 0%,
  rgb(var(--color-primary) / 0.2) 20%,
  rgb(var(--color-primary)) 50%,
  rgb(var(--color-primary) / 0.2) 80%,
  transparent 100%);
  animation: slide 3s linear infinite;
}
@keyframes slide {
  0% {
   top: 2%;
  }
  50% {
   top: 98%;
  }
  100% {
   top: 2%;
  }
}
</style>
