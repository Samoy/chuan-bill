<script setup lang="ts">
import type { UploadFile, UploadSuccessEvent } from 'wot-design-uni/components/wd-upload/types'
import type { AddBillDTO, ResultTempFileVO, TempFileVO } from '@/api/globals'

defineOptions({
  name: 'OcrEdit',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

enum TaskStatus {
  Init = 'init',
  Pending = 'pending',
  Success = 'success',
  Failed = 'failed',
}

const actionUrl = import.meta.env.VITE_API_UPLOAD_TEMP_FILE_URL
const fileList = ref<UploadFile[]>([])
const tempFileInfo = ref<TempFileVO>()
const taskResult = ref<AddBillDTO>()
const taskStatus = ref(TaskStatus.Init)
const toast = useGlobalToast()

async function startTask() {
  const fileId = tempFileInfo.value?.fileId
  if (fileId) {
    taskStatus.value = TaskStatus.Pending
    try {
      const res = await Apis.ai.ocr({ params: { fileId } })
      if (res.code === 200) {
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
    tempFileInfo.value = tempFileInfoRes.data
  }
  if (tempFileInfo.value) {
    startTask()
  }
  else {
    toast.error('上传失败，请重试上传')
  }
}
</script>

<template>
  <view class="flex flex-col items-center justify-center gap-3">
    <wd-upload
      v-model="fileList"
      :action="actionUrl"
      accept="image"
      :limit="1"
      reupload
      :header="{
        // FIXME：暂时使用固定token
        token: 'VmkIL7QUN2B2LgLsbcqZdKdqrbnbDa4FQcch2E0qGt3Le6vihyd0sxzyRXDTS3ov',
      }"
      custom-class="w-full! h-50!"
      :custom-preview-class="`${taskStatus !== TaskStatus.Pending ? 'border-2 border-dashed rounded-xl border-gray-200 dark:border-gray-600' : ''}`"
      @success="uploadSuccess"
    >
      <view
        class="h-50 w-full flex flex-col items-center justify-center gap-3 border-2 border-gray-300 rounded-xl border-dashed dark:border-gray-600"
      >
        <view class="h-16 w-16 flex items-center justify-center rounded-full bg-primary/10">
          <view class="i-lucide:camera text-3xl text-primary" />
        </view>
        <text class="text-xs text-gray-500">
          点击此处上传图片，AI将自动识别账单
        </text>
      </view>
      <template #preview-cover>
        <view v-if="taskStatus === TaskStatus.Pending" class="absolute inset-0 flex flex-col items-center justify-center gap-3 rounded-xl bg-black/60">
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
      </template>
    </wd-upload>
    <text v-if="taskStatus === TaskStatus.Pending" class="text-center text-xs text-gray-400">
      AI识别中，请稍候...
    </text>
    <template v-if="taskStatus === TaskStatus.Failed">
      <view class="h-8 w-8 flex items-center justify-center rounded-full bg-red-200">
        <!-- 失败图标 -->
        <text class="i-carbon:close text-xs text-red-500" />
      </view>
      <text class="text-center text-xs text-gray-400">
        AI识别失败，请重试或者手动输入
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
  </view>
</template>

<style scoped lang="scss">
:deep(.wd-upload__evoke-slot){
  @apply w-full h-50!
}
:deep(.wd-upload__preview){
  @apply w-full h-full!
}
:deep(.wd-upload__close){
  @apply hidden
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
