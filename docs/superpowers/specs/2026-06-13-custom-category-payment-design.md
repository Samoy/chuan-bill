# 自定义账单类目和支付方式功能设计

## 概述

将添加/编辑账单时的类目和支付方式选择器从 `wd-picker`（纯文字下拉）升级为 `wd-action-sheet` + 胶囊网格选择器，支持用户自定义类目和支付方式的增删改及拖拽排序。

**Issue:** [GitHub #10](https://github.com/Samoy/chuan-bill/issues/10)

## 现状分析

### 前端
- `ManualEdit.vue` 使用 `wd-picker`（纯文字下拉选择器）选择类目和支付方式
- `FilterModal.vue` 已有胶囊网格模式（`wd-radio-group shape="button"` + 3列grid + 图标）
- `wd-action-sheet` 在项目中已广泛使用（10+ 个文件）

### 后端
- 数据库 `t_category` 和 `t_payment_method` 表已支持用户自定义（`user_id` 字段）
- 仅有读取接口（`GET /bill/categories`、`GET /bill/payment-methods`），无增删改 API
- 系统预设：`user_id = NULL, is_default = 1`；用户自定义：`user_id = 当前用户`

### 数据模型
- 类目：按 type（expense/income）分组，与账单类型关联
- 支付方式：不区分类型，收入支出共用
- "其他"类目和"其他"支付方式为系统预设兜底项

## 设计方案

### 组件架构

创建 `GridPickerPopup.vue` 可复用组件，封装 `wd-action-sheet` + 胶囊网格 + 编辑模式 + 增删改拖拽排序全部逻辑。类目和支付方式选择器共用同一组件，通过 props 区分。

**组件接口：**

```ts
// GridPickerPopup.vue Props
interface GridPickerPopupProps {
  modelValue: string                    // 当前选中的 ID
  title: string                         // 弹框标题（"选择类目" / "选择支付方式"）
  items: GridPickerItem[]               // 所有选项（预设 + 自定义）
  type?: 'expense' | 'income'           // 仅类目需要，用于新增时指定类型
  entity: 'category' | 'paymentMethod'  // 决定调用哪个 API
  showOthers?: boolean                  // 是否显示"其他"项（默认 false）
}

// GridPickerPopup.vue Emits
interface GridPickerPopupEmits {
  'update:modelValue': [value: string]
  'change': [value: string]
  'itemsUpdated': []                    // 列表变化后通知父组件刷新 store
}

// GridPickerItem 类型
interface GridPickerItem {
  id: string
  name: string
  icon: string
  isDefault: boolean
  sortOrder: number
}
```

**ManualEdit.vue 中的使用方式：**

```html
<GridPickerPopup
  v-model="formData.categoryId"
  :items="categoryItems"
  title="选择类目"
  entity="category"
  :type="formData.type"
  @change="validateField('categoryId')"
/>

<GridPickerPopup
  v-model="formData.paymentMethodId"
  :items="paymentMethodItems"
  title="选择支付方式"
  entity="paymentMethod"
  @change="validateField('paymentMethodId')"
/>
```

### UI 布局与交互

#### 普通模式（选择状态）

```
┌─────────────────────────────────────┐
│  选择类目              [编辑] [关闭] │  ← ActionSheet 标题栏
├─────────────────────────────────────┤
│                                     │
│  ┌─────┐ ┌─────┐ ┌─────┐          │
│  │ 🍜  │ │ 🛒  │ │ 🚗  │          │  ← 预设类目（3列网格）
│  │餐饮  │ │购物  │ │交通  │          │
│  └─────┘ └─────┘ └─────┘          │
│  ... 更多预设类目 ...                │
│  ─ ─ ─ ─ 自定义类目 ─ ─ ─ ─ ─ ─ ─  │  ← 分隔线 + 标签（仅有自定义项时显示）
│  ┌─────┐ ┌─────┐ ┌─────┐          │
│  │ 💰  │ │ 🎁  │ │ ➕  │          │  ← 自定义 + 新增按钮
│  │奶茶  │ │宠物  │ │新增  │          │
│  └─────┘ └─────┘ └─────┘          │
│                                     │
└─────────────────────────────────────┘
```

- 预设类目按 `sort_order` 固定排序
- "其他"类目/支付方式在记账弹框中**不显示**（仅在数据库和 AI 层面保留）
- 自定义类目在预设之后，支持选择
- 末尾显示"新增"按钮（带 `+` 图标）

#### 编辑模式（点击编辑按钮后）

```
┌─────────────────────────────────────┐
│  编辑类目              [完成]        │  ← 标题变为"编辑"，关闭变为"完成"
├─────────────────────────────────────┤
│                                     │
│  ┌─────┐ ┌─────┐ ┌─────┐          │
│  │ 🍜  │ │ 🛒  │ │ 🚗  │          │  ← 预设类目（不可操作）
│  │餐饮  │ │购物  │ │交通  │          │
│  │ 🔒  │ │ 🔒  │ │ 🔒  │          │  ← 锁定图标
│  └─────┘ └─────┘ └─────┘          │
│  ... 预设类目 ...                    │
│  ─ ─ ─ ─ 自定义类目 ─ ─ ─ ─ ─ ─ ─  │
│  ┌─────┐ ┌─────┐                   │
│  │ 💰  │ │ 🎁  │                   │  ← 自定义类目（可操作）
│  │奶茶  │ │宠物  │                   │
│  │✏️ 🗑│ │✏️ 🗑│                   │  ← 编辑/删除小图标
│  └─────┘ └─────┘                   │  ← 支持拖拽排序
│                                     │
└─────────────────────────────────────┘
```

- 预设项显示锁定图标，不可编辑/删除/拖拽
- 自定义项显示编辑和删除小图标
- 自定义项支持拖拽排序
- "新增"按钮在编辑模式下仍然可用
- 点击"完成"退出编辑模式并保存排序

#### 新增/编辑子表单

```
┌─────────────────────────────────────┐
│  新增类目                  [取消]    │
├─────────────────────────────────────┤
│                                     │
│  名称：[________________]           │  ← 输入框（最多 8 个字符）
│                                     │
│  图标：                             │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐          │
│  │🍜 │ │🛒 │ │🚗 │ │🎮 │          │  ← 图标网格（4-6列，可滚动）
│  └───┘ └───┘ └───┘ └───┘          │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐          │
│  │🏠 │ │📱 │ │🏥 │ │📚 │          │
│  └───┘ └───┘ └───┘ └───┘          │
│  ...更多图标...                     │
│                                     │
│         [ 保  存 ]                  │  ← 保存按钮
│                                     │
└─────────────────────────────────────┘
```

- 使用条件渲染切换视图：ActionSheet 内部通过 `v-if` 在网格视图和表单视图之间切换（不使用嵌套 ActionSheet）
- 名称输入框限制最大 8 个字符
- 图标选择器展示常用 UnoCSS 图标（约 40-50 个 `i-lucide:*` 图标），按分类平铺展示，不分组
- 编辑模式复用同一表单，预填充现有数据

#### "其他"类目处理

- 记账弹框（ManualEdit）：网格中**隐藏**"其他"预设项（`showOthers = false`），包括"其他支出"、"其他收入"、"其他"支付方式
- 筛选弹框（FilterModal）：**保留**"其他"项，排序调整为：系统预设（不含"其他"）→ 用户自定义 → "其他"（最末尾）
- 实现方式：在 `billStore` 中新增 getter 方法，对 FilterModal 使用的列表进行排序，将 `isDefault === true` 且名称包含"其他"的项移到末尾

#### 访客模式

- 未登录用户：`GridPickerPopup` 隐藏编辑按钮，仅作为胶囊网格选择器
- 未登录时点击"新增"按钮：提示用户先登录
- 访客模式下使用 `LOCAL_PAY_CATEGORY_LIST` / `LOCAL_PAYMENT_METHOD_LIST` 常量数据

### 后端 API 设计

#### 新增端点

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | `/bill/categories` | 新增自定义类目 | `AddCategoryDTO` |
| PUT | `/bill/categories/{id}` | 更新自定义类目 | `UpdateCategoryDTO` |
| DELETE | `/bill/categories/{id}` | 删除自定义类目 | — |
| PUT | `/bill/categories/sort` | 批量更新类目排序 | `SortDTO` |
| POST | `/bill/payment-methods` | 新增自定义支付方式 | `AddPaymentMethodDTO` |
| PUT | `/bill/payment-methods/{id}` | 更新自定义支付方式 | `UpdatePaymentMethodDTO` |
| DELETE | `/bill/payment-methods/{id}` | 删除自定义支付方式 | — |
| PUT | `/bill/payment-methods/sort` | 批量更新支付方式排序 | `SortDTO` |

#### DTO 定义

```java
// 新增类目
public class AddCategoryDTO {
    @NotBlank(message = "类目名称不能为空")
    private String name;
    @NotBlank(message = "类目图标不能为空")
    private String icon;
    @NotBlank(message = "类目类型不能为空")
    private String type;  // expense / income
}

// 更新类目
public class UpdateCategoryDTO {
    private String name;
    private String icon;
}

// 新增支付方式
public class AddPaymentMethodDTO {
    @NotBlank(message = "支付方式名称不能为空")
    private String name;
    @NotBlank(message = "支付方式图标不能为空")
    private String icon;
}

// 更新支付方式
public class UpdatePaymentMethodDTO {
    private String name;
    private String icon;
}

// 排序
public class SortDTO {
    @NotEmpty(message = "排序列表不能为空")
    private List<String> ids;
}
```

#### 业务约束

1. **权限控制：** 只能操作 `user_id = 当前用户` 的记录，系统预设（`is_default = true`）不可修改/删除
2. **删除保护：** 删除前检查 `t_bill` 表中是否引用了该 ID，有则返回错误
3. **新增默认值：** 自动设置 `user_id`、`is_default = false`、`sort_order = max + 1`
4. **排序接口：** 接收排序后的 ID 数组，按数组顺序更新 `sort_order`（从当前最大值 +1 开始递增）

#### 新增错误码

```java
// ResultEnum 新增
CATEGORY_HAS_BILLS(1001, "该类目下存在账单，无法删除"),
PAYMENT_METHOD_HAS_BILLS(1002, "该支付方式下存在账单，无法删除"),
CATEGORY_NOT_FOUND(1003, "类目不存在"),
PAYMENT_METHOD_NOT_FOUND(1004, "支付方式不存在"),
CANNOT_MODIFY_DEFAULT(1005, "系统预设不可修改"),
```

### 数据流

#### billStore.ts 改造

新增方法（登录后使用）：

```ts
// 类目 CRUD
async function addCategory(data: AddCategoryDTO): Promise<void>
async function updateCategory(id: string, data: UpdateCategoryDTO): Promise<void>
async function deleteCategory(id: string): Promise<void>
async function sortCategory(ids: string[]): Promise<void>

// 支付方式 CRUD
async function addPaymentMethod(data: AddPaymentMethodDTO): Promise<void>
async function updatePaymentMethod(id: string, data: UpdatePaymentMethodDTO): Promise<void>
async function deletePaymentMethod(id: string): Promise<void>
async function sortPaymentMethod(ids: string[]): Promise<void>
```

每个 CRUD 操作完成后自动调用 `fetchCategoryList()` / `fetchPaymentMethodList()` 刷新列表。

新增 getter（供 FilterModal 使用）：

```ts
// 返回排序后的列表，"其他"项在末尾
function getCategoryListForFilter(type?: string): CategoryVO[]
function getPaymentMethodListForFilter(): PaymentMethodVO[]
```

#### 前端 API 定义（apiDefinitions.ts 新增）

```
bill.addCategory: ['POST', '/bill/categories']
bill.updateCategory: ['PUT', '/bill/categories/{id}']
bill.deleteCategory: ['DELETE', '/bill/categories/{id}']
bill.sortCategories: ['PUT', '/bill/categories/sort']
bill.addPaymentMethod: ['POST', '/bill/payment-methods']
bill.updatePaymentMethod: ['PUT', '/bill/payment-methods/{id}']
bill.deletePaymentMethod: ['DELETE', '/bill/payment-methods/{id}']
bill.sortPaymentMethods: ['PUT', '/bill/payment-methods/sort']
```

前端 `alova-gen` 后需遵循项目的 `alova-api-fix` skill 约定手动修复类型定义。

### 拖拽排序实现

使用 uni-app 原生的 `movable-area` + `movable-view` 实现拖拽排序，不引入额外依赖。

**交互流程：**
1. 进入编辑模式后，自定义项变为可拖拽状态
2. 长按胶囊触发拖拽，拖拽过程中其他项自动重排
3. 松手后调用 `sortCategory()` / `sortPaymentMethod()` 保存新顺序
4. 预设项不可拖拽（锁定状态）

### 图标选择器

维护一个常用图标列表（约 40-50 个 `i-lucide:*` 图标），覆盖记账常见场景。

- 以 4-6 列网格展示，支持滚动
- 点击选中，高亮显示
- 图标列表可以按分类分组（餐饮、交通、购物等）
- 图标数据在前端常量文件中定义，不从后端获取

## 影响范围

### 前端文件变更

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `src/pages/bill/components/GridPickerPopup.vue` | 新增 | 核心可复用组件 |
| `src/pages/bill/components/ManualEdit.vue` | 修改 | 替换 wd-picker 为 GridPickerPopup |
| `src/store/billStore.ts` | 修改 | 新增 CRUD 方法 |
| `src/api/apiDefinitions.ts` | 修改 | 新增 8 个 API 端点 |
| `src/api/globals.d.ts` | 修改 | alova-gen 后修复类型定义 |
| `src/constant/icons.ts` | 新增 | UnoCSS 图标常量列表（约 40-50 个 i-lucide:* 图标） |
| `src/pages/bill/components/FilterModal.vue` | 修改 | 使用 store 新增的排序 getter，确保"其他"项在末尾 |

### 后端文件变更

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `controller/BillController.java` | 修改 | 新增 8 个端点 |
| `service/ICategoryService.java` | 修改 | 新增 CRUD 方法签名 |
| `service/impl/CategoryServiceImpl.java` | 修改 | 实现 CRUD 方法 |
| `service/IPaymentMethodService.java` | 修改 | 新增 CRUD 方法签名 |
| `service/impl/PaymentMethodServiceImpl.java` | 修改 | 实现 CRUD 方法 |
| `dto/AddCategoryDTO.java` | 新增 | 新增类目 DTO |
| `dto/UpdateCategoryDTO.java` | 新增 | 更新类目 DTO |
| `dto/AddPaymentMethodDTO.java` | 新增 | 新增支付方式 DTO |
| `dto/UpdatePaymentMethodDTO.java` | 新增 | 更新支付方式 DTO |
| `dto/SortDTO.java` | 新增 | 排序 DTO |
| `enums/ResultEnum.java` | 修改 | 新增错误码 |

### 不受影响

- 数据库表结构（已有 `user_id` 字段支持用户自定义）
- FilterModal 的胶囊网格样式（保持现有 wd-radio-group 方式）
- 账单统计相关功能
- 本地/访客模式的基础记账功能
