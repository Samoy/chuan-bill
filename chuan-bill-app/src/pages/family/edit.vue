<script setup lang="ts">
import type { FamilyVO } from '@/api/globals'

definePage({
  name: 'family-edit',
  style: {
    navigationBarTitleText: '编辑家庭',
  },
})

const familyStore = useFamilyStore()
const toast = useGlobalToast()
const message = useGlobalMessage()
const router = useRouter()

const familyId = ref('')
const family = ref<FamilyVO | null>(null)
const loading = ref(false)
const saving = ref(false)

const formData = ref({
  name: '',
  avatar: '',
  description: '',
})

onLoad((options) => {
  if (options?.familyId) {
    familyId.value = options.familyId
  }
})

onShow(async () => {
  if (familyId.value) {
    loading.value = true
    try {
      // 获取最新的家庭详情
      await familyStore.fetchFamilyDetail(familyId.value)
      family.value = familyStore.currentFamily

      // 检查权限：仅户主可编辑
      if (!family.value?.isOwner) {
        toast.error('只有户主可以编辑家庭信息')
        router.back()
        return
      }

      // 初始化表单数据
      formData.value = {
        name: family.value.name || '',
        avatar: family.value.avatar || '',
        description: family.value.description || '',
      }
    }
    finally {
      loading.value = false
    }
  }
})

async function handleSave() {
  if (!formData.value.name.trim()) {
    toast.warning('请输入家庭名称')
    return
  }

  saving.value = true
  try {
    const result = await familyStore.updateFamily({
      id: familyId.value,
      name: formData.value.name.trim(),
      avatar: formData.value.avatar || undefined,
      description: formData.value.description.trim() || undefined,
    })
    if (result) {
      toast.success('家庭信息已更新')
      router.back()
    }
  }
  finally {
    saving.value = false
  }
}

function handleDelete() {
  message.confirm({
    title: '确认删除',
    msg: '删除家庭后所有数据将无法恢复，确定要删除吗？',
    beforeConfirm: async ({ resolve }) => {
      const success = await familyStore.deleteFamily(familyId.value)
      if (success) {
        resolve(true)
        toast.success('家庭已删除')
        router.back()
      }
    },
  })
}

function selectAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempFilePath = res.tempFilePaths[0]
      // TODO: 上传图片到服务器
      // 这里暂时使用本地路径，实际应该上传后获取URL
      formData.value.avatar = tempFilePath
    },
  })
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <!-- 头像选择 -->
      <view class="mb-4 flex items-center gap-4">
        <view
          class="h-16 w-16 flex items-center justify-center overflow-hidden rounded-xl bg-gray-100 dark:bg-gray-700"
          @click="selectAvatar"
        >
          <image v-if="formData.avatar" :src="formData.avatar" class="h-full w-full" mode="aspectFill" />
          <view v-else class="i-lucide:camera h-6 w-6 text-gray-400" />
        </view>
        <view class="flex-1">
          <text class="block text-sm font-500">
            家庭头像
          </text>
          <text class="mt-0.5 block text-xs text-gray-500">
            点击选择头像
          </text>
        </view>
      </view>

      <!-- 家庭名称 -->
      <view class="mb-4">
        <text class="mb-2 block text-sm text-gray-600">
          家庭名称
        </text>
        <wd-input v-model="formData.name" placeholder="请输入家庭名称" :maxlength="20" />
      </view>

      <!-- 家庭描述 -->
      <view>
        <text class="mb-2 block text-sm text-gray-600">
          家庭描述（可选）
        </text>
        <wd-textarea v-model="formData.description" custom-class="p-0!" placeholder="简单介绍一下你的家庭" :maxlength="200" show-word-limit />
      </view>
    </view>

    <!-- 操作按钮 -->
    <wd-button type="primary" block size="large" :loading="saving" @click="handleSave">
      保存
    </wd-button>

    <wd-button type="error" plain block size="large" @click="handleDelete">
      删除家庭
    </wd-button>
  </view>
</template>
