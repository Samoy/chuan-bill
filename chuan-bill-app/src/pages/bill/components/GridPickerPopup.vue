<script setup lang="ts">
import type { CategoryVO, PaymentMethodVO } from '@/api/globals'
import { ICON_LIST } from '@/constant/icons'

defineOptions({
  name: 'GridPickerPopup',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})
const props = withDefaults(defineProps<{
  modelValue?: string
  title: string
  items: GridPickerItem[]
  type?: 'expense' | 'income'
  entity: 'category' | 'paymentMethod'
  showOthers?: boolean
}>(), {
  showOthers: false,
  modelValue: '',
})
const emit = defineEmits<{
  'update:modelValue': [value: string]
  'change': [value: string]
  'itemsUpdated': []
}>()
const globalToast = useGlobalToast()
const globalMessage = useGlobalMessage()

type GridPickerItem = CategoryVO | PaymentMethodVO

const billStore = useBillStore()
const user = useUserStore()

const visible = defineModel<boolean>('visible', { default: false })
const editMode = ref(false)
const showForm = ref(false)
const editingItem = ref<GridPickerItem | null>(null)
const formName = ref('')
const formIcon = ref('')
const saving = ref(false)

// 过滤后的列表（隐藏"其他"）
const filteredItems = computed(() => {
  if (props.showOthers) {
    return props.items
  }
  return props.items.filter(item => !(item.isDefault && item.name?.includes('其他')))
})

// 分组：预设 + 自定义
const presetItems = computed(() => filteredItems.value.filter(item => item.isDefault))
const customItems = computed(() => filteredItems.value.filter(item => !item.isDefault))

// 显示文本
const displayText = computed(() => {
  const selected = props.items.find(item => item.id === props.modelValue)
  return selected?.name || '请选择'
})

function handleSelect(id: string) {
  if (editMode.value)
    return
  emit('update:modelValue', id)
  emit('change', id)
  visible.value = false
}

function enterEditMode() {
  editMode.value = true
}

function exitEditMode() {
  editMode.value = false
}

function openAddForm() {
  editingItem.value = null
  formName.value = ''
  formIcon.value = ''
  showForm.value = true
}

function openEditForm(item: GridPickerItem) {
  editingItem.value = item
  formName.value = item.name || ''
  formIcon.value = item.icon || ''
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingItem.value = null
  formName.value = ''
  formIcon.value = ''
}

async function handleSave() {
  if (!formName.value.trim()) {
    globalToast.show('请输入名称')
    return
  }
  if (!formIcon.value) {
    globalToast.show('请选择图标')
    return
  }

  saving.value = true
  try {
    if (props.entity === 'category') {
      if (editingItem.value) {
        await billStore.updateCategory(editingItem.value.id!, {
          name: formName.value.trim(),
          icon: formIcon.value,
        })
      }
      else {
        await billStore.addCategory({
          name: formName.value.trim(),
          icon: formIcon.value,
          type: props.type || 'expense',
        })
      }
    }
    else {
      if (editingItem.value) {
        await billStore.updatePaymentMethod(editingItem.value.id!, {
          name: formName.value.trim(),
          icon: formIcon.value,
        })
      }
      else {
        await billStore.addPaymentMethod({
          name: formName.value.trim(),
          icon: formIcon.value,
        })
      }
    }
    emit('itemsUpdated')
    closeForm()
    globalToast.success(editingItem.value ? '修改成功' : '添加成功')
  }
  catch (error: any) {
    globalMessage.alert(error.message || '操作失败')
  }
  finally {
    saving.value = false
  }
}

async function handleDelete(item: GridPickerItem) {
  try {
    if (props.entity === 'category') {
      await billStore.deleteCategory(item.id!)
    }
    else {
      await billStore.deletePaymentMethod(item.id!)
    }
    // 如果删除的是当前选中项，清除选中
    if (props.modelValue === item.id) {
      emit('update:modelValue', '')
      emit('change', '')
    }
    emit('itemsUpdated')
    globalToast.success('删除成功')
  }
  catch (error: any) {
    globalMessage.alert(error.message || '删除失败')
  }
}

function confirmDelete(item: GridPickerItem) {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除"${item.name}"吗？`,
    success: (res) => {
      if (res.confirm) {
        handleDelete(item)
      }
    },
  })
}

function handleClose() {
  if (editMode.value) {
    editMode.value = false
  }
  visible.value = false
}

// 获取选中项的图标
const selectedIcon = computed(() => {
  const selected = props.items.find(item => item.id === props.modelValue)
  return selected?.icon || ''
})
</script>

<template>
  <!-- 触发器 -->
  <view
    class="flex items-center justify-between rounded-xl bg-gray-50/80 px-3 py-2 dark:bg-black/30"
    @click="visible = true"
  >
    <view class="flex items-center gap-2 overflow-hidden">
      <text v-if="selectedIcon" :class="selectedIcon" class="text-lg" />
      <text class="truncate text-sm" :class="modelValue ? 'text-gray-800 dark:text-gray-200' : 'text-gray-400'">
        {{ displayText }}
      </text>
    </view>
    <text class="i-lucide:chevron-right text-gray-400" />
  </view>

  <!-- ActionSheet -->
  <wd-action-sheet
    v-model="visible"
    :title="showForm ? (editingItem ? `编辑${title.replace('选择', '')}` : `新增${title.replace('选择', '')}`) : (editMode ? `编辑${title.replace('选择', '')}` : title)"
    position="bottom"
    :closable="!showForm"
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    :z-index="999"
    @close="handleClose"
  >
    <view class="min-h-[40vh] px-4 pb-4">
      <!-- 网格视图 -->
      <view v-if="!showForm">
        <!-- 标题栏操作按钮 -->
        <view v-if="user.isLoggedIn" class="mb-3 flex items-center justify-end gap-3">
          <view
            v-if="!editMode"
            class="flex items-center gap-1 text-sm text-primary"
            @click="enterEditMode"
          >
            <text class="i-lucide:pencil" />
            <text>编辑</text>
          </view>
          <view
            v-else
            class="flex items-center gap-1 text-sm text-primary"
            @click="exitEditMode"
          >
            <text class="i-lucide:check" />
            <text>完成</text>
          </view>
        </view>

        <!-- 预设项 -->
        <view v-if="presetItems.length > 0" class="grid grid-cols-4 gap-3">
          <view
            v-for="item in presetItems"
            :key="item.id"
            class="flex flex-col items-center justify-center rounded-xl py-3 transition-colors"
            :class="[
              modelValue === item.id && !editMode ? 'bg-primary/10 text-primary' : 'bg-gray-50 dark:bg-white/5',
            ]"
            @click="handleSelect(item.id!)"
          >
            <view class="relative">
              <text :class="item.icon" class="text-2xl" />
              <text
                v-if="editMode"
                class="i-lucide:lock absolute text-xs text-gray-400 -right-1 -top-1"
              />
            </view>
            <text class="mt-1 max-w-full truncate text-xs">
              {{ item.name }}
            </text>
          </view>
        </view>

        <!-- 自定义项分隔线 -->
        <view v-if="customItems.length > 0 || (user.isLoggedIn && !editMode)" class="my-3 flex items-center gap-2">
          <view class="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
          <text class="text-xs text-gray-400">
            自定义{{ title.replace('选择', '') }}
          </text>
          <view class="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
        </view>

        <!-- 自定义项 -->
        <view class="grid grid-cols-4 gap-3">
          <view
            v-for="item in customItems"
            :key="item.id"
            class="flex flex-col items-center justify-center rounded-xl py-3 transition-colors"
            :class="[
              modelValue === item.id && !editMode ? 'bg-primary/10 text-primary' : 'bg-gray-50 dark:bg-white/5',
            ]"
            @click="editMode ? undefined : handleSelect(item.id!)"
          >
            <view class="relative">
              <text :class="item.icon" class="text-2xl" />
              <!-- 编辑/删除按钮 -->
              <view v-if="editMode" class="absolute flex gap-0.5 -right-2 -top-2">
                <view
                  class="h-4 w-4 flex items-center justify-center rounded-full bg-blue-500"
                  @click.stop="openEditForm(item)"
                >
                  <text class="i-lucide:pencil text-[10px] text-white" />
                </view>
                <view
                  class="h-4 w-4 flex items-center justify-center rounded-full bg-red-500"
                  @click.stop="confirmDelete(item)"
                >
                  <text class="i-lucide:x text-[10px] text-white" />
                </view>
              </view>
            </view>
            <text class="mt-1 max-w-full truncate text-xs">
              {{ item.name }}
            </text>
          </view>

          <!-- 新增按钮 -->
          <view
            v-if="user.isLoggedIn"
            class="flex flex-col items-center justify-center border-2 border-gray-300 rounded-xl border-dashed py-3 dark:border-gray-600"
            @click="openAddForm"
          >
            <text class="i-lucide:plus text-2xl text-gray-400" />
            <text class="mt-1 text-xs text-gray-400">
              新增
            </text>
          </view>
        </view>

        <!-- 未登录提示 -->
        <view v-if="!user.isLoggedIn" class="mt-4 text-center text-xs text-gray-400">
          登录后可自定义{{ title.replace('选择', '') }}
        </view>
      </view>

      <!-- 表单视图 -->
      <view v-else>
        <!-- 名称输入 -->
        <view class="mb-4">
          <text class="mb-2 block text-sm text-gray-600 dark:text-gray-400">
            名称
          </text>
          <wd-input
            v-model="formName"
            :maxlength="8"
            placeholder="请输入名称（最多8个字）"
            show-word-limit
            no-border
            custom-class="bg-gray-50 rounded-xl dark:bg-black/30"
          />
        </view>

        <!-- 图标选择 -->
        <view class="mb-4">
          <text class="mb-2 block text-sm text-gray-600 dark:text-gray-400">
            图标
          </text>
          <scroll-view scroll-y class="h-[30vh]">
            <view class="grid grid-cols-6 gap-3">
              <view
                v-for="icon in ICON_LIST"
                :key="icon"
                class="h-10 w-10 flex items-center justify-center rounded-lg transition-colors"
                :class="formIcon === icon ? 'bg-primary/10 text-primary' : 'bg-gray-50 dark:bg-white/5'"
                @click="formIcon = icon"
              >
                <text :class="icon" class="text-xl" />
              </view>
            </view>
          </scroll-view>
        </view>

        <!-- 操作按钮 -->
        <view class="flex gap-3">
          <wd-button plain block @click="closeForm">
            取消
          </wd-button>
          <wd-button type="primary" block :loading="saving" @click="handleSave">
            保存
          </wd-button>
        </view>
      </view>
    </view>
  </wd-action-sheet>
</template>
