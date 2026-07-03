# 账单同步功能改进设计

## 概述

改进账单同步功能，使后端支持部分成功场景，前端精确标记每条账单的同步状态，并优化 UI 展示。

## 背景

当前 `POST /bill/batchCreate` 接口返回 `Result<Integer>`（成功数量），存在以下问题：
- 后端 `saveBatch` 若部分失败仍返回总数，前端错误标记全部成功
- 前端按时间顺序标记前 N 条为已同步，假设脆弱
- 无法区分全部成功、部分成功、全部失败三种状态
- 失败账单无法被识别和重试

## 设计

### 1. 后端响应模型

新增 `BatchSyncResultVO`：

```java
public class BatchSyncResultVO {
    private int total;              // 总数
    private int successCount;       // 成功数
    private int failedCount;        // 失败数
    private String status;          // ALL_SUCCESS | PARTIAL_SUCCESS | ALL_FAILED
    private List<BillSyncDetailVO> details;
}
```

新增 `BillSyncDetailVO`：

```java
public class BillSyncDetailVO {
    private int index;              // 批次中的索引（0-based）
    private String status;          // SUCCESS | FAILED
    private Long billId;            // 成功时返回服务器生成的 ID
    private String reason;          // 失败时的错误原因
}
```

状态枚举值：
- `ALL_SUCCESS`：全部成功（successCount == total）
- `PARTIAL_SUCCESS`：部分成功（0 < successCount < total）
- `ALL_FAILED`：全部失败（successCount == 0）

### 2. 后端 Service 逻辑

修改 `BillServiceImpl.batchCreate`：

- 将 `saveBatch` 改为逐条保存，每条单独 try-catch
- 成功的记录 `billId`，失败的记录失败原因
- 根据成功/失败数量计算 `status`
- 返回 `BatchSyncResultVO` 替代 `int`

关键点：
- 不使用 `@Transactional` 注解在方法级别，逐条独立提交
- 失败原因捕获 `Exception.getMessage()`，截取合理长度

### 3. 后端 Controller 变更

```java
@PostMapping("/batchCreate")
public Result<BatchSyncResultVO> batchCreate(@Validated @RequestBody BatchCreateBillDTO dto) {
    String userId = StpUtil.getLoginIdAsString();
    return Result.success(billService.batchCreate(userId, dto));
}
```

### 4. 前端 Store 变更

修改 `billStore.syncLocalBillToServer`：

- 返回值改为 `{ success: boolean, result: BatchSyncResultVO }`
- 根据 `details` 中的 index 精确标记每条账单的 `syncStatus`
  - `SUCCESS` → `'success'`
  - `FAILED` → `'failed'`
- 新增 `retryFailedSync()` 方法：将所有 `'failed'` 状态重置为 `'init'`，然后调用同步

新增 computed：
- `failedSyncCount`：统计 `syncStatus === 'failed'` 的账单数

### 5. 前端 UI 变更（SyncStatusPopup.vue）

#### 颜色调整
- 待同步数字：`text-orange-500`（警告色）
- 已同步数字：`text-green-500`（成功色）

#### 同步结果展示
- 全部成功：绿色背景，显示"同步成功，共 X 条"
- 部分成功：橙色背景，显示"部分成功，X 条成功，Y 条失败"
- 全部失败：红色背景，显示"同步失败，X 条失败"

#### 失败原因列表
- 部分成功/全部失败时，显示失败账单列表（账单名称 + 失败原因）
- 列表区域最大高度限制，超出滚动

#### 最大高度约束
- 弹窗内容区域设置 `max-h-[70vh]`，内容超出时 `overflow-y-auto`
- 确保在小屏设备上不会超出视口

#### 重试按钮
- 存在失败账单时，显示"重试失败（X 条）"按钮
- 点击调用 `retryFailedSync()`

### 6. API 类型更新

运行 `pnpm alova-gen` 重新生成 API 定义，然后使用 `alova-api-fix` skill 修复类型。

前端需手动新增 `BatchSyncResultVO` 和 `BillSyncDetailVO` 的类型定义（或等 alova-gen 自动生成）。

## 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `BatchSyncResultVO.java` | 新增 | 批量同步结果 VO |
| `BillSyncDetailVO.java` | 新增 | 单条账单同步详情 VO |
| `IBillService.java` | 修改 | `batchCreate` 返回类型改为 `BatchSyncResultVO` |
| `BillServiceImpl.java` | 修改 | 逐条保存逻辑，返回详细结果 |
| `BillController.java` | 修改 | 返回类型改为 `Result<BatchSyncResultVO>` |
| `billStore.ts` | 修改 | 处理新响应结构，新增重试方法和 failedSyncCount |
| `SyncStatusPopup.vue` | 修改 | UI 颜色、结果展示、失败列表、重试按钮、最大高度 |
| `apiDefinitions.ts` | 自动生成 | alova-gen 重新生成 |
| `globals.d.ts` | 自动生成+修复 | alova-gen + alova-api-fix |
