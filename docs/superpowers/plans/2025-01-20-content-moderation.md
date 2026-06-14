# 内容安全审核实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 集成阿里云内容安全文本审核服务，通过自定义注解实现声明式内容审核

**Architecture:** 使用 Hibernate Validator 自定义约束注解 + 阿里云 Green SDK + Redis 缓存，在 DTO 字段上添加 @TextModeration 注解实现自动审核

**Tech Stack:** Spring Boot 3, Hibernate Validator, 阿里云 green20220302 SDK 3.3.3, Redis

---

## 文件结构

### 新建文件
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/enums/ModerationScene.java` - 审核场景枚举
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/validation/TextModeration.java` - 自定义约束注解
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/validation/TextModerationConstraintValidator.java` - 校验器实现
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/ContentSafetyConfig.java` - 阿里云客户端配置
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/ContentSafetyService.java` - 内容安全服务接口
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/ContentSafetyServiceImpl.java` - 内容安全服务实现
- `chuan-bill-server/src/test/java/com/samoy/chuanbillserver/service/ContentSafetyServiceTest.java` - 单元测试

### 修改文件
- `chuan-bill-server/pom.xml` - 添加阿里云 SDK 依赖
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java` - 添加审核失败错误码
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/CreateFamilyDTO.java` - 添加 @TextModeration 注解
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UpdateFamilyDTO.java` - 添加 @TextModeration 注解
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UserProfileUpdateDTO.java` - 添加 @TextModeration 注解
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/AddBillDTO.java` - 添加 @TextModeration 注解
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UpdateBillDTO.java` - 添加 @TextModeration 注解

---

### Task 1: 添加 Maven 依赖

**Files:**
- Modify: `chuan-bill-server/pom.xml`

- [ ] **Step 1: 添加阿里云内容安全 SDK 依赖**

在 `<dependencies>` 标签内添加：

```xml
<!-- 阿里云内容安全SDK -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>green20220302</artifactId>
    <version>3.3.3</version>
</dependency>
```

- [ ] **Step 2: 验证依赖下载**

运行: `cd chuan-bill-server && mvn dependency:resolve -DincludeArtifactIds=green20220302`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/pom.xml
git commit -m "deps: 添加阿里云内容安全SDK依赖"
```

---

### Task 2: 添加审核失败错误码

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java`

- [ ] **Step 1: 添加内容安全相关错误码**

在 `// 消息相关错误码 4050+` 之前添加：

```java
// 内容安全相关错误码 4100+
CONTENT_MODERATION_FAILED(4101, "内容包含违规信息，请修改后重试"),
```

完整的枚举应包含：

```java
// 消息相关错误码 4050+
MESSAGE_NOT_FOUND(4051, "消息不存在"),

// 内容安全相关错误码 4100+
CONTENT_MODERATION_FAILED(4101, "内容包含违规信息，请修改后重试"),
```

- [ ] **Step 2: 编译验证**

运行: `cd chuan-bill-server && mvn compile`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java
git commit -m "feat: 添加内容安全审核失败错误码"
```

---

### Task 3: 创建审核场景枚举

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/enums/ModerationScene.java`

- [ ] **Step 1: 创建 ModerationScene 枚举**

```java
package com.samoy.chuanbillserver.enums;

import lombok.Getter;

/**
 * 内容安全审核场景
 */
@Getter
public enum ModerationScene {

    /** 昵称检测 */
    NICKNAME("nickname_detection"),

    /** 家庭名称检测（复用昵称检测） */
    FAMILY_NAME("nickname_detection"),

    /** 描述检测（评论类） */
    DESCRIPTION("comment_detection"),

    /** 标题检测 */
    TITLE("title_detection");

    /** 阿里云服务类型 */
    private final String service;

    ModerationScene(String service) {
        this.service = service;
    }
}
```

- [ ] **Step 2: 编译验证**

运行: `cd chuan-bill-server && mvn compile`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/enums/ModerationScene.java
git commit -m "feat: 创建内容安全审核场景枚举"
```

---

### Task 4: 创建 @TextModeration 自定义注解

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/validation/TextModeration.java`

- [ ] **Step 1: 创建 validation 包目录**

运行: `mkdir -p chuan-bill-server/src/main/java/com/samoy/chuanbillserver/validation`

- [ ] **Step 2: 创建 @TextModeration 注解**

```java
package com.samoy.chuanbillserver.validation;

import com.samoy.chuanbillserver.enums.ModerationScene;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 文本内容安全审核注解
 * <p>
 * 用于DTO字段上，触发阿里云内容安全检测
 * </p>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TextModerationConstraintValidator.class)
@Documented
public @interface TextModeration {

    /**
     * 审核场景
     */
    ModerationScene scene() default ModerationScene.TITLE;

    /**
     * 校验失败时的提示信息
     */
    String message() default "内容包含违规信息";

    /**
     * 分组校验
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
}
```

- [ ] **Step 3: 编译验证（会失败，因为 Validator 还未创建）**

运行: `cd chuan-bill-server && mvn compile`

Expected: 编译失败，提示找不到 `TextModerationConstraintValidator`

- [ ] **Step 4: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/validation/TextModeration.java
git commit -m "feat: 创建@TextModeration自定义约束注解"
```

---

### Task 5: 创建阿里云客户端配置

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/ContentSafetyConfig.java`

- [ ] **Step 1: 创建 ContentSafetyConfig 配置类**

```java
package com.samoy.chuanbillserver.config;

import com.aliyun.green20220302.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云内容安全配置
 */
@Configuration
public class ContentSafetyConfig {

    @Value("${ALIBABA_CLOUD_ACCESS_KEY_ID}")
    private String accessKeyId;

    @Value("${ALIBABA_CLOUD_ACCESS_KEY_SECRET}")
    private String accessKeySecret;

    @Bean
    public Client contentSafetyClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint("green-cip.cn-shanghai.aliyuncs.com");
        return new Client(config);
    }
}
```

- [ ] **Step 2: 编译验证**

运行: `cd chuan-bill-server && mvn compile`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/config/ContentSafetyConfig.java
git commit -m "feat: 创建阿里云内容安全客户端配置"
```

---

### Task 6: 创建内容安全服务

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/ContentSafetyService.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/ContentSafetyServiceImpl.java`

- [ ] **Step 1: 创建 ContentSafetyService 接口**

```java
package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.enums.ModerationScene;

/**
 * 内容安全服务
 */
public interface ContentSafetyService {

    /**
     * 检测文本内容是否合规
     *
     * @param text  待检测文本
     * @param scene 审核场景
     * @return true-审核通过，false-审核不通过
     */
    boolean checkText(String text, ModerationScene scene);
}
```

- [ ] **Step 2: 创建 ContentSafetyServiceImpl 实现类**

```java
package com.samoy.chuanbillserver.service.impl;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationRequest;
import com.aliyun.green20220302.models.TextModerationResponse;
import com.aliyun.green20220302.models.TextModerationResponseBody;
import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.service.IContentSafetyService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * 内容安全服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSafetyServiceImpl implements ContentSafetyService {

    private final Client contentSafetyClient;
    private final StringRedisTemplate redisTemplate;

    /** 缓存key前缀 */
    private static final String CACHE_PREFIX = "content:moderation:text:";

    /** 缓存过期时间：14天 */
    private static final Duration CACHE_TTL = Duration.ofDays(14);

    @Override
    public boolean checkText(String text, ModerationScene scene) {
        if (!StringUtils.hasText(text)) {
            return true;
        }

        String cacheKey = CACHE_PREFIX + DigestUtils.md5Hex(text);

        // 1. 检查缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("命中缓存: key={}", cacheKey);
            return Boolean.parseBoolean(cached);
        }

        // 2. 调用阿里云API
        boolean passed = callAliyunApi(text, scene.getService());

        // 3. 存入缓存
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(passed), CACHE_TTL);
        log.debug("缓存写入: key={}, passed={}", cacheKey, passed);

        return passed;
    }

    /**
     * 调用阿里云文本审核API
     *
     * @param text    待检测文本
     * @param service 服务类型
     * @return true-审核通过，false-审核不通过
     */
    private boolean callAliyunApi(String text, String service) {
        try {
            TextModerationRequest request = new TextModerationRequest()
                    .setService(service)
                    .setTextContent(text);

            TextModerationResponse response = contentSafetyClient.textModeration(request);

            if (response.getBody() == null) {
                log.warn("阿里云内容安全响应为空");
                return true;
            }

            TextModerationResponseBody body = response.getBody();

            // 检查响应码
            if (!"200".equals(body.getCode())) {
                log.warn("阿里云内容安全调用失败: code={}, message={}", body.getCode(), body.getMessage());
                return true;
            }

            // 检查审核结果
            if (body.getData() != null && body.getData().getAdvice() != null) {
                String label = body.getData().getLabel();
                log.info("文本审核结果: text={}, label={}", text, label);
                // label 为 nonLabel 表示审核通过
                return "nonLabel".equals(label);
            }

            return true;
        } catch (Exception e) {
            log.error("调用阿里云内容安全服务异常", e);
            // 异常时放行，避免阻塞正常业务
            return true;
        }
    }
}
```

- [ ] **Step 3: 编译验证**

运行: `cd chuan-bill-server && mvn compile`

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/ContentSafetyService.java
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/ContentSafetyServiceImpl.java
git commit -m "feat: 创建内容安全服务及实现"
```

---

### Task 7: 创建 TextModerationConstraintValidator

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/validation/TextModerationConstraintValidator.java`

- [ ] **Step 1: 创建校验器实现**

```java
package com.samoy.chuanbillserver.validation;

import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IContentSafetyService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文本内容安全审核校验器
 */
@Component
@RequiredArgsConstructor
public class TextModerationConstraintValidator implements ConstraintValidator<TextModeration, String> {

    private final ContentSafetyService contentSafetyService;

    private ModerationScene scene;

    @Override
    public void initialize(TextModeration annotation) {
        this.scene = annotation.scene();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 空值不校验，由 @NotBlank 等注解处理
        if (!StringUtils.hasText(value)) {
            return true;
        }

        boolean passed = contentSafetyService.checkText(value, scene);

        if (!passed) {
            // 审核不通过，抛出业务异常
            throw new BusinessException(ResultEnum.CONTENT_MODERATION_FAILED);
        }

        return true;
    }
}
```

- [ ] **Step 2: 编译验证**

运行: `cd chuan-bill-server && mvn compile`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/validation/TextModerationConstraintValidator.java
git commit -m "feat: 创建文本审核校验器实现"
```

---

### Task 8: 修改 DTO 添加 @TextModeration 注解

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/CreateFamilyDTO.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UpdateFamilyDTO.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UserProfileUpdateDTO.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/AddBillDTO.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UpdateBillDTO.java`

- [ ] **Step 1: 修改 CreateFamilyDTO**

添加 import:
```java
import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.validation.TextModeration;
```

在 `name` 字段添加注解:
```java
@NotBlank(message = "家庭名称不能为空")
@Size(max = 20, message = "家庭名称长度不能超过20个字符")
@TextModeration(scene = ModerationScene.FAMILY_NAME)
@Schema(description = "家庭名称", example = "我的小家", requiredMode = Schema.RequiredMode.REQUIRED)
private String name;
```

在 `description` 字段添加注解:
```java
@Size(max = 200, message = "家庭描述长度不能超过200个字符")
@TextModeration(scene = ModerationScene.DESCRIPTION)
@Schema(description = "家庭描述", example = "温馨的三口之家")
private String description;
```

- [ ] **Step 2: 修改 UpdateFamilyDTO**

添加 import:
```java
import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.validation.TextModeration;
```

在 `name` 字段添加注解:
```java
@Size(max = 20, message = "家庭名称长度不能超过20个字符")
@TextModeration(scene = ModerationScene.FAMILY_NAME)
@Schema(description = "家庭名称", example = "我的小家")
private String name;
```

在 `description` 字段添加注解:
```java
@Size(max = 200, message = "家庭描述长度不能超过200个字符")
@TextModeration(scene = ModerationScene.DESCRIPTION)
@Schema(description = "家庭描述", example = "温馨的三口之家")
private String description;
```

- [ ] **Step 3: 修改 UserProfileUpdateDTO**

添加 import:
```java
import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.validation.TextModeration;
```

在 `nickname` 字段添加注解:
```java
@Size(max = 50, message = "昵称长度不能超过 50 个字符")
@TextModeration(scene = ModerationScene.NICKNAME)
@Schema(description = "昵称", example = "张三")
private String nickname;
```

- [ ] **Step 4: 修改 AddBillDTO**

添加 import:
```java
import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.validation.TextModeration;
```

在 `name` 字段添加注解:
```java
@NotBlank(message = "账单名称不能为空")
@Size(min = 1, max = 50, message = "账单名称长度在 1 到 50 个字符之间")
@TextModeration(scene = ModerationScene.TITLE)
@Schema(description = "账单名称", example = "早餐", requiredMode = Schema.RequiredMode.REQUIRED)
private String name;
```

在 `remark` 字段添加注解:
```java
@Size(max = 500, message = "账单备注长度不能超过 500 个字符")
@TextModeration(scene = ModerationScene.DESCRIPTION)
@Schema(description = "账单备注", example = "在公司楼下吃的早餐")
private String remark;
```

- [ ] **Step 5: 修改 UpdateBillDTO**

添加 import:
```java
import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.validation.TextModeration;
```

在 `name` 字段添加注解（如果有）:
```java
@Size(min = 1, max = 50, message = "账单名称长度在 1 到 50 个字符之间")
@TextModeration(scene = ModerationScene.TITLE)
@Schema(description = "账单名称", example = "早餐")
private String name;
```

在 `remark` 字段添加注解（如果有）:
```java
@Size(max = 500, message = "账单备注长度不能超过 500 个字符")
@TextModeration(scene = ModerationScene.DESCRIPTION)
@Schema(description = "账单备注", example = "在公司楼下吃的早餐")
private String remark;
```

- [ ] **Step 6: 编译验证**

运行: `cd chuan-bill-server && mvn compile`

Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/
git commit -m "feat: 在DTO字段添加@TextModeration审核注解"
```

---

### Task 9: 编写单元测试

**Files:**
- Create: `chuan-bill-server/src/test/java/com/samoy/chuanbillserver/service/ContentSafetyServiceTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.service.impl.ContentSafetyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentSafetyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ContentSafetyServiceImpl contentSafetyService;

    @Test
    void checkText_withEmptyText_shouldReturnTrue() {
        assertTrue(contentSafetyService.checkText(null, ModerationScene.TITLE));
        assertTrue(contentSafetyService.checkText("", ModerationScene.TITLE));
        assertTrue(contentSafetyService.checkText("   ", ModerationScene.TITLE));
    }

    @Test
    void checkText_withCachedResult_shouldReturnCachedValue() {
        String text = "测试文本";
        String cacheKey = "content:moderation:text:" + org.springframework.util.DigestUtils.md5Hex(text);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("true");

        assertTrue(contentSafetyService.checkText(text, ModerationScene.TITLE));

        verify(valueOperations).get(cacheKey);
        verify(valueOperations, never()).set(anyString(), anyString(), any());
    }

    @Test
    void checkText_withCachedFalse_shouldReturnFalse() {
        String text = "违规文本";
        String cacheKey = "content:moderation:text:" + org.springframework.util.DigestUtils.md5Hex(text);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("false");

        assertFalse(contentSafetyService.checkText(text, ModerationScene.TITLE));
    }
}
```

- [ ] **Step 2: 运行测试**

运行: `cd chuan-bill-server && mvn test -Dtest=ContentSafetyServiceTest`

Expected: Tests pass

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-server/src/test/java/com/samoy/chuanbillserver/service/ContentSafetyServiceTest.java
git commit -m "test: 添加内容安全服务单元测试"
```

---

### Task 10: 集成测试验证

**Files:**
- 无新建文件

- [ ] **Step 1: 启动应用验证配置**

运行: `cd chuan-bill-server && mvn spring-boot:run`

Expected: 应用启动成功，无报错

- [ ] **Step 2: 测试审核通过场景**

使用 curl 或 Postman 发送请求：

```bash
curl -X POST http://localhost:8080/api/family/create \
  -H "Content-Type: application/json" \
  -d '{"name": "我的小家", "description": "温馨的三口之家"}'
```

Expected: 创建成功

- [ ] **Step 3: 测试审核不通过场景**

```bash
curl -X POST http://localhost:8080/api/family/create \
  -H "Content-Type: application/json" \
  -d '{"name": "违规内容测试", "description": "测试描述"}'
```

Expected: 返回错误码 4101，消息 "内容包含违规信息，请修改后重试"

- [ ] **Step 4: 验证缓存生效**

重复发送相同请求，观察日志中是否出现 "命中缓存" 字样

Expected: 第二次请求命中缓存

- [ ] **Step 5: 最终提交**

```bash
git add -A
git commit -m "feat: 完成内容安全审核功能集成"
```

---

## 审核场景映射表

| 业务场景 | DTO | 字段 | ModerationScene | 阿里云服务 |
|---------|-----|------|-----------------|-----------|
| 创建家庭 | CreateFamilyDTO | name | FAMILY_NAME | nickname_detection |
| 创建家庭 | CreateFamilyDTO | description | DESCRIPTION | comment_detection |
| 更新家庭 | UpdateFamilyDTO | name | FAMILY_NAME | nickname_detection |
| 更新家庭 | UpdateFamilyDTO | description | DESCRIPTION | comment_detection |
| 更新资料 | UserProfileUpdateDTO | nickname | NICKNAME | nickname_detection |
| 添加账单 | AddBillDTO | name | TITLE | title_detection |
| 添加账单 | AddBillDTO | remark | DESCRIPTION | comment_detection |
| 更新账单 | UpdateBillDTO | name | TITLE | title_detection |
| 更新账单 | UpdateBillDTO | remark | DESCRIPTION | comment_detection |

## 注意事项

1. **异常处理**: 阿里云 API 调用异常时默认放行，避免阻塞正常业务
2. **缓存策略**: 基于文本内容 MD5 命中，与用户无关，全局复用
3. **校验顺序**: @TextModeration 应放在 @NotBlank/@Size 之后，先校验格式再校验内容
4. **性能考虑**: Redis 缓存 14 天，减少 API 调用次数
