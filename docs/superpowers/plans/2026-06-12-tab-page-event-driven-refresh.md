# Tab页面事件驱动刷新实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现Tab页面事件驱动刷新，移除onShow中的接口请求，改用EventBus事件通知机制

**Architecture:** 使用EventBus事件总线，Tab页面监听数据变化事件，数据变化页面发送事件通知

**Tech Stack:** Vue 3 Composition API, TypeScript, Pinia, uni-app

---

## 文件结构

### 新增文件
- `src/utils/eventBus.ts` - EventBus事件总线工具类
- `src/constant/events.ts` - 事件常量定义

### 修改文件
- `src/pages/family/index.vue` - 移除onShow请求，添加事件监听
- `src/pages/statistics/index.vue` - 移除onShow请求，添加事件监听
- `src/pages/mine/index.vue` - 移除onShow请求，添加事件监听
- `src/pages/bill/index.vue` - 添加事件监听（保持onLoad请求）
- `src/pages/bill/components/QuickBillModal.vue` - 添加事件发送
- `src/pages/bill/components/BillDetailModal.vue` - 添加事件发送
- `src/pages/family/edit.vue` - 添加事件发送
- `src/pages/family/detail.vue` - 添加事件发送
- `src/pages/mine/profile.vue` - 添加事件发送

---

## Task 1: 创建EventBus工具类

**Files:**
- Create: `src/utils/eventBus.ts`

- [ ] **Step 1: 创建EventBus类**

```typescript
// src/utils/eventBus.ts
type EventHandler = (...args: any[]) => void

class EventBus {
  private events: Map<string, Set<EventHandler>> = new Map()

  on(event: string, handler: EventHandler) {
    if (!this.events.has(event)) {
      this.events.set(event, new Set())
    }
    this.events.get(event)!.add(handler)
  }

  off(event: string, handler: EventHandler) {
    this.events.get(event)?.delete(handler)
  }

  emit(event: string, ...args: any[]) {
    this.events.get(event)?.forEach(handler => handler(...args))
  }
}

export const eventBus = new EventBus()
```

- [ ] **Step 2: 验证文件创建成功**

Run: `cat src/utils/eventBus.ts`
Expected: 显示EventBus类代码

- [ ] **Step 3: 提交代码**

```bash
git add src/utils/eventBus.ts
git commit -m "feat(utils): 添加EventBus事件总线工具类"
```

---

## Task 2: 创建事件常量定义

**Files:**
- Create: `src/constant/events.ts`

- [ ] **Step 1: 创建事件常量文件**

```typescript
// src/constant/events.ts
export const EVENTS = {
  BILL: {
    UPDATED: 'bill:updated',
    CREATED: 'bill:created',
    DELETED: 'bill:deleted',
  },
  FAMILY: {
    UPDATED: 'family:updated',
    MEMBER_CHANGED: 'family:member:changed',
  },
  STATISTICS: {
    UPDATED: 'statistics:updated',
  },
  USER: {
    UPDATED: 'user:updated',
  },
} as const
```

- [ ] **Step 2: 验证文件创建成功**

Run: `cat src/constant/events.ts`
Expected: 显示EVENTS常量定义

- [ ] **Step 3: 提交代码**

```bash
git add src/constant/events.ts
git commit -m "feat(constant): 添加事件常量定义"
```

---

## Task 3: 修改family/index.vue - 移除onShow请求，添加事件监听

**Files:**
- Modify: `src/pages/family/index.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 移除onShow中的数据请求**

将：
```typescript
// 页面加载时获取数据
onShow(async () => {
  if (user.isLoggedIn) {
    await familyStore.fetchFamilyList()
    await messageStore.fetchUnreadCount()
  }
})
```

改为：
```typescript
// 页面加载时获取数据（仅首次）
onLoad(() => {
  if (user.isLoggedIn) {
    familyStore.fetchFamilyList()
    messageStore.fetchUnreadCount()
  }
})
```

- [ ] **Step 3: 添加事件监听**

在`<script setup lang="ts">`部分添加：

```typescript
// 监听家庭数据变化事件
function handleFamilyUpdated() {
  if (user.isLoggedIn) {
    familyStore.fetchFamilyList()
    messageStore.fetchUnreadCount()
  }
}

onMounted(() => {
  eventBus.on(EVENTS.FAMILY.UPDATED, handleFamilyUpdated)
  eventBus.on(EVENTS.FAMILY.MEMBER_CHANGED, handleFamilyUpdated)
})

onUnmounted(() => {
  eventBus.off(EVENTS.FAMILY.UPDATED, handleFamilyUpdated)
  eventBus.off(EVENTS.FAMILY.MEMBER_CHANGED, handleFamilyUpdated)
})
```

- [ ] **Step 4: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 5: 提交代码**

```bash
git add src/pages/family/index.vue
git commit -m "refactor(family): 移除onShow请求，改用事件驱动刷新"
```

---

## Task 4: 修改statistics/index.vue - 移除onShow请求，添加事件监听

**Files:**
- Modify: `src/pages/statistics/index.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 移除onShow中的数据请求**

将：
```typescript
onShow(() => {
  nextTick(() => {
    statisticsStore.setAnalysisContext(AiSuggestionType.USER)
    statisticsStore.fetchAll(currentMonth.value)
    statisticsStore.fetchAiSuggestionCached(AiSuggestionType.USER, currentMonth.value)
    if (user.isLoggedIn) {
      budgetStore.fetchBudget(currentMonth.value)
    }
  })
})
```

改为：
```typescript
// 首次加载时获取数据
onLoad(() => {
  statisticsStore.setAnalysisContext(AiSuggestionType.USER)
  statisticsStore.fetchAll(currentMonth.value)
  statisticsStore.fetchAiSuggestionCached(AiSuggestionType.USER, currentMonth.value)
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(currentMonth.value)
  }
})
```

- [ ] **Step 3: 添加事件监听**

在`<script setup lang="ts">`部分添加：

```typescript
// 监听账单和家庭数据变化事件
function handleDataUpdated() {
  statisticsStore.fetchAll(currentMonth.value)
  statisticsStore.fetchAiSuggestionCached(AiSuggestionType.USER, currentMonth.value)
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(currentMonth.value)
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
```

- [ ] **Step 4: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 5: 提交代码**

```bash
git add src/pages/statistics/index.vue
git commit -m "refactor(statistics): 移除onShow请求，改用事件驱动刷新"
```

---

## Task 5: 修改mine/index.vue - 移除onShow请求，添加事件监听

**Files:**
- Modify: `src/pages/mine/index.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 移除onShow中的数据请求**

将：
```typescript
// 页面显示时获取未读消息数
onShow(() => {
  if (user.isLoggedIn) {
    messageStore.fetchUnreadCount()
  }
})
```

改为：
```typescript
// 首次加载时获取未读消息数
onLoad(() => {
  if (user.isLoggedIn) {
    messageStore.fetchUnreadCount()
  }
})
```

- [ ] **Step 3: 添加事件监听**

在`<script setup lang="ts">`部分添加：

```typescript
// 监听用户和账单数据变化事件
function handleDataUpdated() {
  if (user.isLoggedIn) {
    messageStore.fetchUnreadCount()
  }
}

onMounted(() => {
  eventBus.on(EVENTS.USER.UPDATED, handleDataUpdated)
  eventBus.on(EVENTS.BILL.UPDATED, handleDataUpdated)
})

onUnmounted(() => {
  eventBus.off(EVENTS.USER.UPDATED, handleDataUpdated)
  eventBus.off(EVENTS.BILL.UPDATED, handleDataUpdated)
})
```

- [ ] **Step 4: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 5: 提交代码**

```bash
git add src/pages/mine/index.vue
git commit -m "refactor(mine): 移除onShow请求，改用事件驱动刷新"
```

---

## Task 6: 修改bill/index.vue - 添加事件监听

**Files:**
- Modify: `src/pages/bill/index.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 添加事件监听**

在`<script setup lang="ts">`部分添加：

```typescript
// 监听家庭数据变化事件（家庭账单相关）
function handleFamilyUpdated() {
  refresh()
}

onMounted(() => {
  eventBus.on(EVENTS.FAMILY.UPDATED, handleFamilyUpdated)
})

onUnmounted(() => {
  eventBus.off(EVENTS.FAMILY.UPDATED, handleFamilyUpdated)
})
```

- [ ] **Step 3: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 4: 提交代码**

```bash
git add src/pages/bill/index.vue
git commit -m "refactor(bill): 添加家庭数据变化事件监听"
```

---

## Task 7: 修改QuickBillModal.vue - 添加事件发送

**Files:**
- Modify: `src/pages/bill/components/QuickBillModal.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 在成功回调中添加事件发送**

找到添加/修改账单成功的回调函数，在成功后添加：

```typescript
eventBus.emit(EVENTS.BILL.UPDATED)
```

- [ ] **Step 3: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 4: 提交代码**

```bash
git add src/pages/bill/components/QuickBillModal.vue
git commit -m "refactor(bill): 添加账单更新事件发送"
```

---

## Task 8: 修改BillDetailModal.vue - 添加事件发送

**Files:**
- Modify: `src/pages/bill/components/BillDetailModal.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 在删除成功回调中添加事件发送**

找到删除账单成功的回调函数，在成功后添加：

```typescript
eventBus.emit(EVENTS.BILL.UPDATED)
```

- [ ] **Step 3: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 4: 提交代码**

```bash
git add src/pages/bill/components/BillDetailModal.vue
git commit -m "refactor(bill): 添加账单删除事件发送"
```

---

## Task 9: 修改family/edit.vue - 添加事件发送

**Files:**
- Modify: `src/pages/family/edit.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 在成功回调中添加事件发送**

找到创建/编辑家庭成功的回调函数，在成功后添加：

```typescript
eventBus.emit(EVENTS.FAMILY.UPDATED)
```

- [ ] **Step 3: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 4: 提交代码**

```bash
git add src/pages/family/edit.vue
git commit -m "refactor(family): 添加家庭更新事件发送"
```

---

## Task 10: 修改family/detail.vue - 添加事件发送

**Files:**
- Modify: `src/pages/family/detail.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 在成员变化成功回调中添加事件发送**

找到成员变化（添加/移除/退出）成功的回调函数，在成功后添加：

```typescript
eventBus.emit(EVENTS.FAMILY.MEMBER_CHANGED)
```

- [ ] **Step 3: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 4: 提交代码**

```bash
git add src/pages/family/detail.vue
git commit -m "refactor(family): 添加家庭成员变化事件发送"
```

---

## Task 11: 修改mine/profile.vue - 添加事件发送

**Files:**
- Modify: `src/pages/mine/profile.vue`

- [ ] **Step 1: 添加事件导入**

在`<script setup lang="ts">`部分添加导入：

```typescript
import { eventBus } from '@/utils/eventBus'
import { EVENTS } from '@/constant/events'
```

- [ ] **Step 2: 在成功回调中添加事件发送**

找到修改个人信息成功的回调函数，在成功后添加：

```typescript
eventBus.emit(EVENTS.USER.UPDATED)
```

- [ ] **Step 3: 验证修改正确**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 4: 提交代码**

```bash
git add src/pages/mine/profile.vue
git commit -m "refactor(mine): 添加用户信息更新事件发送"
```

---

## Task 12: 整体验证与测试

**Files:**
- 无新增/修改文件

- [ ] **Step 1: 运行类型检查**

Run: `pnpm type-check`
Expected: 无类型错误

- [ ] **Step 2: 运行lint检查**

Run: `pnpm lint`
Expected: 无lint错误

- [ ] **Step 3: 启动开发服务器测试**

Run: `pnpm dev`
Expected: 开发服务器启动成功

- [ ] **Step 4: 手动测试验证**

1. 测试Tab切换是否还有loading
2. 测试添加账单后统计页面是否自动刷新
3. 测试创建家庭后家庭列表是否自动刷新
4. 测试首次加载是否正常

- [ ] **Step 5: 提交最终代码**

```bash
git add .
git commit -m "feat: 完成Tab页面事件驱动刷新优化"
```

---

## 验收标准

1. ✅ Tab切换时不再有loading请求
2. ✅ 数据变化时相关Tab页面自动刷新
3. ✅ 首次加载功能正常
4. ✅ 无TypeScript类型错误
5. ✅ 无ESLint错误
6. ✅ 所有功能正常工作
