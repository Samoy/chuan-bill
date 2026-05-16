# 小川记账 - 前端

<p>
  <img src="https://img.shields.io/badge/uni--app-3.0-brightgreen" alt="uni-app">
  <img src="https://img.shields.io/badge/Vue-3-brightgreen" alt="Vue">
  <img src="https://img.shields.io/badge/TypeScript-5.5-blue" alt="TypeScript">
  <img src="https://img.shields.io/badge/wot--design--uni-1.14-orange" alt="wot-design-uni">
</p>

## 简介

基于 uni-app + Vue 3 + TypeScript 的跨平台记账应用前端

## 技术栈

- 框架：uni-app 3.0 + Vue 3.4
- 语言：TypeScript 5.5
- UI 组件：wot-design-uni 1.14
- 样式：UnoCSS
- 状态管理：Pinia
- 请求库：Alova.js
- 图表：ECharts

## 特性

- 跨平台：H5、微信小程序、APP
- 文件路由：基于 vite-plugin-uni-pages
- 组件自动导入：unplugin-auto-import
- 布局系统：vite-plugin-uni-layouts
- 主题定制：CSS 变量 + 暗黑模式

## 快速开始

### 环境要求

- Node.js >= 20.19.0
- pnpm >= 9.9.0

### 安装

```bash
# 安装依赖
pnpm install
```

### 开发

```bash
# H5 开发
pnpm dev

# 微信小程序开发
pnpm dev:mp-weixin

# APP 开发
pnpm dev:app
```

### 构建

```bash
# H5 构建
pnpm build:h5

# 微信小程序构建
pnpm build:mp-weixin

# APP 构建
pnpm build:app
```

## 项目结构

```
src/
├── pages/           # 主包页面
├── subPages/        # 分包页面
├── components/      # 公共组件
├── store/           # Pinia 状态
├── api/             # API 接口
├── layouts/         # 布局组件
├── utils/           # 工具函数
└── static/          # 静态资源
```

## 开发规范

- 组件：Vue 3 `<script setup>` 语法
- 样式：UnoCSS 优先，SCSS 仅用于复杂场景
- 状态：Pinia Store，use{Name}Store 命名
- 请求：Alova.js，自动导入
- 反馈：GlobalToast/GlobalMessage/GlobalLoading

## 常用命令

```bash
# 代码检查
pnpm lint

# 代码修复
pnpm lint:fix

# 类型检查
pnpm type-check

# 重新生成 API
pnpm alova-gen
```

## 相关文档

- [wot-design-uni 文档](https://wot-ui.cn)
- [uni-app 文档](https://uniapp.dcloud.net.cn)
- [UnoCSS 文档](https://unocss.dev)
