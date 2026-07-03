# 账单同步功能改进实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 改进账单同步功能，后端支持部分成功场景，前端精确标记每条账单状态，优化 UI 展示。

**Architecture:** 后端将 `batchCreate` 从 `saveBatch` 改为逐条保存，返回 `BatchSyncResultVO`（含每条状态）。前端根据返回的 details 精确标记 `syncStatus`，新增重试机制和改进 UI。

**Tech Stack:** Spring Boot 3 / Java 17, MyBatis-Plus, Vue 3 / TypeScript, Pinia, Alova.js, wot-design-uni, UnoCSS

---

### Task 1: 后端 - 新增 VO 类

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/vo/BatchSyncResultVO.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/vo/BillSyncDetailVO.java`

- [ ] **Step 1: 创建 BillSyncDetailVO**

```java
package com.samoy.chuanbillserver.vo;

import lombok.Data;

/**
 * 单条账单同步详情
 */
@Data
public class BillSyncDetailVO {
    /** 批次中的索引（0-based） */
    private int index;
    /** 同步状态：SUCCESS / FAILED */
    private String status;
    /** 成功时返回服务器生成的 ID */
    private Long billId;
    /** 失败时的错误原因 */
    private String reason;

    public static BillSyncDetailVO success(int index, Long billId) {
        BillSyncDetailVO detail = new BillSyncDetailVO();
        detail.setIndex(index);
        detail.setStatus("SUCCESS");
        detail.setBillId(billId);
        return detail;
    }

    public static BillSyncDetailVO failed(int index, String reason) {
        BillSyncDetailVO detail = new BillSyncDetailVO();
        detail.setIndex(index);
        detail.setStatus("FAILED");
        detail.setReason(reason);
        return detail;
    }
}
```

- [ ] **Step 2: 创建 BatchSyncResultVO**

```java
package com.samoy.chuanbillserver.vo;

import lombok.Data;
import java.util.List;

/**
 * 批量同步结果
 */
@Data
public class BatchSyncResultVO {
    /** 总数 */
    private int total;
    /** 成功数 */
    private int successCount;
    /** 失败数 */
    private int failedCount;
    /** 状态：ALL_SUCCESS / PARTIAL_SUCCESS / ALL_FAILED */
    private String status;
    /** 每条账单的同步详情 */
    private List<BillSyncDetailVO> details;

    public static BatchSyncResultVO of(List<BillSyncDetailVO> details, int total) {
        BatchSyncResultVO result = new BatchSyncResultVO();
        result.setTotal(total);
        long successCount = details.stream()
                .filter(d -> "SUCCESS".equals(d.getStatus()))
                .count();
        result.setSuccessCount((int) successCount);
        result.setFailedCount(total - (int) successCount);
        if (successCount == total) {
            result.setStatus("ALL_SUCCESS");
        } else if (successCount == 0) {
            result.setStatus("ALL_FAILED");
        } else {
            result.setStatus("PARTIAL_SUCCESS");
        }
        result.setDetails(details);
        return result;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/vo/BatchSyncResultVO.java \
       chuan-bill-server/src/main/java/com/samoy/chuanbillserver/vo/BillSyncDetailVO.java
git commit -m "feat(server): 新增批量同步结果 VO 类"
```

---

### Task 2: 后端 - 修改 Service 层

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBillService.java:59`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java:106-133`

- [ ] **Step 1: 修改 IBillService 接口**

将 `batchCreate` 的返回类型从 `int` 改为 `BatchSyncResultVO`：

```java
// IBillService.java 第 57-59 行
/**
 * 批量添加账单
 *
 * @param userId 用户ID
 * @param dto    批量账单信息
 * @return 批量同步结果，包含每条账单的同步状态
 */
BatchSyncResultVO batchCreate(String userId, BatchCreateBillDTO dto);
```

需要在文件顶部添加 import：
```java
import com.samoy.chuanbillserver.vo.BatchSyncResultVO;
```

- [ ] **Step 2: 修改 BillServiceImpl 实现**

将 `batchCreate` 方法（第 106-133 行）替换为逐条保存逻辑：

```java
@Override
public BatchSyncResultVO batchCreate(String userId, BatchCreateBillDTO dto) {
    List<AddBillDTO> bills = dto.getBills();
    if (bills == null || bills.isEmpty()) {
        return BatchSyncResultVO.of(Collections.emptyList(), 0);
    }

    List<BillSyncDetailVO> details = new ArrayList<>();
    for (int i = 0; i < bills.size(); i++) {
        AddBillDTO addBillDTO = bills.get(i);
        try {
            Bill bill = new Bill();
            bill.setUserId(userId);
            bill.setName(addBillDTO.getName());
            bill.setCategoryId(addBillDTO.getCategoryId());
            bill.setPaymentMethodId(addBillDTO.getPaymentMethodId());
            bill.setType(addBillDTO.getType());
            bill.setAmount(addBillDTO.getAmount());
            bill.setTime(addBillDTO.getTime());
            bill.setRemark(addBillDTO.getRemark());
            bill.setSource(addBillDTO.getSource() != null ? addBillDTO.getSource() : "manual");
            if (addBillDTO.getFamilyId() != null) {
                bill.setFamilyId(addBillDTO.getFamilyId());
            }
            this.save(bill);
            details.add(BillSyncDetailVO.success(i, bill.getId()));
        } catch (Exception e) {
            String reason = e.getMessage();
            if (reason != null && reason.length() > 200) {
                reason = reason.substring(0, 200);
            }
            details.add(BillSyncDetailVO.failed(i, reason));
        }
    }
    return BatchSyncResultVO.of(details, bills.size());
}
```

需要在文件顶部添加 imports：
```java
import com.samoy.chuanbillserver.vo.BatchSyncResultVO;
import com.samoy.chuanbillserver.vo.BillSyncDetailVO;
import java.util.ArrayList;
import java.util.Collections;
```

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBillService.java \
       chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java
git commit -m "feat(server): batchCreate 改为逐条保存并返回每条同步状态"
```

---

### Task 3: 后端 - 修改 Controller

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java:62-67`

- [ ] **Step 1: 修改 BillController 返回类型**

```java
// BillController.java 第 62-67 行
@PostMapping("/batchCreate")
@Operation(summary = "批量添加账单", description = "批量创建账单记录，用于数据同步")
public Result<BatchSyncResultVO> batchCreate(@Validated @RequestBody BatchCreateBillDTO dto) {
    String userId = StpUtil.getLoginIdAsString();
    return Result.success(billService.batchCreate(userId, dto));
}
```

需要在文件顶部添加 import：
```java
import com.samoy.chuanbillserver.vo.BatchSyncResultVO;
```

- [ ] **Step 2: 验证后端编译**

```bash
cd chuan-bill-server && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java
git commit -m "feat(server): batchCreate 接口返回 BatchSyncResultVO"
```

---

### Task 4: 前端 - 重新生成 API 定义并修复类型

**Files:**
- Regenerate: `chuan-bill-app/src/api/apiDefinitions.ts`
- Regenerate+Fix: `chuan-bill-app/src/api/globals.d.ts`

- [ ] **Step 1: 确保后端已启动**

后端需要运行中才能生成 API 定义。确认 `mvn spring-boot:run` 或等效服务已启动。

- [ ] **Step 2: 重新生成 API 定义**

```bash
cd chuan-bill-app && pnpm alova-gen
```

Expected: `apiDefinitions.ts` 和 `globals.d.ts` 已更新

- [ ] **Step 3: 运行 alova-api-fix 修复类型**

使用 `alova-api-fix` skill 修复生成的类型定义（金额字段类型、DTO 字段重复等问题）。

```bash
# 按照 alova-api-fix skill 的指引修复 globals.d.ts
```

- [ ] **Step 4: 验证类型正确**

检查 `globals.d.ts` 中 `BatchSyncResultVO` 和 `BillSyncDetailVO` 类型已生成，`batchCreate` 的返回类型已更新。

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-app/src/api/apiDefinitions.ts chuan-bill-app/src/api/globals.d.ts
git commit -m "chore: 重新生成 API 定义（含批量同步结果类型）"
```

---

### Task 5: 前端 - 修改 billStore 同步逻辑

**Files:**
- Modify: `chuan-bill-app/src/store/billStore.ts`

- [ ] **Step 1: 新增 failedSyncCount computed**

在 `syncedCount` 定义之后（第 31 行后）添加：

```typescript
const failedSyncCount = computed(() =>
  localBillList.value.filter(bill => bill.syncStatus === 'failed').length,
)
```

- [ ] **Step 2: 修改 syncLocalBillToServer 方法**

将 `syncLocalBillToServer` 方法（第 150-198 行）替换为：

```typescript
async function syncLocalBillToServer(): Promise<{ success: boolean, successCount: number, failedCount: number }> {
  const pendingBills = localBillList.value.filter(bill => bill.syncStatus === 'init')
  if (pendingBills.length === 0) {
    return { success: true, successCount: 0, failedCount: 0 }
  }

  try {
    const bills = pendingBills.map(bill => ({
      name: bill.name!,
      categoryId: bill.category?.id || '',
      type: bill.type!,
      amount: bill.amount!,
      time: bill.time!,
      paymentMethodId: bill.paymentMethod?.id,
      remark: bill.remark,
    }))
    const response = await Apis.bill.batchCreate({ data: { bills } })

    if (response.success && response.data) {
      const result = response.data

      // 根据 details 精确标记每条账单状态
      for (const detail of result.details || []) {
        const pendingBill = pendingBills[detail.index]
        if (!pendingBill)
          continue
        const bill = localBillList.value.find(item => item.id === pendingBill.id)
        if (bill) {
          bill.syncStatus = detail.status === 'SUCCESS' ? 'success' : 'failed'
        }
      }

      lastSyncTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
      return {
        success: true,
        successCount: result.successCount || 0,
        failedCount: result.failedCount || 0,
      }
    }

    console.error('同步本地账单失败:', response.message)
    return { success: false, successCount: 0, failedCount: pendingBills.length }
  }
  catch (error) {
    console.error('同步本地账单失败:', error)
    return { success: false, successCount: 0, failedCount: pendingBills.length }
  }
}
```

- [ ] **Step 3: 新增 retryFailedSync 方法**

在 `syncLocalBillToServer` 方法之后添加：

```typescript
/**
 * 重试失败的同步账单
 * 将 'failed' 状态重置为 'init'，然后重新同步
 */
async function retryFailedSync() {
  localBillList.value
    .filter(bill => bill.syncStatus === 'failed')
    .forEach(bill => bill.syncStatus = 'init')
  return syncLocalBillToServer()
}
```

- [ ] **Step 4: 更新 return 语句**

在 `return` 对象中添加 `failedSyncCount` 和 `retryFailedSync`：

```typescript
return {
  getMonthlyBillStats,
  localBillList,
  hasLocalBills,
  pendingSyncCount,
  syncedCount,
  failedSyncCount,
  lastSyncTime,
  addLocalBill,
  updateLocalBill,
  deleteLocalBill,
  clearLocalBills,
  syncLocalBillToServer,
  retryFailedSync,
  categoryListMap,
  paymentMethodList,
  isInitialzed,
  initBillData,
  getCategoryList,
  getPaymentMethodList,
}
```

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-app/src/store/billStore.ts
git commit -m "feat(store): 同步方法支持每条状态标记，新增重试和 failedSyncCount"
```

---

### Task 6: 前端 - 修改 SyncStatusPopup UI

**Files:**
- Modify: `chuan-bill-app/src/pages/mine/components/SyncStatusPopup.vue`

- [ ] **Step 1: 修改 script 部分**

替换整个 `<script setup>` 块：

```vue
<script setup lang="ts">
defineOptions({
  name: 'SyncStatusPopup',
})

const modelValue = defineModel<boolean>({ default: false })
const billStore = useBillStore()
const toast = useGlobalToast()
const loading = ref(false)
const syncResult = ref<{ status: string, successCount: number, failedCount: number, details: Array<{ index: number, status: string, reason?: string }> } | null>(null)

// 同步状态文案
const statusText = computed(() => {
  if (loading.value)
    return '同步中...'
  if (syncResult.value?.status === 'ALL_SUCCESS')
    return '同步成功'
  if (syncResult.value?.status === 'PARTIAL_SUCCESS')
    return '部分成功'
  if (syncResult.value?.status === 'ALL_FAILED')
    return '同步失败'
  return ''
})

// 失败账单详情列表
const failedDetails = computed(() => {
  if (!syncResult.value?.details)
    return []
  return syncResult.value.details.filter(d => d.status === 'FAILED')
})

// 开始同步
async function startSync() {
  if (loading.value)
    return

  loading.value = true
  syncResult.value = null

  try {
    const result = await billStore.syncLocalBillToServer()

    if (result.success) {
      syncResult.value = {
        status: result.failedCount === 0 ? 'ALL_SUCCESS' : 'PARTIAL_SUCCESS',
        successCount: result.successCount,
        failedCount: result.failedCount,
        details: [],
      }
      if (result.failedCount === 0) {
        toast.success(`成功同步${result.successCount}条账单`)
      }
      else {
        toast.warning(`${result.successCount}条成功，${result.failedCount}条失败`)
      }
    }
    else {
      syncResult.value = {
        status: 'ALL_FAILED',
        successCount: 0,
        failedCount: result.failedCount,
        details: [],
      }
      toast.error('同步失败，请重试')
    }
  }
  catch {
    syncResult.value = {
      status: 'ALL_FAILED',
      successCount: 0,
      failedCount: billStore.pendingSyncCount,
      details: [],
    }
    toast.error('同步失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 重试失败的账单
async function retryFailed() {
  if (loading.value)
    return

  loading.value = true
  syncResult.value = null

  try {
    const result = await billStore.retryFailedSync()

    if (result.success) {
      syncResult.value = {
        status: result.failedCount === 0 ? 'ALL_SUCCESS' : 'PARTIAL_SUCCESS',
        successCount: result.successCount,
        failedCount: result.failedCount,
        details: [],
      }
      if (result.failedCount === 0) {
        toast.success(`成功同步${result.successCount}条账单`)
      }
      else {
        toast.warning(`${result.successCount}条成功，${result.failedCount}条失败`)
      }
    }
    else {
      syncResult.value = {
        status: 'ALL_FAILED',
        successCount: 0,
        failedCount: result.failedCount,
        details: [],
      }
      toast.error('同步失败，请重试')
    }
  }
  catch {
    syncResult.value = {
      status: 'ALL_FAILED',
      successCount: 0,
      failedCount: billStore.failedSyncCount,
      details: [],
    }
    toast.error('同步失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 关闭弹窗
function handleClose() {
  syncResult.value = null
  modelValue.value = false
}
</script>
```

- [ ] **Step 2: 修改 template 部分**

替换整个 `<template>` 块：

```vue
<template>
  <wd-action-sheet
    :model-value="modelValue"
    position="bottom"
    closable
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    :z-index="999"
    title="账单同步"
    @update:model-value="handleClose"
  >
    <view class="max-h-[70vh] overflow-y-auto p-4">
      <!-- 同步状态统计 -->
      <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <view class="mb-3 flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            待同步
          </text>
          <text class="text-sm font-medium text-orange-500">
            {{ billStore.pendingSyncCount }}条
          </text>
        </view>
        <view class="mb-3 flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            已同步
          </text>
          <text class="text-sm font-medium text-green-500">
            {{ billStore.syncedCount }}条
          </text>
        </view>
        <view v-if="billStore.failedSyncCount > 0" class="mb-3 flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            同步失败
          </text>
          <text class="text-sm font-medium text-red-500">
            {{ billStore.failedSyncCount }}条
          </text>
        </view>
        <view v-if="billStore.lastSyncTime" class="flex items-center justify-between">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            上次同步
          </text>
          <text class="text-sm text-gray-500">
            {{ billStore.lastSyncTime }}
          </text>
        </view>
      </view>

      <!-- 同步结果 -->
      <view v-if="statusText" class="mb-4 rounded-xl p-4" :class="{
        'bg-green-50 dark:bg-green-900/20': syncResult?.status === 'ALL_SUCCESS',
        'bg-orange-50 dark:bg-orange-900/20': syncResult?.status === 'PARTIAL_SUCCESS',
        'bg-red-50 dark:bg-red-900/20': syncResult?.status === 'ALL_FAILED',
      }">
        <view class="flex items-center gap-2">
          <view
            v-if="loading"
            class="i-lucide:loader-2 h-5 w-5 animate-spin text-primary"
          />
          <view
            v-else-if="syncResult?.status === 'ALL_SUCCESS'"
            class="i-lucide:check-circle h-5 w-5 text-green-500"
          />
          <view
            v-else-if="syncResult?.status === 'PARTIAL_SUCCESS'"
            class="i-lucide:alert-circle h-5 w-5 text-orange-500"
          />
          <view
            v-else
            class="i-lucide:x-circle h-5 w-5 text-red-500"
          />
          <text class="text-sm font-medium" :class="{
            'text-green-700 dark:text-green-300': syncResult?.status === 'ALL_SUCCESS',
            'text-orange-700 dark:text-orange-300': syncResult?.status === 'PARTIAL_SUCCESS',
            'text-red-700 dark:text-red-300': syncResult?.status === 'ALL_FAILED',
          }">
            {{ statusText }}
            <text v-if="syncResult?.status === 'PARTIAL_SUCCESS'" class="ml-1">
              ，{{ syncResult.successCount }}条成功，{{ syncResult.failedCount }}条失败
            </text>
            <text v-if="syncResult?.status === 'ALL_FAILED' && syncResult?.failedCount" class="ml-1">
              ，{{ syncResult.failedCount }}条失败
            </text>
          </text>
        </view>

        <!-- 失败原因列表 -->
        <view v-if="failedDetails.length > 0" class="mt-3 max-h-32 overflow-y-auto">
          <view
            v-for="detail in failedDetails"
            :key="detail.index"
            class="mb-1 flex items-start gap-2 text-xs text-red-600 dark:text-red-400"
          >
            <text class="i-lucide:alert-triangle mt-0.5 h-3 w-3 flex-shrink-0" />
            <text>{{ detail.reason || '未知错误' }}</text>
          </view>
        </view>
      </view>

      <!-- 同步按钮 -->
      <view class="flex gap-2">
        <wd-button
          v-if="billStore.failedSyncCount > 0 && !loading"
          type="warning"
          block
          @click="retryFailed"
        >
          重试失败（{{ billStore.failedSyncCount }}条）
        </wd-button>
        <wd-button
          type="primary"
          block
          :loading="loading"
          :disabled="billStore.pendingSyncCount === 0 && billStore.failedSyncCount === 0"
          @click="startSync"
        >
          {{ billStore.pendingSyncCount === 0 && billStore.failedSyncCount === 0 ? '暂无待同步数据' : '开始同步' }}
        </wd-button>
      </view>
    </view>
  </wd-action-sheet>
</template>
```

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/pages/mine/components/SyncStatusPopup.vue
git commit -m "feat(sync): 优化同步弹窗 UI，支持部分成功展示和重试"
```

---

### Task 7: 前端 - 验证类型检查和 Lint

**Files:**
- None (verification only)

- [ ] **Step 1: 运行类型检查**

```bash
cd chuan-bill-app && pnpm type-check
```

Expected: 无类型错误

- [ ] **Step 2: 运行 Lint**

```bash
cd chuan-bill-app && pnpm lint:fix
```

Expected: 无 lint 错误

- [ ] **Step 3: 修复任何发现的问题并提交**

如有类型或 lint 错误，修复后提交：
```bash
git add -A
git commit -m "fix: 修复类型检查和 lint 问题"
```

---

### Task 8: 后端 - 验证编译

**Files:**
- None (verification only)

- [ ] **Step 1: 编译后端**

```bash
cd chuan-bill-server && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 如有编译错误，修复并提交**

```bash
git add -A
git commit -m "fix: 修复编译错误"
```
