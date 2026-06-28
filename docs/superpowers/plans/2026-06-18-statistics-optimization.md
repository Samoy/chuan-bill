# 统计页面优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 拆分个人统计和家庭统计的 store 以修复数据混乱 bug，并注册全局 echarts 莫兰迪色系主题以修复分类颜色重复问题。

**Architecture:** 将 `useStatisticsStore` 拆分为 `usePersonalStatisticsStore` 和 `useFamilyStatisticsStore`，共享组件通过 `familyId` prop 选择对应 store。echarts 注册全局 `morandi` 主题，删除组件中的硬编码 `CHART_COLORS`。

**Tech Stack:** Vue 3 `<script setup>`, Pinia, echarts 6, TypeScript, UnoCSS

## Global Constraints

- Vue 3 `<script setup>` exclusively, TypeScript strict mode
- UnoCSS utilities as primary styling; SCSS only for complex cases
- Never use raw `uni.showToast` -- use `GlobalToast`/`GlobalMessage`/`GlobalLoading`
- Pinia stores: `defineStore` with `use{Name}Store` naming
- 2-space indent, LF line endings, UTF-8
- Conventional Commits required (`feat:`, `fix:`, `chore:`, `refactor:`)
- Auto-imports: Vue Composition API, Pinia, custom stores/utils are auto-imported -- no manual imports needed

## File Structure

```
chuan-bill-app/src/
├── store/
│   ├── personalStatisticsStore.ts  # NEW - 个人统计 store
│   ├── familyStatisticsStore.ts    # NEW - 家庭统计 store
│   └── statisticsStore.ts          # DELETE - 旧的共用 store
├── utils/
│   └── echarts-setup.ts            # MODIFY - 注册莫兰迪主题
├── pages/
│   ├── statistics/
│   │   ├── index.vue               # MODIFY - 使用 usePersonalStatisticsStore
│   │   └── components/
│   │       ├── CategoryChart.vue    # MODIFY - 删除 CHART_COLORS，通过 familyId 选择 store
│   │       ├── AiSuggestionCard.vue # MODIFY - 通过 familyId 选择 store
│   │       └── DailyTrendChart.vue  # MODIFY - 使用 usePersonalStatisticsStore
│   ├── family/
│   │   ├── statistics.vue          # MODIFY - 使用 useFamilyStatisticsStore
│   │   └── components/
│   │       └── MemberRankingChart.vue # MODIFY - 删除 CHART_COLORS，使用莫兰迪色
│   └── bill/
│       └── index.vue               # MODIFY - 使用 usePersonalStatisticsStore
```

---

### Task 1: 注册 echarts 莫兰迪色系主题

**Files:**
- Modify: `chuan-bill-app/src/utils/echarts-setup.ts`

**Interfaces:**
- Produces: `echarts.registerTheme('morandi', {...})` -- 全局主题，后续图表组件自动应用

- [ ] **Step 1: 修改 echarts-setup.ts 注册莫兰迪主题**

```typescript
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { DataZoomComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { provideEcharts } from 'uni-echarts/shared'

echarts.use([PieChart, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer, DataZoomComponent])

// 莫兰迪色系 - 低饱和度、柔和护眼
export const MORANDI_COLORS = [
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

echarts.registerTheme('morandi', {
  color: MORANDI_COLORS,
  backgroundColor: 'transparent',
})

export function setupEcharts() {
  provideEcharts(echarts)
}
```

- [ ] **Step 2: 运行 type-check 验证无类型错误**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: PASS (no type errors)

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/utils/echarts-setup.ts
git commit -m "feat(echarts): 注册莫兰迪色系全局主题"
```

---

### Task 2: 更新 CategoryChart 使用 echarts 主题

**Files:**
- Modify: `chuan-bill-app/src/pages/statistics/components/CategoryChart.vue:1-69`

**Interfaces:**
- Consumes: `MORANDI_COLORS` from `echarts-setup.ts` (auto-imported if exported, otherwise inline)
- Produces: 无 `CHART_COLORS`，饼图自动使用 echarts 主题色

- [ ] **Step 1: 删除 CHART_COLORS 并更新 pieOption 使用 echarts 主题色**

在 `CategoryChart.vue` 中：
1. 删除第 17 行的 `CHART_COLORS` 常量
2. 删除 `pieOption` 中的 `color: CHART_COLORS`
3. 删除分类列表中 `CHART_COLORS[index % CHART_COLORS.length]` 的引用，改为使用 echarts 主题色

修改后的 `<script setup>` 部分：

```vue
<script setup lang="ts">
defineOptions({
  name: 'CategoryChart',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

const props = defineProps<{
  month: string
  familyId?: string
}>()

const statisticsStore = useStatisticsStore()
const themeStore = useManualThemeStore()

const activeType = ref<'expense' | 'income'>('expense')

// 莫兰迪色系 - 与 echarts 主题一致
const MORANDI_COLORS = [
  '#8D9BA3', '#B5C4B1', '#C9B1A0', '#A3B5C4',
  '#C4B5A3', '#B1B5C4', '#C4A3B5', '#B5C4C4', '#C4C4A3',
]

const segmentedOptions = [
  { value: 'expense', payload: { label: '支出', icon: 'i-icon-park-outline:expenses' } },
  { value: 'income', payload: { label: '收入', icon: 'i-icon-park-outline:income' } },
]

const pieOption = computed(() => {
  const data = statisticsStore.categoryData
  const isDark = themeStore.isDark
  if (!data.length) {
    return {}
  }

  return {
    grid: {
      top: '10%',
      bottom: '10%',
      containLabel: true,
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
      backgroundColor: isDark ? '#333' : '#fff',
      borderColor: 'transparent',
      borderWidth: 0,
    },
    color: MORANDI_COLORS,
    series: [{
      type: 'pie',
      radius: ['0%', '90%'],
      center: ['50%', '50%'],
      itemStyle: {
        borderColor: isDark ? '#1a1a1a' : '#fff',
        borderWidth: 2,
      },
      label: { show: false },
      data: data.map(item => ({
        value: item.amount,
        name: item.categoryName,
      })),
    }],
  }
})

watch(activeType, (type) => {
  statisticsStore.fetchCategoryBreakdown(props.month, type, props.familyId)
})

watch(() => props.month, () => {
  statisticsStore.fetchCategoryBreakdown(props.month, activeType.value, props.familyId)
})
</script>
```

模板中分类列表的颜色引用（第 126 行）也需更新：

```vue
<view
  class="h-3 w-3 shrink-0 rounded-sm"
  :style="{ backgroundColor: MORANDI_COLORS[index % MORANDI_COLORS.length] }"
/>
```

- [ ] **Step 2: 运行 type-check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/pages/statistics/components/CategoryChart.vue
git commit -m "fix(chart): CategoryChart 使用莫兰迪色系替代硬编码 CHART_COLORS"
```

---

### Task 3: 更新 MemberRankingChart 使用莫兰迪色系

**Files:**
- Modify: `chuan-bill-app/src/pages/family/components/MemberRankingChart.vue:1-100`

**Interfaces:**
- Produces: 进度条使用莫兰迪色，无 `CHART_COLORS`

- [ ] **Step 1: 更新 MemberRankingChart 的进度条颜色**

在 `MemberRankingChart.vue` 中：
1. 修改 `barColor` computed，使用莫兰迪色系中的颜色
2. 支出使用灰蓝 `#8D9BA3`，收入使用灰绿 `#B5C4B1`

修改 `barColor` 部分：

```typescript
const barColor = computed(() =>
  activeTab.value === 'expense' ? '#8D9BA3' : '#B5C4B1',
)
```

- [ ] **Step 2: 运行 type-check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/pages/family/components/MemberRankingChart.vue
git commit -m "fix(chart): MemberRankingChart 使用莫兰迪色系"
```

---

### Task 4: 创建 usePersonalStatisticsStore

**Files:**
- Create: `chuan-bill-app/src/store/personalStatisticsStore.ts`
- Reference: `chuan-bill-app/src/store/statisticsStore.ts` (现有实现)

**Interfaces:**
- Produces: `usePersonalStatisticsStore()` -- auto-imported
  - `overview: Ref<BillMonthlyStatsVO | null>`
  - `categoryData: Ref<CategoryStatItem[]>`
  - `dailyTrend: Ref<{ days: string[], expenses: number[], incomes: number[] } | null>`
  - `aiSuggestion: Ref<string>`
  - `aiCached: Ref<boolean>`
  - `aiRemainingCount: Ref<number>`
  - `overviewLoading: Ref<boolean>`
  - `categoryLoading: Ref<boolean>`
  - `trendLoading: Ref<boolean>`
  - `aiLoading: Ref<boolean>`
  - `fetchOverview(month: string): Promise<void>`
  - `fetchCategoryBreakdown(month: string, type: 'expense' | 'income'): Promise<void>`
  - `fetchDailyTrend(month: string): Promise<void>`
  - `fetchAiSuggestion(month: string, regenerate?: boolean): Promise<void>`
  - `fetchAiSuggestionCached(month: string): Promise<void>`
  - `fetchAll(month: string): Promise<void>`
  - `reset(): void`

- [ ] **Step 1: 创建 personalStatisticsStore.ts**

基于现有 `statisticsStore.ts` 创建，移除所有 `familyId` 参数和 `setAnalysisContext`/`currentAnalysisType`/`currentFamilyId`：

```typescript
import type { BillMonthlyStatsVO } from '@/api/globals'
import dayjs from 'dayjs'
import { add, divide, multiply } from 'mathjs'
import { AiSuggestionType } from '@/constant/ai'

interface CategoryStatItem {
  categoryId: string
  categoryName: string
  categoryIcon: string
  amount: number
  percentage: number
}

export const usePersonalStatisticsStore = defineStore('personalStatistics', () => {
  const user = useUserStore()
  const billStore = useBillStore()

  const overview = ref<BillMonthlyStatsVO | null>(null)
  const categoryData = ref<CategoryStatItem[]>([])
  const dailyTrend = ref<{ days: string[], expenses: number[], incomes: number[] } | null>(null)
  const aiSuggestion = ref('')
  const aiCached = ref(false)
  const aiRemainingCount = ref(-1)
  const overviewLoading = ref(false)
  const categoryLoading = ref(false)
  const trendLoading = ref(false)
  const aiLoading = ref(false)

  async function fetchOverview(month: string) {
    overviewLoading.value = true
    try {
      overview.value = await billStore.getMonthlyBillStats(month) || null
    }
    finally {
      overviewLoading.value = false
    }
  }

  async function fetchCategoryBreakdown(month: string, type: 'expense' | 'income') {
    categoryLoading.value = true
    try {
      if (user.isLoggedIn) {
        const res = await Apis.statistics.getCategoryStats({ params: { month, type } })
        if (res.success && res.data) {
          categoryData.value = res.data.map(item => ({
            categoryId: item.categoryId || '',
            categoryName: item.categoryName || '',
            categoryIcon: item.categoryIcon || '',
            amount: Number(item.amount || 0),
            percentage: Number(item.percentage || 0),
          }))
          return
        }
      }

      // 本地计算
      const filtered = billStore.localBillList.filter(
        bill => bill.type === type && dayjs(bill.time).isSame(dayjs(month), 'month'),
      )

      if (filtered.length === 0) {
        categoryData.value = []
        return
      }

      const categoryMap = new Map<string, { categoryId: string, categoryName: string, categoryIcon: string, amount: number }>()
      for (const bill of filtered) {
        const catId = bill.category?.id || ''
        const existing = categoryMap.get(catId)
        const amount = Number(bill.amount || 0)
        if (existing) {
          existing.amount = add(existing.amount, amount) as number
        }
        else {
          categoryMap.set(catId, {
            categoryId: catId,
            categoryName: bill.category?.name || '未知',
            categoryIcon: bill.category?.icon || '',
            amount,
          })
        }
      }

      let total = 0
      for (const item of categoryMap.values()) {
        total = add(total, item.amount) as number
      }

      const sorted = [...categoryMap.values()].sort((a, b) => b.amount - a.amount)
      categoryData.value = sorted.map(item => ({
        ...item,
        percentage: total > 0 ? Number(multiply(divide(item.amount, total), 100).toFixed(1)) : 0,
      }))
    }
    finally {
      categoryLoading.value = false
    }
  }

  async function fetchDailyTrend(month: string) {
    trendLoading.value = true
    try {
      const daysInMonth = dayjs(month).daysInMonth()
      const days = Array.from({ length: daysInMonth }, (_, i) => String(i + 1))
      const expenses = Array.from({ length: daysInMonth }, () => 0)
      const incomes = Array.from({ length: daysInMonth }, () => 0)

      if (user.isLoggedIn) {
        const res = await Apis.statistics.getDailyTrend({ params: { month } })
        if (res.success && res.data) {
          for (const item of res.data) {
            if (item.date) {
              const dayIndex = dayjs(item.date).date() - 1
              if (dayIndex >= 0 && dayIndex < daysInMonth) {
                expenses[dayIndex] = Number(item.expense || 0)
                incomes[dayIndex] = Number(item.income || 0)
              }
            }
          }
        }
      }
      else {
        const filtered = billStore.localBillList.filter(
          bill => dayjs(bill.time).isSame(dayjs(month), 'month'),
        )
        for (const bill of filtered) {
          const dayIndex = dayjs(bill.time).date() - 1
          if (dayIndex >= 0 && dayIndex < daysInMonth) {
            const amount = Number(bill.amount || 0)
            if (bill.type === 'expense') {
              expenses[dayIndex] = add(expenses[dayIndex], amount) as number
            }
            else if (bill.type === 'income') {
              incomes[dayIndex] = add(incomes[dayIndex], amount) as number
            }
          }
        }
      }

      dailyTrend.value = { days, expenses, incomes }
    }
    finally {
      trendLoading.value = false
    }
  }

  async function fetchAiSuggestion(month: string, regenerate = false) {
    if (!user.isLoggedIn)
      return
    aiLoading.value = true
    try {
      const res = await Apis.ai.analysis({ params: { month, analysisType: AiSuggestionType.USER, regenerate } })
      if (res.success && res.data) {
        aiSuggestion.value = res.data.content || ''
        aiCached.value = res.data.cached || false
        aiRemainingCount.value = res.data.remainingCount || -1
      }
      else {
        aiSuggestion.value = ''
        aiCached.value = false
      }
    }
    catch {
      aiSuggestion.value = ''
      aiCached.value = false
    }
    finally {
      aiLoading.value = false
    }
  }

  async function fetchAiSuggestionCached(month: string) {
    if (!user.isLoggedIn)
      return
    try {
      const res = await Apis.ai.analysis({ params: { analysisType: AiSuggestionType.USER, month }, meta: { silent: true } } as any)
      if (res.success && res.data) {
        aiSuggestion.value = res.data.content || ''
        aiCached.value = res.data.cached || false
        aiRemainingCount.value = res.data.remainingCount || -1
      }
      else {
        aiSuggestion.value = ''
        aiCached.value = false
      }
    }
    catch {
      aiSuggestion.value = ''
      aiCached.value = false
    }
  }

  async function fetchAll(month: string) {
    await Promise.all([
      fetchOverview(month),
      fetchCategoryBreakdown(month, 'expense'),
      fetchDailyTrend(month),
    ])
  }

  function reset() {
    overview.value = null
    categoryData.value = []
    dailyTrend.value = null
    aiSuggestion.value = ''
    aiCached.value = false
    aiRemainingCount.value = -1
    overviewLoading.value = false
    categoryLoading.value = false
    trendLoading.value = false
    aiLoading.value = false
  }

  return {
    overview,
    categoryData,
    dailyTrend,
    aiSuggestion,
    aiCached,
    aiRemainingCount,
    overviewLoading,
    categoryLoading,
    trendLoading,
    aiLoading,
    fetchOverview,
    fetchCategoryBreakdown,
    fetchDailyTrend,
    fetchAiSuggestion,
    fetchAiSuggestionCached,
    fetchAll,
    reset,
  }
})
```

- [ ] **Step 2: 运行 type-check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/store/personalStatisticsStore.ts
git commit -m "feat(store): 创建 usePersonalStatisticsStore"
```

---

### Task 5: 创建 useFamilyStatisticsStore

**Files:**
- Create: `chuan-bill-app/src/store/familyStatisticsStore.ts`
- Reference: `chuan-bill-app/src/store/statisticsStore.ts` (现有实现)

**Interfaces:**
- Produces: `useFamilyStatisticsStore()` -- auto-imported
  - 同 `usePersonalStatisticsStore` 的 state，但所有 action 需要 `familyId` 参数
  - `fetchOverview(month: string, familyId: string): Promise<void>`
  - `fetchCategoryBreakdown(month: string, type: 'expense' | 'income', familyId: string): Promise<void>`
  - `fetchDailyTrend(month: string, familyId: string): Promise<void>`
  - `fetchAiSuggestion(month: string, familyId: string, regenerate?: boolean): Promise<void>`
  - `fetchAiSuggestionCached(month: string, familyId: string): Promise<void>`
  - `fetchAll(month: string, familyId: string): Promise<void>`
  - `reset(): void`

- [ ] **Step 1: 创建 familyStatisticsStore.ts**

基于现有 `statisticsStore.ts` 创建，保留 `familyId` 参数，移除 `setAnalysisContext`/`currentAnalysisType`/`currentFamilyId`：

```typescript
import type { BillMonthlyStatsVO } from '@/api/globals'
import dayjs from 'dayjs'
import { add, divide, multiply } from 'mathjs'
import { AiSuggestionType } from '@/constant/ai'

interface CategoryStatItem {
  categoryId: string
  categoryName: string
  categoryIcon: string
  amount: number
  percentage: number
}

export const useFamilyStatisticsStore = defineStore('familyStatistics', () => {
  const user = useUserStore()
  const billStore = useBillStore()

  const overview = ref<BillMonthlyStatsVO | null>(null)
  const categoryData = ref<CategoryStatItem[]>([])
  const dailyTrend = ref<{ days: string[], expenses: number[], incomes: number[] } | null>(null)
  const aiSuggestion = ref('')
  const aiCached = ref(false)
  const aiRemainingCount = ref(-1)
  const overviewLoading = ref(false)
  const categoryLoading = ref(false)
  const trendLoading = ref(false)
  const aiLoading = ref(false)

  async function fetchOverview(month: string, familyId: string) {
    overviewLoading.value = true
    try {
      overview.value = await billStore.getMonthlyBillStats(month, familyId) || null
    }
    finally {
      overviewLoading.value = false
    }
  }

  async function fetchCategoryBreakdown(month: string, type: 'expense' | 'income', familyId: string) {
    categoryLoading.value = true
    try {
      if (user.isLoggedIn) {
        const res = await Apis.statistics.getCategoryStats({ params: { month, type, familyId } })
        if (res.success && res.data) {
          categoryData.value = res.data.map(item => ({
            categoryId: item.categoryId || '',
            categoryName: item.categoryName || '',
            categoryIcon: item.categoryIcon || '',
            amount: Number(item.amount || 0),
            percentage: Number(item.percentage || 0),
          }))
          return
        }
      }

      // 家庭统计不支持本地计算
      categoryData.value = []
    }
    finally {
      categoryLoading.value = false
    }
  }

  async function fetchDailyTrend(month: string, familyId: string) {
    trendLoading.value = true
    try {
      const daysInMonth = dayjs(month).daysInMonth()
      const days = Array.from({ length: daysInMonth }, (_, i) => String(i + 1))
      const expenses = Array.from({ length: daysInMonth }, () => 0)
      const incomes = Array.from({ length: daysInMonth }, () => 0)

      if (user.isLoggedIn) {
        const res = await Apis.statistics.getDailyTrend({ params: { month, familyId } })
        if (res.success && res.data) {
          for (const item of res.data) {
            if (item.date) {
              const dayIndex = dayjs(item.date).date() - 1
              if (dayIndex >= 0 && dayIndex < daysInMonth) {
                expenses[dayIndex] = Number(item.expense || 0)
                incomes[dayIndex] = Number(item.income || 0)
              }
            }
          }
        }
      }

      dailyTrend.value = { days, expenses, incomes }
    }
    finally {
      trendLoading.value = false
    }
  }

  async function fetchAiSuggestion(month: string, familyId: string, regenerate = false) {
    if (!user.isLoggedIn)
      return
    aiLoading.value = true
    try {
      const res = await Apis.ai.analysis({ params: { month, analysisType: AiSuggestionType.FAMILY, familyId, regenerate } })
      if (res.success && res.data) {
        aiSuggestion.value = res.data.content || ''
        aiCached.value = res.data.cached || false
        aiRemainingCount.value = res.data.remainingCount || -1
      }
      else {
        aiSuggestion.value = ''
        aiCached.value = false
      }
    }
    catch {
      aiSuggestion.value = ''
      aiCached.value = false
    }
    finally {
      aiLoading.value = false
    }
  }

  async function fetchAiSuggestionCached(month: string, familyId: string) {
    if (!user.isLoggedIn)
      return
    try {
      const res = await Apis.ai.analysis({ params: { analysisType: AiSuggestionType.FAMILY, month, familyId }, meta: { silent: true } } as any)
      if (res.success && res.data) {
        aiSuggestion.value = res.data.content || ''
        aiCached.value = res.data.cached || false
        aiRemainingCount.value = res.data.remainingCount || -1
      }
      else {
        aiSuggestion.value = ''
        aiCached.value = false
      }
    }
    catch {
      aiSuggestion.value = ''
      aiCached.value = false
    }
  }

  async function fetchAll(month: string, familyId: string) {
    await Promise.all([
      fetchOverview(month, familyId),
      fetchCategoryBreakdown(month, 'expense', familyId),
      fetchDailyTrend(month, familyId),
    ])
  }

  function reset() {
    overview.value = null
    categoryData.value = []
    dailyTrend.value = null
    aiSuggestion.value = ''
    aiCached.value = false
    aiRemainingCount.value = -1
    overviewLoading.value = false
    categoryLoading.value = false
    trendLoading.value = false
    aiLoading.value = false
  }

  return {
    overview,
    categoryData,
    dailyTrend,
    aiSuggestion,
    aiCached,
    aiRemainingCount,
    overviewLoading,
    categoryLoading,
    trendLoading,
    aiLoading,
    fetchOverview,
    fetchCategoryBreakdown,
    fetchDailyTrend,
    fetchAiSuggestion,
    fetchAiSuggestionCached,
    fetchAll,
    reset,
  }
})
```

- [ ] **Step 2: 运行 type-check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/store/familyStatisticsStore.ts
git commit -m "feat(store): 创建 useFamilyStatisticsStore"
```

---

### Task 6: 更新共享组件使用 familyId 选择 store

**Files:**
- Modify: `chuan-bill-app/src/pages/statistics/components/CategoryChart.vue`
- Modify: `chuan-bill-app/src/pages/statistics/components/AiSuggestionCard.vue`

**Interfaces:**
- Consumes: `usePersonalStatisticsStore()` 和 `useFamilyStatisticsStore()` (auto-imported)
- Produces: 组件通过 `familyId` prop 自动选择正确的 store

- [ ] **Step 1: 更新 CategoryChart.vue 通过 familyId 选择 store**

替换 `<script setup>` 中的 store 引用逻辑。组件已有 `familyId` prop，现在根据它选择 store：

```vue
<script setup lang="ts">
defineOptions({
  name: 'CategoryChart',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

const props = defineProps<{
  month: string
  familyId?: string
}>()

const personalStore = usePersonalStatisticsStore()
const familyStore = useFamilyStatisticsStore()
const themeStore = useManualThemeStore()

const store = computed(() => props.familyId ? familyStore : personalStore)

const activeType = ref<'expense' | 'income'>('expense')

const MORANDI_COLORS = [
  '#8D9BA3', '#B5C4B1', '#C9B1A0', '#A3B5C4',
  '#C4B5A3', '#B1B5C4', '#C4A3B5', '#B5C4C4', '#C4C4A3',
]

const segmentedOptions = [
  { value: 'expense', payload: { label: '支出', icon: 'i-icon-park-outline:expenses' } },
  { value: 'income', payload: { label: '收入', icon: 'i-icon-park-outline:income' } },
]

const pieOption = computed(() => {
  const data = store.value.categoryData
  const isDark = themeStore.isDark
  if (!data.length) {
    return {}
  }

  return {
    grid: {
      top: '10%',
      bottom: '10%',
      containLabel: true,
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
      backgroundColor: isDark ? '#333' : '#fff',
      borderColor: 'transparent',
      borderWidth: 0,
    },
    color: MORANDI_COLORS,
    series: [{
      type: 'pie',
      radius: ['0%', '90%'],
      center: ['50%', '50%'],
      itemStyle: {
        borderColor: isDark ? '#1a1a1a' : '#fff',
        borderWidth: 2,
      },
      label: { show: false },
      data: data.map(item => ({
        value: item.amount,
        name: item.categoryName,
      })),
    }],
  }
})

function fetchData() {
  if (props.familyId) {
    familyStore.fetchCategoryBreakdown(props.month, activeType.value, props.familyId)
  }
  else {
    personalStore.fetchCategoryBreakdown(props.month, activeType.value)
  }
}

watch(activeType, fetchData)

watch(() => props.month, fetchData)
</script>
```

模板中更新 store 引用：

```vue
<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <!-- 标题和切换 -->
    <view class="mb-3 flex items-center justify-between">
      <text class="text-base font-500">
        分类统计
      </text>
      <wd-segmented
        v-model:value="activeType"
        :options="segmentedOptions"
        size="small"
        custom-class="!rounded-2xl dark:!bg-[var(--wot-dark-background3)] w-200px!"
      >
        <template #label="{ option }">
          <view class="w-200px flex items-center justify-center gap-1">
            <view class="text-xs" :class="[option.payload.icon]" />
            <view class="text-xs">
              {{ option.payload.label }}
            </view>
          </view>
        </template>
      </wd-segmented>
    </view>

    <!-- 饼图 -->
    <view v-if="!store.value.categoryLoading && store.value.categoryData.length">
      <uni-echarts :option="pieOption" autoresize custom-style="height: 220px; width: 100%;" />
    </view>
    <view v-else-if="store.value.categoryLoading" class="flex items-center justify-center py-8">
      <wd-skeleton :row="0" animation="gradient">
        <template #label="{ option }">
          <view class="h-200px flex items-center justify-center gap-1">
            <view class="text-xs dark:text-white" :class="[option.payload.icon]" />
            <view class="text-xs">
              {{ option.payload.label }}
            </view>
          </view>
        </template>
      </wd-skeleton>
    </view>
    <view v-else class="flex items-center justify-center py-8">
      <text class="text-sm text-gray-400">
        暂无数据
      </text>
    </view>

    <!-- 分类列表 -->
    <view v-if="store.value.categoryData.length" class="mt-3 flex flex-col gap-2">
      <view
        v-for="(item, index) in store.value.categoryData"
        :key="item.categoryId"
        class="flex items-center gap-2"
      >
        <view
          class="h-3 w-3 shrink-0 rounded-sm"
          :style="{ backgroundColor: MORANDI_COLORS[index % MORANDI_COLORS.length] }"
        />
        <view class="h-4 w-4 flex shrink-0 items-center justify-center text-gray-500 dark:text-gray-300" :class="[item.categoryIcon]" />
        <text class="flex-1 text-xs text-gray-500 dark:text-gray-300">
          {{ item.categoryName }}
        </text>
        <text class="text-sm font-500">
          ¥{{ item.amount.toFixed(2) }}
        </text>
        <text class="w-12 text-right text-xs text-gray-500 dark:text-gray-300">
          {{ item.percentage.toFixed(1) }}%
        </text>
      </view>
    </view>
  </view>
</template>
```

- [ ] **Step 2: 更新 AiSuggestionCard.vue 通过 familyId 选择 store**

替换 store 引用逻辑：

```vue
<script setup lang="ts">
import { AiSuggestionType } from '@/constant/ai'

defineOptions({
  name: 'AiSuggestionCard',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

const props = defineProps<{
  month: string
  analysisType?: typeof AiSuggestionType.FAMILY | typeof AiSuggestionType.USER
  familyId?: string
}>()

const AI_DAILY_LIMIT = 5

const user = useUserStore()
const personalStore = usePersonalStatisticsStore()
const familyStore = useFamilyStatisticsStore()

const store = computed(() => props.familyId ? familyStore : personalStore)

function handleLogin() {
  user.requireAuth(() => {
    if (props.familyId) {
      familyStore.fetchAiSuggestion(props.month, props.familyId)
    }
    else {
      personalStore.fetchAiSuggestion(props.month)
    }
  })
}

function fetchAnalysis() {
  if (props.familyId) {
    familyStore.fetchAiSuggestion(props.month, props.familyId, true)
  }
  else {
    personalStore.fetchAiSuggestion(props.month, true)
  }
}

const remainingLabel = computed(() => {
  const count = store.value.aiRemainingCount
  if (count < 0)
    return ''
  return `(${count}/${AI_DAILY_LIMIT})`
})

const isDisabled = computed(() => {
  return store.value.aiRemainingCount === 0
})

watch(() => props.month, () => {
  store.value.aiSuggestion = ''
})
</script>
```

模板中将所有 `statisticsStore` 替换为 `store.value`：

```vue
<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <view class="mb-3 flex items-center gap-2">
      <text class="text-base font-500">
        AI 消费建议
      </text>
    </view>

    <!-- 未登录状态 -->
    <view v-if="!user.isLoggedIn" class="flex flex-col items-center gap-3 py-6">
      <view class="flex items-center gap-2 text-sm text-gray-400">
        <view class="i-lucide:sparkles" /> 登录后获取AI消费建议
      </view>
      <wd-button custom-class="w-full" type="primary" block @click="handleLogin">
        立即登录
      </wd-button>
    </view>

    <!-- 加载中 -->
    <view v-else-if="store.value.aiLoading" class="py-4">
      <wd-skeleton :row="4" animation="gradient" />
    </view>

    <!-- 已有数据（缓存或新生成） -->
    <view v-else-if="store.value.aiSuggestion">
      <view class="text-sm text-gray-600 leading-relaxed dark:text-gray-300">
        {{ store.value.aiSuggestion }}
      </view>
      <!-- 生成按钮 -->
      <view class="mt-3">
        <view
          class="ai-btn mx-auto box-border w-fit flex items-center justify-center gap-1.5 rounded-full px-8 py-1.5 text-sm text-white font-500 transition-transform active:scale-95"
          :class="{ 'ai-btn-disabled': isDisabled }"
          @click="!isDisabled && fetchAnalysis()"
        >
          <view class="i-lucide:sparkles" />
          <text>{{ isDisabled ? '今日次数已用完' : '重新生成' }} {{ remainingLabel }}</text>
        </view>
      </view>
    </view>

    <!-- 已登录但无数据 -->
    <view v-else class="flex flex-col items-center gap-3 py-6">
      <view
        class="ai-btn mx-auto box-border w-fit flex items-center justify-center gap-1.5 rounded-full px-8 py-1.5 text-sm text-white font-500 transition-transform active:scale-95"
        :class="{ 'ai-btn-disabled': isDisabled }"
        @click="!isDisabled && fetchAnalysis()"
      >
        <view class="i-lucide:sparkles" />
        <text>{{ isDisabled ? '今日次数已用完' : '获取AI分析' }} {{ remainingLabel }}</text>
      </view>
    </view>

    <!-- AI免责提示 -->
    <view v-if="store.value.aiSuggestion" class="mt-3 flex items-center justify-center gap-1 text-xs text-gray-400 dark:text-gray-500">
      <view class="i-lucide:info text-10px" />
      <text>内容由AI生成，仅供参考</text>
    </view>
  </view>
</template>
```

- [ ] **Step 3: 运行 type-check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add chuan-bill-app/src/pages/statistics/components/CategoryChart.vue chuan-bill-app/src/pages/statistics/components/AiSuggestionCard.vue
git commit -m "refactor(chart): 共享组件通过 familyId 选择对应 store"
```

---

### Task 7: 更新页面使用新 store

**Files:**
- Modify: `chuan-bill-app/src/pages/statistics/index.vue`
- Modify: `chuan-bill-app/src/pages/family/statistics.vue`
- Modify: `chuan-bill-app/src/pages/statistics/components/DailyTrendChart.vue`
- Modify: `chuan-bill-app/src/pages/bill/index.vue`

**Interfaces:**
- Consumes: `usePersonalStatisticsStore()` 和 `useFamilyStatisticsStore()` (auto-imported)
- Produces: 各页面使用独立 store，数据互不干扰

- [ ] **Step 1: 更新 pages/statistics/index.vue 使用 usePersonalStatisticsStore**

替换 `useStatisticsStore()` 为 `usePersonalStatisticsStore()`，移除 `setAnalysisContext` 调用：

```vue
<script setup lang="ts">
import dayjs from 'dayjs'
import { AiSuggestionType } from '@/constant/ai'
import { EVENTS } from '@/constant/events'
import { setupEcharts } from '@/utils/echarts-setup'
import { eventBus } from '@/utils/eventBus'
import AiSuggestionCard from './components/AiSuggestionCard.vue'
import BudgetCard from './components/BudgetCard.vue'
import BudgetSettingPopup from './components/BudgetSettingPopup.vue'
import CategoryChart from './components/CategoryChart.vue'
import DailyTrendChart from './components/DailyTrendChart.vue'

definePage({
  name: 'statistics',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '统计',
  },
})

setupEcharts()

const statisticsStore = usePersonalStatisticsStore()
const budgetStore = useBudgetStore()
const user = useUserStore()
const showSettingPopup = ref(false)

const currentMonth = ref(dayjs().format('YYYY-MM'))

const pageVisible = ref(true)
const needsRefresh = ref(false)

const showMonthPicker = ref(false)

const monthOptions = computed(() => {
  const options = []
  for (let i = 0; i < 12; i++) {
    const month = dayjs().subtract(i, 'month').format('YYYY-MM')
    options.push({ label: month, value: month })
  }
  return options
})

function prevMonth() {
  currentMonth.value = dayjs(currentMonth.value).subtract(1, 'month').format('YYYY-MM')
}

function nextMonth() {
  const next = dayjs(currentMonth.value).add(1, 'month')
  if (next.isAfter(dayjs(), 'month')) {
    return
  }
  currentMonth.value = next.format('YYYY-MM')
}

function onMonthSelect({ value }: { value: string }) {
  currentMonth.value = value
  showMonthPicker.value = false
}

onLoad(() => {
  statisticsStore.fetchAll(currentMonth.value)
  statisticsStore.fetchAiSuggestionCached(currentMonth.value)
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(currentMonth.value)
  }
})

onPullDownRefresh(() => {
  handleDataUpdated()
    .finally(() => uni.stopPullDownRefresh())
})

onShow(() => {
  pageVisible.value = true
  if (needsRefresh.value) {
    needsRefresh.value = false
    handleDataUpdated()
  }
})

onHide(() => {
  pageVisible.value = false
})

watch(currentMonth, (month) => {
  statisticsStore.fetchAll(month)
  statisticsStore.fetchAiSuggestionCached(month)
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(month)
  }
})

watch(() => user.isLoggedIn, () => {
  statisticsStore.fetchAll(currentMonth.value)
  statisticsStore.fetchAiSuggestionCached(currentMonth.value)
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(currentMonth.value)
  }
})

async function handleDataUpdated() {
  if (!pageVisible.value) {
    needsRefresh.value = true
    return
  }
  await statisticsStore.fetchAll(currentMonth.value)
  await statisticsStore.fetchAiSuggestionCached(currentMonth.value)
  if (user.isLoggedIn) {
    await budgetStore.fetchBudget(currentMonth.value)
  }
}

onMounted(() => {
  eventBus.on(EVENTS.BILL.UPDATED, handleDataUpdated)
  eventBus.on(EVENTS.FAMILY.UPDATED, handleDataUpdated)
})

onUnmounted(() => {
  eventBus.off(EVENTS.BILL.UPDATED, handleDataUpdated)
  eventBus.off(EVENTS.FAMILY.UPDATED, handleDataUpdated)
})
</script>
```

模板部分保持不变，`statisticsStore.overviewLoading` 等引用自动指向新 store。

- [ ] **Step 2: 更新 pages/family/statistics.vue 使用 useFamilyStatisticsStore**

替换 `useStatisticsStore()` 为 `useFamilyStatisticsStore()`，移除 `setAnalysisContext` 调用：

```vue
<script setup lang="ts">
import dayjs from 'dayjs'
import { AiSuggestionType } from '@/constant/ai'
import AiSuggestionCard from '@/pages/statistics/components/AiSuggestionCard.vue'
import { setupEcharts } from '@/utils/echarts-setup'
import CategoryChart from '../statistics/components/CategoryChart.vue'
import MemberRankingChart from './components/MemberRankingChart.vue'

definePage({
  name: 'family-statistics',
  layout: 'default',
  style: {
    navigationBarTitleText: '家庭统计',
  },
})

setupEcharts()

const statisticsStore = useFamilyStatisticsStore()

const familyId = ref('')
const currentMonth = ref(dayjs().format('YYYY-MM'))
const showMonthPicker = ref(false)

const monthOptions = computed(() => {
  const options = []
  for (let i = 0; i < 12; i++) {
    const month = dayjs().subtract(i, 'month').format('YYYY-MM')
    options.push({ label: month, value: month })
  }
  return options
})

function prevMonth() {
  currentMonth.value = dayjs(currentMonth.value).subtract(1, 'month').format('YYYY-MM')
}

function nextMonth() {
  const next = dayjs(currentMonth.value).add(1, 'month')
  if (next.isAfter(dayjs(), 'month')) {
    return
  }
  currentMonth.value = next.format('YYYY-MM')
}

function onMonthSelect({ value }: { value: string }) {
  currentMonth.value = value
  showMonthPicker.value = false
}

onLoad((options) => {
  if (options?.familyId) {
    familyId.value = options.familyId
  }
  if (options?.familyName) {
    uni.setNavigationBarTitle({ title: `${decodeURIComponent(options.familyName)}账单统计` })
  }
  statisticsStore.fetchAll(currentMonth.value, familyId.value)
  statisticsStore.fetchAiSuggestionCached(currentMonth.value, familyId.value)
})

watch(currentMonth, (month) => {
  statisticsStore.fetchAll(month, familyId.value)
  statisticsStore.fetchAiSuggestionCached(month, familyId.value)
})
</script>
```

模板部分保持不变，`statisticsStore.overviewLoading` 等引用自动指向新 store。

- [ ] **Step 3: 更新 DailyTrendChart.vue 使用 usePersonalStatisticsStore**

`DailyTrendChart` 仅在个人统计页面使用，直接替换 store：

```vue
<script setup lang="ts">
defineOptions({
  name: 'DailyTrendChart',
  options: { virtualHost: true, styleIsolation: 'shared' },
})

const props = defineProps<{
  month: string
}>()

const statisticsStore = usePersonalStatisticsStore()
// ... 其余代码保持不变
```

- [ ] **Step 4: 更新 pages/bill/index.vue 使用 usePersonalStatisticsStore**

替换第 30 行的 `useStatisticsStore()` 为 `usePersonalStatisticsStore()`：

```typescript
const statisStore = usePersonalStatisticsStore()
```

- [ ] **Step 5: 运行 type-check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: PASS

- [ ] **Step 6: 运行 lint**

Run: `cd chuan-bill-app && pnpm lint:fix`
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add chuan-bill-app/src/pages/statistics/index.vue chuan-bill-app/src/pages/family/statistics.vue chuan-bill-app/src/pages/statistics/components/DailyTrendChart.vue chuan-bill-app/src/pages/bill/index.vue
git commit -m "refactor(pages): 各页面使用独立的 statistics store"
```

---

### Task 8: 删除旧的 statisticsStore 并最终验证

**Files:**
- Delete: `chuan-bill-app/src/store/statisticsStore.ts`

**Interfaces:**
- 无消费者引用旧 store

- [ ] **Step 1: 确认无文件引用旧 store**

Run: `cd chuan-bill-app && grep -r "useStatisticsStore" src/ --include="*.ts" --include="*.vue" | grep -v "auto-imports.d.ts"`
Expected: 无输出（所有引用已迁移）

- [ ] **Step 2: 删除旧 store 文件**

```bash
rm chuan-bill-app/src/store/statisticsStore.ts
```

- [ ] **Step 3: 运行 type-check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: PASS

- [ ] **Step 4: 运行 lint**

Run: `cd chuan-bill-app && pnpm lint:fix`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-app/src/store/statisticsStore.ts
git commit -m "chore(store): 删除旧的共用 statisticsStore"
```

---

### Task 9: 最终集成验证

**Files:**
- 无文件改动，纯验证

- [ ] **Step 1: 运行完整 lint 和 type-check**

Run: `cd chuan-bill-app && pnpm lint:fix && pnpm type-check`
Expected: 两项均 PASS

- [ ] **Step 2: 搜索确认无遗留引用**

Run: `cd chuan-bill-app && grep -rn "CHART_COLORS" src/ --include="*.ts" --include="*.vue"`
Expected: 无输出

Run: `cd chuan-bill-app && grep -rn "setAnalysisContext" src/ --include="*.ts" --include="*.vue"`
Expected: 无输出

- [ ] **Step 3: 启动开发服务器验证**

Run: `cd chuan-bill-app && pnpm dev`
Expected: 无编译错误，页面正常加载

手动验证清单：
- 进入个人统计页面 → 数据正常显示
- 进入家庭统计页面 → 数据正常显示
- 从家庭统计返回 → 个人统计数据不混乱
- 饼图颜色为莫兰迪色系
- 成员排行榜进度条为莫兰迪色系
- 新分类颜色不重复

- [ ] **Step 4: 最终提交（如有必要）**

如果验证中发现小问题并修复：

```bash
git add -A
git commit -m "fix: 统计页面优化最终修复"
```
