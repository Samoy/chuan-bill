# 通知功能设计文档

> 日期：2026-05-12 | 状态：已确认

## 概述

为小川记账应用实现消息通知功能。当前阶段实现站内消息通知（用户打开应用可见未读消息），不做实时推送，但设计上预留推送扩展性。

## 需求范围

| 功能 | 描述 | 默认状态 |
|------|------|----------|
| 家庭事件消息 | 加入/转让/审批通知 | 已完成，不动 |
| 每日记账提醒 | 未记账时提醒，用户可设置提醒时间 | 关闭 |
| 家庭成员记账通知 | 成员新增账单时通知其他成员 | 始终开启 |
| 账单详情权限 | 家庭成员可查看他人共享账单（仅查看） | - |
| 消息列表增强 | 类型筛选 Tab、点击跳转、账单消息渲染 | - |

## 一、用户偏好模块（后端）

### 1.1 数据库表

新建 `t_user_preference` 表，采用 key-value 模式存储用户偏好：

```sql
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

### 1.2 实体类

`UserPreference.java`：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键 |
| userId | String | 用户ID |
| prefKey | String | 偏好键名 |
| prefValue | String | 偏好值 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### 1.3 Service 接口

`IUserPreferenceService`：

| 方法 | 说明 |
|------|------|
| `String getValue(String userId, String key)` | 获取单个偏好值，不存在返回 null |
| `void setValue(String userId, String key, String value)` | 设置单个偏好（INSERT ON DUPLICATE UPDATE） |
| `Map<String, String> getAll(String userId)` | 获取用户所有偏好，返回 Map |
| `void deleteValue(String userId, String key)` | 删除某个偏好 |

### 1.4 Controller

`UserPreferenceController`（路径前缀 `/preference`）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/preference/get?key=xxx` | 获取单个偏好 |
| GET | `/preference/all` | 获取所有偏好 |
| POST | `/preference/set?key=xxx&value=xxx` | 设置偏好 |
| DELETE | `/preference/delete?key=xxx` | 删除偏好 |

### 1.5 通知相关偏好 Key 命名

| Key | Value 示例 | 说明 |
|-----|-----------|------|
| `notification.billReminder.enabled` | `"true"` / `"false"` | 是否开启每日提醒 |
| `notification.billReminder.time` | `"20:00"` | 提醒时间 |
| `notification.billReminder.lastSentDate` | `"2026-05-12"` | 上次发送提醒的日期（防重复） |

后续扩展只需约定新的 key 命名规范，如 `notification.xxx.enabled`。

## 二、每日记账提醒调度（Quartz）

### 2.1 依赖配置

`pom.xml` 添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

`application.yml` 添加：

```yaml
spring:
  quartz:
    job-store-type: memory
```

使用默认 `RAMJobStore`，无需建 Quartz 数据库表。应用启动时自动注册 Job。

### 2.2 Quartz 配置类

`QuartzConfig.java`：定义 `JobDetail` + `CronTrigger` Bean，Spring Boot 自动注册到 Scheduler。

- JobDetail: `BillReminderJob`
- CronTrigger: `"0 * * * * ?"`（每分钟执行）

### 2.3 Job 执行逻辑

`BillReminderJob`：

```
1. 获取当前时间的 HH:mm（如 "20:00"）
2. 查询 t_user_preference 中：
   - pref_key = 'notification.billReminder.enabled' AND pref_value = 'true'
   - 同时 pref_key = 'notification.billReminder.time' AND pref_value = 当前 HH:mm
   → 取交集，得到该时间点需要提醒的用户列表
3. 对每个用户：
   a. 检查 pref_key='notification.billReminder.lastSentDate' 的值是否等于今天
   b. 如果等于今天 → 跳过
   c. 查询该用户今日（当天 00:00 ~ 23:59）是否已有账单记录
   d. 今日无账单 → 调用 messageService.sendMessage() 插入提醒消息
      - title: "记账提醒"
      - content: "您今天还没有记账哦，点击记录一笔"
      - type: "system"
      - relatedId: null
      - relatedType: null
   e. 写入 lastSentDate = 今天
```

### 2.4 防重复策略

使用 `lastSentDate` 偏好 key 记录上次发送日期。Job 执行时先检查该值，如果等于今天则跳过。每次成功发送后更新为当天日期。

## 三、家庭成员记账通知

### 3.1 触发位置

`BillServiceImpl.addBill()` 方法中，在账单创建成功后，判断 `bill.getFamilyId()` 是否非空。

### 3.2 通知逻辑

```
if (bill.getFamilyId() != null) {
    // getMembers 第一个参数为请求者 ID（用于权限校验），传记账人自己
    List<FamilyMemberVO> members = familyService.getMembers(bill.getUserId(), bill.getFamilyId());

    // 获取记账人昵称（从成员列表中匹配自己）
    String nickname = members.stream()
        .filter(m -> m.getUserId().equals(bill.getUserId()))
        .findFirst()
        .map(FamilyMemberVO::getUserNickname)
        .orElse("未知用户");

    // Bill 实体只有 categoryId，需通过 Category 查询获取分类名
    Category category = categoryMapper.selectById(bill.getCategoryId());
    String categoryName = category != null ? category.getName() : "未分类";

    String content = String.format("{\"categoryName\":\"%s\",\"amount\":\"%s\",\"type\":\"%s\"}",
        categoryName, bill.getAmount(), bill.getType());

    for (FamilyMemberVO member : members) {
        // 跳过记账人自己
        if (member.getUserId().equals(bill.getUserId())) continue;

        messageService.sendMessage(
            member.getUserId(),
            nickname + " 记了一笔账单",
            content,
            "bill",
            bill.getId(),
            "bill"
        );
    }
}
```

### 3.3 消息字段

| 字段 | 值 |
|------|-----|
| title | `"{用户昵称} 记了一笔账单"` |
| content | `{"categoryName":"餐饮","amount":"50.00","type":"expense"}` |
| type | `"bill"` |
| relatedId | 账单 ID |
| relatedType | `"bill"` |

### 3.4 前端渲染

消息列表中对 `type="bill"` 的消息解析 content JSON：

- 标题：直接显示 `msg.title`
- 内容：解析 JSON，显示 `"{categoryName} ¥{amount}"`
- 金额颜色：`type === "expense"` → `text-red-400`，`type === "income"` → `text-green-500`
- 点击：跳转到账单详情页

## 四、账单详情权限修复

### 4.1 修改位置

`BillServiceImpl.getBillDetail()` 方法（第 183-193 行）

### 4.2 当前逻辑

```java
if (!Objects.equals(bill.getUserId(), userId)) {
    throw new BusinessException(ResultEnum.BILL_NOT_ALLOW_VIEW);
}
```

### 4.3 修改为

```java
if (!Objects.equals(bill.getUserId(), userId)) {
    if (bill.getFamilyId() == null
        || !familyService.isMember(userId, bill.getFamilyId())) {
        throw new BusinessException(ResultEnum.BILL_NOT_ALLOW_VIEW);
    }
}
```

逻辑：账单创建者可查看 → 否则检查是否为家庭共享账单 → 是则验证家庭成员身份 → 通过则允许查看。

`updateBill` 和 `deleteBill` 保持原逻辑不变（仅创建者可操作）。

## 五、前端 - API 层

### 5.1 新增 API 定义

在 `src/api/apiDefinitions.ts` 中新增 `preference` 命名空间：

| Key | Method | Path | 说明 |
|-----|--------|------|------|
| `preference.get` | GET | `/preference/get` | 获取单个偏好（参数：key） |
| `preference.getAll` | GET | `/preference/all` | 获取所有偏好 |
| `preference.set` | POST | `/preference/set` | 设置偏好（参数：key, value） |
| `preference.delete` | DELETE | `/preference/delete` | 删除偏好（参数：key） |

新增后执行 `pnpm alova-gen` 重新生成 `globals.d.ts`，再用 `alova-api-fix` skill 修复类型。

## 六、前端 - 通知设置对接后端

### 6.1 修改文件

`src/pages/mine/components/NotificationSettingsPopup.vue`

### 6.2 加载设置（onMounted）

```
1. 调用 GET /preference/all 获取用户所有偏好
2. 解析 notification.billReminder.enabled → settings.billReminderEnabled
3. 解析 notification.billReminder.time → settings.billReminderTime
4. 如果没有任何偏好记录，使用默认值（enabled=false, time="20:00"）
```

### 6.3 保存设置（saveSettings）

```
1. 调用 POST /preference/set?key=notification.billReminder.enabled&value=true/false
2. 调用 POST /preference/set?key=notification.billReminder.time&value=20:00
3. 显示 toast "设置已保存"
```

### 6.4 默认值调整

- `billReminderEnabled` 默认改为 `false`（当前是 `true`）
- `pushEnabled` 和 `familyNotificationEnabled` 相关 UI 保留但暂不对接后端

## 七、前端 - 消息列表增强

### 7.1 修改文件

`src/pages/message/index.vue`

### 7.2 消息类型筛选 Tab

在消息列表顶部增加类型 Tab：

```
[全部] [系统] [家庭] [账单]
```

- 默认选中「全部」
- 切换 Tab 时调用 `fetchMessageList({ page: 1, size: 20, type: tabType })` 刷新列表
- Tab 使用 `wd-tabs` 组件

### 7.3 消息点击跳转

点击消息时，根据 `msg.type` 跳转：

| type | 跳转目标 | 携带参数 |
|------|----------|----------|
| `bill` | 账单列表页 | `?id=${msg.relatedId}` |
| `family` | 家庭页面 | - |
| `system` | 仅标记已读 | 不跳转 |
| `budget` | 暂不处理 | - |

### 7.4 账单类型消息渲染

对 `type="bill"` 的消息解析 content JSON：

```
标题：msg.title（"小明 记了一笔账单"）
内容："{categoryName} ¥{amount}"（"餐饮 ¥50.00"）
金额颜色：type === "expense" ? "text-red-400" : "text-green-500"
```

`type="bill"` 且 content 为空的消息（旧数据），降级显示 msg.content 原文。

## 八、前端 - 账单页跳转支持

### 8.1 修改文件

`src/pages/bill/index.vue`

### 8.2 跳转流程

```
消息列表点击 bill 类型消息
  → router.push(`/pages/bill/index?id=${msg.relatedId}`)
  → 账单页 onLoad 检测到 id 参数
  → 调用 Apis.bill.getBillDetail({ params: { id } })
  → 设置 currentBill，打开 BillDetailModal
```

### 8.3 账单页改动

`onLoad` 中增加 `id` 参数检测：

```ts
onLoad((options) => {
  refresh()
  if (options?.id) {
    openBillById(options.id)
  }
})
```

新增 `openBillById` 方法：通过 API 获取账单详情，设置 `currentBill`，打开 `showBillDetailModal`。

## 九、涉及文件汇总

### 后端新增

| 文件 | 说明 |
|------|------|
| `entity/UserPreference.java` | 用户偏好实体 |
| `dao/UserPreferenceMapper.java` | 偏好 Mapper |
| `service/IUserPreferenceService.java` | 偏好 Service 接口 |
| `service/impl/UserPreferenceServiceImpl.java` | 偏好 Service 实现 |
| `controller/UserPreferenceController.java` | 偏好 Controller |
| `config/QuartzConfig.java` | Quartz 配置类 |
| `job/BillReminderJob.java` | 每日记账提醒 Job |

### 后端修改

| 文件 | 改动 |
|------|------|
| `pom.xml` | 添加 spring-boot-starter-quartz 依赖 |
| `application.yml` | 添加 quartz 配置 |
| `init.sql` | 添加 t_user_preference 建表语句 |
| `BillServiceImpl.java` | `addBill()` 增加家庭账单通知（需注入 `CategoryMapper` 查询分类名）；`getBillDetail()` 增加家庭成员权限 |

### 前端修改

| 文件 | 改动 |
|------|------|
| `src/api/apiDefinitions.ts` | 新增 preference API 定义 |
| `src/api/globals.d.ts` | 重新生成（alova-gen + alova-api-fix） |
| `src/pages/mine/components/NotificationSettingsPopup.vue` | 对接后端偏好 API，修改默认值 |
| `src/pages/message/index.vue` | 类型筛选 Tab、点击跳转、账单消息渲染 |
| `src/pages/bill/index.vue` | onLoad 支持 id 参数打开详情弹窗 |
