<script setup lang="ts">
import { themeColorOptions } from '@/composables/types/theme'

defineOptions({
  name: 'ThemePickerPopup',
})

const modelValue = defineModel<boolean>({ default: false })
const themeStore = useManualTheme()

function selectTheme(color: typeof themeColorOptions[0]) {
  themeStore.selectThemeColor(color)
  modelValue.value = false
}
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    position="bottom"
    closable
    :z-index="999"
    safe-area-inset-bottom
    title="主题颜色"
    custom-class="rounded-tl-2xl rounded-tr-2xl"
  >
    <view class="p-4">
      <view class="grid grid-cols-3 gap-4">
        <view
          v-for="color in themeColorOptions"
          :key="color.value"
          class="flex flex-col items-center gap-2 rounded-xl p-3 transition-all"
          :class="themeStore.currentThemeColor.value.value === color.value ? 'bg-primary/10 ring-2 ring-primary' : 'bg-gray-50 dark:bg-gray-800'"
          @click="selectTheme(color)"
        >
          <view
            class="h-10 w-10 rounded-full"
            :style="{ backgroundColor: color.primary }"
          />
          <text class="text-xs text-gray-600 dark:text-gray-400">
            {{ color.name }}
          </text>
        </view>
      </view>
    </view>
  </wd-action-sheet>
</template>
