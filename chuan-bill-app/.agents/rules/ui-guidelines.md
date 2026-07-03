---
description: UI/UX 指南、样式规范和组件使用
paths:
  - **/*.vue
  - **/*.scss
  - src/components/**/*
  - src/uni_modules/**/*
  - uno.config.ts
---

# UI 与样式指南

## 🎨 样式系统
- **引擎**: UnoCSS (原子化 CSS) 是**首选且强制**的样式方案。
- **配置**: `uno.config.ts`。
- **预处理**: SCSS 仅在 UnoCSS 无法实现时使用（复杂选择器、CSS 变量、动画等）。
- **主题**: 通过 `src/theme.json` 和 CSS 变量支持亮色/暗色模式切换。

### UnoCSS 优先原则（强制）

**模板中优先使用 UnoCSS 类**，仅在以下情况才使用 `<style>` 块：

| 可用 UnoCSS | 必须用 `<style>` |
|------------|-----------------|
| 布局：`flex`, `grid`, `gap`, `p-`, `m-` | SCSS 嵌套选择器（如 `&:active`） |
| 颜色：`text-`, `bg-`, `border-`, `shadow-` | Deep selectors（`:deep()`） |
| 字体：`text-sm`, `font-bold`, `italic` | CSS 变量定义 |
| 圆角：`rounded-xl`, `rounded-full` | 复杂动画 keyframes |
| 响应式：`sm:`, `md:`, `dark:` | 复杂阴影/渐变（可用 CSS 变量简化） |
| 过渡：`transition-all`, `duration-300` | 伪元素 `::before`, `::after` |

**示例**：
```vue
<!-- ✅ 正确：优先使用 UnoCSS -->
<view class="flex items-center gap-3 p-4 bg-white rounded-xl shadow-sm">
  <text class="text-sm font-medium text-gray-600">内容</text>
</view>

<!-- ❌ 错误：过度使用 style -->
<view class="my-view">
  <text class="my-text">内容</text>
</view>
<style scoped>
.my-view { display: flex; align-items: center; gap: 12px; padding: 16px; background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.my-text { font-size: 14px; font-weight: 500; color: #666; }
</style>
```

### UnoCSS 约定
- 使用工具类: `flex`, `items-center`, `text-primary`, `m-4`.
- 响应式前缀: `sm:`, `md:` (在移动端优先的 uni-app 中较少使用)。
- 图标: 通过 UnoCSS preset 使用 `i-lucide:{icon-name}`。

## 🧩 组件库
- **核心库**: `wot-design-uni` (`wd-` 前缀)。
- **文档**: [wot-design-uni](https://wot-ui.cn).
- **自定义组件**: 在 `src/components` 中创建。

## 📢 全局反馈
- **Toast/Message**: 请勿直接使用 `uni.showToast`。
- **标准**: 使用 `GlobalToast`, `GlobalMessage`, `GlobalLoading` 组件。
- **Skill**: 参考 **`global-feedback`** skill 查看使用示例。

## 📱 布局
- **系统**: `vite-plugin-uni-layouts`。
- **默认**: `src/layouts/default.vue`。
- **TabBar**: `src/layouts/tabbar.vue`。
