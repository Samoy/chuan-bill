# 通知系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现小川记账应用的通知系统，包括用户偏好管理、账单提醒、家庭通知等功能

**Architecture:** 采用事件驱动方案，使用 Spring Event 解耦业务逻辑与通知逻辑。用户偏好表存储通知设置，Quartz 定时任务处理账单提醒，Spring Event 处理家庭通知的实时推送。

**Tech Stack:** Spring Boot 3, MySQL, Quartz, Spring Event, 极光推送 SDK (io.github.jpush:jiguang-sdk:5.3.0), MyBatis-Plus

---

## 文件结构

### 后端文件

**新建文件：**
- `chuan-bill-server/src/main/resources/db/migration/V20260511__create_user_preference_table.sql` - 数据库迁移脚本
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/entity/UserPreference.java` - 用户偏好实体
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/mapper/UserPreferenceMapper.java` - MyBatis Mapper
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UserPreferenceDTO.java` - 偏好设置 DTO
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IUserPreferenceService.java` - 用户偏好服务接口
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserPreferenceServiceImpl.java` - 用户偏好服务实现
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/INotificationService.java` - 通知服务接口
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/NotificationServiceImpl.java` - 通知服务实现
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserPreferenceController.java` - 用户偏好 API
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/JPushConfig.java` - 极光推送配置
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/scheduler/BillReminderScheduler.java` - 账单提醒定时任务
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/event/FamilyMemberChangedEvent.java` - 家庭成员变动事件
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/event/FamilyBillChangedEvent.java` - 家庭账单变更事件
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/event/NotificationEventListener.java` - 事件监听器

**修改文件：**
- `chuan-bill-server/pom.xml` - 添加依赖
- `chuan-bill-server/src/main/resources/application.yml` - 添加配置
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java` - 注册时创建偏好
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/FamilyServiceImpl.java` - 触发事件
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java` - 触发事件
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBillService.java` - 添加方法

### 前端文件

**新建文件：**
- `chuan-bill-app/src/api/preference.ts` - 偏好设置 API

**修改文件：**
- `chuan-bill-app/src/pages/mine/components/NotificationSettingsPopup.vue` - 通知设置弹窗

---

## 任务分解

### Task 1: 数据库与依赖配置

**Files:**
- Modify: `chuan-bill-server/pom.xml`
- Create: `chuan-bill-server/src/main/resources/db/migration/V20260511__create_user_preference_table.sql`
- Modify: `chuan-bill-server/src/main/resources/application.yml`

- [ ] **Step 1: 添加 Maven 依赖**

```xml
<!-- pom.xml 的 dependencies 中添加 -->
<!-- 极光推送 SDK -->
<dependency>
    <groupId>io.github.jpush</groupId>
    <artifactId>jiguang-sdk</artifactId>
    <version>5.3.0</version>
</dependency>

<!-- Quartz Scheduler -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

- [ ] **Step 2: 创建数据库迁移脚本**

```sql
-- V20260511__create_user_preference_table.sql
CREATE TABLE `t_user_preference` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `push_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '推送总开关，0关闭，1开启',
    `bill_reminder_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账单提醒开关，0关闭，1开启',
    `bill_reminder_time` VARCHAR(5) NOT NULL DEFAULT '20:00' COMMENT '账单提醒时间，格式HH:mm',
    `family_notification_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '家庭通知开关，0关闭，1开启',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    
    UNIQUE KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户偏好设置表';
```

- [ ] **Step 3: 添加配置文件**

```yaml
# application.yml 中添加
jpush:
  app-key: ${JPUSH_APP_KEY:your-app-key}
  master-secret: ${JPUSH_MASTER_SECRET:your-master-secret}
  production: false
```

- [ ] **Step 4: 运行数据库迁移**

```bash
cd chuan-bill-server
# 如果使用 Flyway
mvn flyway:migrate
# 或者手动执行 SQL
mysql -u root -p chuan_bill < src/main/resources/db/migration/V20260511__create_user_preference_table.sql
```

- [ ] **Step 5: Commit**

```bash
git add chuan-bill-server/pom.xml chuan-bill-server/src/main/resources/
git commit -m "chore: 添加极光推送和Quartz依赖，创建用户偏好表"
```

---

### Task 2: 用户偏好实体与 Mapper

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/entity/UserPreference.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/mapper/UserPreferenceMapper.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UserPreferenceDTO.java`

- [ ] **Step 1: 创建 UserPreference 实体**

```java
// UserPreference.java
package com.samoy.chuanbillserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_user_preference")
public class UserPreference {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String userId;
    
    private Boolean pushEnabled;
    
    private Boolean billReminderEnabled;
    
    private String billReminderTime;
    
    private Boolean familyNotificationEnabled;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 2: 创建 UserPreferenceMapper**

```java
// UserPreferenceMapper.java
package com.samoy.chuanbillserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samoy.chuanbillserver.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
```

- [ ] **Step 3: 创建 UserPreferenceDTO**

```java
// UserPreferenceDTO.java
package com.samoy.chuanbillserver.dto;

import lombok.Data;

@Data
public class UserPreferenceDTO {
    private Boolean pushEnabled;
    private Boolean billReminderEnabled;
    private String billReminderTime;
    private Boolean familyNotificationEnabled;
}
```

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/entity/UserPreference.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/mapper/UserPreferenceMapper.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UserPreferenceDTO.java
git commit -m "feat: 添加用户偏好实体、Mapper和DTO"
```

---

### Task 3: 用户偏好服务

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IUserPreferenceService.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserPreferenceServiceImpl.java`

- [ ] **Step 1: 创建 IUserPreferenceService 接口**

```java
// IUserPreferenceService.java
package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.dto.UserPreferenceDTO;
import com.samoy.chuanbillserver.entity.UserPreference;
import java.util.List;

public interface IUserPreferenceService {
    
    /**
     * 获取用户偏好设置
     */
    UserPreferenceDTO getPreference(String userId);
    
    /**
     * 更新用户偏好设置
     */
    void updatePreference(String userId, UserPreferenceDTO dto);
    
    /**
     * 创建默认偏好记录（用户注册时调用）
     */
    void createDefaultPreference(String userId);
    
    /**
     * 查询需要提醒的用户（定时任务使用）
     */
    List<UserPreference> findUsersToRemind(String time);
    
    /**
     * 检查用户是否开启家庭通知
     */
    boolean isFamilyNotificationEnabled(String userId);
}
```

- [ ] **Step 2: 创建 UserPreferenceServiceImpl 实现**

```java
// UserPreferenceServiceImpl.java
package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.samoy.chuanbillserver.dto.UserPreferenceDTO;
import com.samoy.chuanbillserver.entity.UserPreference;
import com.samoy.chuanbillserver.mapper.UserPreferenceMapper;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements IUserPreferenceService {
    
    private final UserPreferenceMapper userPreferenceMapper;
    
    @Override
    public UserPreferenceDTO getPreference(String userId) {
        UserPreference preference = userPreferenceMapper.selectOne(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
        );
        
        if (preference == null) {
            // 如果不存在，创建默认记录
            createDefaultPreference(userId);
            preference = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                    .eq(UserPreference::getUserId, userId)
            );
        }
        
        UserPreferenceDTO dto = new UserPreferenceDTO();
        BeanUtils.copyProperties(preference, dto);
        return dto;
    }
    
    @Override
    public void updatePreference(String userId, UserPreferenceDTO dto) {
        UserPreference preference = userPreferenceMapper.selectOne(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
        );
        
        if (preference == null) {
            createDefaultPreference(userId);
            preference = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                    .eq(UserPreference::getUserId, userId)
            );
        }
        
        BeanUtils.copyProperties(dto, preference);
        userPreferenceMapper.updateById(preference);
    }
    
    @Override
    public void createDefaultPreference(String userId) {
        UserPreference preference = new UserPreference();
        preference.setId(IdUtil.fastSimpleUUID());
        preference.setUserId(userId);
        preference.setPushEnabled(true);
        preference.setBillReminderEnabled(true);
        preference.setBillReminderTime("20:00");
        preference.setFamilyNotificationEnabled(true);
        
        userPreferenceMapper.insert(preference);
    }
    
    @Override
    public List<UserPreference> findUsersToRemind(String time) {
        return userPreferenceMapper.selectList(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getBillReminderTime, time)
                .eq(UserPreference::getPushEnabled, true)
                .eq(UserPreference::getBillReminderEnabled, true)
        );
    }
    
    @Override
    public boolean isFamilyNotificationEnabled(String userId) {
        UserPreference preference = userPreferenceMapper.selectOne(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
        );
        
        return preference != null 
            && preference.getPushEnabled() 
            && preference.getFamilyNotificationEnabled();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IUserPreferenceService.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserPreferenceServiceImpl.java
git commit -m "feat: 实现用户偏好服务"
```

---

### Task 4: 极光推送配置与通知服务

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/JPushConfig.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/INotificationService.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/NotificationServiceImpl.java`

- [ ] **Step 1: 创建 JPushConfig 配置类**

```java
// JPushConfig.java
package com.samoy.chuanbillserver.config;

import cn.jpush.api.JPushClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jpush")
@Data
public class JPushConfig {
    
    private String appKey;
    private String masterSecret;
    private boolean production = false;
    
    @Bean
    public JPushClient jPushClient() {
        return new JPushClient(masterSecret, appKey);
    }
}
```

- [ ] **Step 2: 创建 INotificationService 接口**

```java
// INotificationService.java
package com.samoy.chuanbillserver.service;

import java.util.List;

public interface INotificationService {
    
    /**
     * 发送账单提醒
     */
    void sendBillReminder(String userId);
    
    /**
     * 发送家庭成员变动通知
     */
    void sendFamilyMemberChangedNotification(List<String> userIds, String changeType);
    
    /**
     * 发送家庭账单变更通知
     */
    void sendFamilyBillChangedNotification(List<String> userIds, String changeType);
}
```

- [ ] **Step 3: 创建 NotificationServiceImpl 实现**

```java
// NotificationServiceImpl.java
package com.samoy.chuanbillserver.service.impl;

import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import cn.jpush.api.push.model.Platform;
import com.samoy.chuanbillserver.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {
    
    private final JPushClient jPushClient;
    
    @Override
    public void sendBillReminder(String userId) {
        sendPushByAlias(userId, "记账提醒", "今天还没有记账哦，记得记录您的收支~");
    }
    
    @Override
    public void sendFamilyMemberChangedNotification(List<String> userIds, String changeType) {
        String title = "家庭成员变动";
        String content = getMemberChangeDescription(changeType);
        
        for (String userId : userIds) {
            sendPushByAlias(userId, title, content);
        }
    }
    
    @Override
    public void sendFamilyBillChangedNotification(List<String> userIds, String changeType) {
        String title = "家庭账单变更";
        String content = getBillChangeDescription(changeType);
        
        for (String userId : userIds) {
            sendPushByAlias(userId, title, content);
        }
    }
    
    private void sendPushByAlias(String userId, String title, String content) {
        try {
            PushPayload payload = PushPayload.newBuilder()
                .setPlatform(Platform.all())
                .setAudience(Audience.alias(userId))
                .setNotification(Notification.newBuilder()
                    .setAlert(content)
                    .addPlatformNotification(AndroidNotification.newBuilder()
                        .setTitle(title)
                        .build())
                    .addPlatformNotification(IosNotification.newBuilder()
                        .setAlert(title + ": " + content)
                        .build())
                    .build())
                .build();
            
            PushResult result = jPushClient.sendPush(payload);
            log.info("推送发送成功，用户ID: {}, 标题: {}, msgId: {}", userId, title, result.msg_id);
        } catch (Exception e) {
            log.error("推送发送失败，用户ID: {}", userId, e);
        }
    }
    
    private String getMemberChangeDescription(String changeType) {
        return switch (changeType) {
            case "JOIN" -> "有新成员加入了您的家庭";
            case "LEAVE" -> "有成员退出了您的家庭";
            case "REMOVE" -> "有成员被移出了您的家庭";
            default -> "家庭成员发生变动";
        };
    }
    
    private String getBillChangeDescription(String changeType) {
        return switch (changeType) {
            case "CREATE" -> "有新的家庭账单被创建";
            case "UPDATE" -> "家庭账单被修改";
            case "DELETE" -> "家庭账单被删除";
            default -> "家庭账单发生变更";
        };
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/JPushConfig.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/INotificationService.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/NotificationServiceImpl.java
git commit -m "feat: 实现极光推送配置和通知服务"
```

---

### Task 5: 用户偏好 API

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserPreferenceController.java`

- [ ] **Step 1: 创建 UserPreferenceController**

```java
// UserPreferenceController.java
package com.samoy.chuanbillserver.controller;

import com.samoy.chuanbillserver.dto.UserPreferenceDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/preference")
@RequiredArgsConstructor
public class UserPreferenceController {
    
    private final IUserPreferenceService userPreferenceService;
    
    @GetMapping
    public Result<UserPreferenceDTO> getPreference() {
        // 从 Sa-Token 获取当前用户ID
        String userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsString();
        UserPreferenceDTO dto = userPreferenceService.getPreference(userId);
        return Result.success(dto);
    }
    
    @PutMapping
    public Result<Void> updatePreference(@RequestBody UserPreferenceDTO dto) {
        String userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsString();
        userPreferenceService.updatePreference(userId, dto);
        return Result.success();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserPreferenceController.java
git commit -m "feat: 实现用户偏好API"
```

---

### Task 6: 定时任务

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/scheduler/BillReminderScheduler.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBillService.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java`

- [ ] **Step 1: 创建 BillReminderScheduler**

```java
// BillReminderScheduler.java
package com.samoy.chuanbillserver.scheduler;

import com.samoy.chuanbillserver.entity.UserPreference;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.INotificationService;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillReminderScheduler {
    
    private final IUserPreferenceService preferenceService;
    private final IBillService billService;
    private final INotificationService notificationService;
    
    /**
     * 每分钟执行一次，检查是否有用户需要提醒
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAndSendReminders() {
        // 1. 获取当前时间（HH:mm 格式）
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        
        // 2. 查询需要提醒的用户
        List<UserPreference> usersToRemind = preferenceService.findUsersToRemind(currentTime);
        
        // 3. 遍历用户，检查是否需要发送提醒
        for (UserPreference preference : usersToRemind) {
            try {
                sendReminderIfNeeded(preference);
            } catch (Exception e) {
                log.error("发送账单提醒失败，用户ID: {}", preference.getUserId(), e);
            }
        }
    }
    
    private void sendReminderIfNeeded(UserPreference preference) {
        String userId = preference.getUserId();
        
        // 检查推送开关
        if (!preference.getPushEnabled() || !preference.getBillReminderEnabled()) {
            return;
        }
        
        // 检查当天是否已记账
        if (billService.hasBillToday(userId)) {
            return;
        }
        
        // 发送提醒
        notificationService.sendBillReminder(userId);
    }
}
```

- [ ] **Step 2: 添加 IBillService 方法**

```java
// IBillService.java 中添加方法
/**
 * 检查用户今天是否有账单记录
 */
boolean hasBillToday(String userId);
```

- [ ] **Step 3: 实现 hasBillToday 方法**

```java
// BillServiceImpl.java 中添加实现
@Override
public boolean hasBillToday(String userId) {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
    
    return billMapper.selectCount(
        new LambdaQueryWrapper<Bill>()
            .eq(Bill::getUserId, userId)
            .ge(Bill::getTime, startOfDay)
            .le(Bill::getTime, endOfDay)
    ) > 0;
}
```

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/scheduler/BillReminderScheduler.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBillService.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java
git commit -m "feat: 实现账单提醒定时任务"
```

---

### Task 7: 事件驱动

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/event/FamilyMemberChangedEvent.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/event/FamilyBillChangedEvent.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/event/NotificationEventListener.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/FamilyServiceImpl.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java`

- [ ] **Step 1: 创建事件类**

```java
// FamilyMemberChangedEvent.java
package com.samoy.chuanbillserver.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FamilyMemberChangedEvent {
    private final String familyId;
    private final String userId;        // 触发变动的用户
    private final String targetUserId;  // 被影响的用户
    private final String changeType;    // JOIN, LEAVE, REMOVE
}
```

```java
// FamilyBillChangedEvent.java
package com.samoy.chuanbillserver.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FamilyBillChangedEvent {
    private final String familyId;
    private final String userId;        // 触发变更的用户
    private final String billId;
    private final String changeType;    // CREATE, UPDATE, DELETE
}
```

- [ ] **Step 2: 创建事件监听器**

```java
// NotificationEventListener.java
package com.samoy.chuanbillserver.event;

import com.samoy.chuanbillserver.service.IFamilyService;
import com.samoy.chuanbillserver.service.INotificationService;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {
    
    private final INotificationService notificationService;
    private final IUserPreferenceService preferenceService;
    private final IFamilyService familyService;
    
    /**
     * 家庭成员变动监听
     */
    @EventListener
    @Async
    public void onFamilyMemberChanged(FamilyMemberChangedEvent event) {
        try {
            // 获取家庭所有成员
            List<String> members = familyService.getFamilyMemberIds(event.getFamilyId());
            
            // 过滤需要通知的成员（排除触发者，检查通知开关）
            List<String> membersToNotify = members.stream()
                .filter(userId -> !userId.equals(event.getUserId()))
                .filter(preferenceService::isFamilyNotificationEnabled)
                .collect(Collectors.toList());
            
            // 发送通知
            notificationService.sendFamilyMemberChangedNotification(
                membersToNotify, 
                event.getChangeType()
            );
        } catch (Exception e) {
            log.error("处理家庭成员变动事件失败", e);
        }
    }
    
    /**
     * 家庭账单变更监听
     */
    @EventListener
    @Async
    public void onFamilyBillChanged(FamilyBillChangedEvent event) {
        try {
            // 获取家庭所有成员
            List<String> members = familyService.getFamilyMemberIds(event.getFamilyId());
            
            // 过滤需要通知的成员（排除触发者，检查通知开关）
            List<String> membersToNotify = members.stream()
                .filter(userId -> !userId.equals(event.getUserId()))
                .filter(preferenceService::isFamilyNotificationEnabled)
                .collect(Collectors.toList());
            
            // 发送通知
            notificationService.sendFamilyBillChangedNotification(
                membersToNotify, 
                event.getChangeType()
            );
        } catch (Exception e) {
            log.error("处理家庭账单变更事件失败", e);
        }
    }
}
```

- [ ] **Step 3: 在 FamilyServiceImpl 中触发事件**

```java
// FamilyServiceImpl.java 中添加
@Autowired
private ApplicationEventPublisher eventPublisher;

// 在 joinFamily 方法中添加
eventPublisher.publishEvent(new FamilyMemberChangedEvent(
    familyId, userId, null, "JOIN"
));

// 在 leaveFamily 方法中添加
eventPublisher.publishEvent(new FamilyMemberChangedEvent(
    familyId, userId, null, "LEAVE"
));

// 在 removeMember 方法中添加
eventPublisher.publishEvent(new FamilyMemberChangedEvent(
    familyId, operatorUserId, targetUserId, "REMOVE"
));
```

- [ ] **Step 4: 在 BillServiceImpl 中触发事件**

```java
// BillServiceImpl.java 中添加
@Autowired
private ApplicationEventPublisher eventPublisher;

// 在 createBill 方法中，如果账单有 familyId，触发事件
if (bill.getFamilyId() != null) {
    eventPublisher.publishEvent(new FamilyBillChangedEvent(
        bill.getFamilyId(), userId, bill.getId(), "CREATE"
    ));
}

// 在 updateBill 方法中，如果账单有 familyId，触发事件
if (bill.getFamilyId() != null) {
    eventPublisher.publishEvent(new FamilyBillChangedEvent(
        bill.getFamilyId(), userId, bill.getId(), "UPDATE"
    ));
}

// 在 deleteBill 方法中，如果账单有 familyId，触发事件
if (bill.getFamilyId() != null) {
    eventPublisher.publishEvent(new FamilyBillChangedEvent(
        bill.getFamilyId(), userId, billId, "DELETE"
    ));
}
```

- [ ] **Step 5: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/event/
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/FamilyServiceImpl.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java
git commit -m "feat: 实现事件驱动通知"
```

---

### Task 8: 用户注册时创建偏好

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 修改 UserServiceImpl**

```java
// UserServiceImpl.java 中添加
@Autowired
private IUserPreferenceService userPreferenceService;

// 在 register 方法中，用户创建成功后添加
userPreferenceService.createDefaultPreference(user.getId());
```

- [ ] **Step 2: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java
git commit -m "feat: 用户注册时自动创建偏好记录"
```

---

### Task 9: 前端 API 与页面修改

**Files:**
- Create: `chuan-bill-app/src/api/preference.ts`
- Modify: `chuan-bill-app/src/pages/mine/components/NotificationSettingsPopup.vue`

- [ ] **Step 1: 创建 preference.ts API 文件**

```typescript
// preference.ts
import { alovaInstance } from '@/api/core/instance'

export interface UserPreference {
  pushEnabled: boolean
  billReminderEnabled: boolean
  billReminderTime: string
  familyNotificationEnabled: boolean
}

/**
 * 获取用户偏好设置
 */
export function getPreference() {
  return alovaInstance.Get<UserPreference>('/user/preference')
}

/**
 * 更新用户偏好设置
 */
export function updatePreference(data: UserPreference) {
  return alovaInstance.Put('/user/preference', data)
}
```

- [ ] **Step 2: 修改 NotificationSettingsPopup.vue**

```vue
<script setup lang="ts">
import { getPreference, updatePreference, type UserPreference } from '@/api/preference'

defineOptions({
  name: 'NotificationSettingsPopup',
})

const modelValue = defineModel<boolean>({ default: false })
const toast = useGlobalToast()

// 通知设置
const settings = ref<UserPreference>({
  pushEnabled: true,
  billReminderEnabled: true,
  billReminderTime: '20:00',
  familyNotificationEnabled: true,
})

const timePickerVisible = ref(false)
const timeValue = ref('20:00')

// 加载设置
onMounted(async () => {
  try {
    const { data } = await getPreference()
    settings.value = data
    timeValue.value = data.billReminderTime
  } catch (error) {
    console.error('加载通知设置失败', error)
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
    await updatePreference(settings.value)
    toast.success('设置已保存')
  } catch (error) {
    toast.error('保存失败')
    console.error('保存通知设置失败', error)
  }
}

// 时间选择确认
function onTimeConfirm({ value }: { value: number[] }) {
  settings.value.billReminderTime = `${String(value[0]).padStart(2, '0')}:${String(value[1]).padStart(2, '0')}`
  saveSettings()
}
</script>
```

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-app/src/api/preference.ts
git add chuan-bill-app/src/pages/mine/components/NotificationSettingsPopup.vue
git commit -m "feat: 前端对接通知设置API"
```

---

### Task 10: 运行 alova-gen 并修复类型

**Files:**
- Modify: `chuan-bill-app/src/api/globals.d.ts` (自动生成)

- [ ] **Step 1: 运行 alova-gen**

```bash
cd chuan-bill-app
pnpm alova-gen
```

- [ ] **Step 2: 运行 alova-api-fix**

```bash
pnpm alova-api-fix
```

- [ ] **Step 3: 验证类型正确**

```bash
pnpm type-check
```

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-app/src/api/
git commit -m "chore: 重新生成API定义"
```

---

### Task 11: 测试与验证

- [ ] **Step 1: 启动后端服务**

```bash
cd chuan-bill-server
mvn spring-boot:run
```

- [ ] **Step 2: 启动前端服务**

```bash
cd chuan-bill-app
pnpm dev
```

- [ ] **Step 3: 测试用户偏好 API**

```bash
# 获取偏好设置
curl -X GET http://localhost:8080/user/preference \
  -H "Authorization: Bearer your-token"

# 更新偏好设置
curl -X PUT http://localhost:8080/user/preference \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "pushEnabled": true,
    "billReminderEnabled": true,
    "billReminderTime": "21:00",
    "familyNotificationEnabled": true
  }'
```

- [ ] **Step 4: 测试前端页面**

1. 打开"我的"页面
2. 点击"通知设置"
3. 验证设置加载正确
4. 修改设置并保存
5. 验证设置保存成功

- [ ] **Step 5: 测试定时任务**

1. 将账单提醒时间设置为当前时间后 1-2 分钟
2. 等待定时任务触发
3. 检查是否收到推送通知（需要配置极光推送）

- [ ] **Step 6: Final Commit**

```bash
git add .
git commit -m "feat: 完成通知系统实现"
```

---

## 自查清单

- [ ] 所有任务都有完整的代码，没有 TBD/TODO
- [ ] 文件路径准确
- [ ] 类型和方法签名一致
- [ ] 设计文档中的所有需求都有对应的实现
- [ ] 测试步骤完整
