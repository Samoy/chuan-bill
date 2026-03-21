<script setup lang="ts">
onLaunch(() => {})
</script>

<style lang="scss">
.page-wraper {
  min-height: calc(100vh - var(--window-top));
  box-sizing: border-box;
  background: #faf8fc;
}

.wot-theme-dark.page-wraper {
  background: #222;
}

:root,
page {
  --wot-segmented-padding: 6px;
}
</style>
