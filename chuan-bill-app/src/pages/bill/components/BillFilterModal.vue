<script setup lang="ts">
defineOptions({
  name: 'BillFilterModal',
  virtualHost: true,
  addGlobalClass: true,
  styleIsolation: 'shared',
})

const props = withDefaults(defineProps<{
  modelValue: boolean
  currentFilters?: {
    type?: string
    categoryId?: string
    paymentMethodId?: string
    startDate?: string
    endDate?: string
    minAmount?: number
    maxAmount?: number
  }
}>(), {
  currentFilters: () => ({}),
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'confirm': [filters: any]
  'reset': []
}>()

// 使用 computed 处理 v-model
const showPopup = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val),
})

type FilterParams = NonNullable<typeof props.currentFilters>

// 临时筛选表单数据
const tempFilters = ref<FilterParams>({})

// 分类和支付方式选项
const categories = ref<any[]>([])
const paymentMethods = ref<any[]>([])

// 账单类型选项
const typeOptions = [
  { label: '全部', value: '', icon: 'i-lucide:layers' },
  { label: '支出', value: 'expense', icon: 'i-lucide:trending-down' },
  { label: '收入', value: 'income', icon: 'i-lucide:trending-up' },
]

// 监听弹框打开/关闭
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      // 打开时初始化表单数据
      tempFilters.value = { ...props.currentFilters }
      loadOptions()
    }
  },
  { immediate: true },
)

// 加载分类和支付方式数据
async function loadOptions() {
  try {
    const [categoriesRes, paymentMethodsRes] = await Promise.all([
      Apis.general.getCategories({ params: {} }),
      Apis.general.getPaymentMethods({}),
    ])
    categories.value = categoriesRes.data || []
    paymentMethods.value = paymentMethodsRes.data || []
  }
  catch (error) {
    console.error('加载选项数据失败:', error)
  }
}

// 根据类型过滤分类
const filteredCategories = computed(() => {
  if (!tempFilters.value.type)
    return categories.value
  return categories.value.filter(c => c.type === tempFilters.value.type)
})

// 格式化日期为 yyyy-MM-dd
function formatDateToString(date: any): string {
  if (!date)
    return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 确认筛选
function handleConfirm() {
  const filters: any = { ...tempFilters.value }
  // 转换日期格式
  if (filters.startDate)
    filters.startDate = formatDateToString(filters.startDate)
  if (filters.endDate)
    filters.endDate = formatDateToString(filters.endDate)
  emit('confirm', filters)
  emit('update:modelValue', false)
}

// 重置筛选
function handleReset() {
  tempFilters.value = {}
  emit('reset')
  emit('update:modelValue', false)
}

// 关闭弹框
function handleClose() {
  emit('update:modelValue', false)
}
</script>

<template>
  <wd-popup
    v-model="showPopup"
    position="bottom"
    :close-on-click-modal="true"
    custom-class="!rounded-3xl !rounded-b-none"
    @close="handleClose"
  >
    <view class="filter-modal">
      <!-- 拖动指示条 -->
      <view class="drag-indicator" />

      <!-- 标题栏 -->
      <view class="flex items-center justify-between px-5 py-4">
        <view class="flex items-center gap-2.5">
          <view class="i-lucide:sliders-horizontal text-primary text-lg" />
          <text class="text-base text-[var(--wot-font-color)] font-semibold">
            筛选条件
          </text>
        </view>
        <view
          class="i-lucide:x rounded-lg bg-[var(--wot-background-2)] p-2 text-lg text-[var(--wot-font-color-secondary)] transition-transform active:scale-90"
          @click="handleClose"
        />
      </view>

      <!-- 筛选表单 -->
      <scroll-view scroll-y class="filter-content">
        <!-- 账单类型 - Pill式选择器 -->
        <view class="filter-card">
          <view class="card-header">
            <view class="i-lucide:tag text-primary" />
            <text class="text-sm text-[var(--wot-font-color)] font-semibold">
              账单类型
            </text>
          </view>
          <view class="type-pills">
            <view
              v-for="option in typeOptions"
              :key="option.value"
              class="type-pill"
              :class="{ active: tempFilters.type === option.value }"
              @click="tempFilters.type = option.value"
            >
              <view :class="option.icon" class="pill-icon" />
              <text>{{ option.label }}</text>
            </view>
          </view>
        </view>

        <!-- 分类选择 -->
        <view class="filter-card">
          <view class="card-header">
            <view class="i-lucide:folder-tree text-primary" />
            <text class="text-sm text-[var(--wot-font-color)] font-semibold">
              分类
            </text>
          </view>
          <wd-select-picker
            v-model="tempFilters.categoryId"
            placeholder="选择分类"
            :columns="filteredCategories.map(c => ({ value: c.id, label: c.name }))"
            custom-class="!bg-[var(--wot-background-2)] !rounded-xl"
          />
        </view>

        <!-- 支付方式选择 -->
        <view class="filter-card">
          <view class="card-header">
            <view class="i-lucide:credit-card text-primary" />
            <text class="text-sm text-[var(--wot-font-color)] font-semibold">
              支付方式
            </text>
          </view>
          <wd-select-picker
            v-model="tempFilters.paymentMethodId"
            placeholder="选择支付方式"
            :columns="paymentMethods.map(p => ({ value: p.id, label: p.name }))"
            custom-class="!bg-[var(--wot-background-2)] !rounded-xl"
          />
        </view>

        <!-- 日期范围 -->
        <view class="filter-card">
          <view class="card-header">
            <view class="i-lucide:calendar text-primary" />
            <text class="text-sm text-[var(--wot-font-color)] font-semibold">
              日期范围
            </text>
          </view>
          <view class="flex items-center gap-2">
            <view class="min-h-11 flex flex-1 items-center gap-2 rounded-xl bg-[var(--wot-background-2)] px-3">
              <view class="i-lucide:calendar-range text-sm text-[var(--wot-font-color-placeholder)]" />
              <wd-datetime-picker
                v-model="tempFilters.startDate"
                title="开始日期"
                :show-toolbar="true"
                mode="date"
              >
                <template #default>
                  <view class="flex-1 py-3 text-center text-sm text-[var(--wot-font-color)]">
                    {{ tempFilters.startDate ? formatDateToString(tempFilters.startDate) : '开始日期' }}
                  </view>
                </template>
              </wd-datetime-picker>
            </view>
            <view class="flex flex-shrink-0 items-center gap-2">
              <view class="h-px w-3 bg-[var(--wot-border-color)]" />
              <text class="text-xs text-[var(--wot-font-color-placeholder)]">
                至
              </text>
              <view class="h-px w-3 bg-[var(--wot-border-color)]" />
            </view>
            <view class="min-h-11 flex flex-1 items-center rounded-xl bg-[var(--wot-background-2)] px-3">
              <wd-datetime-picker
                v-model="tempFilters.endDate"
                title="结束日期"
                :show-toolbar="true"
                mode="date"
              >
                <template #default>
                  <view class="flex-1 py-3 text-center text-sm text-[var(--wot-font-color)]">
                    {{ tempFilters.endDate ? formatDateToString(tempFilters.endDate) : '结束日期' }}
                  </view>
                </template>
              </wd-datetime-picker>
            </view>
          </view>
        </view>

        <!-- 金额范围 -->
        <view class="filter-card">
          <view class="card-header">
            <view class="i-lucide:wallet text-primary" />
            <text class="text-sm text-[var(--wot-font-color)] font-semibold">
              金额范围
            </text>
          </view>
          <view class="flex items-center gap-3">
            <view class="min-h-11 flex flex-1 items-center gap-2 rounded-xl bg-[var(--wot-background-2)] px-3">
              <text class="text-primary text-sm font-semibold">
                ¥
              </text>
              <wd-input
                v-model="tempFilters.minAmount"
                placeholder="最低"
                type="number"
                custom-class="!bg-transparent !p-0"
              />
            </view>
            <view class="flex-shrink-0">
              <view class="h-1.5 w-1.5 rounded-full bg-[var(--wot-border-color)]" />
            </view>
            <view class="min-h-11 flex flex-1 items-center gap-2 rounded-xl bg-[var(--wot-background-2)] px-3">
              <text class="text-primary text-sm font-semibold">
                ¥
              </text>
              <wd-input
                v-model="tempFilters.maxAmount"
                placeholder="最高"
                type="number"
                custom-class="!bg-transparent !p-0"
              />
            </view>
          </view>
        </view>

        <!-- 底部占位 -->
        <view class="h-5" />
      </scroll-view>

      <!-- 底部按钮 -->
      <view class="flex gap-3 border-t border-[var(--wot-border-color)] bg-white px-4 py-4">
        <view
          class="flex flex-1 items-center justify-center gap-2 border border-[var(--wot-border-color)] rounded-xl bg-[var(--wot-background-2)] px-5 py-3.5"
          @click="handleReset"
        >
          <view class="i-lucide:rotate-ccw text-[var(--wot-font-color-secondary)]" />
          <text class="text-sm text-[var(--wot-font-color-secondary)] font-semibold">
            重置
          </text>
        </view>
        <view
          class="shadow-primary/30 flex flex-1 items-center justify-center gap-2 rounded-xl from-[var(--wot-color-primary)] to-[#667eea] bg-gradient-to-r px-5 py-3.5 text-white shadow-lg"
          @click="handleConfirm"
        >
          <view class="i-lucide:check text-white" />
          <text class="text-sm font-semibold">
            确定筛选
          </text>
        </view>
      </view>
    </view>
  </wd-popup>
</template>

<style lang="scss" scoped>
// UnoCSS 无法实现的样式才写在这里

// 拖动指示条
.drag-indicator {
  width: 36px;
  height: 4px;
  background: var(--wot-border-color);
  border-radius: 2px;
  margin: 12px auto 0;
}

// 筛选内容区
.filter-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
}

// 筛选卡片
.filter-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--wot-border-color);

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 14px;
  }
}

// 类型选择Pill
.type-pills {
  display: flex;
  gap: 10px;

  .type-pill {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 12px 8px;
    border-radius: 12px;
    background: var(--wot-background-2);
    border: 2px solid transparent;
    transition: all 0.25s ease;
    cursor: pointer;

    .pill-icon {
      font-size: 16px;
      color: var(--wot-font-color-secondary);
      transition: color 0.25s;
    }

    text {
      font-size: 14px;
      font-weight: 500;
      color: var(--wot-font-color-secondary);
      transition: color 0.25s;
    }

    &:active {
      transform: scale(0.97);
    }

    &.active {
      background: linear-gradient(135deg, var(--wot-color-primary) 0%, #667eea 100%);
      border-color: transparent;

      .pill-icon,
      text {
        color: #fff;
      }
    }
  }
}

// 深色模式适配
:root.dark {
  .filter-card,
  .filter-footer {
    background: var(--wot-dark-background2);
    border-color: var(--wot-dark-border-color);
  }
}
</style>
