# 预算管理功能设计文档

> 日期: 2026-05-17
> 状态: 待实现

## 1. 概述

为"小川记账"添加个人月度预算管理功能。用户可在统计页设置每月总预算，系统实时计算支出并在统计页展示预算进度卡片，当支出达到阈值时发送消息通知。

### 核心决策

| 决策项 | 选择 | 原因 |
|--------|------|------|
| 预算粒度 | 仅总预算 | 简洁直接，适合第一版 |
| 已用金额计算 | 实时从账单计算 | 数据准确，无需维护一致性 |
| 预算范围 | 仅个人预算 | 降低复杂度，家庭预算后续迭代 |
| UI 入口 | 统计页内嵌卡片 | 自然融合，用户容易发现 |
| 提醒方式 | 视觉提示 + 消息通知 | 进度条颜色变化 + 超阈值消息推送 |

## 2. 后端设计

### 2.1 已有骨架文件

| 层 | 文件 | 状态 |
|---|---|---|
| Entity | `entity/Budget.java` | 已有，字段完整 |
| Mapper | `dao/BudgetMapper.java` + `mapper/BudgetMapper.xml` | 空接口，无自定义 SQL |
| Service | `service/IBudgetService.java` + `service/impl/BudgetServiceImpl.java` | 空接口/实现 |

### 2.2 新增文件

| 层 | 文件 | 说明 |
|---|---|---|
| Controller | `controller/BudgetController.java` | 预算相关接口 |
| DTO | `dto/SetBudgetDTO.java` | 设置/修改预算请求体 |
| VO | `vo/BudgetVO.java` | 预算详情响应体 |

### 2.3 修改文件

| 文件 | 修改内容 |
|---|---|
| `IBudgetService.java` | 新增业务方法声明 |
| `BudgetServiceImpl.java` | 实现业务逻辑 |
| `BudgetMapper.xml` | 新增实时计算已用金额的 SQL |
| `ResultEnum.java` | 新增预算相关错误码 (5000+) |
| `BillServiceImpl.java` | 新增账单保存后的预算预警检查 |
| `NotificationSettingsPopup.vue` | 新增"预算提醒"开关条目 |

### 2.4 API 接口

#### GET /budget/current

获取指定月份的预算信息（含实时计算的已用金额）。

**参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| month | String | 否 | 月份，格式 YYYY-MM，默认当月 |

**响应: `Result<BudgetVO>`**

```json
{
  "code": 200,
  "data": {
    "id": "budget_001",
    "userId": "user_001",
    "month": "2026-05",
    "amount": "5000.00",
    "useAmount": "3600.00",
    "remainingAmount": "1400.00",
    "usagePercent": 72.00,
    "createTime": "2026-05-01 10:00:00",
    "updateTime": "2026-05-15 14:30:00"
  }
}
```

无预算时返回 `data: null`。

**业务逻辑:**

1. 查询 `t_budget` 中该用户该月的记录
2. 若存在，实时从 `t_bill` 表 `SUM(amount)` 计算该月支出（`type = 'expense'` 且未删除）
3. 计算 `remainingAmount = amount - useAmount`，`usagePercent = useAmount / amount * 100`
4. 若不存在，返回 null

#### POST /budget/set

设置或修改当月预算。若当月已有预算则更新金额，无则创建。

**请求体: `SetBudgetDTO`**

```java
@Data
@Schema(description = "设置预算请求")
public class SetBudgetDTO {
    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    @Schema(description = "预算金额")
    private BigDecimal amount;
}
```

**响应: `Result<BudgetVO>`** （返回设置后的完整预算信息，同 GET）

**业务逻辑:**

1. 查询当月是否已有预算
2. 有 → 更新 `amount` 字段
3. 无 → 创建新记录，`month` 为当月第一天，`use_amount` 默认 0
4. 返回完整预算信息（含实时计算的已用金额）

#### DELETE /budget/delete

删除当月预算。

**参数:** 无（自动取当月）

**响应: `Result<Boolean>`**

**业务逻辑:**

1. 查询当月预算是否存在
2. 不存在 → 抛出 `BUDGET_NOT_FOUND`
3. 存在 → 软删除

### 2.5 错误码

在 `ResultEnum` 中新增（5000+ 范围）：

| 枚举名 | code | message |
|--------|------|---------|
| `BUDGET_NOT_FOUND` | 5001 | 预算不存在 |

### 2.6 实时计算已用金额

在 `BudgetMapper.xml` 中新增 SQL：

```sql
<select id="getMonthlyExpense" resultType="java.math.BigDecimal">
    SELECT COALESCE(SUM(amount), 0)
    FROM t_bill
    WHERE user_id = #{userId}
      AND type = 'expense'
      AND deleted = 0
      AND DATE_FORMAT(time, '%Y-%m') = #{month}
</select>
```

在 `BudgetMapper.java` 中新增方法：

```java
BigDecimal getMonthlyExpense(@Param("userId") String userId, @Param("month") String month);
```

### 2.7 预算预警通知

#### 触发时机

在 `BillServiceImpl` 的 `addBill()` 和 `updateBill()` 方法末尾，账单保存成功后调用 `checkBudgetAlert(userId)`。

#### 预警逻辑

```java
private void checkBudgetAlert(String userId) {
    // 1. 查询通知总开关和预算提醒开关
    //    notification.master.enabled = false → 跳过
    //    notification.budget.enabled = false → 跳过
    // 2. 查询当月预算
    //    无预算 → 跳过
    // 3. 实时计算当月支出
    // 4. 计算使用率 = 支出 / 预算金额 * 100
    // 5. 判断阈值：
    //    - 使用率 >= 100% → 检查当月是否已发送超支通知，未发送则发送
    //    - 使用率 >= 80% 且 < 100% → 检查当月是否已发送预警通知，未发送则发送
}
```

#### 通知内容

| 条件 | type | title | content | relatedType |
|------|------|-------|---------|-------------|
| ≥ 80% 且 < 100% | `budget` | 预算预警 | "本月支出已达预算的 XX%，请注意控制开支" | `budget` |
| ≥ 100% | `budget` | 预算超支 | "本月支出已超出预算 ¥XXX，请合理安排消费" | `budget` |

#### 防重复通知

通过查询 `t_message` 表当月同类型消息判断：

```java
// 查询当月是否已发送过同类型预算通知
int count = messageService.count(new LambdaQueryWrapper<Message>()
    .eq(Message::getUserId, userId)
    .eq(Message::getType, "budget")
    .eq(Message::getTitle, title)  // "预算预警" 或 "预算超支"
    .apply("DATE_FORMAT(create_time, '%Y-%m') = {0}", currentMonth)
);
if (count > 0) return; // 已通知过，跳过
```

#### 通知设置检查

通过 `IPreferenceService` 读取用户偏好设置：

- `notification.master.enabled` = `false` → 跳过所有通知
- `notification.budget.enabled` = `false` → 跳过预算通知

## 3. 前端设计

### 3.1 新增文件

| 类型 | 文件 | 说明 |
|------|------|------|
| 组件 | `src/pages/statistics/components/BudgetCard.vue` | 预算进度卡片 |
| 组件 | `src/pages/statistics/components/BudgetSettingPopup.vue` | 预算设置/编辑弹窗 |
| Store | `src/store/budgetStore.ts` | 预算状态管理 |

### 3.2 修改文件

| 文件 | 修改内容 |
|------|------|
| `src/pages/statistics/index.vue` | 在 Overview 卡片下方插入 BudgetCard |
| `src/pages/mine/components/NotificationSettingsPopup.vue` | 新增"预算提醒"开关条目 |
| `src/api/apiDefinitions.ts` | 通过 `pnpm alova-gen` 自动生成 |
| `src/api/globals.d.ts` | 通过 `pnpm alova-gen` 自动生成 |

### 3.3 BudgetCard 组件

**位置:** 统计页 Overview 卡片（收支概览）与 CategoryChart 之间。

**Props:**

| Prop | 类型 | 说明 |
|------|------|------|
| month | string | 当前月份 YYYY-MM |

**展示逻辑:**

- 仅 `user.isLoggedIn` 时渲染
- 有预算时：展示环形进度条 + 已用/总预算金额 + 剩余金额
- 无预算时：展示引导文案"尚未设置预算，点击设置"
- 点击卡片 → 打开 BudgetSettingPopup

**进度条颜色规则:**

| 使用率 | 颜色 | 说明 |
|--------|------|------|
| 0% - 60% | 绿色 | 安全 |
| 60% - 80% | 黄色 | 注意 |
| 80% - 100% | 橙色 | 预警 |
| > 100% | 红色 | 超支 |

**超预算状态:** 红色进度条 + 显示超支金额（如"超支 ¥500"）。

**样式:** 遵循统计页现有卡片风格：`mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]`

### 3.4 BudgetSettingPopup 组件

**交互方式:** `wd-action-sheet` 底部弹出面板（与 NotificationSettingsPopup 一致）。

**内容:**

- 标题："设置预算"
- 金额输入框（`wd-input`，type="number"，带 ¥ 前缀）
- 保存按钮（`wd-button` type="primary"）
- 删除按钮（已有预算时显示，`wd-button` type="error" plain，需二次确认）

**行为:**

- 打开时加载当月预算（如有），预填金额
- 保存成功 → 关闭弹窗，刷新 BudgetCard 数据
- 删除成功 → 关闭弹窗，BudgetCard 显示引导状态

### 3.5 budgetStore

```typescript
// src/store/budgetStore.ts
export const useBudgetStore = defineStore('budget', () => {
  const currentBudget = ref<BudgetVO | null>(null)
  const loading = ref(false)

  // 获取指定月份预算
  async function fetchBudget(month?: string) { ... }

  // 设置预算
  async function setBudget(amount: number) { ... }

  // 删除预算
  async function deleteBudget() { ... }

  return { currentBudget, loading, fetchBudget, setBudget, deleteBudget }
})
```

- 调用 `Apis.budget.getCurrentBudget()` / `Apis.budget.setBudget()` / `Apis.budget.deleteBudget()`
- `fetchBudget` 跟随 `statisticsStore.currentMonth` 变化自动调用

### 3.6 统计页集成

在 `statistics/index.vue` 中：

1. 导入 `BudgetCard` 组件
2. 在 Overview 卡片 `</view>` 后、CategoryChart 前插入：

```vue
<BudgetCard :month="currentMonth" />
```

3. 监听 `currentMonth` 变化时调用 `budgetStore.fetchBudget(currentMonth)`

### 3.7 通知设置集成

在 `NotificationSettingsPopup.vue` 的"家庭通知"条目下方新增：

```vue
<!-- 预算提醒 -->
<view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
  <view>
    <text class="block text-sm font-medium">预算提醒</text>
    <text class="mt-1 block text-xs text-gray-500">
      支出达到预算阈值时提醒
    </text>
  </view>
  <wd-switch
    v-model="settings.budgetNotificationEnabled"
    :disabled="!settings.masterEnabled"
    size="20px"
    @change="onSettingChange('notification.budget.enabled', settings.budgetNotificationEnabled)"
  />
</view>
```

对应 `settings` 状态新增 `budgetNotificationEnabled: true`，onMounted 时从偏好设置加载 `notification.budget.enabled`。

## 4. 数据流

```
用户设置预算
  → POST /budget/set
  → BudgetServiceImpl.setBudget()
  → INSERT/UPDATE t_budget
  → 返回 BudgetVO（含实时计算的 useAmount）

用户添加账单
  → POST /bill/add
  → BillServiceImpl.addBill()
  → INSERT t_bill
  → checkBudgetAlert(userId)
    → 查询通知开关
    → 查询当月预算
    → 实时计算当月支出
    → 判断阈值 → 发送消息通知（如需要）

用户查看统计页
  → GET /budget/current?month=2026-05
  → BudgetServiceImpl.getCurrentBudget()
  → 查询 t_budget + 实时 SUM(t_bill)
  → 返回 BudgetVO
  → BudgetCard 渲染进度条
```

## 5. 实现顺序

1. **后端基础:** BudgetController + DTO + VO + Service 方法 + Mapper SQL
2. **后端预警:** BillServiceImpl 集成预算预警检查
3. **前端基础:** budgetStore + BudgetCard + BudgetSettingPopup
4. **前端集成:** 统计页插入 BudgetCard + 月份联动
5. **通知设置:** NotificationSettingsPopup 新增预算提醒开关
6. **API 生成:** `pnpm alova-gen` 重新生成 API 定义
