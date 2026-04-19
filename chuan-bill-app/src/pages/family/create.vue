<script setup lang="ts">
definePage({
  name: 'family-create',
  style: {
    navigationBarTitleText: '创建家庭',
  },
})

const familyStore = useFamilyStore()
const toast = useGlobalToast()

const formData = ref({
  name: '',
  description: '',
})
const loading = ref(false)

async function handleCreate() {
  if (!formData.value.name.trim()) {
    toast.warning('请输入家庭名称')
    return
  }
  loading.value = true
  try {
    const result = await familyStore.createFamily({
      name: formData.value.name.trim(),
      description: formData.value.description.trim(),
    })
    if (result) {
      toast.success('家庭创建成功')
      uni.navigateBack()
    }
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="mb-4">
        <text class="mb-2 block text-sm text-gray-600">
          家庭名称
        </text>
        <wd-input v-model="formData.name" placeholder="请输入家庭名称" :maxlength="20" />
      </view>
      <view>
        <text class="mb-2 block text-sm text-gray-600">
          家庭描述（可选）
        </text>
        <wd-textarea v-model="formData.description" custom-class="p-0!" placeholder="简单介绍一下你的家庭" :maxlength="200" show-word-limit />
      </view>
    </view>

    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="flex items-start gap-3">
        <view class="i-lucide:info h-5 w-5 shrink-0 text-gray-400" />
        <view>
          <text class="block text-sm text-gray-600">
            创建家庭后，你将自动成为户主
          </text>
          <text class="mt-1 block text-xs text-gray-400">
            可以通过邀请码邀请家人加入，共同管理家庭收支
          </text>
        </view>
      </view>
    </view>

    <wd-button type="primary" block size="large" :loading="loading" @click="handleCreate">
      创建家庭
    </wd-button>
  </view>
</template>
