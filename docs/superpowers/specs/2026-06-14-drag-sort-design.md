# 自定义类目和支付方式拖拽排序功能设计文档

**日期：** 2026-06-14
**Issue：** [#24](https://github.com/Samoy/chuan-bill/issues/24)
**状态：** 设计完成

## 1. 概述

### 1.1 需求背景

用户需要对自定义类目和支付方式进行拖拽排序，以满足个性化需求。当前系统仅支持固定的排序方式，无法满足用户的自定义排序需求。

### 1.2 核心需求

1. **预设条目**：置灰显示，不可拖拽，自定义条目不能拖到预设条目前面
2. **自定义条目**：可拖拽排序，支持浮动预览和占位符效果
3. **新增按钮**：固定在最后，不可拖拽，自定义条目不能拖到新增按钮后面
4. **编辑模式**：进入编辑模式后才能进行拖拽排序
5. **提示信息**：在区域顶部右侧弱提示可进行拖拽排序

### 1.3 技术约束

- 必须兼容微信小程序平台（主推平台）
- 后端 API 已支持排序功能，仅需前端改造
- 使用自定义触摸事件实现，确保跨平台兼容性

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────┐
│      GridPickerPopup.vue            │
│         (UI 渲染层)                 │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│         useDragSort.ts              │
│        (拖拽逻辑层)                 │
├─────────────────────────────────────┤
│  • 触摸事件处理                      │
│  • 浮动预览和占位符管理               │
│  • 目标位置计算                      │
│  • 约束规则实现                      │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│           BillStore                 │
│         (状态管理层)                 │
└─────────────────────────────────────┘
```

### 2.2 组件职责

#### GridPickerPopup.vue
- UI 渲染和布局
- 调用 useDragSort 组合式函数
- 传递配置参数
- 处理排序完成事件

#### useDragSort.ts
- 触摸事件监听和处理
- 浮动预览和占位符管理
- 目标位置计算
- 约束规则实现
- 排序结果计算

## 3. 详细设计

### 3.1 useDragSort 组合式函数

#### 3.1.1 函数签名

```typescript
interface DragSortOptions<T> {
  items: Ref<T[]>                    // 可排序的项目列表
  presetCount: Ref<number>          // 预设项目数量（不可拖拽）
  hasAddButton: Ref<boolean>        // 是否有新增按钮
  onSortEnd: (newOrder: string[]) => Promise<void>  // 排序完成回调
  disabled?: Ref<boolean>           // 是否禁用拖拽
}

interface DragSortReturn<T> {
  dragState: Readonly<Ref<DragState>>    // 拖拽状态
  getItemProps: (index: number) => {     // 获取项目属性
    onTouchStart: (e: TouchEvent) => void
    style: Record<string, string>
    class: string[]
  }
  containerProps: {                      // 容器属性
    onTouchMove: (e: TouchEvent) => void
    onTouchEnd: () => void
  }
}

interface DragState {
  isDragging: boolean                   // 是否正在拖拽
  dragIndex: number                     // 被拖拽的项目索引
  targetIndex: number                   // 目标位置索引
  position: { x: number; y: number }    // 手指位置
  offset: { x: number; y: number }      // 触摸偏移
  elementRect: {                        // 被拖拽元素尺寸
    width: number
    height: number
  }
}
```

#### 3.1.2 核心方法

**触摸事件处理：**
- `handleTouchStart(e, index)`：记录初始触摸位置，检查是否可拖拽，初始化拖拽状态
- `handleTouchMove(e)`：更新浮动预览位置，计算目标位置，应用磁吸约束
- `handleTouchEnd()`：计算最终排序，调用 onSortEnd 回调，重置拖拽状态

**位置计算算法：**
- `calculateTargetIndex(touchY)`：获取所有项目的位置信息，计算触摸点所在的网格行，考虑网格列数（3列）
- `applyConstraints(index)`：确保不小于 presetCount，确保不大于 items.length - 1，磁吸效果：越界时弹回

#### 3.1.3 约束规则实现

```typescript
function applyMagneticEffect(targetIndex: number): number {
  const minIndex = presetCount.value  // 预设条目之后
  const maxIndex = items.value.length - 1  // 新增按钮之前

  if (targetIndex < minIndex) {
    // 磁吸效果：自动弹回
    return minIndex
  }
  if (targetIndex > maxIndex) {
    // 磁吸效果：自动弹回
    return maxIndex
  }
  return targetIndex
}
```

### 3.2 UI 交互设计

#### 3.2.1 交互流程

1. **进入编辑模式**：点击编辑图标
2. **显示提示**：右上角弱提示"长按可拖拽排序"
3. **长按拖拽**：触发拖拽排序
4. **松手提交**：保存新顺序

#### 3.2.2 视觉反馈

| 元素 | 状态 | 视觉表现 |
|------|------|----------|
| 预设条目 | 始终禁用 | 灰色背景，透明度 0.6 |
| 自定义条目 | 可拖拽 | 蓝色背景，手型光标 |
| 被拖拽条目 | 拖拽中 | 浮动预览，阴影效果 |
| 目标位置 | 占位符 | 灰色虚线边框 |
| 新增按钮 | 始终禁用 | 绿色背景，透明度 0.6 |

#### 3.2.3 拖拽状态视觉变化

- **浮动预览**：被拖拽条目跟随手指移动，添加阴影效果（无旋转）
- **占位符**：目标位置显示半透明占位符，灰色虚线边框
- **磁吸效果**：拖拽到预设区域或新增按钮后面时自动弹回

### 3.3 数据流设计

#### 3.3.1 数据流概览

```
触摸事件 (touchstart)
    ↓
拖拽状态 (dragState)
    ↓
位置计算 (targetIndex)
    ↓
UI 更新 (占位符)
    ↓
松手触发
    ↓
计算新顺序 (reorderItems)
    ↓
提取 ID 列表 (idList)
    ↓
调用 API (sortCategories)
    ↓
刷新列表 (emit事件)
```

#### 3.3.2 关键数据结构

**拖拽状态 (DragState)：**
```typescript
interface DragState {
  isDragging: boolean
  dragIndex: number      // 被拖拽项原始索引
  targetIndex: number    // 目标位置索引
  position: {            // 手指当前位置
    x: number
    y: number
  }
  offset: {              // 触摸点偏移量
    x: number
    y: number
  }
  elementRect: {         // 被拖拽元素尺寸
    width: number
    height: number
  }
}
```

**排序结果：**
```typescript
// 排序前
items = [
  { id: 'preset1', isDefault: true },
  { id: 'preset2', isDefault: true },
  { id: 'custom1', isDefault: false },
  { id: 'custom2', isDefault: false },
  { id: 'custom3', isDefault: false },
]

// 拖拽 custom1 到 custom3 后面
// 排序后（仅自定义部分变化）
newOrder = [
  'custom2',
  'custom3',
  'custom1',
]

// 调用 API
await sortCategories(newOrder)
```

#### 3.3.3 API 调用说明

**类目排序：**
```
POST /api/category/sort
Body: { "ids": ["custom2", "custom3", "custom1"] }
```

**支付方式排序：**
```
POST /api/payment-method/sort
Body: { "ids": ["custom2", "custom3", "custom1"] }
```

**说明：** API 仅接收自定义条目的 ID 列表，预设条目不参与排序。

## 4. 错误处理与边界情况

### 4.1 错误处理策略

#### 4.1.1 网络错误
- **场景**：API 调用失败
- **处理**：显示错误提示 Toast，保持当前排序不变，不退出编辑模式
- **用户操作**：可以重试操作

#### 4.1.2 并发操作
- **场景**：快速连续拖拽
- **处理**：使用防抖机制（300ms），最后一次拖拽生效，避免重复 API 调用

### 4.2 边界情况处理

| 场景 | 处理方式 |
|------|----------|
| 只有 0-1 个自定义条目 | 禁用拖拽功能，不显示提示 |
| 拖拽超出容器边界 | 限制在容器范围内，磁吸效果 |
| 拖拽到原位置 | 不触发 API 调用 |
| 触摸点在项目间隙 | 取最近的项目作为目标 |
| 编辑模式退出时拖拽中 | 取消拖拽，恢复原状 |
| 弹窗关闭时拖拽中 | 取消拖拽，恢复原状 |

### 4.3 用户反馈机制

**成功反馈：**
- 显示简短的 Toast 提示："排序已保存"
- 条目平滑移动到新位置

**失败反馈：**
- 显示错误提示："排序保存失败，请重试"
- 保持原有排序不变

## 5. 实现计划

### 5.1 文件结构

```
chuan-bill-app/src/
├── composables/
│   └── useDragSort.ts          # 拖拽排序组合式函数
├── pages/bill/components/
│   └── GridPickerPopup.vue     # 修改：集成拖拽排序功能
└── types/
    └── drag-sort.ts            # 拖拽排序类型定义
```

### 5.2 实现步骤

1. **创建类型定义** (`types/drag-sort.ts`)
   - 定义 DragSortOptions、DragSortReturn、DragState 等接口

2. **实现组合式函数** (`composables/useDragSort.ts`)
   - 实现触摸事件处理
   - 实现位置计算算法
   - 实现约束规则
   - 实现排序结果计算

3. **修改 GridPickerPopup.vue**
   - 集成 useDragSort 组合式函数
   - 添加拖拽相关的 UI 元素
   - 处理排序完成事件

4. **测试与优化**
   - 测试各种边界情况
   - 优化性能和用户体验

## 6. 验收标准

### 6.1 功能验收

- [ ] 预设条目置灰显示，不可拖拽
- [ ] 自定义条目可拖拽排序
- [ ] 浮动预览和占位符效果正常
- [ ] 磁吸效果正常（不能拖到预设条目前面和新增按钮后面）
- [ ] 新增按钮固定在最后，不可拖拽
- [ ] 编辑模式下显示拖拽提示
- [ ] 松手后调用 API 保存排序
- [ ] API 调用失败时显示错误提示

### 6.2 性能验收

- [ ] 拖拽过程流畅，无卡顿
- [ ] 防抖机制正常工作
- [ ] 内存泄漏检查通过

### 6.3 兼容性验收

- [ ] 微信小程序平台正常工作
- [ ] H5 平台正常工作
- [ ] 不同屏幕尺寸适配正常
