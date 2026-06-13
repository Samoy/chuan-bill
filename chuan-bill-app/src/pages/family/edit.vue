<script setup lang="ts">
import type { FamilyVO } from '@/api/globals'
import { EVENTS } from '@/constant/events'
import { eventBus } from '@/utils/eventBus'

definePage({
  name: 'family-edit',
  layout: 'default',
  style: {
    navigationBarTitleText: '创建家庭',
  },
})

const familyStore = useFamilyStore()
const toast = useGlobalToast()
const message = useGlobalMessage()
const router = useRouter()

const familyId = ref('')
const family = ref<FamilyVO | null>(null)
const isEdit = ref(false)
const formData = ref({
  name: '',
  avatar: '',
  description: '',
})

onLoad((options) => {
  if (options?.familyId) {
    familyId.value = options.familyId
    isEdit.value = true
    uni.setNavigationBarTitle({ title: '编辑家庭' })
    getFamilyDetail()
  }
})

async function getFamilyDetail() {
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

function handleSave() {
  if (isEdit.value) {
    editSave()
  }
  else {
    addSave()
  }
}

async function addSave() {
  if (!formData.value.name.trim()) {
    toast.warning('请输入家庭名称')
    return
  }
  const result = await familyStore.createFamily({
    name: formData.value.name.trim(),
    avatar: formData.value.avatar || undefined,
    description: formData.value.description.trim() || undefined,
  })
  if (result) {
    toast.success('创建成功')
    eventBus.emit(EVENTS.FAMILY.UPDATED)
    router.back()
  }
}

async function editSave() {
  if (!formData.value.name.trim()) {
    toast.warning('请输入家庭名称')
    return
  }
  const result = await familyStore.updateFamily({
    id: familyId.value,
    name: formData.value.name.trim(),
    avatar: formData.value.avatar || undefined,
    description: formData.value.description.trim() || undefined,
  })
  if (result) {
    toast.success('家庭信息已更新')
    eventBus.emit(EVENTS.FAMILY.UPDATED)
    router.back()
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
        eventBus.emit(EVENTS.FAMILY.UPDATED)
        router.back()
      }
    },
  })
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <!-- 头像选择 -->
      <view class="mb-4 flex items-center gap-4">
        <ImageUpload v-model:url="formData.avatar" />
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
        <wd-input v-model="formData.name" placeholder="请输入家庭名称" :maxlength="15" no-border custom-class="mt-2" />
        <wd-divider custom-class="!mt-2 !px-0" />
      </view>

      <!-- 家庭描述 -->
      <view>
        <text class="mb-2 block text-sm text-gray-600">
          家庭描述（可选）
        </text>
        <wd-textarea
          v-model="formData.description" custom-class="p-0!" placeholder="简单介绍一下你的家庭" :maxlength="200"
          show-word-limit
        />
      </view>
    </view>

    <!-- 操作按钮 -->
    <wd-button type="primary" block @click="handleSave">
      保存
    </wd-button>

    <wd-button v-if="isEdit" type="error" plain block @click="handleDelete">
      删除家庭
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
</style>
