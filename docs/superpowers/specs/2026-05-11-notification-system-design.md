# 通知系统设计文档

## 概述

本文档描述了小川记账应用的通知系统设计，包括用户偏好管理、账单提醒、家庭通知等功能。

## 设计目标

1. **用户偏好管理**：用户可以自定义通知开关和提醒时间
2. **账单提醒**：每天定时提醒未记账的用户
3. **家庭通知**：实时通知家庭成员变动、账单变更等事件
4. **多端支持**：通过极光推送支持微信小程序、APP 等多端

## 技术方案

### 架构选择

采用**事件驱动方案**，使用 Spring Event 解耦业务逻辑与通知逻辑。

### 技术栈

- **数据库**：MySQL（用户偏好表）
- **定时任务**：Quartz（Spring Boot 集成）
- **事件驱动**：Spring Event
- **推送服务**：极光推送 SDK
- **客户端绑定**：极光 alias（用户ID）

### Maven 依赖

```xml
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

---

## 数据库设计

### 用户偏好表 (t_user_preference)

```sql
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

**设计说明**：
- 与前端 `NotificationSettingsPopup.vue` 字段一一对应
- `bill_reminder_time` 存储 "HH:mm" 格式，方便前端展示和定时任务查询
- 用户注册时自动创建默认记录（所有开关默认开启）

---

## 后端架构设计

### 包结构

```
com.samoy.chuanbillserver
├── controller
│   └── UserPreferenceController.java    # 用户偏好 API
├── service
│   ├── IUserPreferenceService.java      # 用户偏好服务接口
│   ├── impl
│   │   └── UserPreferenceServiceImpl.java
│   ├── INotificationService.java        # 通知服务接口
│   └── impl
│       └── NotificationServiceImpl.java # 通知服务实现
├── scheduler
│   └── BillReminderScheduler.java       # 账单提醒定时任务
├── event
│   ├── BillCreatedEvent.java            # 账单创建事件
│   ├── FamilyMemberChangedEvent.java    # 家庭成员变动事件
│   ├── FamilyBillChangedEvent.java      # 家庭账单变更事件
│   └── NotificationEventListener.java   # 事件监听器
├── entity
│   └── UserPreference.java              # 用户偏好实体
├── mapper
│   └── UserPreferenceMapper.java        # MyBatis Mapper
├── dto
│   └── UserPreferenceDTO.java           # 偏好设置 DTO
└── config
    └── JPushConfig.java                 # 极光推送配置
```

### 核心类职责

| 类 | 职责 |
|---|------|
| `UserPreferenceController` | 提供偏好设置的 REST API |
| `UserPreferenceService` | 管理用户偏好设置的 CRUD |
| `NotificationService` | 封装极光推送，提供统一的发送接口 |
| `BillReminderScheduler` | Quartz 定时任务，检查并发送账单提醒 |
| `NotificationEventListener` | 监听业务事件，触发对应通知 |

---

## API 设计

### 用户偏好 API

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/user/preference` | 获取当前用户偏好设置 |
| `PUT` | `/user/preference` | 更新用户偏好设置 |

### 请求/响应示例

```java
// GET /user/preference
{
    "code": 200,
    "message": "success",
    "data": {
        "pushEnabled": true,
        "billReminderEnabled": true,
        "billReminderTime": "20:00",
        "familyNotificationEnabled": true
    }
}

// PUT /user/preference
{
    "pushEnabled": true,
    "billReminderEnabled": false,
    "billReminderTime": "21:00",
    "familyNotificationEnabled": true
}
```

### DTO 设计

```java
@Data
public class UserPreferenceDTO {
    private Boolean pushEnabled;
    private Boolean billReminderEnabled;
    private String billReminderTime;
    private Boolean familyNotificationEnabled;
}
```

---

## 定时任务设计

### 账单提醒调度器

```java
@Service
@Slf4j
public class BillReminderScheduler {
    
    @Autowired
    private UserPreferenceService preferenceService;
    
    @Autowired
    private BillService billService;
    
    @Autowired
    private NotificationService notificationService;
    
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

### Quartz 配置

```java
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
    public Trigger billReminderTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(jobDetail)
            .withIdentity("billReminderTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))
            .build();
    }
}
```

---

## 事件驱动设计

### 事件定义

```java
// 账单创建事件
@Getter
@AllArgsConstructor
public class BillCreatedEvent {
    private final String userId;
    private final String billId;
    private final String billName;
    private final BigDecimal amount;
}

// 家庭成员变动事件
@Getter
@AllArgsConstructor
public class FamilyMemberChangedEvent {
    private final String familyId;
    private final String userId;        // 触发变动的用户
    private final String targetUserId;  // 被影响的用户
    private final String changeType;    // JOIN, LEAVE, REMOVE
}

// 家庭账单变更事件
@Getter
@AllArgsConstructor
public class FamilyBillChangedEvent {
    private final String familyId;
    private final String userId;        // 触发变更的用户
    private final String billId;
    private final String changeType;    // CREATE, UPDATE, DELETE
}
```

### 事件监听器

```java
@Component
@Slf4j
public class NotificationEventListener {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserPreferenceService preferenceService;
    
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
                .filter(userId -> preferenceService.isFamilyNotificationEnabled(userId))
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
            // 类似逻辑，通知家庭成员
        } catch (Exception e) {
            log.error("处理家庭账单变更事件失败", e);
        }
    }
}
```

### 业务代码中触发事件

```java
@Service
public class FamilyServiceImpl implements IFamilyService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Override
    public void joinFamily(String userId, String familyId) {
        // 1. 添加成员逻辑
        familyMemberMapper.insert(member);
        
        // 2. 发布事件（异步通知）
        eventPublisher.publishEvent(new FamilyMemberChangedEvent(
            familyId, userId, null, "JOIN"
        ));
    }
}
```

---

## 极光推送集成

### 配置类

```java
@Configuration
@ConfigurationProperties(prefix = "jpush")
@Data
public class JPushConfig {
    private String appKey;
    private String masterSecret;
    private boolean production = false;  // 是否生产环境
    
    @Bean
    public JPushClient jPushClient() {
        return new JPushClient(masterSecret, appKey);
    }
}
```

### 配置文件

```yaml
jpush:
  app-key: ${JPUSH_APP_KEY}
  master-secret: ${JPUSH_MASTER_SECRET}
  production: false  # 开发环境为 false，生产环境为 true
```

### 通知服务

```java
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    @Autowired
    private JPushClient jPushClient;
    
    @Autowired
    private UserPreferenceService preferenceService;
    
    @Override
    public void sendBillReminder(String userId) {
        sendPushByAlias(userId, "记账提醒", "今天还没有记账哦，记得记录您的收支~");
    }
    
    @Override
    public void sendFamilyMemberChangedNotification(List<String> userIds, String changeType) {
        String title = "家庭成员变动";
        String content = getChangeTypeDescription(changeType);
        
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
            
            jPushClient.sendPush(payload);
            log.info("推送发送成功，用户ID: {}, 标题: {}", userId, title);
        } catch (Exception e) {
            log.error("推送发送失败，用户ID: {}", userId, e);
        }
    }
}
```

---

## 前端对接设计

### API 调用

```typescript
// 获取用户偏好
const { data: preference } = await alova.Get('/user/preference')

// 更新用户偏好
await alova.Put('/user/preference', {
  pushEnabled: true,
  billReminderEnabled: true,
  billReminderTime: '21:00',
  familyNotificationEnabled: true
})
```

### NotificationSettingsPopup.vue 修改

```vue
<script setup lang="ts">
// 加载设置
onMounted(async () => {
  const { data } = await getPreference()
  settings.value = data
})

// 保存设置
async function saveSettings() {
  await updatePreference(settings.value)
  toast.success('设置已保存')
}
</script>
```

### 客户端绑定极光 alias

```typescript
// 用户登录后调用
function bindJPushAlias(userId: string) {
  // #ifdef APP-PLUS
  plus.push.getClientInfoAsync((info) => {
    // APP 端绑定 alias
    jpush.setAlias({ sequence: 1, alias: userId })
  })
  // #endif
  
  // #ifdef MP-WEIXIN
  // 微信小程序端绑定 alias
  jpush.setAlias({ sequence: 1, alias: userId })
  // #endif
}
```

---

## 实施计划

### 阶段一：数据库与后端基础

1. 创建用户偏好表
2. 实现 UserPreference 实体和 Mapper
3. 实现 UserPreferenceService
4. 实现 UserPreferenceController

### 阶段二：极光推送集成

1. 添加极光推送依赖
2. 配置 JPushConfig
3. 实现 NotificationService

### 阶段三：定时任务

1. 添加 Quartz 依赖
2. 配置 QuartzConfig
3. 实现 BillReminderScheduler

### 阶段四：事件驱动

1. 定义事件类
2. 实现 NotificationEventListener
3. 在业务代码中触发事件

### 阶段五：前端对接

1. 修改 NotificationSettingsPopup.vue
2. 实现 API 调用
3. 集成极光推送 SDK
4. 实现 alias 绑定

---

## 用户注册时自动创建偏好记录

### 实现方式

在 `UserServiceImpl.register()` 方法中，用户注册成功后自动创建默认偏好记录：

```java
@Service
public class UserServiceImpl implements IUserService {
    
    @Autowired
    private UserPreferenceService userPreferenceService;
    
    @Override
    public void register(UserRegisterDTO dto) {
        // 1. 创建用户
        User user = new User();
        user.setId(IdUtil.fastSimpleUUID());
        // ... 设置其他字段
        userMapper.insert(user);
        
        // 2. 创建默认偏好记录
        userPreferenceService.createDefaultPreference(user.getId());
    }
}
```

### 默认偏好记录

```java
@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {
    
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
}
```

---

## 注意事项

1. **用户注册时自动创建偏好记录**
2. **定时任务每分钟检查一次**，查询当前时间需要提醒的用户
3. **家庭通知实时异步发送**，失败不影响主业务
4. **极光 alias 使用业务用户ID**，支持多设备推送
5. **前端保存设置后立即生效**，无需重启服务

---

## 附录

### 参考文档

- [极光推送官方文档](https://docs.jiguang.cn/)
- [Spring Event 文档](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html)
- [Quartz Scheduler 文档](http://www.quartz-scheduler.org/documentation/)

### 相关文件

- `NotificationSettingsPopup.vue` - 前端通知设置弹窗
- `init.sql` - 数据库初始化脚本
