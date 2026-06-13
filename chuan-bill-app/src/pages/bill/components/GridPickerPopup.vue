<script setup lang="ts">
import type { CategoryVO, PaymentMethodVO } from '@/api/globals'

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
  customClass?: string
  customStyle?: string
}>(), {
  showOthers: false,
  modelValue: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'change': [value: string]
  'itemsUpdated': []
}>()

type GridPickerItem = CategoryVO | PaymentMethodVO

const globalToast = useGlobalToast()
const globalMessage = useGlobalMessage()

const billStore = useBillStore()
const user = useUserStore()

const visible = defineModel<boolean>('visible', { default: false })
const editMode = ref(false)

// 过滤后的列表（隐藏"其他"）
const filteredItems = computed(() => {
  if (props.showOthers) {
    return props.items
  }
  return props.items.filter(item => !(item.isDefault && item.name?.includes('其他')))
})

// 预设项和自定义项
const presetItems = computed(() => filteredItems.value.filter(item => item.isDefault))
const customItems = computed(() => filteredItems.value.filter(item => !item.isDefault))

// 显示文本
const displayText = computed(() => {
  const selected = props.items.find(item => item.id === props.modelValue)
  return selected?.name || '请选择'
})

// 自定义项的默认图标
function getDefaultIcon() {
  if (props.entity === 'paymentMethod') {
    return 'i-icon-park-outline:payment-method'
  }
  return props.type === 'income' ? 'i-icon-park-outline:income' : 'i-icon-park-outline:expenses'
}

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

async function handleTagConfirm({ value }: { value: string }) {
  const name = value.trim()
  if (!name)
    return
  if (name.length > 4) {
    globalToast.show('最多4个字符')
    return
  }

  try {
    const icon = getDefaultIcon()
    if (props.entity === 'category') {
      await billStore.addCategory({ name, icon, type: props.type || 'expense' })
    }
    else {
      await billStore.addPaymentMethod({ name, icon })
    }
    emit('itemsUpdated')
    globalToast.success('添加成功')
  }
  catch (error: any) {
    globalMessage.alert(error.message || '添加失败')
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
  globalMessage.confirm({
    title: '确认删除',
    msg: `确定要删除【${item.name}】吗？`,
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    confirmButtonProps: {
      type: 'error',
    },
    success: (res) => {
      if (res.action === 'confirm') {
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
</script>

<template>
  <!-- 触发器 -->
  <view
    class="flex items-center justify-between rounded-xl bg-gray-50/80 px-3 py-2 dark:bg-black/30"
    :class="customClass"
    :style="customStyle"
    @click="visible = true"
  >
    <view class="flex items-center gap-2 overflow-hidden">
      <text class="truncate text-sm" :class="modelValue ? 'text-gray-800 dark:text-gray-200' : 'text-gray-400'">
        {{ displayText }}
      </text>
    </view>
    <text class="i-lucide:chevron-right text-gray-400" />
  </view>

  <!-- ActionSheet -->
  <wd-action-sheet
    v-model="visible"
    :title="editMode ? `编辑${title.replace('选择', '')}` : title"
    position="bottom"
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    :z-index="999"
    @close="handleClose"
  >
    <!-- 编辑/完成图标按钮（关闭按钮左侧） -->
    <text
      v-if="user.isLoggedIn"
      :class="editMode ? 'i-lucide:check' : 'i-lucide:square-pen'"
      class="absolute right-10 top-[var(--wot-action-sheet-close-top,25px)] box-border h-4 w-4 text-black/65 dark:text-[#e8e6e3cc]"
      @click="editMode ? exitEditMode() : enterEditMode()"
    />
    <view class="min-h-[30vh] px-4 pb-4">
      <!-- 普通模式：radio-group 选择 -->
      <view v-if="!editMode" class="grid grid-cols-3 gap-2">
        <wd-radio-group
          :model-value="modelValue"
          shape="button"
          custom-class="contents"
          @update:model-value="handleSelect"
        >
          <wd-radio
            v-for="item in filteredItems"
            :key="item.id"
            :value="item.id!"
            custom-class="normal-radio"
          >
            <text :class="transformUnoCSS(item.icon || '')" class="mr-1" /> {{ item.name }}
          </wd-radio>
        </wd-radio-group>

        <!-- 新增标签（行内末尾） -->
        <wd-tag
          v-if="user.isLoggedIn"
          type="primary"
          round
          dynamic
          custom-class="!flex !justify-center !items-center mr-10px ![width:calc(100%-10px)] !h-8"
          @confirm="handleTagConfirm"
        >
          <template #add>
            <view class="h-full flex items-center gap-1">
              <text class="i-lucide:plus" />
              <text>新增</text>
            </view>
          </template>
        </wd-tag>
      </view>

      <!-- 编辑模式：预设项不可操作 + 自定义项可删除 -->
      <view v-else class="grid grid-cols-3 gap-2">
        <!-- 预设项（不可操作） -->
        <view
          v-for="item in presetItems"
          :key="item.id"
          class="h-8 inline-flex items-center justify-center rounded-xl bg-gray-100 px-3 text-sm text-gray-300 dark:bg-white/10"
        >
          <text :class="transformUnoCSS(item.icon || '')" class="mr-1" /> {{ item.name }}
        </view>

        <!-- 自定义项（可删除） -->
        <wd-tag
          v-for="item in customItems"
          :key="item.id"
          type="primary"
          round
          closable
          custom-class="!flex !justify-center !items-center !w-auto"
          @close="confirmDelete(item)"
        >
          {{ item.name }}
          <template #icon>
            <text :class="transformUnoCSS(item.icon || '')" />
          </template>
        </wd-tag>

        <!-- 新增标签（行内末尾） -->
        <wd-tag
          v-if="user.isLoggedIn"
          type="primary"
          round
          dynamic
          custom-class="!flex !justify-center !items-center !h-8 !w-auto"
          @confirm="handleTagConfirm"
        >
          <template #add>
            <view class="flex items-center gap-1">
              <text class="i-lucide:plus" />
              <text>新增</text>
            </view>
          </template>
        </wd-tag>
      </view>

      <!-- 未登录提示 -->
      <view v-if="!user.isLoggedIn" class="mt-4 text-center text-xs text-gray-400">
        登录后可自定义{{ title.replace('选择', '') }}
      </view>
    </view>
  </wd-action-sheet>
</template>

<style lang="scss" scoped>
:deep(.wd-radio.is-button .wd-radio__label) {
  max-width: none !important;
  width: 100%;
  border: none !important;
  @apply h-8 items-center flex justify-center py-0;
}
.normal-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
      @apply border-primary! bg-primary! text-white! shadow-primary/20 shadow-lg;
    }
  }
}
</style>
