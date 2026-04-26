<script setup lang="ts">
const { themeVars, theme } = useManualTheme()
const colorPrimary = computed(() => {
  return hexToRgbString(themeVars.value.colorTheme || themeColorOptions[0].primary)
})
</script>

<template>
  <wd-config-provider :theme-vars="themeVars" :theme="theme" :custom-class="`page-wraper ${theme}`" :custom-style="`--color-primary:${colorPrimary};`">
    <ku-root-view />
    <wd-notify />
    <wd-message-box />
    <wd-toast />
    <global-loading />
    <global-toast />
    <global-message />
  </wd-config-provider>
</template>
