<script setup lang="ts">
import type { UploadBuildFormDataOption, UploadChangeEvent, UploadFile } from 'wot-design-uni/components/wd-upload/types'

definePage({
  name: 'profile-edit',
  layout: 'default',
  style: {
    navigationBarTitleText: '个人信息',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()
const router = useRouter()

// 表单数据
const formData = ref({
  nickname: '',
  avatar: '',
  gender: '0',
})

// 头像上传
const fileList = ref<UploadFile[]>([])
const actionUrl = 'https://up-z1.qiniup.com'
const fileCdnUrl = ref<string>()

// 性别选项
const genderOptions = [
  { label: '男', value: '1' },
  { label: '女', value: '2' },
  { label: '保密', value: '0' },
]

// 页面加载
onLoad(() => {
  if (!userStore.isLoggedIn) {
    userStore.requireAuth(() => initData())
    return
  }
  initData()
})

function initData() {
  // 从 store 初始化数据
  formData.value = {
    nickname: userStore.nickname || '',
    avatar: userStore.avatar || '',
    gender: userStore.gender || '0',
  }

  // 获取最新资料
  userStore.getProfile().then(() => {
    formData.value = {
      nickname: userStore.nickname || '',
      avatar: userStore.avatar || '',
      gender: userStore.gender || '0',
    }
    if (formData.value.avatar) {
      fileList.value = [{ url: formData.value.avatar, status: 'success' }]
    }
  })
}

// 头像上传变化
function uploadChange(e: UploadChangeEvent) {
  const { fileList: files } = e
  const file = files[0]
  if (file.status === 'success' && fileCdnUrl.value) {
    toast.success('上传成功')
    formData.value.avatar = fileCdnUrl.value
  }
}

async function buildFormData({ file, formData, resolve }: UploadBuildFormDataOption) {
  let imageName = file.url.substring(file.url.lastIndexOf('/') + 1)
  // #ifdef H5
  imageName = imageName + file.name
  // #endif
  const res = await Apis.file.getUploadToken({ params: { fileName: imageName }, meta: { slient: true } })
  if (res.success) {
    fileCdnUrl.value = res.data?.cdnUrl
    formData = {
      ...formData,
      token: res.data?.token,
      key: res.data?.key,
      success_action_status: '200',
    }
  }
  resolve(formData)
}

// 保存资料
async function handleSave() {
  if (!formData.value.nickname.trim()) {
    toast.warning('请输入昵称')
    return
  }

  const success = await userStore.updateProfile({
    nickname: formData.value.nickname.trim(),
    avatar: formData.value.avatar || undefined,
    gender: formData.value.gender,
  })

  if (success) {
    toast.success('资料已更新')
    router.back()
  }
  else {
    toast.error('更新失败，请重试')
  }
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- 头像区域 -->
    <view class="flex flex-col items-center py-6">
      <wd-upload
        v-model:file-list="fileList"
        :header="{ token: userStore.token }"
        :show-limit-num="false"
        custom-evoke-class="rounded-full!"
        :limit="1"
        reupload
        :multiple="false"
        :build-form-data="buildFormData"
        accept="image"
        image-mode="aspectFill"
        :action="actionUrl"
        @change="uploadChange"
      />
      <text class="mt-2 text-sm text-gray-500">
        点击更换头像
      </text>
    </view>

    <!-- 表单区域 -->
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <!-- 手机号 -->
      <view class="mb-4 flex items-center justify-between py-2">
        <text class="text-sm text-gray-600">
          手机号
        </text>
        <view class="flex items-center gap-1">
          <text class="text-sm text-gray-400">
            {{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>
      <!-- 昵称 -->
      <view class="mb-4">
        <text class="mb-2 block text-sm text-gray-600">
          昵称
        </text>
        <wd-input
          v-model="formData.nickname"
          placeholder="请输入昵称"
          :maxlength="20"
          no-border
          custom-class="mt-2"
        />
        <wd-divider custom-class="!mt-2 !px-0" />
      </view>

      <!-- 性别 -->
      <view>
        <text class="mb-2 block text-sm text-gray-600">
          性别
        </text>
        <wd-radio-group v-model="formData.gender" shape="button" custom-class="flex mt-2">
          <wd-radio
            v-for="item in genderOptions"
            :key="item.value"
            :value="item.value"
            custom-class="flex-1 profile-gender-radio"
          >
            {{ item.label }}
          </wd-radio>
        </wd-radio-group>
      </view>
    </view>

    <!-- 保存按钮 -->
    <wd-button type="primary" block @click="handleSave">
      保存
    </wd-button>
  </view>
</template>

<style lang="scss" scoped>
:deep(.wd-upload__evoke) {
  @apply rounded-full! w-20! h-20!;
}

:deep(.wd-upload__picture){
  @apply rounded-full! w-20! h-20!
}

:deep(.wd-upload__preview .wd-icon.wd-upload__close){
  @apply hidden!
}

.profile-gender-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
      @apply border-primary! bg-primary! text-white!;
    }
  }
}

:deep(.wd-radio.is-button .wd-radio__label) {
  max-width: none !important;
  width: 100%;
  border: none !important;
  @apply h-9 items-center flex justify-center py-0;
}
</style>
