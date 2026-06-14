# Tab页面事件驱动刷新设计方案

## 背景

当前Tab页面（bill、family、statistics、mine）在每次`onShow`时都会请求接口，导致：
- 用户频繁切换Tab时产生大量不必要的网络请求
- 用户体验不佳（每次切换都有loading）
- 浪费流量和服务器资源

## 目标

- Tab页面不再在`onShow`时自动请求接口
- 数据变化时通过事件通知相关Tab页面刷新
- 保持功能完整性，不影响现有业务逻辑

## 技术方案

### 架构设计

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   账单页面      │    │   家庭页面      │    │   统计页面      │
│   (数据变化)    │    │   (数据变化)    │    │   (数据变化)    │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                      │                      │
         ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EventBus (事件总线)                          │
│  - bill:updated      - family:updated      - statistics:updated │
└─────────────────────────────────────────────────────────────────┘
         │                      │                      │
         ▼                      ▼                      ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   统计页面      │    │   账单页面      │    │   账单页面      │
│   (监听刷新)    │    │   (监听刷新)    │    │   (监听刷新)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 1. EventBus工具类

**文件位置**: `src/utils/eventBus.ts`

```typescript
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

### 2. 事件常量定义

**文件位置**: `src/constant/events.ts`

```typescript
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

### 3. Tab页面修改

#### bill/index.vue
- **当前逻辑**: `onLoad`时调用`refresh()`
- **修改后**: 保持`onLoad`时请求（首次加载必须请求）
- **新增**: 监听`family:updated`事件刷新（家庭账单相关）

#### family/index.vue
- **当前逻辑**: `onShow`时调用`familyStore.fetchFamilyList()`和`messageStore.fetchUnreadCount()`
- **修改后**: 移除`onShow`中的数据请求
- **新增**: 监听`family:updated`和`family:member:changed`事件

#### statistics/index.vue
- **当前逻辑**: `onShow`时调用`statisticsStore.fetchAll()`
- **修改后**: 移除`onShow`中的数据请求
- **新增**: 监听`bill:updated`和`family:updated`事件

#### mine/index.vue
- **当前逻辑**: `onShow`时调用`messageStore.fetchUnreadCount()`
- **修改后**: 移除`onShow`中的数据请求
- **新增**: 监听`user:updated`和`bill:updated`事件

### 4. 数据变化页面修改

#### 账单相关页面
- `bill/QuickBillModal.vue`: 添加/修改账单后发送`bill:updated`
- `bill/BillDetailModal.vue`: 删除账单后发送`bill:updated`

#### 家庭相关页面
- `family/edit.vue`: 创建/编辑家庭后发送`family:updated`
- `family/detail.vue`: 成员变化后发送`family:member:changed`

#### 用户相关页面
- `mine/profile.vue`: 修改个人信息后发送`user:updated`

## 实现步骤

### 第一阶段：基础设施
1. 创建`src/utils/eventBus.ts`
2. 创建`src/constant/events.ts`

### 第二阶段：Tab页面改造
1. 修改`family/index.vue` - 移除onShow请求，添加事件监听
2. 修改`statistics/index.vue` - 移除onShow请求，添加事件监听
3. 修改`mine/index.vue` - 移除onShow请求，添加事件监听
4. 修改`bill/index.vue` - 添加事件监听（保持onLoad请求）

### 第三阶段：数据变化页面改造
1. 修改`bill/QuickBillModal.vue` - 添加事件发送
2. 修改`bill/BillDetailModal.vue` - 添加事件发送
3. 修改`family/edit.vue` - 添加事件发送
4. 修改`family/detail.vue` - 添加事件发送
5. 修改`mine/profile.vue` - 添加事件发送

### 第四阶段：测试验证
1. 测试Tab切换是否还有loading
2. 测试数据变化后Tab页面是否正确刷新
3. 测试首次加载是否正常

## 风险与注意事项

1. **首次加载**: Tab页面首次加载时仍需请求数据，不能移除`onLoad`中的请求
2. **内存泄漏**: 必须在`onUnmounted`中取消事件监听
3. **事件命名**: 事件名称需统一规范，避免冲突
4. **向后兼容**: 不影响现有业务逻辑，只是优化刷新时机

## 预期效果

- Tab切换时不再有loading请求
- 数据变化时相关Tab页面自动刷新
- 用户体验提升，流量消耗减少
