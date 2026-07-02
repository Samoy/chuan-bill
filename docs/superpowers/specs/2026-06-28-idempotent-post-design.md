# POST接口幂等设计方案

## 概述

当前系统所有POST接口均无幂等校验，用户快速双击提交按钮会导致重复数据写入（如重复账单）。本方案通过AOP + 自定义注解 + Redis实现服务端幂等防护，前端零改动。

## 需求

- **范围**: 全部POST接口（33个，覆盖10个Controller）
- **幂等键**: 服务端自动生成（userId + 请求路径 + 请求体SHA-256）
- **时间窗口**: 500ms（仅防快速双击，不阻塞自动化工具）
- **重复响应**: `Result.error(429, "请勿重复提交")`
- **前端**: 零改动

## 架构

```
请求 → Controller(@Idempotent) → AOP切面 → Redis SETNX
                                              ↓
                                     key存在 → 抛出IdempotentException → 429
                                     key不存在 → 设置TTL → 放行执行 → 删除key
```

## 组件设计

### 1. `@Idempotent` 注解

路径: `com.samoy.chuanbillserver.annotation.Idempotent`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    long window() default 500L;
    String message() default "请勿重复提交";
}
```

- `window`: 幂等时间窗口，单位毫秒，默认500ms，可通过注解参数覆盖
- `message`: 重复提交时的提示信息，默认"请勿重复提交"

### 2. `IdempotentAspect` 切面

路径: `com.samoy.chuanbillserver.aspect.IdempotentAspect`

核心逻辑（`@Around` 环绕通知）：

1. 通过 Sa-Token 的 `StpUtil.getLoginIdAsLong()` 获取当前用户ID
2. 获取请求URI（`HttpServletRequest.getRequestURI()`）
3. 读取请求体：包装 `HttpServletRequest` 缓存InputStream，避免流被消费后丢失；multipart请求（文件上传）跳过请求体哈希，仅用userId + URI作为幂等键
4. 生成幂等键：`SHA-256(userId + ":" + uri + ":" + requestBody)`（multipart则为`SHA-256(userId + ":" + uri)`）
5. 构建Redis key：`"idempotent:" + hash`
6. 执行 `StringRedisTemplate.opsForValue().setIfAbsent(key, "1", window, MILLISECONDS)`
   - 成功（key不存在）→ 放行方法执行 → 执行完成后删除key
   - 失败（key已存在）→ 抛出 `IdempotentException`

**方法执行完后立即删除key**：不等TTL自然过期，只在并发重叠期间拦截重复。执行完后允许用户正常再次提交。

### 3. `IdempotentException` 异常

路径: `com.samoy.chuanbillserver.exception.IdempotentException`

继承 `RuntimeException`，携带 `code = 429` 和提示信息。

### 4. `ResultEnum` 补充

新增枚举值：

```java
REPEAT_SUBMIT(429, "请勿重复提交")
```

### 5. `GlobalExceptionHandler` 扩展

新增 `IdempotentException` 处理器：

```java
@ExceptionHandler(IdempotentException.class)
public Result<?> handleIdempotentException(IdempotentException e) {
    log.info("重复提交: {}", e.getMessage());
    return Result.error(e.getCode(), e.getMessage());
}
```

## 依赖变更

`pom.xml` 新增：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

Redis 复用现有 `StringRedisTemplate` Bean，无需额外配置。

## Controller改造

现有10个Controller共33个POST方法，每个方法添加 `@Idempotent` 注解：

| Controller | 方法数 | 示例 |
|------------|--------|------|
| AuthController | 6 | login-password, login-phone, login-wechat, send-code, retrieve-password, logout |
| BillController | 7 | add, batchCreate, update, delete, categories, payment-methods, export |
| BudgetController | 1 | set |
| FamilyController | 9 | create, update, delete, join, leave, remove-member, transfer-owner, handle-approve, refresh-invite-code |
| FileController | 1 | temp/upload |
| MessageController | 2 | mark-read, mark-all-read |
| UserController | 8 | profile/update, password/update-by-old, password/update-by-code, phone/code, phone/update-by-code, phone/update-by-password, phone/bind, account/delete |
| UserPreferenceController | 2 | set, delete |

## 新增/修改文件清单

| 操作 | 文件 |
|------|------|
| 新增 | `annotation/Idempotent.java` |
| 新增 | `aspect/IdempotentAspect.java` |
| 新增 | `exception/IdempotentException.java` |
| 新增 | `config/RepeatableRequestWrapper.java`（请求体缓存包装器） |
| 修改 | `pom.xml`（添加AOP依赖） |
| 修改 | `result/ResultEnum.java`（新增REPEAT_SUBMIT） |
| 修改 | `exception/GlobalExceptionHandler.java`（新增异常处理） |
| 修改 | 10个Controller（共33个POST方法加注解） |

## 前端影响

零改动。前端 `handleAlovaResponse` 已有通用的 `code !== 200` 错误处理，429响应会自动toast提示。

## 设计决策

| 决策 | 理由 |
|------|------|
| 500ms窗口而非更长 | 仅防快速双击，不阻塞自动化工具和正常重复操作 |
| 方法执行完删除key而非等TTL过期 | 允许用户在操作完成后立即再次提交相同内容 |
| SHA-256而非MD5 | 更安全，防碰撞能力更强 |
| 全部POST统一注解 | 一致性好，新接口加注解即可，无需记忆哪些需要哪些不需要 |
