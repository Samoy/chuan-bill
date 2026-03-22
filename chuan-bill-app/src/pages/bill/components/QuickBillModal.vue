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
    custom-class="!rounded-3xl !rounded-b-none"
    :z-index="100"
  >
    <view class="quick-bill-modal">
      <!-- 拖动指示条 -->
      <view class="drag-indicator" />

      <!-- 标题栏 -->
      <view class="flex items-center justify-between px-5 py-4">
        <view class="flex items-center gap-2.5">
          <view class="i-lucide:pen-line text-primary text-lg" />
          <text class="text-base text-[var(--wot-font-color)] font-semibold">
            记一笔
          </text>
        </view>
        <view
          class="i-lucide:x rounded-lg bg-[var(--wot-background-2)] p-2 text-lg text-[var(--wot-font-color-secondary)] transition-transform active:scale-90"
          @click="showPopup = false"
        />
      </view>

      <!-- 记账方式切换 -->
      <view class="flex gap-2 px-5 pb-4">
        <view
          v-for="option in sourceOptions"
          :key="option.value"
          class="flex flex-1 items-center justify-center gap-1.5 border-2 border-transparent rounded-xl bg-[var(--wot-background-2)] px-3 py-2.5 transition-all"
          :class="{ '!bg-[var(--wot-color-primary)] border-[var(--wot-color-primary)]': source === option.value }"
          @click="source = option.value"
        >
          <view
            class="text-sm"
            :class="[option.payload.icon, source === option.value ? 'text-white' : 'text-[var(--wot-font-color-secondary)]']"
          />
          <text
            class="text-xs font-medium"
            :class="source === option.value ? 'text-white' : 'text-[var(--wot-font-color-secondary)]'"
          >
            {{ option.payload.text }}
          </text>
        </view>
      </view>

      <!-- 表单内容 -->
      <scroll-view scroll-y class="form-content">
        <!-- 金额输入 - 视觉焦点 -->
        <view class="amount-hero" :class="formData.type">
          <view class="mb-5 flex items-center justify-center gap-2">
            <text
              class="text-3xl font-bold transition-colors duration-300"
              :class="formData.type === 'expense' ? 'text-[#ff6b6b]' : 'text-[#51cf66]'"
            >
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
          <view class="flex justify-center gap-3">
            <view
              class="flex flex-1 items-center justify-center gap-2 border-2 rounded-xl px-5 py-3.5 transition-all duration-300"
              :class="formData.type === 'expense'
                ? 'bg-[#ff6b6b] border-transparent shadow-lg shadow-[#ff6b6b]/30'
                : 'bg-[var(--wot-background-2)] border-transparent'"
              @click="formData.type = 'expense'"
            >
              <view
                class="text-lg"
                :class="formData.type === 'expense' ? 'i-lucide:trending-down text-white' : 'i-lucide:trending-down text-[var(--wot-font-color-secondary)]'"
              />
              <text
                class="text-sm font-semibold"
                :class="formData.type === 'expense' ? 'text-white' : 'text-[var(--wot-font-color-secondary)]'"
              >
                支出
              </text>
            </view>
            <view
              class="flex flex-1 items-center justify-center gap-2 border-2 rounded-xl px-5 py-3.5 transition-all duration-300"
              :class="formData.type === 'income'
                ? 'bg-[#51cf66] border-transparent shadow-lg shadow-[#51cf66]/30'
                : 'bg-[var(--wot-background-2)] border-transparent'"
              @click="formData.type = 'income'"
            >
              <view
                class="text-lg"
                :class="formData.type === 'income' ? 'i-lucide:trending-up text-white' : 'i-lucide:trending-up text-[var(--wot-font-color-secondary)]'"
              />
              <text
                class="text-sm font-semibold"
                :class="formData.type === 'income' ? 'text-white' : 'text-[var(--wot-font-color-secondary)]'"
              >
                收入
              </text>
            </view>
          </view>
        </view>

        <!-- 账单名称 -->
        <view class="form-card">
          <view class="flex items-start gap-3.5">
            <view class="row-icon">
              <view class="i-lucide:file-text" />
            </view>
            <view class="min-w-0 flex-1">
              <text class="mb-2 block text-xs text-[var(--wot-font-color-secondary)] font-medium">
                账单名称
              </text>
              <wd-input
                v-model="formData.name"
                placeholder="简单描述这笔账"
                custom-class="!bg-transparent !p-0 text-sm"
              />
            </view>
          </view>
        </view>

        <!-- 分类选择 -->
        <view class="form-card">
          <view class="flex items-start gap-3.5">
            <view class="row-icon category">
              <view class="i-lucide:tag" />
            </view>
            <view class="min-w-0 flex-1">
              <text class="mb-2 block text-xs text-[var(--wot-font-color-secondary)] font-medium">
                分类
              </text>
              <wd-select-picker
                v-model="formData.categoryId"
                placeholder="选择分类"
                :columns="filteredCategories.map(c => ({ value: c.id, label: c.name }))"
                custom-class="!bg-transparent"
              />
            </view>
          </view>
        </view>

        <!-- 支付方式 -->
        <view class="form-card">
          <view class="flex items-start gap-3.5">
            <view class="row-icon payment">
              <view class="i-lucide:credit-card" />
            </view>
            <view class="min-w-0 flex-1">
              <text class="mb-2 block text-xs text-[var(--wot-font-color-secondary)] font-medium">
                支付方式
              </text>
              <wd-select-picker
                v-model="formData.paymentMethodId"
                placeholder="选择支付方式（可选）"
                :columns="paymentMethods.map(p => ({ value: p.id, label: p.name }))"
                clearable
                custom-class="!bg-transparent"
              />
            </view>
          </view>
        </view>

        <!-- 日期选择 -->
        <view class="form-card">
          <view class="flex items-start gap-3.5">
            <view class="row-icon date">
              <view class="i-lucide:calendar" />
            </view>
            <view class="min-w-0 flex-1">
              <text class="mb-2 block text-xs text-[var(--wot-font-color-secondary)] font-medium">
                日期
              </text>
              <wd-datetime-picker
                v-model="formData.time"
                title="选择日期"
                :show-toolbar="true"
                mode="datetime"
              >
                <template #default>
                  <view class="flex items-center justify-between py-2">
                    <text class="text-sm text-[var(--wot-font-color)]">
                      {{ formData.time ? formatDateTime(new Date(formData.time)) : '选择日期时间' }}
                    </text>
                    <view class="i-lucide:chevron-right text-[var(--wot-font-color-placeholder)]" />
                  </view>
                </template>
              </wd-datetime-picker>
            </view>
          </view>
        </view>

        <!-- 备注 -->
        <view class="form-card">
          <view class="flex items-start gap-3.5">
            <view class="row-icon remark">
              <view class="i-lucide:message-square" />
            </view>
            <view class="min-w-0 flex-1">
              <text class="mb-2 block text-xs text-[var(--wot-font-color-secondary)] font-medium">
                备注
              </text>
              <wd-textarea
                v-model="formData.remark"
                placeholder="选填：补充说明..."
                :maxlength="200"
                :rows="2"
                custom-class="!bg-transparent !p-0 text-sm"
              />
            </view>
          </view>
        </view>

        <!-- 底部占位 -->
        <view class="h-5" />
      </scroll-view>

      <!-- 提交按钮 -->
      <view
        class="mx-4 mb-3 flex items-center justify-center gap-2.5 rounded-2xl py-4 text-base text-white font-bold transition-all active:scale-98"
        :class="formData.type === 'expense'
          ? 'bg-gradient-to-r from-[#ff6b6b] to-[#ee5a5a] shadow-xl shadow-[#ff6b6b]/40'
          : 'bg-gradient-to-r from-[#51cf66] to-[#3cc955] shadow-xl shadow-[#51cf66]/40'"
        @click="handleSubmit"
      >
        <view class="i-lucide:check-circle" />
        <text>确认记账</text>
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

// 表单内容区
.form-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
}

// 金额英雄区
.amount-hero {
  background: #fff;
  border-radius: 20px;
  padding: 24px 20px;
  margin-bottom: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--wot-border-color);

  // 金额输入样式（deep selector）
  :deep(.amount-input) {
    .wd-input-number__input {
      font-size: 40px !important;
      font-weight: 700 !important;
      text-align: center;
      color: var(--wot-font-color);
    }
  }
}

// 行图标
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

// 表单卡片
.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--wot-border-color);
}

// 深色模式适配
:root.dark {
  .amount-hero,
  .form-card {
    background: var(--wot-dark-background2);
    border-color: var(--wot-dark-border-color);
  }

  .form-content {
    background: transparent;
  }
}
</style>
