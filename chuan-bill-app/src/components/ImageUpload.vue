<script lang="ts" setup>
import type { UploadBuildFormDataOption, UploadChangeEvent, UploadFile, UploadStatusType } from 'wot-design-uni/components/wd-upload/types'

defineOptions({
  name: 'ImageUpload',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const props = withDefaults(defineProps<Props>(), {
  accept: 'image',
  disabled: false,
})

const emit = defineEmits<{
  'update:url': [url: string]
  'success': [url: string]
  'error': [message: string]
}>()

interface Props {
  url: string
  accept?: 'image' | 'video'
  disabled?: boolean
}

const toast = useToast()

const UPLOAD_URL = 'https://up-z1.qiniup.com'

const fileList = ref<UploadFile[]>([])
const fileCdnUrl = ref<string>()
const uploadStatus = ref<UploadStatusType>()

watch(() => props.url, (newUrl) => {
  if (newUrl && !fileList.value.some(f => f.url === newUrl)) {
    fileList.value = [{ url: newUrl, status: 'success' }]
    uploadStatus.value = 'success'
  }
  else if (!newUrl) {
    fileList.value = []
  }
}, { immediate: true })

function getFileName(file: any): string {
  let fileName = file.url?.substring(file.url.lastIndexOf('/') + 1) || 'image'
  // #ifdef H5
  if (file.name) {
    fileName = file.name
  }
  // #endif
  return fileName
}

async function buildFormData({ file, formData, resolve }: UploadBuildFormDataOption) {
  const fileName = getFileName(file)
  try {
    const res = await Apis.file.getUploadToken({ params: { fileName: encodeURIComponent(fileName) }, meta: { slient: true } })
    if (res.success && res.data) {
      fileCdnUrl.value = res.data.cdnUrl
      resolve({
        ...formData,
        token: res.data.token,
        key: res.data.key,
        success_action_status: '200',
      })
    }
    else {
      toast.error(res.message || '获取上传凭证失败')
      emit('error', res.message || '获取上传凭证失败')
      resolve(formData)
    }
  }
  catch (error) {
    console.error(error)
    emit('error', '上传失败，请稍后再试')
    toast.error('上传失败，请稍后再试')
    resolve(formData)
  }
}

function handleFileChange(e: UploadChangeEvent) {
  const { fileList: files } = e
  const file = files[0]
  if (file.status === 'success' && fileCdnUrl.value) {
    toast.success('上传成功')
    emit('update:url', fileCdnUrl.value)
    emit('success', fileCdnUrl.value)
    fileCdnUrl.value = undefined
  }
  // 上传失败
  else if (file.status === 'fail') {
    toast.error('上传失败，请重试')
    fileList.value = fileList.value.filter(f => f.status !== 'fail')
  }
  uploadStatus.value = file.status
}

function handleFileError() {
  toast.error('上传失败，请重试')
  uploadStatus.value = 'fail'
}
</script>

<template>
  <view>
    <wd-upload
      v-model:file-list="fileList" :action="UPLOAD_URL" :accept="props.accept"
      :disabled="props.disabled || uploadStatus === 'loading'" :limit="1" :show-limit-num="false" :multiple="false"
      reupload :build-form-data="buildFormData" :custom-class="uploadStatus !== 'fail' ? 'wd-upload-success' : ''"
      image-mode="aspectFill" @change="handleFileChange" @fail="handleFileError"
    />
  </view>
</template>

<style lang="scss">
:deep(.wd-upload-success .wd-upload__preview .wd-icon.wd-upload__close) {
  display: none;
}
</style>
