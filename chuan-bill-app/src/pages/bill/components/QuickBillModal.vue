<script setup lang="ts">
defineOptions({
  name: 'QuickBillModal',
  virtualHost: true,
  addGlobalClass: true,
  styleIsolation: 'shared',
})

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

const toast = useToast('globalToast')

interface Props {
  modelValue: boolean
}

// 表单数据
const formData = ref<Record<string, any>>({
  name: '',
  type: 'expense' as 'income' | 'expense',
  categoryId: '',
  paymentMethodId: '',
  time: new Date(),
  remark: '',
})

// 分类和支付方式选项
const categories = ref<any[]>([])
const paymentMethods = ref<any[]>([])

// 记账方式选项
const sourceOptions = [
  { payload: { text: '手动', icon: 'i-lucide:square-pen' }, value: 'manual' },
  { payload: { text: '拍照', icon: 'i-lucide:camera' }, value: 'ocr' },
  { payload: { text: '语音', icon: 'i-lucide:mic' }, value: 'voice' },
]
const source = ref('manual')

// 使用 computed 处理 v-model
const showPopup = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val),
})

// 监听弹框打开
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      // 重置表单
      resetForm()
      loadOptions()
    }
  },
  { immediate: true },
)

// 根据类型过滤分类
const filteredCategories = computed(() => {
  if (!formData.value.type)
    return categories.value
  return categories.value.filter(c => c.type === formData.value.type)
})

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
    toast.show('加载数据失败')
  }
}

// 重置表单
function resetForm() {
  formData.value = {
    name: '',
    amount: null as number,
    type: 'expense',
    categoryId: '',
    paymentMethodId: '',
    time: new Date(),
    remark: '',
  }
  source.value = 'manual'
}

// 格式化时间为 yyyy-MM-dd HH:mm
function formatDateTime(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 提交表单
async function handleSubmit() {
  // 前端验证
  if (!formData.value.name?.trim()) {
    toast.show('请输入账单名称')
    return
  }
  if (!formData.value.amount || formData.value.amount <= 0) {
    toast.show('请输入有效金额')
    return
  }
  if (!formData.value.categoryId) {
    toast.show('请选择分类')
    return
  }
  if (!formData.value.time) {
    toast.show('请选择日期')
    return
  }

  try {
    toast.loading('正在保存...')

    // 转换时间为正确格式
    const timeValue = formData.value.time instanceof Date
      ? formatDateTime(formData.value.time)
      : formData.value.time

    const res = await Apis.general.addBill({
      data: {
        name: formData.value.name.trim(),
        amount: Number(formData.value.amount),
        type: formData.value.type,
        categoryId: formData.value.categoryId,
        paymentMethodId: formData.value.paymentMethodId || undefined,
        time: timeValue,
        remark: formData.value.remark?.trim() || undefined,
        source: source.value as any,
      },
    })
    toast.close()

    if (res.data) {
      toast.success('记账成功')
      emit('success')
      emit('update:modelValue', false)
    }
  }
  catch (error: any) {
    toast.close()
    console.error('保存失败:', error)
    toast.show(error.message || '保存失败')
  }
}

// 类型切换时清空分类选择
watch(
  () => formData.value.type,
  () => {
    formData.value.categoryId = ''
  },
)
</script>

<template>
  <wd-popup
    v-model="showPopup"
    position="bottom"
    :close-on-click-modal="true"
    custom-style="border-radius: 24px 24px 0 0; overflow: hidden;"
    :z-index="100"
  >
    <view class="quick-bill-modal">
      <!-- 拖动指示条 -->
      <view class="drag-indicator" />

      <!-- 标题栏 -->
      <view class="modal-header">
        <view class="header-content">
          <view class="i-lucide:pen-line header-icon" />
          <text class="modal-title">
            记一笔
          </text>
        </view>
        <view class="i-lucide:x modal-close" @click="showPopup = false" />
      </view>

      <!-- 记账方式切换 -->
      <view class="source-selector">
        <view
          v-for="option in sourceOptions"
          :key="option.value"
          class="source-btn"
          :class="{ active: source === option.value }"
          @click="source = option.value"
        >
          <view :class="option.payload.icon" class="source-icon" />
          <text>{{ option.payload.text }}</text>
        </view>
      </view>

      <!-- 表单内容 -->
      <scroll-view scroll-y class="form-content">
        <!-- 金额输入 - 视觉焦点 -->
        <view class="amount-hero" :class="formData.type">
          <view class="amount-display">
            <text class="currency">
              ¥
            </text>
            <wd-input-number
              v-model="formData.amount"
              :min="0.01"
              :max="9999999999.99"
              :decimal-length="2"
              placeholder="0.00"
              custom-class="amount-input"
            />
          </view>
          <view class="type-toggle">
            <view
              class="type-btn"
              :class="{ active: formData.type === 'expense', expense: formData.type === 'expense' }"
              @click="formData.type = 'expense'"
            >
              <view class="i-lucide:trending-down" />
              <text>支出</text>
            </view>
            <view
              class="type-btn"
              :class="{ active: formData.type === 'income', income: formData.type === 'income' }"
              @click="formData.type = 'income'"
            >
              <view class="i-lucide:trending-up" />
              <text>收入</text>
            </view>
          </view>
        </view>

        <!-- 账单名称 -->
        <view class="form-card">
          <view class="card-row">
            <view class="row-icon">
              <view class="i-lucide:file-text" />
            </view>
            <view class="row-content">
              <text class="row-label">
                账单名称
              </text>
              <wd-input
                v-model="formData.name"
                placeholder="简单描述这笔账"
                custom-style="--wd-input-font-size: 15px; --wd-input-padding: 0;"
              />
            </view>
          </view>
        </view>

        <!-- 分类选择 -->
        <view class="form-card">
          <view class="card-row">
            <view class="row-icon category">
              <view class="i-lucide:tag" />
            </view>
            <view class="row-content">
              <text class="row-label">
                分类
              </text>
              <wd-select-picker
                v-model="formData.categoryId"
                placeholder="选择分类"
                :columns="filteredCategories.map(c => ({ value: c.id, label: c.name }))"
                custom-style="background: transparent; --wd-select-picker-padding: 0;"
              />
            </view>
          </view>
        </view>

        <!-- 支付方式 -->
        <view class="form-card">
          <view class="card-row">
            <view class="row-icon payment">
              <view class="i-lucide:credit-card" />
            </view>
            <view class="row-content">
              <text class="row-label">
                支付方式
              </text>
              <wd-select-picker
                v-model="formData.paymentMethodId"
                placeholder="选择支付方式（可选）"
                :columns="paymentMethods.map(p => ({ value: p.id, label: p.name }))"
                clearable
                custom-style="background: transparent; --wd-select-picker-padding: 0;"
              />
            </view>
          </view>
        </view>

        <!-- 日期选择 -->
        <view class="form-card">
          <view class="card-row">
            <view class="row-icon date">
              <view class="i-lucide:calendar" />
            </view>
            <view class="row-content">
              <text class="row-label">
                日期
              </text>
              <wd-datetime-picker
                v-model="formData.time"
                title="选择日期"
                :show-toolbar="true"
                mode="datetime"
              >
                <template #default>
                  <view class="date-trigger">
                    {{ formData.time ? formatDateTime(new Date(formData.time)) : '选择日期时间' }}
                    <view class="i-lucide:chevron-right trigger-arrow" />
                  </view>
                </template>
              </wd-datetime-picker>
            </view>
          </view>
        </view>

        <!-- 备注 -->
        <view class="form-card remark-card">
          <view class="card-row remark-row">
            <view class="row-icon remark">
              <view class="i-lucide:message-square" />
            </view>
            <view class="row-content">
              <text class="row-label">
                备注
              </text>
              <wd-textarea
                v-model="formData.remark"
                placeholder="选填：补充说明..."
                :maxlength="200"
                :rows="2"
                custom-style="background: transparent; --wd-textarea-padding: 0; font-size: 15px;"
              />
            </view>
          </view>
        </view>

        <!-- 底部占位 -->
        <view class="bottom-spacer" />
      </scroll-view>

      <!-- 提交按钮 -->
      <view class="form-footer">
        <view
          class="submit-btn"
          :class="formData.type"
          @click="handleSubmit"
        >
          <view class="i-lucide:check-circle submit-icon" />
          <text>确认记账</text>
        </view>
      </view>
    </view>
  </wd-popup>
</template>

<style lang="scss" scoped>
.quick-bill-modal {
  background: linear-gradient(180deg, var(--wot-background) 0%, var(--wot-background-2) 100%);
  border-radius: 24px 24px 0 0;
  max-height: 90vh;
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

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;

  .header-content {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .header-icon {
    font-size: 20px;
    color: var(--wot-color-primary);
  }

  .modal-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--wot-font-color);
    letter-spacing: 0.5px;
  }

  .modal-close {
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

.source-selector {
  display: flex;
  gap: 8px;
  padding: 0 20px 16px;

  .source-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 10px 12px;
    border-radius: 12px;
    background: var(--wot-background-2);
    border: 2px solid transparent;
    transition: all 0.25s ease;
    cursor: pointer;

    .source-icon {
      font-size: 16px;
      color: var(--wot-font-color-secondary);
      transition: color 0.25s;
    }

    text {
      font-size: 13px;
      font-weight: 500;
      color: var(--wot-font-color-secondary);
      transition: color 0.25s;
    }

    &:active {
      transform: scale(0.97);
    }

    &.active {
      background: var(--wot-color-primary);
      border-color: var(--wot-color-primary);

      .source-icon,
      text {
        color: #fff;
      }
    }
  }
}

.form-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
}

/* 金额英雄区 */
.amount-hero {
  background: #fff;
  border-radius: 20px;
  padding: 24px 20px;
  margin-bottom: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--wot-border-color);

  &.expense .amount-display {
      .currency { color: #ff6b6b; }
    }

  &.income .amount-display {
      .currency { color: #51cf66; }
    }

  .amount-display {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-bottom: 20px;
  }

  .currency {
    font-size: 32px;
    font-weight: 700;
    transition: color 0.3s;
  }

  :deep(.amount-input) {
    .wd-input-number__input {
      font-size: 40px !important;
      font-weight: 700 !important;
      text-align: center;
      color: var(--wot-font-color);
    }
  }

  .type-toggle {
    display: flex;
    gap: 12px;
    justify-content: center;

    .type-btn {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 14px 20px;
      border-radius: 14px;
      background: var(--wot-background-2);
      border: 2px solid transparent;
      transition: all 0.3s ease;
      cursor: pointer;

      view {
        font-size: 18px;
        color: var(--wot-font-color-secondary);
        transition: color 0.3s;
      }

      text {
        font-size: 15px;
        font-weight: 600;
        color: var(--wot-font-color-secondary);
        transition: color 0.3s;
      }

      &:active {
        transform: scale(0.97);
      }

      &.expense.active {
        background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
        border-color: transparent;
        box-shadow: 0 4px 16px rgba(255, 107, 107, 0.35);

        view, text { color: #fff; }
      }

      &.income.active {
        background: linear-gradient(135deg, #51cf66 0%, #3cc955 100%);
        border-color: transparent;
        box-shadow: 0 4px 16px rgba(81, 207, 102, 0.35);

        view, text { color: #fff; }
      }
    }
  }
}

/* 表单卡片 */
.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--wot-border-color);

  .card-row {
    display: flex;
    align-items: flex-start;
    gap: 14px;
  }

  .row-icon {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    background: linear-gradient(135deg, var(--wot-color-primary) 0%, #667eea 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    view {
      font-size: 18px;
      color: #fff;
    }

    &.category {
      background: linear-gradient(135deg, #f06595 0%, #cc6699 100%);
    }

    &.payment {
      background: linear-gradient(135deg, #f093fb 0%, #c471ed 100%);
    }

    &.date {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }

    &.remark {
      background: linear-gradient(135deg, #96fbc4 0%, #a8edea 100%);

      view { color: #2d8a6e; }
    }
  }

  .row-content {
    flex: 1;
    min-width: 0;
  }

  .row-label {
    display: block;
    font-size: 12px;
    font-weight: 500;
    color: var(--wot-font-color-secondary);
    margin-bottom: 8px;
  }

  .date-trigger {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;
    font-size: 15px;
    color: var(--wot-font-color);

    .trigger-arrow {
      font-size: 16px;
      color: var(--wot-font-color-placeholder);
    }
  }
}

.remark-card {
  .remark-row {
    align-items: flex-start;
  }
}

.bottom-spacer {
  height: 20px;
}

/* 提交按钮 */
.form-footer {
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid var(--wot-border-color);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.06);

  .submit-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 16px 24px;
    border-radius: 16px;
    font-size: 17px;
    font-weight: 700;
    transition: all 0.3s ease;
    cursor: pointer;

    .submit-icon {
      font-size: 20px;
    }

    &:active {
      transform: scale(0.98);
    }

    &.expense {
      background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
      color: #fff;
      box-shadow: 0 6px 24px rgba(255, 107, 107, 0.4);
    }

    &.income {
      background: linear-gradient(135deg, #51cf66 0%, #3cc955 100%);
      color: #fff;
      box-shadow: 0 6px 24px rgba(81, 207, 102, 0.4);
    }
  }
}

/* 深色模式适配 */
:root.dark {
  .amount-hero,
  .form-card,
  .form-footer {
    background: var(--wot-dark-background2);
    border-color: var(--wot-dark-border-color);
  }

  .form-content {
    background: transparent;
  }

  .date-trigger {
    color: var(--wot-dark-font-color);
  }
}
</style>
