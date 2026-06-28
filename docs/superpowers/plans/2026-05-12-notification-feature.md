# 通知功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现站内消息通知功能，包括每日记账提醒（Quartz 调度）、家庭成员记账通知、账单详情权限修复、前端消息列表增强。

**Architecture:** 后端新增用户偏好模块（key-value 表）存储通知设置，使用 spring-boot-starter-quartz 实现每日记账提醒调度，复用现有 IMessageService 发送消息。前端新增 preference API 定义，增强消息列表页支持类型筛选和点击跳转。

**Tech Stack:** Spring Boot 3.5.11, MyBatis-Plus 3.5.15, spring-boot-starter-quartz (RAMJobStore), Vue 3, uni-app, Alova.js, wot-design-uni

---

## Task 1: 数据库 - 用户偏好表

**Files:**
- Modify: `chuan-bill-server/init.sql:233` (在 t_message 表定义之后)

- [ ] **Step 1: 在 init.sql 中添加 t_user_preference 建表语句**

在 `t_message` 表定义（第 233 行 `) ENGINE = InnoDB...`）之后，`t_ai_suggestion` 表定义之前，插入：

```sql
-- ===============================
-- 用户偏好设置表(t_user_preference)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_user_preference` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `pref_key` VARCHAR(100) NOT NULL COMMENT '偏好键名',
    `pref_value` TEXT COMMENT '偏好值（JSON字符串）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_key` (`user_id`, `pref_key`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户偏好设置表';
```

- [ ] **Step 2: 执行建表语句**

在 MySQL 中执行上述 SQL，确认表创建成功。

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/init.sql
git commit -m "feat(db): 添加用户偏好设置表 t_user_preference"
```

---

## Task 2: 后端 - UserPreference 实体和 Mapper

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/entity/UserPreference.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dao/UserPreferenceMapper.java`

- [ ] **Step 1: 创建 UserPreference 实体**

创建文件 `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/entity/UserPreference.java`：

```java
package com.samoy.chuanbillserver.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("t_user_preference")
public class UserPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("pref_key")
    private String prefKey;

    @TableField("pref_value")
    private String prefValue;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 UserPreferenceMapper**

创建文件 `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dao/UserPreferenceMapper.java`：

```java
package com.samoy.chuanbillserver.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samoy.chuanbillserver.entity.UserPreference;

public interface UserPreferenceMapper extends BaseMapper<UserPreference> {}
```

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/entity/UserPreference.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dao/UserPreferenceMapper.java
git commit -m "feat: 添加 UserPreference 实体和 Mapper"
```

---

## Task 3: 后端 - UserPreference Service 和 Controller

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IUserPreferenceService.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserPreferenceServiceImpl.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserPreferenceController.java`

- [ ] **Step 1: 创建 IUserPreferenceService 接口**

创建文件 `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IUserPreferenceService.java`：

```java
package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.entity.UserPreference;
import java.util.Map;

public interface IUserPreferenceService extends IService<UserPreference> {

    /**
     * 获取单个偏好值，不存在返回 null
     */
    String getValue(String userId, String key);

    /**
     * 设置单个偏好（INSERT ON DUPLICATE UPDATE）
     */
    void setValue(String userId, String key, String value);

    /**
     * 获取用户所有偏好，返回 Map
     */
    Map<String, String> getAll(String userId);

    /**
     * 删除某个偏好
     */
    void deleteValue(String userId, String key);
}
```

- [ ] **Step 2: 创建 UserPreferenceServiceImpl**

创建文件 `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserPreferenceServiceImpl.java`：

```java
package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.UserPreferenceMapper;
import com.samoy.chuanbillserver.entity.UserPreference;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserPreferenceServiceImpl extends ServiceImpl<UserPreferenceMapper, UserPreference>
        implements IUserPreferenceService {

    @Override
    public String getValue(String userId, String key) {
        UserPreference pref = this.getOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .eq(UserPreference::getPrefKey, key));
        return pref != null ? pref.getPrefValue() : null;
    }

    @Override
    public void setValue(String userId, String key, String value) {
        UserPreference existing = this.getOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .eq(UserPreference::getPrefKey, key));
        if (existing != null) {
            existing.setPrefValue(value);
            this.updateById(existing);
        } else {
            UserPreference pref = new UserPreference();
            pref.setId(IdUtil.fastSimpleUUID());
            pref.setUserId(userId);
            pref.setPrefKey(key);
            pref.setPrefValue(value);
            this.save(pref);
        }
    }

    @Override
    public Map<String, String> getAll(String userId) {
        List<UserPreference> list = this.list(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId));
        Map<String, String> map = new HashMap<>();
        for (UserPreference pref : list) {
            map.put(pref.getPrefKey(), pref.getPrefValue());
        }
        return map;
    }

    @Override
    public void deleteValue(String userId, String key) {
        this.remove(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .eq(UserPreference::getPrefKey, key));
    }
}
```

- [ ] **Step 3: 创建 UserPreferenceController**

创建文件 `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserPreferenceController.java`：

```java
package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preference")
@Tag(name = "preference", description = "用户偏好设置相关接口")
public class UserPreferenceController {

    @Resource
    private IUserPreferenceService userPreferenceService;

    @GetMapping("/get")
    @Operation(summary = "获取单个偏好", description = "根据键名获取单个偏好值")
    public Result<String> get(
            @Parameter(description = "偏好键名", required = true) @RequestParam String key) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(userPreferenceService.getValue(userId, key));
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有偏好", description = "获取用户所有偏好设置")
    public Result<Map<String, String>> getAll() {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(userPreferenceService.getAll(userId));
    }

    @PostMapping("/set")
    @Operation(summary = "设置偏好", description = "设置单个偏好值")
    public Result<Boolean> set(
            @Parameter(description = "偏好键名", required = true) @RequestParam String key,
            @Parameter(description = "偏好值", required = true) @RequestParam String value) {
        String userId = StpUtil.getLoginIdAsString();
        userPreferenceService.setValue(userId, key, value);
        return Result.success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除偏好", description = "删除单个偏好设置")
    public Result<Boolean> delete(
            @Parameter(description = "偏好键名", required = true) @RequestParam String key) {
        String userId = StpUtil.getLoginIdAsString();
        userPreferenceService.deleteValue(userId, key);
        return Result.success(true);
    }
}
```

- [ ] **Step 4: 运行 Spotless 格式化**

```bash
cd chuan-bill-server && mvn spotless:apply
```

- [ ] **Step 5: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IUserPreferenceService.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserPreferenceServiceImpl.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserPreferenceController.java
git commit -m "feat: 添加用户偏好 Service 和 Controller"
```

---

## Task 4: 后端 - Quartz 每日记账提醒

**Files:**
- Modify: `chuan-bill-server/pom.xml` (添加 quartz 依赖)
- Modify: `chuan-bill-server/src/main/resources/application.yaml` (添加 quartz 配置)
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/QuartzConfig.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/job/BillReminderJob.java`

- [ ] **Step 1: 在 pom.xml 中添加 Quartz 依赖**

在 `pom.xml` 的 `<dependencies>` 中，在 `spring-boot-starter-websocket` 依赖之后添加：

```xml
<!-- Quartz 调度器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

- [ ] **Step 2: 在 application.yaml 中添加 Quartz 配置**

在 `application.yaml` 的 `spring:` 下添加：

```yaml
  quartz:
    job-store-type: memory
```

- [ ] **Step 3: 创建 QuartzConfig**

创建文件 `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/QuartzConfig.java`：

```java
package com.samoy.chuanbillserver.config;

import com.samoy.chuanbillserver.job.BillReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail billReminderJobDetail() {
        return JobBuilder.newJob(BillReminderJob.class)
                .withIdentity("billReminderJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger billReminderTrigger(JobDetail billReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(billReminderJobDetail)
                .withIdentity("billReminderTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))
                .build();
    }
}
```

- [ ] **Step 4: 创建 BillReminderJob**

创建文件 `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/job/BillReminderJob.java`：

```java
package com.samoy.chuanbillserver.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.entity.UserPreference;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.IMessageService;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillReminderJob implements Job {

    private static final String KEY_ENABLED = "notification.billReminder.enabled";
    private static final String KEY_TIME = "notification.billReminder.time";
    private static final String KEY_LAST_SENT = "notification.billReminder.lastSentDate";

    @Resource
    private IUserPreferenceService userPreferenceService;

    @Resource
    private IBillService billService;

    @Resource
    private IMessageService messageService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        log.debug("BillReminderJob 执行，当前时间: {}, 今天: {}", currentTime, today);

        // 查询所有开启提醒且时间匹配当前时间的用户
        List<UserPreference> enabledPrefs = userPreferenceService.list(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getPrefKey, KEY_ENABLED)
                        .eq(UserPreference::getPrefValue, "true"));

        for (UserPreference enabledPref : enabledPrefs) {
            String userId = enabledPref.getUserId();

            try {
                // 检查提醒时间是否匹配
                String reminderTime = userPreferenceService.getValue(userId, KEY_TIME);
                if (!currentTime.equals(reminderTime)) {
                    continue;
                }

                // 检查是否今天已发送过
                String lastSentDate = userPreferenceService.getValue(userId, KEY_LAST_SENT);
                if (today.equals(lastSentDate)) {
                    continue;
                }

                // 检查今天是否已有账单
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
                long billCount = billService.count(
                        new LambdaQueryWrapper<Bill>()
                                .eq(Bill::getUserId, userId)
                                .ge(Bill::getTime, startOfDay)
                                .le(Bill::getTime, endOfDay));

                if (billCount > 0) {
                    // 今天已有账单，记录已发送避免重复检查
                    userPreferenceService.setValue(userId, KEY_LAST_SENT, today);
                    continue;
                }

                // 发送提醒消息
                messageService.sendMessage(
                        userId,
                        "记账提醒",
                        "您今天还没有记账哦，点击记录一笔",
                        "system",
                        null,
                        null);

                // 记录已发送日期
                userPreferenceService.setValue(userId, KEY_LAST_SENT, today);
                log.info("已发送记账提醒给用户: {}", userId);
            } catch (Exception e) {
                log.error("发送记账提醒失败，用户: {}", userId, e);
            }
        }
    }
}
```

- [ ] **Step 5: 运行 Spotless 格式化**

```bash
cd chuan-bill-server && mvn spotless:apply
```

- [ ] **Step 6: 验证编译通过**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 7: Commit**

```bash
git add chuan-bill-server/pom.xml \
  chuan-bill-server/src/main/resources/application.yaml \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/QuartzConfig.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/job/BillReminderJob.java
git commit -m "feat: 添加 Quartz 每日记账提醒调度"
```

---

## Task 5: 后端 - 家庭成员记账通知

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java:88-106`

- [ ] **Step 1: 在 BillServiceImpl 中注入 IMessageService 和 ICategoryService**

`ICategoryService` 已注入（第 53-54 行）。需要添加 `IMessageService` 注入。

在 `BillServiceImpl.java` 第 63 行 `private IUserService userService;` 之后添加：

```java
@Resource
private IMessageService messageService;
```

同时在文件顶部的 import 区域添加（如果不存在）：

```java
import com.samoy.chuanbillserver.entity.Category;
import com.samoy.chuanbillserver.vo.FamilyMemberVO;
import java.util.List;
```

注意：`Category` 和 `List` 的 import 已存在，只需确认 `FamilyMemberVO` 的 import：

```java
import com.samoy.chuanbillserver.vo.FamilyMemberVO;
```

- [ ] **Step 2: 修改 addBill 方法，添加家庭账单通知逻辑**

将 `addBill` 方法（第 88-106 行）修改为：

```java
@Override
public boolean addBill(String userId, AddBillDTO addBillDTO) {
    Bill bill = new Bill();
    bill.setUserId(userId);
    bill.setName(addBillDTO.getName());
    bill.setCategoryId(addBillDTO.getCategoryId());
    bill.setPaymentMethodId(addBillDTO.getPaymentMethodId());
    bill.setType(addBillDTO.getType());
    bill.setAmount(addBillDTO.getAmount());
    bill.setTime(addBillDTO.getTime());
    bill.setRemark(addBillDTO.getRemark());
    bill.setSource(addBillDTO.getSource());
    if (addBillDTO.getFamilyId() != null) {
        if (!familyService.isMember(userId, addBillDTO.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }
        bill.setFamilyId(addBillDTO.getFamilyId());
    }
    boolean saved = this.save(bill);

    // 家庭账单通知：通知家庭其他成员
    if (saved && bill.getFamilyId() != null) {
        sendFamilyBillNotification(bill, userId);
    }

    return saved;
}
```

- [ ] **Step 3: 添加 sendFamilyBillNotification 私有方法**

在 `BillServiceImpl.java` 的 `buildQueryWrapper` 方法之前（约第 222 行）添加：

```java
/**
 * 发送家庭账单通知给家庭其他成员
 */
private void sendFamilyBillNotification(Bill bill, String userId) {
    try {
        List<FamilyMemberVO> members = familyService.getMembers(userId, bill.getFamilyId());

        // 获取记账人昵称
        String nickname = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .map(FamilyMemberVO::getUserNickname)
                .orElse("未知用户");

        // 获取分类名
        Category category = categoryService.getById(bill.getCategoryId());
        String categoryName = category != null ? category.getName() : "未分类";

        String content = String.format(
                "{\"categoryName\":\"%s\",\"amount\":\"%s\",\"type\":\"%s\"}",
                categoryName, bill.getAmount().toPlainString(), bill.getType());

        for (FamilyMemberVO member : members) {
            if (member.getUserId().equals(userId)) {
                continue;
            }
            messageService.sendMessage(
                    member.getUserId(),
                    nickname + " 记了一笔账单",
                    content,
                    "bill",
                    bill.getId(),
                    "bill");
        }
    } catch (Exception e) {
        log.error("发送家庭账单通知失败，账单ID: {}", bill.getId(), e);
    }
}
```

- [ ] **Step 4: 运行 Spotless 格式化**

```bash
cd chuan-bill-server && mvn spotless:apply
```

- [ ] **Step 5: 验证编译通过**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 6: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java
git commit -m "feat: 家庭账单创建时通知其他成员"
```

---

## Task 6: 后端 - 账单详情权限修复

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java:182-193`

- [ ] **Step 1: 修改 getBillDetail 方法**

将 `getBillDetail` 方法（第 182-193 行）修改为：

```java
@Override
public BillVO getBillDetail(String userId, String billId) {
    Bill bill = this.getById(billId);
    if (bill == null) {
        throw new BusinessException(ResultEnum.BILL_NOT_FOUND);
    }
    if (!Objects.equals(bill.getUserId(), userId)) {
        // 如果是家庭共享账单，检查是否为家庭成员
        if (bill.getFamilyId() == null
                || !familyService.isMember(userId, bill.getFamilyId())) {
            throw new BusinessException(ResultEnum.BILL_NOT_ALLOW_VIEW);
        }
    }
    // 单个账单查询，直接使用简化版本
    return this.getBillVO(bill);
}
```

- [ ] **Step 2: 运行 Spotless 格式化**

```bash
cd chuan-bill-server && mvn spotless:apply
```

- [ ] **Step 3: 验证编译通过**

```bash
cd chuan-bill-server && mvn compile -q
```

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java
git commit -m "fix: 家庭成员可查看共享账单详情（仅查看）"
```

---

## Task 7: 前端 - 添加 Preference API 定义

**Files:**
- Modify: `chuan-bill-app/src/api/apiDefinitions.ts`

- [ ] **Step 1: 在 apiDefinitions.ts 中添加 preference API**

在 `apiDefinitions.ts` 的 `POST` 部分末尾（`auth.loginByPassword` 之后）添加：

```ts
'preference.set': ['POST', '/preference/set'],
'preference.delete': ['DELETE', '/preference/delete'],
```

在 `GET` 部分末尾（`ai.analysis` 之前）添加：

```ts
'preference.get': ['GET', '/preference/get'],
'preference.getAll': ['GET', '/preference/all'],
```

- [ ] **Step 2: 重新生成 globals.d.ts**

```bash
cd chuan-bill-app && pnpm alova-gen
```

- [ ] **Step 3: 修复生成的类型**

```bash
cd chuan-bill-app && pnpm alova-fix
```

如果 `alova-fix` 脚本不存在，手动检查 `globals.d.ts` 中新增的 preference 类型是否正确。

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-app/src/api/apiDefinitions.ts \
  chuan-bill-app/src/api/globals.d.ts
git commit -m "feat: 添加 preference API 定义和类型"
```

---

## Task 8: 前端 - 通知设置对接后端

**Files:**
- Modify: `chuan-bill-app/src/pages/mine/components/NotificationSettingsPopup.vue`

- [ ] **Step 1: 修改 NotificationSettingsPopup.vue 的 script 部分**

将整个 `<script setup>` 块替换为：

```vue
<script setup lang="ts">
defineOptions({
  name: 'NotificationSettingsPopup',
})

const modelValue = defineModel<boolean>({ default: false })
const toast = useGlobalToast()

// 通知设置
const settings = ref({
  pushEnabled: false,
  billReminderEnabled: false,
  billReminderTime: '20:00',
  familyNotificationEnabled: false,
})

const timePickerVisible = ref(false)
const timeValue = ref('20:00')

// 加载设置
onMounted(async () => {
  try {
    const res = await Apis.preference.getAll()
    if (res.success && res.data) {
      const prefs = res.data
      if (prefs['notification.billReminder.enabled'] !== undefined) {
        settings.value.billReminderEnabled = prefs['notification.billReminder.enabled'] === 'true'
      }
      if (prefs['notification.billReminder.time'] !== undefined) {
        settings.value.billReminderTime = prefs['notification.billReminder.time']
        timeValue.value = prefs['notification.billReminder.time']
      }
    }
  }
  catch {
    // 静默失败，使用默认值
  }
})

// 消息推送开关变化
function onPushChange(value: boolean) {
  if (!value) {
    settings.value.billReminderEnabled = false
    settings.value.familyNotificationEnabled = false
  }
  saveSettings()
}

// 保存设置
async function saveSettings() {
  try {
    await Apis.preference.set({
      params: {
        key: 'notification.billReminder.enabled',
        value: String(settings.value.billReminderEnabled),
      },
    })
    await Apis.preference.set({
      params: {
        key: 'notification.billReminder.time',
        value: settings.value.billReminderTime,
      },
    })
    toast.success('设置已保存')
  }
  catch {
    toast.error('保存失败')
  }
}

// 时间选择确认
function onTimeConfirm({ value }: { value: number[] }) {
  settings.value.billReminderTime = `${String(value[0]).padStart(2, '0')}:${String(value[1]).padStart(2, '0')}`
  saveSettings()
}
</script>
```

- [ ] **Step 2: 验证前端编译通过**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-app/src/pages/mine/components/NotificationSettingsPopup.vue
git commit -m "feat: 通知设置对接后端偏好 API"
```

---

## Task 9: 前端 - 消息列表增强

**Files:**
- Modify: `chuan-bill-app/src/pages/message/index.vue`

- [ ] **Step 1: 重写 message/index.vue**

将整个文件替换为：

```vue
<script setup lang="ts">
import type { MessageVO } from '@/api/globals'

definePage({
  name: 'message-list',
  style: {
    navigationBarTitleText: '消息通知',
  },
})

const router = useRouter()
const messageStore = useMessageStore()
const currentPage = ref(1)
const pageSize = 20
const finished = ref(false)
const loading = ref(false)

// 类型筛选 Tab
const activeTab = ref('')
const tabs = [
  { name: '', label: '全部' },
  { name: 'system', label: '系统' },
  { name: 'family', label: '家庭' },
  { name: 'bill', label: '账单' },
]

async function loadMessages(page = 1) {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page,
      size: pageSize,
    }
    if (activeTab.value) {
      params.type = activeTab.value
    }
    const result = await messageStore.fetchMessageList(params)
    if (result) {
      currentPage.value = page
      finished.value = !result.records || result.records.length < pageSize
    }
    else {
      finished.value = true
    }
  }
  finally {
    loading.value = false
  }
}

onLoad(() => {
  loadMessages(1)
})

// 切换 Tab
function onTabChange(tab: string) {
  activeTab.value = tab
  finished.value = false
  loadMessages(1)
}

// 加载更多
function loadMore() {
  if (!finished.value && !loading.value) {
    loadMessages(currentPage.value + 1)
  }
}

// 全部已读
async function markAllRead() {
  uni.showModal({
    title: '全部标记已读',
    content: '确认将所有消息标记为已读？',
    success: async (res) => {
      if (res.confirm) {
        const success = await messageStore.markAllAsRead()
        if (success) {
          const toast = useGlobalToast()
          toast.show('已全部标记为已读')
        }
      }
    },
  })
}

// 点击消息
async function handleMessageClick(msg: MessageVO) {
  // 标记已读
  if (msg.status === 0) {
    await messageStore.markAsRead(msg.id!)
  }
  // 根据类型跳转
  if (msg.type === 'bill' && msg.relatedId) {
    router.push(`/pages/bill/index?id=${msg.relatedId}`)
  }
  else if (msg.type === 'family') {
    router.push('/pages/family/index')
  }
}

// 解析账单消息 content
function parseBillContent(msg: MessageVO): { categoryName: string, amount: string, type: string } | null {
  if (msg.type !== 'bill' || !msg.content) return null
  try {
    return JSON.parse(msg.content)
  }
  catch {
    return null
  }
}

// 格式化消息内容显示
function getMessageContent(msg: MessageVO): string {
  const billData = parseBillContent(msg)
  if (billData) {
    return `${billData.categoryName} ¥${billData.amount}`
  }
  return msg.content || ''
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 类型筛选 Tab -->
    <view class="mx-3 flex gap-2">
      <view
        v-for="tab in tabs"
        :key="tab.name"
        class="rounded-full px-4 py-1.5 text-xs transition-all"
        :class="activeTab === tab.name
          ? 'bg-primary text-white'
          : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'"
        @click="onTabChange(tab.name)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 顶部操作 -->
    <view v-if="messageStore.hasUnread" class="mx-3 flex justify-end">
      <text class="text-sm text-primary" @click="markAllRead">
        全部已读
      </text>
    </view>

    <!-- 消息列表 -->
    <view v-if="messageStore.messageList.length > 0" class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view
        v-for="(msg, index) in messageStore.messageList"
        :key="msg.id"
        class="p-4"
        :class="[
          index < messageStore.messageList.length - 1 && 'border-b border-gray-100 dark:border-gray-700',
          msg.status === 0 && 'bg-blue-50/50 dark:bg-blue-900/10',
        ]"
        @click="handleMessageClick(msg)"
      >
        <view class="flex items-start gap-3">
          <!-- 未读标记 -->
          <view v-if="msg.status === 0" class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" />
          <view v-else class="mt-1.5 h-2 w-2 shrink-0" />
          <view class="flex-1">
            <view class="flex items-center justify-between">
              <text class="text-sm font-500" :class="msg.status === 0 ? 'text-gray-900 dark:text-white' : 'text-gray-500'">
                {{ msg.title }}
              </text>
              <text class="text-xs text-gray-400">
                {{ msg.createTime }}
              </text>
            </view>
            <!-- 账单类型消息：解析 JSON 渲染 -->
            <view v-if="parseBillContent(msg)" class="mt-1 flex items-center gap-1">
              <text class="text-xs text-gray-600">
                {{ parseBillContent(msg)?.categoryName }}
              </text>
              <text
                class="text-xs font-500"
                :class="parseBillContent(msg)?.type === 'expense' ? 'text-red-400' : 'text-green-500'"
              >
                ¥{{ parseBillContent(msg)?.amount }}
              </text>
            </view>
            <!-- 普通消息：直接显示 content -->
            <text v-else class="mt-1 block text-xs leading-relaxed" :class="msg.status === 0 ? 'text-gray-600' : 'text-gray-400'">
              {{ msg.content }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!loading" class="mx-3 rounded-2xl bg-white p-8 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="mb-3 flex justify-center">
        <view class="h-16 w-16 flex items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
          <view class="i-lucide:bell h-8 w-8 text-gray-400" />
        </view>
      </view>
      <text class="block text-sm text-gray-500">
        暂无消息
      </text>
    </view>

    <!-- 加载更多 -->
    <view v-if="loading" class="py-4 text-center">
      <wd-loading />
    </view>
    <view v-else-if="finished && messageStore.messageList.length > 0" class="py-4 text-center">
      <text class="text-xs text-gray-400">
        没有更多消息了
      </text>
    </view>
    <view v-else-if="!finished && messageStore.messageList.length > 0" class="py-4 text-center">
      <text class="text-sm text-primary" @click="loadMore">
        加载更多
      </text>
    </view>
  </view>
</template>
```

- [ ] **Step 2: 验证前端编译通过**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-app/src/pages/message/index.vue
git commit -m "feat: 消息列表增加类型筛选、点击跳转和账单消息渲染"
```

---

## Task 10: 前端 - 账单页支持从消息跳转

**Files:**
- Modify: `chuan-bill-app/src/pages/bill/index.vue:181-183`

- [ ] **Step 1: 修改 onLoad 支持 id 参数**

将 `bill/index.vue` 中的 `onLoad` 块（第 181-183 行）：

```ts
onLoad(() => {
  refresh()
})
```

替换为：

```ts
async function openBillById(id: string) {
  try {
    const res = await Apis.bill.getBillDetail({ params: { id } })
    if (res.success && res.data) {
      currentBill.value = res.data
      showBillDetailModal.value = true
    }
  }
  catch {
    // 账单不存在或无权查看
  }
}

onLoad((options) => {
  refresh()
  if (options?.id) {
    openBillById(options.id)
  }
})
```

- [ ] **Step 2: 验证前端编译通过**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-app/src/pages/bill/index.vue
git commit -m "feat: 账单页支持从消息通知跳转查看详情"
```

---

## Task 11: 最终验证

- [ ] **Step 1: 后端完整编译**

```bash
cd chuan-bill-server && mvn clean compile -q
```

- [ ] **Step 2: 后端格式检查**

```bash
cd chuan-bill-server && mvn spotless:check
```

- [ ] **Step 3: 前端类型检查**

```bash
cd chuan-bill-app && pnpm type-check
```

- [ ] **Step 4: 前端 Lint 检查**

```bash
cd chuan-bill-app && pnpm lint
```

- [ ] **Step 5: 如有问题，修复后 Commit**

```bash
git add -A
git commit -m "fix: 修复通知功能实现中的问题"
```
