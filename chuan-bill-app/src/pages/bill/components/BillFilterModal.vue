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
    custom-style="border-radius: 24px 24px 0 0; overflow: hidden;"
    @close="handleClose"
  >
    <view class="filter-modal">
      <!-- 拖动指示条 -->
      <view class="drag-indicator" />

      <!-- 标题栏 -->
      <view class="filter-header">
        <view class="header-content">
          <view class="i-lucide:sliders-horizontal header-icon" />
          <text class="filter-title">
            筛选条件
          </text>
        </view>
        <view class="i-lucide:x filter-close" @click="handleClose" />
      </view>

      <!-- 筛选表单 -->
      <scroll-view scroll-y class="filter-content">
        <!-- 账单类型 - Pill式选择器 -->
        <view class="filter-card">
          <view class="card-header">
            <view class="i-lucide:tag card-icon" />
            <text class="card-label">
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
            <view class="i-lucide:folder-tree card-icon" />
            <text class="card-label">
              分类
            </text>
          </view>
          <wd-select-picker
            v-model="tempFilters.categoryId"
            placeholder="选择分类"
            :columns="filteredCategories.map(c => ({ value: c.id, label: c.name }))"
            custom-style="background: var(--wot-background-2); border-radius: 12px;"
          />
        </view>

        <!-- 支付方式选择 -->
        <view class="filter-card">
          <view class="card-header">
            <view class="i-lucide:credit-card card-icon" />
            <text class="card-label">
              支付方式
            </text>
          </view>
          <wd-select-picker
            v-model="tempFilters.paymentMethodId"
            placeholder="选择支付方式"
            :columns="paymentMethods.map(p => ({ value: p.id, label: p.name }))"
            custom-style="background: var(--wot-background-2); border-radius: 12px;"
          />
        </view>

        <!-- 日期范围 -->
        <view class="filter-card">
          <view class="card-header">
            <view class="i-lucide:calendar card-icon" />
            <text class="card-label">
              日期范围
            </text>
          </view>
          <view class="date-range-container">
            <view class="date-input-wrapper">
              <view class="i-lucide:calendar-range date-icon" />
              <wd-datetime-picker
                v-model="tempFilters.startDate"
                title="开始日期"
                :show-toolbar="true"
                mode="date"
              >
                <template #default>
                  <view class="date-trigger">
                    {{ tempFilters.startDate ? formatDateToString(tempFilters.startDate) : '开始日期' }}
                  </view>
                </template>
              </wd-datetime-picker>
            </view>
            <view class="date-divider">
              <view class="divider-line" />
              <text class="divider-text">
                至
              </text>
              <view class="divider-line" />
            </view>
            <view class="date-input-wrapper">
              <wd-datetime-picker
                v-model="tempFilters.endDate"
                title="结束日期"
                :show-toolbar="true"
                mode="date"
              >
                <template #default>
                  <view class="date-trigger">
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
            <view class="i-lucide:wallet card-icon" />
            <text class="card-label">
              金额范围
            </text>
          </view>
          <view class="amount-range-container">
            <view class="amount-input-group">
              <text class="amount-prefix">
                ¥
              </text>
              <wd-input
                v-model="tempFilters.minAmount"
                placeholder="最低"
                type="number"
                custom-style="background: var(--wot-background-2); border-radius: 12px; --wd-input-padding: 12px;"
              />
            </view>
            <view class="amount-separator">
              <view class="separator-dot" />
            </view>
            <view class="amount-input-group">
              <text class="amount-prefix">
                ¥
              </text>
              <wd-input
                v-model="tempFilters.maxAmount"
                placeholder="最高"
                type="number"
                custom-style="background: var(--wot-background-2); border-radius: 12px; --wd-input-padding: 12px;"
              />
            </view>
          </view>
        </view>

        <!-- 底部占位 -->
        <view class="bottom-spacer" />
      </scroll-view>

      <!-- 底部按钮 -->
      <view class="filter-footer">
        <view class="btn-reset" @click="handleReset">
          <view class="i-lucide:rotate-ccw btn-icon" />
          <text>重置</text>
        </view>
        <view class="btn-confirm" @click="handleConfirm">
          <view class="i-lucide:check btn-icon" />
          <text>确定筛选</text>
        </view>
      </view>
    </view>
  </wd-popup>
</template>

<style lang="scss" scoped>
.filter-modal {
  background: linear-gradient(180deg, var(--wot-background) 0%, var(--wot-background-2) 100%);
  border-radius: 24px 24px 0 0;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.drag-indicator {
  width: 36px;
  height: 4px;
  background: var(--wot-border-color);
  border-radius: 2px;
  margin: 12px auto 0;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 16px;

  .header-content {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .header-icon {
    font-size: 20px;
    color: var(--wot-color-primary);
  }

  .filter-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--wot-font-color);
    letter-spacing: 0.5px;
  }

  .filter-close {
    font-size: 20px;
    color: var(--wot-font-color-secondary);
    padding: 8px;
    border-radius: 8px;
    background: var(--wot-background-2);
    transition: all 0.2s;

    &:active {
      transform: scale(0.9);
      opacity: 0.7;
    }
  }
}

.filter-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
}

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

  .card-icon {
    font-size: 16px;
    color: var(--wot-color-primary);
  }

  .card-label {
    font-size: 14px;
    font-weight: 600;
    color: var(--wot-font-color);
  }
}

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

.date-range-container {
  display: flex;
  align-items: center;
  gap: 8px;

  .date-input-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    background: var(--wot-background-2);
    border-radius: 12px;
    padding: 0 12px;
    min-height: 44px;
  }

  .date-icon {
    font-size: 16px;
    color: var(--wot-font-color-placeholder);
    flex-shrink: 0;
  }

  .date-trigger {
    flex: 1;
    font-size: 14px;
    color: var(--wot-font-color);
    padding: 12px 0;
    text-align: center;
  }

  .date-divider {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;

    .divider-line {
      width: 12px;
      height: 1px;
      background: var(--wot-border-color);
    }

    .divider-text {
      font-size: 12px;
      color: var(--wot-font-color-placeholder);
    }
  }
}

.amount-range-container {
  display: flex;
  align-items: center;
  gap: 12px;

  .amount-input-group {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    background: var(--wot-background-2);
    border-radius: 12px;
    padding: 0 12px;
    min-height: 44px;
  }

  .amount-prefix {
    font-size: 14px;
    font-weight: 600;
    color: var(--wot-color-primary);
  }

  .amount-separator {
    flex-shrink: 0;

    .separator-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--wot-border-color);
    }
  }
}

.bottom-spacer {
  height: 20px;
}

.filter-footer {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-top: 1px solid var(--wot-border-color);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.06);

  .btn-reset,
  .btn-confirm {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 14px 20px;
    border-radius: 14px;
    font-size: 15px;
    font-weight: 600;
    transition: all 0.25s ease;

    .btn-icon {
      font-size: 16px;
    }

    &:active {
      transform: scale(0.97);
    }
  }

  .btn-reset {
    background: var(--wot-background-2);
    color: var(--wot-font-color-secondary);
    border: 1px solid var(--wot-border-color);

    .btn-icon {
      color: var(--wot-font-color-secondary);
    }
  }

  .btn-confirm {
    background: linear-gradient(135deg, var(--wot-color-primary) 0%, #667eea 100%);
    color: #fff;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.35);

    .btn-icon {
      color: #fff;
    }
  }
}

/* 深色模式适配 */
:root.dark {
  .filter-card,
  .filter-footer {
    background: var(--wot-dark-background2);
    border-color: var(--wot-dark-border-color);
  }

  .filter-content {
    background: transparent;
  }

  .btn-reset {
    background: var(--wot-dark-background);
    border-color: var(--wot-dark-border-color);
  }
}
</style>
