# 预算管理功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为"小川记账"添加个人月度预算管理功能，包括后端 API、统计页预算卡片、预算设置弹窗和超预算消息通知。

**Architecture:** 后端遵循现有 Controller → Service → Mapper 分层架构，预算已用金额实时从 t_bill 表计算。前端新增 budgetStore + 两个 Vue 组件（BudgetCard、BudgetSettingPopup），嵌入现有统计页。

**Tech Stack:** Spring Boot 3 / Java 17 / MyBatis-Plus / Sa-Token, Vue 3 / TypeScript / Pinia / Alova.js / wot-design-uni / UnoCSS

**Spec:** `docs/superpowers/specs/2026-05-17-budget-management-design.md`

---

## 文件清单

### 新增文件

| 文件 | 职责 |
|------|------|
| `chuan-bill-server/.../dto/SetBudgetDTO.java` | 设置预算请求体 |
| `chuan-bill-server/.../vo/BudgetVO.java` | 预算详情响应体 |
| `chuan-bill-server/.../controller/BudgetController.java` | 预算 API 接口 |
| `chuan-bill-app/src/store/budgetStore.ts` | 预算状态管理 |
| `chuan-bill-app/src/pages/statistics/components/BudgetCard.vue` | 预算进度卡片 |
| `chuan-bill-app/src/pages/statistics/components/BudgetSettingPopup.vue` | 预算设置弹窗 |

### 修改文件

| 文件 | 修改内容 |
|------|------|
| `chuan-bill-server/.../result/ResultEnum.java` | 新增 BUDGET_NOT_FOUND 错误码 |
| `chuan-bill-server/.../dao/BudgetMapper.java` | 新增 getMonthlyExpense 方法 |
| `chuan-bill-server/.../resources/mapper/BudgetMapper.xml` | 新增实时计算 SQL |
| `chuan-bill-server/.../service/IBudgetService.java` | 新增业务方法声明 |
| `chuan-bill-server/.../service/impl/BudgetServiceImpl.java` | 实现业务逻辑 |
| `chuan-bill-server/.../service/impl/BillServiceImpl.java` | 新增预算预警检查 |
| `chuan-bill-server/src/main/resources/application.yaml` | 新增 notification.budget.enabled 白名单 |
| `chuan-bill-app/src/pages/statistics/index.vue` | 插入 BudgetCard |
| `chuan-bill-app/src/pages/mine/components/NotificationSettingsPopup.vue` | 新增预算提醒开关 |

---

## Task 1: 后端 - DTO、VO、错误码

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/SetBudgetDTO.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/vo/BudgetVO.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java`

- [ ] **Step 1: 创建 SetBudgetDTO**

```java
package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "设置预算请求")
public class SetBudgetDTO {

    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "预算金额")
    private BigDecimal amount;
}
```

- [ ] **Step 2: 创建 BudgetVO**

```java
package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "预算详情响应")
public class BudgetVO {

    @Schema(description = "预算ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "月份，格式 YYYY-MM")
    private String month;

    @Schema(description = "预算金额")
    private BigDecimal amount;

    @Schema(description = "已使用金额")
    private BigDecimal useAmount;

    @Schema(description = "剩余金额")
    private BigDecimal remainingAmount;

    @Schema(description = "使用百分比")
    private BigDecimal usagePercent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 在 ResultEnum 中新增预算错误码**

在 `MESSAGE_NOT_FOUND(4051, "消息不存在")` 之后、枚举末尾的分号之前添加：

```java
// Budget-related error codes 5000+
BUDGET_NOT_FOUND(5001, "预算不存在"),
```

- [ ] **Step 4: 验证编译**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/SetBudgetDTO.java \
        chuan-bill-server/src/main/java/com/samoy/chuanbillserver/vo/BudgetVO.java \
        chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java
git commit -m "feat(budget): 添加预算 DTO、VO 和错误码"
```

---

## Task 2: 后端 - BudgetMapper 实时计算 SQL

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dao/BudgetMapper.java`
- Modify: `chuan-bill-server/src/main/resources/mapper/BudgetMapper.xml`

- [ ] **Step 1: 在 BudgetMapper 接口新增方法**

在 `BudgetMapper.java` 中添加：

```java
package com.samoy.chuanbillserver.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samoy.chuanbillserver.entity.Budget;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;

public interface BudgetMapper extends BaseMapper<Budget> {

    /**
     * 实时计算用户指定月份的支出总额
     */
    BigDecimal getMonthlyExpense(@Param("userId") String userId, @Param("month") String month);
}
```

- [ ] **Step 2: 在 BudgetMapper.xml 添加 SQL**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.samoy.chuanbillserver.dao.BudgetMapper">

    <select id="getMonthlyExpense" resultType="java.math.BigDecimal">
        SELECT COALESCE(SUM(amount), 0)
        FROM t_bill
        WHERE user_id = #{userId}
          AND type = 'expense'
          AND deleted = 0
          AND DATE_FORMAT(time, '%Y-%m') = #{month}
    </select>

</mapper>
```

- [ ] **Step 3: 验证编译**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 4: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dao/BudgetMapper.java \
        chuan-bill-server/src/main/resources/mapper/BudgetMapper.xml
git commit -m "feat(budget): 添加实时计算月度支出的 Mapper SQL"
```

---

## Task 3: 后端 - IBudgetService + BudgetServiceImpl

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBudgetService.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BudgetServiceImpl.java`

- [ ] **Step 1: 更新 IBudgetService 接口**

```java
package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.SetBudgetDTO;
import com.samoy.chuanbillserver.entity.Budget;
import com.samoy.chuanbillserver.vo.BudgetVO;

public interface IBudgetService extends IService<Budget> {

    /**
     * 获取指定月份的预算信息（含实时计算的已用金额）
     */
    BudgetVO getCurrentBudget(String userId, String month);

    /**
     * 设置或修改当月预算
     */
    BudgetVO setBudget(String userId, SetBudgetDTO dto);

    /**
     * 删除当月预算
     */
    boolean deleteBudget(String userId);

    /**
     * 检查预算预警并发送通知（供 BillServiceImpl 调用）
     */
    void checkBudgetAlert(String userId);
}
```

- [ ] **Step 2: 实现 BudgetServiceImpl**

```java
package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.BudgetMapper;
import com.samoy.chuanbillserver.dto.SetBudgetDTO;
import com.samoy.chuanbillserver.entity.Budget;
import com.samoy.chuanbillserver.entity.Message;
import com.samoy.chuanbillserver.result.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IBudgetService;
import com.samoy.chuanbillserver.service.IMessageService;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import com.samoy.chuanbillserver.vo.BudgetVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements IBudgetService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private IMessageService messageService;

    @Resource
    private IUserPreferenceService userPreferenceService;

    @Override
    public BudgetVO getCurrentBudget(String userId, String month) {
        LocalDate monthDate = parseMonth(month);
        Budget budget = getOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, monthDate));
        if (budget == null) {
            return null;
        }
        return convertToVO(budget, userId, month);
    }

    @Override
    public BudgetVO setBudget(String userId, SetBudgetDTO dto) {
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        LocalDate monthDate = parseMonth(currentMonth);

        Budget existing = getOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, monthDate));

        if (existing != null) {
            existing.setAmount(dto.getAmount());
            this.updateById(existing);
            return convertToVO(existing, userId, currentMonth);
        } else {
            Budget budget = new Budget();
            budget.setId(UUID.randomUUID().toString().replace("-", ""));
            budget.setUserId(userId);
            budget.setMonth(monthDate);
            budget.setAmount(dto.getAmount());
            budget.setUseAmount(BigDecimal.ZERO);
            this.save(budget);
            return convertToVO(budget, userId, currentMonth);
        }
    }

    @Override
    public boolean deleteBudget(String userId) {
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        LocalDate monthDate = parseMonth(currentMonth);

        Budget budget = getOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, monthDate));
        if (budget == null) {
            throw new BusinessException(ResultEnum.BUDGET_NOT_FOUND);
        }
        return this.removeById(budget.getId());
    }

    @Override
    public void checkBudgetAlert(String userId) {
        // 1. 检查通知总开关（null 视为默认开启）
        String masterEnabled = userPreferenceService.getValue(userId, "notification.master.enabled");
        if (masterEnabled != null && "false".equals(masterEnabled)) {
            return;
        }

        // 2. 检查预算提醒开关（null 视为默认开启）
        String budgetEnabled = userPreferenceService.getValue(userId, "notification.budget.enabled");
        if (budgetEnabled != null && "false".equals(budgetEnabled)) {
            return;
        }

        // 3. 查询当月预算
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        LocalDate monthDate = parseMonth(currentMonth);
        Budget budget = getOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, monthDate));
        if (budget == null) {
            return;
        }

        // 4. 实时计算当月支出
        BigDecimal expense = baseMapper.getMonthlyExpense(userId, currentMonth);
        if (expense == null) {
            expense = BigDecimal.ZERO;
        }

        // 5. 计算使用率
        BigDecimal usagePercent = expense.multiply(BigDecimal.valueOf(100))
                .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);

        // 6. 判断阈值并发送通知
        if (usagePercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            sendBudgetNotificationIfNotExists(userId, budget.getId(), "预算超支",
                    String.format("本月支出已超出预算 ¥%s，请合理安排消费",
                            expense.subtract(budget.getAmount()).setScale(2, RoundingMode.HALF_UP)));
        } else if (usagePercent.compareTo(BigDecimal.valueOf(80)) >= 0) {
            sendBudgetNotificationIfNotExists(userId, budget.getId(), "预算预警",
                    String.format("本月支出已达预算的 %s%%，请注意控制开支",
                            usagePercent.setScale(0, RoundingMode.HALF_UP)));
        }
    }

    private void sendBudgetNotificationIfNotExists(String userId, String budgetId,
                                                    String title, String content) {
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        // 检查当月是否已发送过同类型通知
        long count = messageService.count(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getType, "budget")
                .eq(Message::getTitle, title)
                .apply("DATE_FORMAT(create_time, '%Y-%m') = {0}", currentMonth));
        if (count == 0) {
            messageService.sendMessage(userId, title, content, "budget", budgetId, "budget");
        }
    }

    private BudgetVO convertToVO(Budget budget, String userId, String month) {
        BigDecimal expense = baseMapper.getMonthlyExpense(userId, month);
        if (expense == null) {
            expense = BigDecimal.ZERO;
        }

        BudgetVO vo = new BudgetVO();
        vo.setId(budget.getId());
        vo.setUserId(budget.getUserId());
        vo.setMonth(month);
        vo.setAmount(budget.getAmount());
        vo.setUseAmount(expense);
        vo.setRemainingAmount(budget.getAmount().subtract(expense));
        vo.setUsagePercent(expense.multiply(BigDecimal.valueOf(100))
                .divide(budget.getAmount(), 2, RoundingMode.HALF_UP));
        vo.setCreateTime(budget.getCreateTime());
        vo.setUpdateTime(budget.getUpdateTime());
        return vo;
    }

    private LocalDate parseMonth(String month) {
        return YearMonth.parse(month, MONTH_FMT).atDay(1);
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 4: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBudgetService.java \
        chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BudgetServiceImpl.java
git commit -m "feat(budget): 实现预算服务层业务逻辑"
```

---

## Task 4: 后端 - BudgetController

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BudgetController.java`

- [ ] **Step 1: 创建 BudgetController**

```java
package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.dto.SetBudgetDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IBudgetService;
import com.samoy.chuanbillserver.vo.BudgetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/budget")
@Tag(name = "budget", description = "预算管理相关接口")
public class BudgetController {

    @Resource
    private IBudgetService budgetService;

    @GetMapping("/current")
    @Operation(summary = "获取当月预算", description = "获取指定月份的预算信息，含实时计算的已用金额")
    public Result<BudgetVO> getCurrentBudget(
            @Parameter(description = "月份，格式 YYYY-MM，默认当月") @RequestParam(required = false) String month) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(budgetService.getCurrentBudget(userId, month));
    }

    @PostMapping("/set")
    @Operation(summary = "设置预算", description = "设置或修改当月预算金额")
    public Result<BudgetVO> setBudget(@Validated @RequestBody SetBudgetDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(budgetService.setBudget(userId, dto));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除预算", description = "删除当月预算")
    public Result<Boolean> deleteBudget() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(budgetService.deleteBudget(userId));
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BudgetController.java
git commit -m "feat(budget): 添加预算管理 Controller 接口"
```

---

## Task 5: 后端 - BillServiceImpl 集成预算预警

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java`

- [ ] **Step 1: 注入 IBudgetService**

在 `BillServiceImpl` 类中现有的 `@Resource` 注入区域添加：

```java
@Resource
private IBudgetService budgetService;
```

- [ ] **Step 2: 在 addBill() 中添加预警检查**

在 `addBill()` 方法中，`this.save(bill)` 之后、家庭通知判断之前插入：

```java
boolean saved = this.save(bill);
// 预算预警检查
if (saved && "expense".equals(bill.getType())) {
    try {
        budgetService.checkBudgetAlert(userId);
    } catch (Exception ignored) {
        // 预算检查失败不影响账单保存
    }
}
if (saved && bill.getFamilyId() != null) {
```

- [ ] **Step 3: 在 updateBill() 中添加预警检查**

`updateBill()` 方法末尾的 `return this.updateById(bill);` 需要改为：

```java
boolean updated = this.updateById(bill);
// 预算预警检查（仅支出类型账单更新时触发）
if (updated && "expense".equals(bill.getType())) {
    try {
        budgetService.checkBudgetAlert(userId);
    } catch (Exception ignored) {
        // 预算检查失败不影响账单更新
    }
}
return updated;
```

- [ ] **Step 4: 验证编译**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java
git commit -m "feat(budget): 账单保存后集成预算预警检查"
```

---

## Task 6: 后端 - 通知设置白名单

**Files:**
- Modify: `chuan-bill-server/src/main/resources/application.yaml`

- [ ] **Step 1: 添加白名单键**

在 `application.yaml` 的 `preference.allowed-keys` 列表末尾添加：

```yaml
    - notification.budget.enabled
```

完整列表变为：

```yaml
preference:
  allowed-keys:
    - notification.master.enabled
    - notification.billReminder.enabled
    - notification.billReminder.time
    - notification.family.enabled
    - notification.system.enabled
    - notification.budget.enabled
```

- [ ] **Step 2: 验证编译**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/resources/application.yaml
git commit -m "feat(budget): 添加预算提醒偏好键到白名单"
```

---

## Task 7: 前端 - API 定义生成

**Files:**
- Regenerate: `chuan-bill-app/src/api/apiDefinitions.ts`
- Regenerate: `chuan-bill-app/src/api/globals.d.ts`

- [ ] **Step 1: 确保后端已启动**

后端需要运行中才能生成 API 定义。确认 `http://localhost:8080` 可访问。

- [ ] **Step 2: 生成 API 定义**

```bash
cd chuan-bill-app && pnpm alova-gen
```

- [ ] **Step 3: 验证生成的预算 API**

检查 `src/api/globals.d.ts` 中是否包含以下内容：
- `BudgetVO` 类型定义
- `SetBudgetDTO` 类型定义
- `Apis.budget.getCurrentBudget` 方法
- `Apis.budget.setBudget` 方法
- `Apis.budget.deleteBudget` 方法

- [ ] **Step 4: 运行 alova-api-fix skill（如需要）**

如果生成的类型有 amount 字段类型问题（应为 string 而非 number），运行 alova-api-fix skill 修复。

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-app/src/api/apiDefinitions.ts chuan-bill-app/src/api/globals.d.ts
git commit -m "feat(budget): 生成预算管理 API 定义"
```

---

## Task 8: 前端 - budgetStore

**Files:**
- Create: `chuan-bill-app/src/store/budgetStore.ts`

- [ ] **Step 1: 创建 budgetStore**

```typescript
import { defineStore } from 'pinia'

export const useBudgetStore = defineStore('budget', () => {
  const currentBudget = ref<BudgetVO | null>(null)
  const loading = ref(false)

  async function fetchBudget(month?: string) {
    loading.value = true
    try {
      const params: Record<string, string> = {}
      if (month) {
        params.month = month
      }
      const res = await Apis.budget.getCurrentBudget({ params })
      if (res.success) {
        currentBudget.value = res.data ?? null
      }
    } catch {
      // 静默失败
    } finally {
      loading.value = false
    }
  }

  async function setBudget(amount: number) {
    const res = await Apis.budget.setBudget({
      data: { amount },
    })
    if (res.success) {
      currentBudget.value = res.data ?? null
    }
    return res
  }

  async function deleteBudget() {
    const res = await Apis.budget.deleteBudget()
    if (res.success) {
      currentBudget.value = null
    }
    return res
  }

  return {
    currentBudget,
    loading,
    fetchBudget,
    setBudget,
    deleteBudget,
  }
})
```

- [ ] **Step 2: 验证类型检查**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/store/budgetStore.ts
git commit -m "feat(budget): 添加预算状态管理 store"
```

---

## Task 9: 前端 - BudgetCard 组件

**Files:**
- Create: `chuan-bill-app/src/pages/statistics/components/BudgetCard.vue`

- [ ] **Step 1: 创建 BudgetCard 组件**

```vue
<script setup lang="ts">
defineOptions({
  name: 'BudgetCard',
})

const props = defineProps<{
  month: string
}>()

const user = useUserStore()
const budgetStore = useBudgetStore()
const showSettingPopup = ref(false)

const budget = computed(() => budgetStore.currentBudget)
const loading = computed(() => budgetStore.loading)

// 进度条颜色
const progressColor = computed(() => {
  if (!budget.value) return 'var(--color-primary)'
  const percent = Number(budget.value.usagePercent)
  if (percent > 100) return '#ef4444'
  if (percent >= 80) return '#f97316'
  if (percent >= 60) return '#eab308'
  return '#22c55e'
})

// 进度条宽度（限制最大 100%）
const progressWidth = computed(() => {
  if (!budget.value) return '0%'
  const percent = Math.min(Number(budget.value.usagePercent), 100)
  return `${percent}%`
})

// 是否超预算
const isOverBudget = computed(() => {
  if (!budget.value) return false
  return Number(budget.value.usagePercent) > 100
})

// 超支金额
const overAmount = computed(() => {
  if (!budget.value || !isOverBudget.value) return '0.00'
  return (Number(budget.value.useAmount) - Number(budget.value.amount)).toFixed(2)
})

// 是否当月
const isCurrentMonth = computed(() => {
  return props.month === dayjs().format('YYYY-MM')
})

function openSetting() {
  if (!isCurrentMonth.value) return
  showSettingPopup.value = true
}

// 月份变化时获取预算
watch(() => props.month, (newMonth) => {
  if (user.isLoggedIn && newMonth) {
    budgetStore.fetchBudget(newMonth)
  }
}, { immediate: true })

// 登录状态变化时获取预算
watch(() => user.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    budgetStore.fetchBudget(props.month)
  }
})
</script>

<template>
  <view v-if="user.isLoggedIn" class="mx-3">
    <!-- 有预算 -->
    <view
      v-if="budget"
      class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]"
      :class="isCurrentMonth && 'active:scale-98 transition-transform'"
      @click="openSetting"
    >
      <view class="mb-3 flex items-center justify-between">
        <text class="text-sm text-gray-500 font-medium">
          本月预算
        </text>
        <text v-if="isCurrentMonth" class="text-xs text-gray-400">
          点击修改
        </text>
      </view>

      <!-- 进度条 -->
      <view class="mb-3 h-2 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-700">
        <view
          class="h-full rounded-full transition-all duration-500"
          :style="{ width: progressWidth, backgroundColor: progressColor }"
        />
      </view>

      <!-- 金额信息 -->
      <view class="flex items-center justify-between">
        <view>
          <text class="text-xs text-gray-400">
            已用
          </text>
          <text class="ml-1 text-sm font-bold" :style="{ color: progressColor }">
            ¥{{ budget.useAmount }}
          </text>
        </view>
        <view>
          <text class="text-xs text-gray-400">
            预算
          </text>
          <text class="ml-1 text-sm font-bold">
            ¥{{ budget.amount }}
          </text>
        </view>
      </view>

      <!-- 剩余/超支提示 -->
      <view class="mt-2 flex items-center justify-between">
        <text v-if="isOverBudget" class="text-xs text-red-500 font-medium">
          超支 ¥{{ overAmount }}
        </text>
        <text v-else class="text-xs text-gray-400">
          剩余 ¥{{ budget.remainingAmount }}
        </text>
        <text class="text-xs font-medium" :style="{ color: progressColor }">
          {{ budget.usagePercent }}%
        </text>
      </view>
    </view>

    <!-- 无预算 -->
    <view
      v-else
      class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]"
      :class="isCurrentMonth && 'active:scale-98 transition-transform cursor-pointer'"
      @click="openSetting"
    >
      <view class="flex items-center justify-center gap-2 py-2">
        <view class="i-lucide:wallet h-4 w-4 text-gray-400" />
        <text class="text-sm text-gray-400">
          {{ isCurrentMonth ? '尚未设置预算，点击设置' : '该月未设置预算' }}
        </text>
      </view>
    </view>

    <!-- 设置弹窗 -->
    <BudgetSettingPopup v-model="showSettingPopup" />
  </view>
</template>
```

- [ ] **Step 2: 验证类型检查**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/pages/statistics/components/BudgetCard.vue
git commit -m "feat(budget): 添加预算进度卡片组件"
```

---

## Task 10: 前端 - BudgetSettingPopup 组件

**Files:**
- Create: `chuan-bill-app/src/pages/statistics/components/BudgetSettingPopup.vue`

- [ ] **Step 1: 创建 BudgetSettingPopup 组件**

```vue
<script setup lang="ts">
defineOptions({
  name: 'BudgetSettingPopup',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

const modelValue = defineModel<boolean>({ default: false })
const budgetStore = useBudgetStore()
const toast = useGlobalToast()
const message = useGlobalMessage()

const amount = ref('')
const saving = ref(false)
const deleting = ref(false)

const hasBudget = computed(() => budgetStore.currentBudget !== null)

// 打开时加载当前预算金额
watch(modelValue, (visible) => {
  if (visible && budgetStore.currentBudget) {
    amount.value = String(budgetStore.currentBudget.amount)
  } else if (visible) {
    amount.value = ''
  }
})

async function onSave() {
  const numAmount = Number(amount.value)
  if (!numAmount || numAmount <= 0) {
    toast.warning('请输入有效的预算金额')
    return
  }
  saving.value = true
  try {
    const res = await budgetStore.setBudget(numAmount)
    if (res.success) {
      toast.success(hasBudget.value ? '预算已更新' : '预算已设置')
      modelValue.value = false
    }
  } catch {
    toast.error('操作失败')
  } finally {
    saving.value = false
  }
}

function onDelete() {
  message.confirm({
    title: '提示',
    msg: '确定删除当月预算吗？',
    confirmButtonProps: { type: 'error' },
    success: async (res) => {
      if (res.action === 'confirm') {
        deleting.value = true
        try {
          const result = await budgetStore.deleteBudget()
          if (result.success) {
            toast.success('预算已删除')
            modelValue.value = false
          }
        } catch {
          toast.error('删除失败')
        } finally {
          deleting.value = false
        }
      }
    },
  })
}
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    position="bottom"
    closable
    :z-index="999"
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    title="设置预算"
  >
    <view class="p-4">
      <!-- 金额输入 -->
      <view class="mb-6 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
        <text class="mb-2 block text-xs text-gray-500">
          预算金额
        </text>
        <view class="flex items-center gap-2">
          <text class="text-2xl text-primary font-bold">
            ¥
          </text>
          <wd-input
            v-model="amount"
            type="number"
            placeholder="请输入月度预算金额"
            custom-class="flex-1"
            :border="false"
          />
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="flex flex-col gap-3">
        <wd-button type="primary" block :loading="saving" @click="onSave">
          {{ hasBudget ? '修改预算' : '设置预算' }}
        </wd-button>
        <wd-button
          v-if="hasBudget"
          type="error"
          plain
          block
          :loading="deleting"
          @click="onDelete"
        >
          删除预算
        </wd-button>
      </view>
    </view>
  </wd-action-sheet>
</template>
```

- [ ] **Step 2: 验证类型检查**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/pages/statistics/components/BudgetSettingPopup.vue
git commit -m "feat(budget): 添加预算设置弹窗组件"
```

---

## Task 11: 前端 - 统计页集成

**Files:**
- Modify: `chuan-bill-app/src/pages/statistics/index.vue`

- [ ] **Step 1: 导入 BudgetCard**

在 `<script setup>` 的 import 区域（或组件自动导入区域之后）确保 BudgetCard 可用。由于项目使用了自动导入，组件会自动注册，无需手动导入。

- [ ] **Step 2: 在模板中插入 BudgetCard**

在统计页模板中，Overview 卡片（收支概览）的 `</view>` 闭合标签之后、CategoryChart 之前插入：

```vue
<!-- 预算卡片 -->
<BudgetCard :month="currentMonth" />
```

具体位置：找到 Overview 卡片的闭合标签（包含收入/支出/结余的那个 `</view>`），在其后添加 BudgetCard。

- [ ] **Step 3: 在 onShow 中获取预算数据**

在 `onShow` 回调中，`fetchAll()` 调用之后添加：

```typescript
if (user.isLoggedIn) {
  budgetStore.fetchBudget(currentMonth.value)
}
```

确保 `budgetStore` 已通过 `useBudgetStore()` 初始化。

- [ ] **Step 4: 监听月份变化**

在现有的 `watch(currentMonth, ...)` 中，添加预算获取：

```typescript
watch(currentMonth, (newMonth) => {
  // ... 现有的统计逻辑
  if (user.isLoggedIn) {
    budgetStore.fetchBudget(newMonth)
  }
})
```

- [ ] **Step 5: 验证类型检查**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 6: 提交**

```bash
git add chuan-bill-app/src/pages/statistics/index.vue
git commit -m "feat(budget): 统计页集成预算卡片"
```

---

## Task 12: 前端 - 通知设置新增预算提醒

**Files:**
- Modify: `chuan-bill-app/src/pages/mine/components/NotificationSettingsPopup.vue`

- [ ] **Step 1: 添加 settings 状态**

在 `settings` ref 中新增 `budgetNotificationEnabled` 字段：

```typescript
const settings = ref({
  masterEnabled: false,
  billReminderEnabled: false,
  billReminderTime: '20:00',
  familyNotificationEnabled: false,
  systemNotificationEnabled: true,
  budgetNotificationEnabled: true,  // 新增
})
```

- [ ] **Step 2: 在 onMounted 中加载偏好值**

在 `onMounted` 的偏好加载逻辑中，添加：

```typescript
if (prefs['notification.budget.enabled'] !== undefined) {
  settings.value.budgetNotificationEnabled = prefs['notification.budget.enabled'] === 'true'
}
```

- [ ] **Step 3: 在模板中添加预算提醒开关**

在"家庭通知"条目之后添加：

```vue
<!-- 预算提醒 -->
<view class="mb-4 flex items-center justify-between rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
  <view>
    <text class="block text-sm font-medium">
      预算提醒
    </text>
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

- [ ] **Step 4: 验证类型检查**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-app/src/pages/mine/components/NotificationSettingsPopup.vue
git commit -m "feat(budget): 通知设置新增预算提醒开关"
```

---

## Task 13: 最终验证

- [ ] **Step 1: 后端编译检查**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 2: 前端类型检查**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 3: 前端 lint 检查**

```bash
cd chuan-bill-app && pnpm lint:fix
```

- [ ] **Step 4: 后端格式检查**

```bash
cd chuan-bill-server && mvn spotless:apply
```

- [ ] **Step 5: 功能验证清单**

手动验证以下场景：
1. 启动后端 + 前端，登录账户
2. 进入统计页，看到"尚未设置预算，点击设置"引导
3. 点击引导，弹出设置弹窗，输入金额保存
4. 预算卡片展示进度条、已用/总预算金额
5. 添加一笔支出账单，预算卡片已用金额更新
6. 支出达到 80% 时，消息中心收到"预算预警"通知
7. 支出超过 100% 时，消息中心收到"预算超支"通知
8. 在"我的" → "通知设置"中，可以看到"预算提醒"开关
9. 关闭"通知总开关"后，不再发送预算通知
10. 删除预算后，卡片恢复引导状态
