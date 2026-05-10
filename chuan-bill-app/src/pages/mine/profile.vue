<script setup lang="ts">
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

// 性别选项
const genderOptions = [
  { label: '男', value: 1 },
  { label: '女', value: 2 },
  { label: '保密', value: 0 },
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
  })
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
      <ImageUpload v-model:url="formData.avatar" />
      <text class="mt-2 text-sm text-gray-500">
        点击更换头像
      </text>
    </view>

    <!-- 表单区域 -->
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
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
  @apply rounded-full! w-80px! h-80px! box-border bg-gray-200;
}

:deep(.wd-upload__picture) {
  @apply rounded-full! box-border bg-gray-200;
}

:deep(.wd-upload__preview) {
  @apply w-80px! h-80px! box-border m-0;
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
