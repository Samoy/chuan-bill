# 统计页面优化设计

**日期**: 2026-06-18
**Issues**: #40, #38
**状态**: 设计完成

## 背景

### Issue #40 (P0)
进入家庭统计页面后返回首页，再进入 Tab 中的统计页面，会显示家庭统计的数据。原因：个人统计和家庭统计共用 `useStatisticsStore()`，退出家庭统计页面后未重置状态。

### Issue #38 (P2)
分类饼图和成员排行榜中，新分类的颜色会与已有分类颜色重复。原因：使用固定的 `CHART_COLORS` 数组按索引分配颜色。

## 设计方案

### 1. Store 拆分 (Issue #40)

**目标**: 将 `useStatisticsStore()` 拆分为两个独立的 store

**文件结构**:
```
src/store/
├── personalStatisticsStore.ts  # 个人统计
├── familyStatisticsStore.ts    # 家庭统计
```

**接口设计**:

`usePersonalStatisticsStore()`:
- `fetchOverview(month: string)`
- `fetchCategoryBreakdown(month: string, type: 'expense' | 'income')`
- `fetchDailyTrend(month: string)`
- `fetchAiSuggestion(month: string, regenerate?: boolean)`
- `fetchAiSuggestionCached(month: string)`
- `fetchAll(month: string)`
- `reset()`

`useFamilyStatisticsStore()`:
- `fetchOverview(month: string, familyId: string)`
- `fetchCategoryBreakdown(month: string, type: 'expense' | 'income', familyId: string)`
- `fetchDailyTrend(month: string, familyId: string)`
- `fetchAiSuggestion(month: string, familyId: string, regenerate?: boolean)`
- `fetchAiSuggestionCached(month: string, familyId: string)`
- `fetchAll(month: string, familyId: string)`
- `reset()`

### 2. echarts 主题配置 (Issue #38)

**目标**: 注册全局莫兰迪色系主题，删除组件中的 `CHART_COLORS`

**实现位置**: `src/utils/echarts-setup.ts`

**莫兰迪色系 palette**:
```typescript
const morandiColors = [
  '#8D9BA3', // 灰蓝
  '#B5C4B1', // 灰绿
  '#C9B1A0', // 灰粉
  '#A3B5C4', // 雾蓝
  '#C4B5A3', // 米灰
  '#B1B5C4', // 薰衣草
  '#C4A3B5', // 玫瑰灰
  '#B5C4C4', // 青灰
  '#C4C4A3', // 暖灰
]
```

**主题注册**:
```typescript
echarts.registerTheme('morandi', {
  color: morandiColors,
  backgroundColor: 'transparent',
})
```

**组件改动**:
- `CategoryChart.vue`: 删除 `CHART_COLORS`，使用 echarts 主题色
- `MemberRankingChart.vue`: 删除 `CHART_COLORS`，进度条使用莫兰迪色

## 数据流

### 个人统计页面
```
pages/statistics/index.vue
  └── usePersonalStatisticsStore()
        └── fetchAll(month)
```

### 家庭统计页面
```
pages/family/statistics.vue
  └── useFamilyStatisticsStore()
        └── fetchAll(month, familyId)
```

## 实施步骤

### 阶段 1: echarts 主题配置
1. 修改 `echarts-setup.ts` - 注册莫兰迪色系主题
2. 修改 `CategoryChart.vue` - 删除 CHART_COLORS，使用主题色
3. 修改 `MemberRankingChart.vue` - 删除 CHART_COLORS，使用主题色

### 阶段 2: Store 拆分
4. 创建 `personalStatisticsStore.ts` - 个人统计 store
5. 创建 `familyStatisticsStore.ts` - 家庭统计 store
6. 删除旧的 `statisticsStore.ts`

### 阶段 3: 页面更新
7. 修改 `pages/statistics/index.vue` - 使用 usePersonalStatisticsStore
8. 修改 `pages/family/statistics.vue` - 使用 useFamilyStatisticsStore
9. 修改 `components/AiSuggestionCard.vue` - 更新 store 引用

### 阶段 4: 验证
10. 运行 `pnpm lint:fix` 和 `pnpm type-check`
11. 手动测试：验证页面切换后数据不混乱
12. 手动测试：验证图表颜色符合莫兰迪色系

## 风险评估

- **低风险**: echarts 主题配置只影响视觉，不影响逻辑
- **中风险**: Store 拆分涉及多个文件，需要确保所有引用正确更新
- **缓解措施**: 运行 type-check 确保类型正确，手动测试验证功能
