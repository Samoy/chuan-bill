<script lang="ts" setup>
import type { BillListDTO } from '@/api/globals'
import dayjs from 'dayjs'
import isEqual from 'lodash.isequal'

defineOptions({
  name: 'FilterModal',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const props = defineProps<{
  familyId?: string
}>()

const emit = defineEmits<{
  submit: [result: Optional<BillListDTO>, isFiltered?: boolean]
}>()

const show = defineModel<boolean>()
const isFiltered = ref(false)

const billStore = useBillStore()
const familyStore = useFamilyStore()
const user = useUserStore()
const formData = ref<Optional<BillListDTO>>({ type: '' })

type DateRange = 'today' | 'week' | 'month' | 'custom'

interface DateRangeItem {
  label: string
  value: DateRange
}

const dateRangeOptions: DateRangeItem[] = [
  { label: '今天', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '自定义', value: 'custom' },
]

const dateRange = ref<DateRange>()

let startDate = dayjs().startOf('d').valueOf()
let endDate = dayjs().endOf('d').valueOf()
const categoryList = computed(() => billStore.getCategoryListForFilter(formData.value.type))
const paymentMethodList = computed(() => billStore.getPaymentMethodListForFilter())
const familyList = computed(() => familyStore.familyList)
const dateTime = ref<[number, number]>([startDate, endDate])

function resetFormData() {
  dateRange.value = undefined
  nextTick(() => {
    formData.value = { type: '' }
  })
}

onLoad(async () => {
  billStore.initBillData()
  // 已登录时加载家庭列表
  if (user.isLoggedIn && !familyStore.hasFamily) {
    await familyStore.fetchFamilyList()
  }
})

watch(() => dateRange.value, (newVal) => {
  if (newVal === 'today') {
    startDate = dayjs().startOf('d').valueOf()
    endDate = dayjs().endOf('d').valueOf()
  }
  else if (newVal === 'week') {
    startDate = dayjs().startOf('w').valueOf()
    endDate = dayjs().endOf('w').valueOf()
  }
  else if (newVal === 'month') {
    startDate = dayjs().startOf('M').valueOf()
    endDate = dayjs().endOf('M').valueOf()
  }
  dateTime.value = [startDate, endDate]
})

watch(() => dateTime.value, (newVal) => {
  if (newVal.length !== 2) {
    return
  }
  formData.value.startDate = dayjs(newVal[0]).format('YYYY-MM-DD')
  formData.value.endDate = dayjs(newVal[1]).format('YYYY-MM-DD')
}, {
  deep: true,
})

watch(() => formData.value, (newVal) => {
  isFiltered.value = !isEqual(newVal, { type: '' })
}, { deep: true })
</script>

<template>
  <wd-popup v-model="show" :z-index="100" safe-area-inset-bottom position="bottom" closable custom-class="rounded-tl-2xl rounded-tr-2xl pb-3!" lock-scroll>
    <view class="relative max-h-82vh min-h-60vh flex flex-col pb-12">
      <!-- 标题，需要固定 -->
      <view class="box-border h-10 w-full flex items-center justify-center text-center text-lg font-500">
        <text>账单筛选</text>
      </view>
      <!-- 筛选区域 -->
      <view class="box-border flex flex-col gap-4 overflow-y-auto px-4">
        <!-- 账单类型 -->
        <view class="flex flex-col gap-1">
          <view class="flex items-center gap-2 font-500">
            <text class="i-tabler:category text-primary" />
            <text>账单类型</text>
          </view>
          <wd-radio-group v-model="formData.type" shape="button" custom-class="flex mt-1" @change="formData.categoryId = ''">
            <wd-radio value="" custom-class="flex-1 all-type-radio">
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
        <!-- 时间范围 -->
        <view class="flex flex-col gap-1">
          <view class="flex items-center gap-2 font-500">
            <text class="i-lucide:calendar text-primary" />
            <text>时间范围</text>
          </view>
          <wd-radio-group v-model="dateRange" shape="button" custom-class="flex mt-1">
            <wd-radio v-for="item in dateRangeOptions" :key="item.value" :value="item.value" custom-class="flex-1 normal-radio">
              {{ item.label }}
            </wd-radio>
          </wd-radio-group>
          <wd-datetime-picker v-if="dateRange === 'custom'" v-model="dateTime" type="date" custom-class="mt-2" />
        </view>
        <!-- 账单类目 -->
        <view class="flex flex-col gap-1">
          <view class="flex items-center gap-2 font-500">
            <text class="i-lucide:tag text-primary" />
            <text>账单类目</text>
          </view>
          <wd-radio-group v-model="formData.categoryId" shape="button" custom-class="grid grid-cols-3 gap-2 mt-1">
            <wd-radio v-for="item in categoryList" :key="item.id" :value="item.id!" custom-class="normal-radio">
              <text :class="transformUnoCSS(item.icon || '')" class="mr-1" /> {{ item.name }}
            </wd-radio>
          </wd-radio-group>
        </view>
        <!-- 支付方式 -->
        <view class="flex flex-col gap-1">
          <view class="flex items-center gap-2 font-500">
            <text class="i-lucide:credit-card text-primary" />
            <text>支付方式</text>
          </view>
          <wd-radio-group v-model="formData.paymentMethodId" shape="button" custom-class="grid grid-cols-3 gap-2 mt-1">
            <wd-radio v-for="item in paymentMethodList" :key="item.id" :value="item.id!" custom-class="normal-radio">
              <text :class="transformUnoCSS(item.icon || '')" class="mr-1" /> {{ item.name }}
            </wd-radio>
          </wd-radio-group>
        </view>
        <!-- 家庭筛选（仅已登录且有家庭时显示） -->
        <view v-if="user.isLoggedIn && familyList.length > 0 && !props.familyId" class="flex flex-col gap-1">
          <view class="flex items-center gap-2 font-500">
            <text class="i-lucide:home text-primary" />
            <text>所属家庭</text>
          </view>
          <wd-radio-group v-model="formData.familyId" shape="button" custom-class="flex flex-wrap gap-2 mt-1">
            <wd-radio v-for="item in familyList" :key="item.id" :value="item.id!" custom-class="normal-radio">
              {{ item.name }}
            </wd-radio>
          </wd-radio-group>
        </view>
      </view>
      <!-- 底部按钮 -->
      <view class="absolute bottom-0 left-4 right-4 box-border h-8 flex items-center justify-center gap-3">
        <wd-button plain type="primary" custom-class="flex-1" @click="resetFormData">
          重置
        </wd-button>
        <wd-button type="primary" custom-class="flex-1" @click="emit('submit', { ...formData, familyId: props.familyId }, isFiltered)">
          确定
        </wd-button>
      </view>
    </view>
  </wd-popup>
</template>

<style lang="scss" scoped>
:deep(.wd-radio.is-button .wd-radio__label){
  max-width:none !important;
  width: 100%;
  border: none !important;
  @apply h-8 items-center flex justify-center py-0;
}
.all-type-radio{
  &.is-checked{
    :deep(.wd-radio__label){
      @apply border-primary! bg-primary! text-white! shadow-primary/20 shadow-lg;
    }
  }
}
.expense-radio{
  &.is-checked{
    :deep(.wd-radio__label){
      @apply border-red-400! bg-red-400! text-white! shadow-red-100/20 shadow-lg;
    }
  }
}
.income-radio{
  &.is-checked{
    :deep(.wd-radio__label){
      @apply border-green-500! bg-green-500! text-white! shadow-green-100/20 shadow-lg;
    }
  }
}
.normal-radio{
  &.is-checked{
    :deep(.wd-radio__label){
      @apply border-primary! bg-primary! text-white! shadow-primary/20 shadow-lg;
    }
  }
}
:deep(.wd-cell.wd-datetime-picker__cell){
  @apply bg-gray-50/80 rounded-xl dark:bg-black/30;
}
</style>
