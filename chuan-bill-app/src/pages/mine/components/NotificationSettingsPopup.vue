<script setup lang="ts">
defineOptions({
  name: 'NotificationSettingsPopup',
})

const modelValue = defineModel<boolean>({ default: false })
const toast = useGlobalToast()

// 通知设置
const settings = ref({
  masterEnabled: false,
  billReminderEnabled: false,
  billReminderTime: '20:00',
  familyNotificationEnabled: false,
  systemNotificationEnabled: true,
})

const timePickerVisible = ref(false)
const timeValue = ref('20:00')

// 加载设置
onMounted(async () => {
  try {
    const res = await Apis.preference.getAll()
    if (res.success && res.data) {
      const prefs = res.data
      if (prefs['notification.master.enabled'] !== undefined) {
        settings.value.masterEnabled = prefs['notification.master.enabled'] === 'true'
      }
      if (prefs['notification.billReminder.enabled'] !== undefined) {
        settings.value.billReminderEnabled = prefs['notification.billReminder.enabled'] === 'true'
      }
      if (prefs['notification.billReminder.time'] !== undefined) {
        settings.value.billReminderTime = prefs['notification.billReminder.time']
        timeValue.value = prefs['notification.billReminder.time']
      }
      if (prefs['notification.family.enabled'] !== undefined) {
        settings.value.familyNotificationEnabled = prefs['notification.family.enabled'] === 'true'
      }
      if (prefs['notification.system.enabled'] !== undefined) {
        settings.value.systemNotificationEnabled = prefs['notification.system.enabled'] === 'true'
      }
    }
  }
  catch {
    // 静默失败，使用默认值
  }
})

// 开关变化时保存
async function onSettingChange(key:string, value: boolean | string) {
  try {
   await Apis.preference.set({
      data: {
        key,
        value: String(value),
      },
    })
    toast.success('设置已保存')
  }catch {
    toast.error('保存失败')
  }
}

// 时间选择确认
function onTimeConfirm({ value }: { value: number[] }) {
  settings.value.billReminderTime = `${String(value[0]).padStart(2, '0')}:${String(value[1]).padStart(2, '0')}`
  onSettingChange('notification.billReminder.time', settings.value.billReminderTime)
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
    title="通知设置"
  >
    <view class="p-4">
      <!-- 通知总开关 -->
      <view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view>
          <text class="block text-sm font-medium">
            通知总开关
          </text>
          <text class="mt-1 block text-xs text-gray-500">
            关闭后所有通知和提醒将被禁用
          </text>
        </view>
        <wd-switch v-model="settings.masterEnabled" size="20px" @change="onSettingChange('notification.master.enabled', settings.masterEnabled)" />
      </view>

      <!-- 系统通知 -->
      <view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view>
          <text class="block text-sm font-medium">
            系统通知
          </text>
          <text class="mt-1 block text-xs text-gray-500">
            系统公告、活动提醒等
          </text>
        </view>
        <wd-switch
          v-model="settings.systemNotificationEnabled"
          :disabled="!settings.masterEnabled"
          size="20px"
          @change="onSettingChange('notification.system.enabled', settings.systemNotificationEnabled)"
        />
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
          <!-- 时间选择器 -->
          <wd-datetime-picker
            v-model="timeValue"
            v-model:visible="timePickerVisible"
            type="time"
            title="选择提醒时间"
            :disabled="!settings.masterEnabled"
            @confirm="onTimeConfirm"
          >
            <text
              v-if="settings.billReminderEnabled && settings.masterEnabled"
              class="text-xs text-primary"
              @click="timePickerVisible = true"
            >
              {{ settings.billReminderTime }}
            </text>
          </wd-datetime-picker>
          <wd-switch
            v-model="settings.billReminderEnabled"
            :disabled="!settings.masterEnabled"
            size="20px"
            @change="onSettingChange('notification.billReminder.enabled', settings.billReminderEnabled)"
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
          :disabled="!settings.masterEnabled"
          size="20px"
          @change="onSettingChange('notification.family.enabled', settings.familyNotificationEnabled)"
        />
      </view>
    </view>
  </wd-action-sheet>
</template>
