# POST接口幂等 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过AOP + 自定义注解 + Redis为所有POST接口添加幂等防护，防止快速双击导致的重复提交。

**Architecture:** 自定义 `@Idempotent` 注解标记在Controller POST方法上，AOP切面拦截这些方法，基于 userId + URI + 请求体SHA-256 生成幂等键，通过Redis SETNX在500ms窗口内拦截重复请求，返回429错误。

**Tech Stack:** Spring Boot 3.5.11, Spring AOP, Redis (StringRedisTemplate), Sa-Token

## Global Constraints

- Java 17, Palantir Java Format via Spotless (`mvn spotless:apply`)
- 中文注释和文档
- Lombok `@Data`, `@Slf4j` 等注解
- 统一返回 `Result<T>` 包装器
- Redis key前缀: `idempotent:`
- 异常通过 `GlobalExceptionHandler` 统一处理
- GET接口不受影响

---

### Task 1: 添加spring-boot-starter-aop依赖

**Files:**
- Modify: `chuan-bill-server/pom.xml:234`（在 `</dependencies>` 前插入）

**Interfaces:**
- Produces: Spring AOP运行时支持，供 `IdempotentAspect` 使用

- [ ] **Step 1: 添加AOP依赖到pom.xml**

在 `chuan-bill-server/pom.xml` 的 `</dependencies>` 标签前添加：

```xml
<!-- AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

在 `<!-- commons-io (显式指定版本避免依赖冲突) -->` 之前插入。

- [ ] **Step 2: 验证依赖下载**

Run: `cd chuan-bill-server && mvn dependency:resolve -pl . -DincludeArtifactIds=spring-boot-starter-aop`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/pom.xml
git commit -m "chore: 添加spring-boot-starter-aop依赖"
```

---

### Task 2: 创建IdempotentException异常类

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/exception/IdempotentException.java`

**Interfaces:**
- Consumes: 无
- Produces: `IdempotentException(Integer code, String message)`，被 `GlobalExceptionHandler` 捕获

- [ ] **Step 1: 创建IdempotentException**

```java
package com.samoy.chuanbillserver.exception;

import lombok.Getter;

@Getter
public class IdempotentException extends RuntimeException {

    private final Integer code;

    public IdempotentException(String message) {
        super(message);
        this.code = 429;
    }

    public IdempotentException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/exception/IdempotentException.java
git commit -m "feat: 创建IdempotentException重复提交异常类"
```

---

### Task 3: 创建@Idempotent注解

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/annotation/Idempotent.java`

**Interfaces:**
- Produces: `@Idempotent` 注解，含 `window()` 和 `message()` 属性
- 被 `IdempotentAspect` 通过 `@Around` 读取

- [ ] **Step 1: 创建注解**

```java
package com.samoy.chuanbillserver.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解，标记在需要防重复提交的Controller方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等时间窗口（毫秒），默认500ms
     */
    long window() default 500L;

    /**
     * 重复提交时的提示信息
     */
    String message() default "请勿重复提交";
}
```

- [ ] **Step 2: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/annotation/Idempotent.java
git commit -m "feat: 创建@Idempotent幂等注解"
```

---

### Task 4: 创建RepeatableRequestBodyFilter请求体缓存过滤器

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/RepeatableRequestBodyFilter.java`

**Interfaces:**
- Produces: 将 `HttpServletRequest` 包装为 `ContentCachingRequestWrapper`，使请求体可多次读取
- 被 `IdempotentAspect` 通过 `HttpServletRequest.getInputStream()` 读取

- [ ] **Step 1: 创建过滤器**

```java
package com.samoy.chuanbillserver.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * 请求体缓存过滤器，使请求体可被多次读取
 * 幂等切面需要读取请求体生成哈希，此过滤器确保请求体不会因读取而丢失
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RepeatableRequestBodyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 仅对非multipart请求进行包装，multipart请求体由Spring的MultipartResolver处理
        if (isMultipartRequest(request)) {
            filterChain.doFilter(request, response);
        } else {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        }
    }

    private boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/RepeatableRequestBodyFilter.java
git commit -m "feat: 创建RepeatableRequestBodyFilter请求体缓存过滤器"
```

---

### Task 5: 创建IdempotentAspect幂等切面

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/aspect/IdempotentAspect.java`

**Interfaces:**
- Consumes: `@Idempotent` 注解属性, `HttpServletRequest`, `StringRedisTemplate`, `StpUtil`
- Produces: 幂等拦截逻辑，重复请求抛出 `IdempotentException`

- [ ] **Step 1: 创建切面**

```java
package com.samoy.chuanbillserver.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.annotation.Idempotent;
import com.samoy.chuanbillserver.exception.IdempotentException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * 幂等切面，拦截带有@Idempotent注解的方法，防止重复提交
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

    private final StringRedisTemplate stringRedisTemplate;

    public IdempotentAspect(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        // 获取用户标识：已登录用userId，未登录用IP地址
        String userIdentifier;
        if (StpUtil.isLogin()) {
            userIdentifier = String.valueOf(StpUtil.getLoginIdAsLong());
        } else {
            userIdentifier = getClientIp(request);
        }

        // 获取请求URI
        String uri = request.getRequestURI();

        // 读取请求体
        String requestBody = "";
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            // multipart请求或未包装的请求，跳过请求体哈希
            requestBody = "";
        } else {
            byte[] body = wrapper.getContentAsByteArray();
            if (body.length > 0) {
                requestBody = new String(body, StandardCharsets.UTF_8);
            }
        }

        // 生成幂等键
        String hash = sha256(userIdentifier + ":" + uri + ":" + requestBody);
        String redisKey = IDEMPOTENT_KEY_PREFIX + hash;

        // 尝试设置key（原子操作）
        Boolean setSuccess = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(redisKey, "1", idempotent.window(), TimeUnit.MILLISECONDS);

        if (Boolean.FALSE.equals(setSuccess)) {
            log.info("重复提交拦截: user={}, uri={}", userIdentifier, uri);
            throw new IdempotentException(idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } finally {
            // 执行完成后删除key，允许用户再次提交
            stringRedisTemplate.delete(redisKey);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

- [ ] **Step 2: 格式化代码**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:apply`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/aspect/IdempotentAspect.java
git commit -m "feat: 创建IdempotentAspect幂等切面"
```

---

### Task 6: 在GlobalExceptionHandler中处理IdempotentException

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/exception/GlobalExceptionHandler.java:41-42`

**Interfaces:**
- Consumes: `IdempotentException(code, message)`
- Produces: `Result.error(429, message)` 返回给前端

- [ ] **Step 1: 添加IdempotentException处理器**

在 `GlobalExceptionHandler.java` 的 `handleBusinessException` 方法之后（第40行后），添加：

```java
/**
 * 处理重复提交异常
 *
 * @param e 异常对象
 * @return 处理结果
 */
@ExceptionHandler(IdempotentException.class)
public Result<?> handleIdempotentException(IdempotentException e) {
    log.info("重复提交: {}", e.getMessage());
    return Result.error(e.getCode(), e.getMessage());
}
```

完整文件内容（修改后）：

```java
package com.samoy.chuanbillserver.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.result.ResultEnum;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.info("未登录异常: {}", e.getMessage(), e);
        return Result.error(ResultEnum.UNAUTHORIZED.getCode(), e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.info("业务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理重复提交异常
     *
     * @param e 异常对象
     * @return 处理结果
     */
    @ExceptionHandler(IdempotentException.class)
    public Result<?> handleIdempotentException(IdempotentException e) {
        log.info("重复提交: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> errorMessages = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        log.info("参数验证异常: {}", errorMessages);
        return Result.error(ResultEnum.BAD_REQUEST.getCode(), errorMessages.get(0));
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        List<String> errorMessages = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        log.info("参数绑定异常: {}", errorMessages);
        return Result.error(ResultEnum.BAD_REQUEST.getCode(), errorMessages.get(0));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.info("系统异常: {}", e.getMessage(), e);
        return Result.error("系统异常，请稍后再试");
    }
}
```

- [ ] **Step 2: 格式化代码**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:apply`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/exception/GlobalExceptionHandler.java
git commit -m "feat: GlobalExceptionHandler添加IdempotentException处理"
```

---

### Task 7: 为BillController添加@Idempotent注解

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java:63,70,77,84,101,141,182`

**Interfaces:**
- Consumes: `@Idempotent` 注解
- Produces: 7个POST方法受幂等保护

- [ ] **Step 1: 添加import和注解**

在BillController的import区域添加：

```java
import com.samoy.chuanbillserver.annotation.Idempotent;
```

在以下7个POST方法上添加 `@Idempotent`：

```java
@Idempotent
@PostMapping("/add")
@Operation(summary = "添加账单", description = "创建新的账单记录")

@Idempotent
@PostMapping("/batchCreate")
@Operation(summary = "批量添加账单", description = "批量创建账单记录，用于数据同步")

@Idempotent
@PostMapping("/update")
@Operation(summary = "更新账单", description = "更新已有账单信息")

@Idempotent
@PostMapping("/delete")
@Operation(summary = "删除账单", description = "根据 ID 删除账单记录")

@Idempotent
@PostMapping("/categories")
@Operation(summary = "新增自定义类目", description = "用户新增自定义类目")

@Idempotent
@PostMapping("/payment-methods")
@Operation(summary = "新增自定义支付方式", description = "用户新增自定义支付方式")

@Idempotent
@PostMapping("/export")
@Operation(summary = "导出账单", description = "根据筛选条件导出账单，支持 Excel 和 PDF 格式")
```

- [ ] **Step 2: 格式化代码**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:apply`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java
git commit -m "feat: BillController添加@Idempotent幂等注解"
```

---

### Task 8: 为AuthController添加@Idempotent注解

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/AuthController.java:34,46,58,70,80,91`

**Interfaces:**
- Consumes: `@Idempotent` 注解
- Produces: 6个POST方法受幂等保护

- [ ] **Step 1: 添加import和注解**

在AuthController的import区域添加：

```java
import com.samoy.chuanbillserver.annotation.Idempotent;
```

在以下6个POST方法上添加 `@Idempotent`：

```java
@Idempotent
@PostMapping("/login-password")
@Operation(summary = "密码登录", description = "使用手机号和密码进行登录")

@Idempotent
@PostMapping("/login-phone")
@Operation(summary = "手机号登录", description = "使用手机号和验证码进行登录")

@Idempotent
@PostMapping("/login-wechat")
@Operation(summary = "微信登录", description = "使用微信小程序 code 进行登录")

@Idempotent
@PostMapping("/send-code")
@Operation(summary = "发送验证码", description = "向指定手机号发送短信验证码")

@Idempotent
@PostMapping("/retrieve-password")
@Operation(summary = "找回密码", description = "使用手机号和验证码进行密码重置")

@Idempotent
@PostMapping("/logout")
@Operation(summary = "登出", description = "用户登出，清除登录信息")
```

- [ ] **Step 2: 格式化代码**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:apply`

- [ ] **Step 3: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/AuthController.java
git commit -m "feat: AuthController添加@Idempotent幂等注解"
```

---

### Task 9: 为FamilyController添加@Idempotent注解

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/FamilyController.java:37,44,59,74,81,89,96,119,126`

**Interfaces:**
- Consumes: `@Idempotent` 注解
- Produces: 9个POST方法受幂等保护

- [ ] **Step 1: 添加import和注解**

在FamilyController的import区域添加：

```java
import com.samoy.chuanbillserver.annotation.Idempotent;
```

在以下9个POST方法上添加 `@Idempotent`：

```java
@Idempotent
@PostMapping("/create")
@Operation(summary = "创建家庭", description = "创建一个新的家庭")

@Idempotent
@PostMapping("/update")
@Operation(summary = "更新家庭信息", description = "更新家庭名称、头像、描述等信息")

@Idempotent
@PostMapping("/delete")
@Operation(summary = "删除家庭", description = "仅户主可删除家庭，删除后所有成员将被移除")

@Idempotent
@PostMapping("/join")
@Operation(summary = "申请加入家庭", description = "通过邀请码申请加入家庭，需要户主审批")

@Idempotent
@PostMapping("/leave")
@Operation(summary = "退出家庭", description = "退出当前所在的家庭，户主不能退出，需先转让户主身份")

@Idempotent
@PostMapping("/remove-member")
@Operation(summary = "移除家庭成员", description = "仅户主可移除家庭成员")

@Idempotent
@PostMapping("/transfer-owner")
@Operation(summary = "转让户主", description = "将户主身份转让给其他家庭成员")

@Idempotent
@PostMapping("/handle-apply")
@Operation(summary = "处理加入申请", description = "户主同意或拒绝加入申请")

@Idempotent
@PostMapping("/refresh-invite-code")
@Operation(summary = "刷新邀请码", description = "重新生成家庭邀请码，仅户主可操作")
```

- [ ] **Step 2: 格式化代码**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:apply`

- [ ] **Step 3: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/FamilyController.java
git commit -m "feat: FamilyController添加@Idempotent幂等注解"
```

---

### Task 10: 为UserController添加@Idempotent注解

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserController.java:30,38,46,60,71,80,91,99`

**Interfaces:**
- Consumes: `@Idempotent` 注解
- Produces: 8个POST方法受幂等保护

- [ ] **Step 1: 添加import和注解**

在UserController的import区域添加：

```java
import com.samoy.chuanbillserver.annotation.Idempotent;
```

在以下8个POST方法上添加 `@Idempotent`：

```java
@Idempotent
@PostMapping("/profile/update")
@Operation(summary = "更新用户资料", description = "更新用户的昵称、头像、性别等信息")

@Idempotent
@PostMapping("/password/update-by-old")
@Operation(summary = "通过旧密码修改密码", description = "使用原密码设置新密码")

@Idempotent
@PostMapping("/password/update-by-code")
@Operation(summary = "通过验证码修改密码", description = "使用手机验证码设置新密码")

@Idempotent
@PostMapping("/phone/code")
@Operation(summary = "获取手机验证码", description = "向当前登录的用户获取手机验证码")

@Idempotent
@PostMapping("/phone/update-by-code")
@Operation(summary = "通过验证码更换手机号", description = "使用手机验证码更换手机号")

@Idempotent
@PostMapping("/phone/update-by-password")
@Operation(summary = "通过密码验证更换手机号", description = "使用密码验证更换手机号")

@Idempotent
@PostMapping("/phone/bind")
@Operation(summary = "绑定手机号", description = "绑定手机号")

@Idempotent
@PostMapping("/account/delete")
@Operation(summary = "注销账号", description = "通过手机验证码验证身份后注销账号")
```

- [ ] **Step 2: 格式化代码**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:apply`

- [ ] **Step 3: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserController.java
git commit -m "feat: UserController添加@Idempotent幂等注解"
```

---

### Task 11: 为剩余Controller添加@Idempotent注解

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BudgetController.java:31`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/FileController.java:25`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/MessageController.java:32,39`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserPreferenceController.java:43,51`

**Interfaces:**
- Consumes: `@Idempotent` 注解
- Produces: 6个POST方法受幂等保护（BudgetController 1 + FileController 1 + MessageController 2 + UserPreferenceController 2）

- [ ] **Step 1: BudgetController — 添加import和注解**

在BudgetController的import区域添加：

```java
import com.samoy.chuanbillserver.annotation.Idempotent;
```

```java
@Idempotent
@PostMapping("/set")
@Operation(summary = "设置预算", description = "设置或修改当月预算金额")
```

- [ ] **Step 2: FileController — 添加import和注解**

在FileController的import区域添加：

```java
import com.samoy.chuanbillserver.annotation.Idempotent;
```

```java
@Idempotent
@PostMapping("/temp/upload")
@Operation(summary = "上传临时文件", description = "上传临时文件到本地，返回fileId供ocr使用")
```

- [ ] **Step 3: MessageController — 添加import和注解**

在MessageController的import区域添加：

```java
import com.samoy.chuanbillserver.annotation.Idempotent;
```

```java
@Idempotent
@PostMapping("/mark-read")
@Operation(summary = "标记消息已读", description = "将指定消息标记为已读")

@Idempotent
@PostMapping("/mark-all-read")
@Operation(summary = "全部标记已读", description = "将所有未读消息标记为已读")
```

- [ ] **Step 4: UserPreferenceController — 添加import和注解**

在UserPreferenceController的import区域添加：

```java
import com.samoy.chuanbillserver.annotation.Idempotent;
```

```java
@Idempotent
@PostMapping("/set")
@Operation(summary = "设置偏好", description = "设置单个偏好值")

@Idempotent
@PostMapping("/delete")
@Operation(summary = "删除偏好", description = "删除单个偏好设置")
```

- [ ] **Step 5: 格式化代码**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:apply`

- [ ] **Step 6: Commit**

```bash
cd E:/Projects/chuan-bill
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BudgetController.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/FileController.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/MessageController.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserPreferenceController.java
git commit -m "feat: Budget/File/Message/Preference Controller添加@Idempotent幂等注解"
```

---

### Task 12: 验证编译通过

**Files:**
- 无新增/修改

**Interfaces:**
- 验证所有改动编译无错误

- [ ] **Step 1: 编译检查**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn clean compile`

Expected: BUILD SUCCESS

- [ ] **Step 2: 格式检查**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:check`

Expected: BUILD SUCCESS（无格式问题）

- [ ] **Step 3: 如有格式问题，修复并重新检查**

Run: `cd E:/Projects/chuan-bill/chuan-bill-server && mvn spotless:apply`

然后重复 Step 2。
