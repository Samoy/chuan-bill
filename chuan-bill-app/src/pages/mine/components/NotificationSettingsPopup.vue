<script setup lang="ts">
defineOptions({
  name: 'NotificationSettingsPopup',
})

const modelValue = defineModel<boolean>()
const toast = useGlobalToast()

// 通知设置
const settings = ref({
  pushEnabled: true,
  billReminderEnabled: true,
  billReminderTime: '21:00',
  familyNotificationEnabled: true,
})

const timePickerVisible = ref(false)
const timeValue = ref([21, 0])

// 加载设置
onMounted(() => {
  // TODO: 从后端加载通知设置
})

// 消息推送开关变化
function onPushChange(value: boolean) {
  if (!value) {
    settings.value.billReminderEnabled = false
    settings.value.familyNotificationEnabled = false
  }
  saveSettings()
}

// 保存设置
async function saveSettings() {
  // TODO: 调用后端保存通知设置
  toast.success('设置已保存')
}

// 时间选择确认
function onTimeConfirm({ value }: { value: number[] }) {
  settings.value.billReminderTime = `${String(value[0]).padStart(2, '0')}:${String(value[1]).padStart(2, '0')}`
  saveSettings()
}
</script>

<template>
  <wd-popup
    v-model="modelValue"
    position="bottom"
    closable
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
  >
    <view class="p-4">
      <view class="mb-4 text-center text-lg font-500">
        通知设置
      </view>

      <!-- 消息推送 -->
      <view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view>
          <text class="block text-sm font-medium">
            消息推送
          </text>
          <text class="mt-1 block text-xs text-gray-500">
            接收应用推送通知
          </text>
        </view>
        <wd-switch v-model="settings.pushEnabled" size="20px" @change="onPushChange" />
      </view>

      <!-- 账单提醒 -->
      <view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view>
          <text class="block text-sm font-medium">
            账单提醒
          </text>
          <text class="mt-1 block text-xs text-gray-500">
            每日提醒记账
          </text>
        </view>
        <view class="flex items-center gap-2">
          <text
            v-if="settings.billReminderEnabled"
            class="text-xs text-primary"
            @click="timePickerVisible = true"
          >
            {{ settings.billReminderTime }}
          </text>
          <wd-switch
            v-model="settings.billReminderEnabled"
            :disabled="!settings.pushEnabled"
            size="20px"
            @change="saveSettings"
          />
        </view>
      </view>

      <!-- 家庭通知 -->
      <view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view>
          <text class="block text-sm font-medium">
            家庭通知
          </text>
          <text class="mt-1 block text-xs text-gray-500">
            成员变动、账单变更等
          </text>
        </view>
        <wd-switch
          v-model="settings.familyNotificationEnabled"
          :disabled="!settings.pushEnabled"
          size="20px"
          @change="saveSettings"
        />
      </view>
    </view>

    <!-- 时间选择器 -->
    <wd-datetime-picker
      v-model="timeValue"
      v-model:visible="timePickerVisible"
      type="time"
      title="选择提醒时间"
      @confirm="onTimeConfirm"
    />
  </wd-popup>
</template>
