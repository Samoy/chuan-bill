<script setup lang="ts">
import type { BillListDTO } from '@/api/globals'
import dayjs from 'dayjs'

defineOptions({
  name: 'ExportFilterPopup',
})

const modelValue = defineModel<boolean>({ default: false })
const toast = useGlobalToast()
const loading = ref(false)

// 筛选条件
const filterData = ref<Optional<BillListDTO>>({ type: '' })
const dateRange = ref<[number, number]>([dayjs().startOf('M').valueOf(), dayjs().endOf('M').valueOf()])

// 导出格式
const formatOptions = [
  { label: 'Excel (.xlsx)', value: 'excel' },
  { label: 'PDF (.pdf)', value: 'pdf' },
]
const selectedFormat = ref('excel')

// 更新日期范围
watch(dateRange, (newVal) => {
  if (newVal.length === 2) {
    filterData.value.startDate = dayjs(newVal[0]).format('YYYY-MM-DD')
    filterData.value.endDate = dayjs(newVal[1]).format('YYYY-MM-DD')
  }
}, { deep: true, immediate: true })

// 导出
async function handleExport() {
  if (loading.value) {
    return
  }

  loading.value = true
  try {
    const params = { ...filterData.value, format: selectedFormat.value }
    const res = await Apis.bill.exportBill({ data: params })

    // res is ArrayBuffer (binary stream)
    const format = selectedFormat.value
    const ext = format === 'excel' ? '.xlsx' : '.pdf'
    const fileName = `bills_${dayjs().format('YYYY-MM-DD')}${ext}`

    // #ifdef H5
    {
      const blob = new Blob([res], {
        type: format === 'excel'
          ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
          : 'application/pdf',
      })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      a.click()
      URL.revokeObjectURL(url)
    }
    // #endif

    // #ifdef MP-WEIXIN
    {
      const filePath = `${wx.env.USER_DATA_PATH}/${fileName}`
      const fs = uni.getFileSystemManager()
      fs.writeFileSync(filePath, res, 'binary')
      await uni.openDocument({ filePath, showMenu: true })
    }
    // #endif

    // #ifdef APP-PLUS
    {
      const filePath = `${plus.io.PRIVATE_DOC}/${fileName}`
      const fs = plus.io.getFileSystemManager()
      fs.writeFileSync(filePath, res, 'binary')
      await uni.openDocument({ filePath, showMenu: true })
    }
    // #endif

    toast.success('导出成功')
    modelValue.value = false
  }
  catch (e: any) {
    // If backend returned JSON error (not binary), parse it
    if (e?.message) {
      toast.error(e.message)
    }
    else {
      toast.error('导出失败，请重试')
    }
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    position="bottom"
    closable
    safe-area-inset-bottom
    :z-index="999"
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    title="数据导出"
  >
    <view class="max-h-80vh p-4">
      <!-- 时间范围 -->
      <view class="mb-4 flex flex-col gap-1">
        <view class="flex items-center gap-2 font-500">
          <text class="i-lucide:calendar text-primary" />
          <text>时间范围</text>
        </view>
        <wd-datetime-picker
          v-model="dateRange"
          type="date"
          custom-class="mt-1"
        />
      </view>

      <!-- 账单类型 -->
      <view class="mb-4 flex flex-col gap-1">
        <view class="flex items-center gap-2 font-500">
          <text class="i-tabler:category text-primary" />
          <text>账单类型</text>
        </view>
        <wd-radio-group v-model="filterData.type" shape="button" custom-class="flex mt-1">
          <wd-radio value="" custom-class="flex-1 normal-radio">
            全部
          </wd-radio>
          <wd-radio value="expense" custom-class="flex-1 expense-radio">
            支出
          </wd-radio>
          <wd-radio value="income" custom-class="flex-1 income-radio">
            收入
          </wd-radio>
        </wd-radio-group>
      </view>

      <!-- 导出格式 -->
      <view class="mb-4 flex flex-col gap-1">
        <view class="flex items-center gap-2 font-500">
          <text class="i-lucide:file-down text-primary" />
          <text>导出格式</text>
        </view>
        <wd-radio-group v-model="selectedFormat" shape="button" custom-class="flex mt-1">
          <wd-radio
            v-for="item in formatOptions"
            :key="item.value"
            :value="item.value"
            custom-class="flex-1 normal-radio"
          >
            {{ item.label }}
          </wd-radio>
        </wd-radio-group>
      </view>

      <!-- 导出按钮 -->
      <wd-button
        type="primary"
        block
        :loading="loading"
        @click="handleExport"
      >
        导出
      </wd-button>
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
.expense-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
      @apply border-red-400! bg-red-400! text-white! shadow-red-400/20 shadow-lg;
    }
  }
}
.income-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
      @apply border-green-500! bg-green-500! text-white! shadow-green-500/20 shadow-lg;
    }
  }
}
.normal-radio {
  &.is-checked {
    :deep(.wd-radio__label) {
      @apply border-primary! bg-primary! text-white! shadow-primary/20 shadow-lg;
    }
  }
}
:deep(.wd-cell.wd-datetime-picker__cell) {
  @apply bg-gray-50/80 rounded-xl dark:bg-black/30;
}
</style>
