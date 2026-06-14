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

const filteredItems = computed(() => {
  if (props.showOthers) {
    return props.items
  }
  return props.items.filter(item => !(item.isDefault && item.name?.includes('其他')))
})

const presetItems = computed(() => filteredItems.value.filter(item => item.isDefault))
const customItems = computed(() => filteredItems.value.filter(item => !item.isDefault))

const displayText = computed(() => {
  const selected = props.items.find(item => item.id === props.modelValue)
  return selected?.name || '请选择'
})

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

// ==================== movable-view 拖拽排序 ====================

const COLS = 3
const ITEM_H = 32
const GAP = 8

const editingCustomItems = ref<GridPickerItem[]>([])
const movableX = ref<number[]>([])
const movableY = ref<number[]>([])

function calcPos(index: number) {
  const col = index % COLS
  const row = Math.floor(index / COLS)
  return {
    x: col * (100 + GAP),
    y: row * (ITEM_H + GAP),
  }
}

function syncPositions() {
  movableX.value = editingCustomItems.value.map((_, i) => calcPos(i).x)
  movableY.value = editingCustomItems.value.map((_, i) => calcPos(i).y)
}

watch(editMode, (val) => {
  if (val) {
    editingCustomItems.value = [...customItems.value]
    syncPositions()
  }
})

watch(() => props.items, () => {
  if (editMode.value) {
    editingCustomItems.value = [...customItems.value]
    syncPositions()
  }
})

function onDragChange(index: number, e: any) {
  if (e.detail.source !== 'touch')
    return

  const x = e.detail.x
  const y = e.detail.y

  const col = Math.round(x / (100 + GAP))
  const row = Math.round(y / (ITEM_H + GAP))
  const maxIndex = editingCustomItems.value.length - 1
  const newIndex = Math.max(0, Math.min(row * COLS + col, maxIndex))

  if (newIndex !== index) {
    const items = [...editingCustomItems.value]
    const [moved] = items.splice(index, 1)
    items.splice(newIndex, 0, moved)
    editingCustomItems.value = items
    syncPositions()
  }
  else {
    movableX.value[index] = calcPos(index).x
    movableY.value[index] = calcPos(index).y
  }
}

function onDragTouchEnd() {
  const finalIds = editingCustomItems.value.map(item => item.id!)
  saveSortOrder(finalIds)
}

async function saveSortOrder(ids: string[]) {
  try {
    if (props.entity === 'category') {
      await billStore.sortCategories(ids)
    }
    else {
      await billStore.sortPaymentMethods(ids)
    }
    emit('itemsUpdated')
  }
  catch (error: any) {
    globalMessage.alert(error.message || '排序失败')
    emit('itemsUpdated')
  }
}

const presetRows = computed(() => Math.ceil(presetItems.value.length / COLS))
const customRows = computed(() => Math.ceil(editingCustomItems.value.length / COLS))
const totalRows = computed(() => presetRows.value + customRows.value)
const movableAreaHeight = computed(() => totalRows.value * (ITEM_H + GAP) + 10)

function getPresetStyle(index: number) {
  const col = index % COLS
  const row = Math.floor(index / COLS)
  return {
    position: 'absolute' as const,
    left: `${col * (100 + GAP)}px`,
    top: `${row * (ITEM_H + GAP)}px`,
    width: '100px',
    height: '32px',
  }
}

function getCustomY(index: number) {
  return (presetRows.value * (ITEM_H + GAP)) + calcPos(index).y
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
    <text
      v-if="user.isLoggedIn"
      :class="editMode ? 'i-lucide:check' : 'i-lucide:square-pen'"
      class="absolute right-10 top-[var(--wot-action-sheet-close-top,25px)] box-border h-4 w-4 text-black/65 dark:text-[#e8e6e3cc]"
      @click="editMode ? exitEditMode() : enterEditMode()"
    />
    <view class="min-h-[30vh] px-4 pb-4">
      <!-- 普通模式 -->
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

      <!-- 编辑模式 -->
      <view v-else>
        <view class="mb-2 text-center text-xs text-gray-400">
          拖拽自定义项可排序
        </view>

        <!-- 合并 movable-area：覆盖所有项 -->
        <movable-area
          class="w-full"
          :style="{ height: `${movableAreaHeight}px` }"
        >
          <!-- 预设项（不可拖拽，置灰） -->
          <view
            v-for="(item, index) in presetItems"
            :key="item.id"
            class="preset-item"
            :style="getPresetStyle(index)"
          >
            <text :class="transformUnoCSS(item.icon || '')" class="mr-1" /> {{ item.name }}
          </view>

          <!-- 自定义项（可拖拽） -->
          <movable-view
            v-for="(item, index) in editingCustomItems"
            :key="item.id"
            :x="movableX[index]"
            :y="getCustomY(index)"
            direction="all"
            :damping="30"
            :friction="3"
            :animation="true"
            class="drag-movable-item"
            @change="onDragChange(index, $event)"
            @touchend="onDragTouchEnd"
          >
            <view class="h-8 inline-flex items-center justify-center rounded-xl bg-primary/10 px-3 text-sm text-primary">
              <text :class="transformUnoCSS(item.icon || '')" class="mr-1" />
              <text>{{ item.name }}</text>
              <text class="i-lucide:grip-vertical ml-1 text-xs opacity-50" />
              <text
                class="i-lucide:x ml-1 text-xs opacity-60"
                @click.stop="confirmDelete(item)"
              />
            </view>
          </movable-view>
        </movable-area>

        <!-- 新增标签 -->
        <view class="mt-2">
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
.preset-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  background: rgb(243 244 246);
  padding: 0 0.75rem;
  font-size: 0.875rem;
  color: rgb(209 213 219);
}
.drag-movable-item {
  width: 100px;
  height: 32px;
}
</style>
