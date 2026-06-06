<script setup lang="ts">
defineOptions({
  name: 'BudgetSettingPopup',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const modelValue = defineModel<boolean>({ default: false })
const budgetStore = useBudgetStore()
const toast = useGlobalToast()
const message = useGlobalMessage()

const amount = ref('')
const saving = ref(false)
const deleting = ref(false)

const hasBudget = computed(() => budgetStore.currentBudget !== null)

// 打开时加载当前预算金额
watch(modelValue, (visible) => {
  if (visible && budgetStore.currentBudget) {
    amount.value = String(budgetStore.currentBudget.amount)
  }
  else if (visible) {
    amount.value = ''
  }
})

async function onSave() {
  const numAmount = Number(amount.value)
  if (!numAmount || numAmount <= 0) {
    toast.warning('请输入有效的预算金额')
    return
  }
  saving.value = true
  try {
    const res = await budgetStore.setBudget(amount.value)
    if (res.success) {
      toast.success(hasBudget.value ? '预算已更新' : '预算已设置')
      modelValue.value = false
    }
  }
  catch {
    toast.error('操作失败')
  }
  finally {
    saving.value = false
  }
}

function onDelete() {
  message.confirm({
    title: '提示',
    msg: '确定删除当月预算吗？',
    confirmButtonProps: { type: 'error' },
    success: async (res) => {
      if (res.action === 'confirm') {
        deleting.value = true
        try {
          const result = await budgetStore.deleteBudget()
          if (result.success) {
            toast.success('预算已删除')
            modelValue.value = false
          }
        }
        catch {
          toast.error('删除失败')
        }
        finally {
          deleting.value = false
        }
      }
    },
  })
}
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    position="bottom"
    closable
    :z-index="999"
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    title="设置预算"
  >
    <view class="p-4 pt-0">
      <!-- 金额输入 -->
      <view class="mb-4 flex flex-col items-center gap-2 rounded-xl bg-gray-50 p-4 dark:bg-[var(--wot-dark-background4)]">
        <view class="flex items-center gap-2 text-[32px]">
          <text class="text-2xl text-primary font-bold">
            ¥
          </text>
          <wd-input
            v-model="amount"
            type="number"
            placeholder="1000"
            custom-class="flex-1 rounded-xl px-1! py-0.5!"
            custom-input-class="text-[32px]! font-bold! h-10!"
            no-border
          />
        </view>
        <text class="block text-xs text-gray-500">
          合理的预算能帮助您更好地控制开支，建议设置为月收入的60%。
        </text>
      </view>

      <!-- 操作按钮 -->
      <view class="flex flex-col gap-3">
        <wd-button type="primary" block :loading="saving" @click="onSave">
          {{ hasBudget ? '修改预算' : '设置预算' }}
        </wd-button>
        <wd-button
          v-if="hasBudget"
          type="error"
          plain
          block
          :loading="deleting"
          @click="onDelete"
        >
          删除预算
        </wd-button>
      </view>
    </view>
  </wd-action-sheet>
</template>
