# 内容安全审核设计文档

## 概述

为满足应用审核要求，在用户记账、创建/编辑家庭、更新个人资料等场景集成阿里云内容安全文本审核服务。采用自定义注解 + Hibernate Validator 的方式实现声明式审核。

## 架构设计

```
DTO字段 (@TextModeration)
    ↓
Hibernate Validator 触发
    ↓
TextModerationConstraintValidator
    ↓
检查Redis缓存 → 命中则返回缓存结果
    ↓
未命中 → 调用阿里云SDK → 存入Redis
    ↓
审核不通过 → 抛出 BusinessException(CONTENT_MODERATION_FAILED)
    ↓
GlobalExceptionHandler 统一处理
    ↓
返回用户
```

## 技术选型

| 组件 | 选择 | 说明 |
|------|------|------|
| SDK | `com.aliyun:green20220302:3.3.3` | 阿里云内容安全新版SDK |
| 验证框架 | Hibernate Validator | 与现有 `spring-boot-starter-validation` 集成 |
| 缓存 | Redis | 项目已有Redis依赖 |
| 异常处理 | BusinessException | 复用现有异常处理机制 |

## 核心组件

### 1. ModerationScene 枚举

定义审核场景，映射阿里云服务类型：

```java
public enum ModerationScene {
    NICKNAME("nickname_detection"),      // 昵称检测
    FAMILY_NAME("nickname_detection"),   // 家庭名称（复用昵称检测）
    DESCRIPTION("comment_detection"),    // 描述检测
    TITLE("title_detection");            // 标题检测

    private final String service;

    ModerationScene(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
```

### 2. @TextModeration 注解

自定义约束注解，用于DTO字段：

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TextModerationConstraintValidator.class)
public @interface TextModeration {
    ModerationScene scene() default ModerationScene.TITLE;
    String message() default "内容包含违规信息";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

### 3. TextModerationConstraintValidator

校验器实现，处理缓存和审核逻辑：

```java
public class TextModerationConstraintValidator
    implements ConstraintValidator<TextModeration, String> {

    @Autowired
    private ContentSafetyService contentSafetyService;

    private ModerationScene scene;

    @Override
    public void initialize(TextModeration annotation) {
        this.scene = annotation.scene();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;  // 空值不校验，由 @NotBlank 等注解处理
        }
        return contentSafetyService.checkText(value, scene);
    }
}
```

### 4. ContentSafetyService

核心服务，封装SDK调用和缓存逻辑：

```java
@Service
public class ContentSafetyService {

    private final Client client;
    private final StringRedisTemplate redisTemplate;

    // 缓存key前缀
    private static final String CACHE_PREFIX = "content:moderation:text:";
    // 缓存过期时间：14天
    private static final Duration CACHE_TTL = Duration.ofDays(14);

    public boolean checkText(String text, ModerationScene scene) {
        String cacheKey = CACHE_PREFIX + DigestUtils.md5Hex(text);

        // 1. 检查缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Boolean.parseBoolean(cached);
        }

        // 2. 调用阿里云SDK
        boolean passed = callAliyunApi(text, scene.getService());

        // 3. 存入缓存
        redisTemplate.opsForValue().set(
            cacheKey,
            String.valueOf(passed),
            CACHE_TTL
        );

        return passed;
    }

    private boolean callAliyunApi(String text, String service) {
        // 调用阿里云 TextModeration API
        // 返回 true 表示审核通过，false 表示不通过
    }
}
```

### 5. AliyunContentSafetyConfig

阿里云客户端配置：

```java
@Configuration
public class AliyunContentSafetyConfig {

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

### 6. ResultEnum 扩展

添加审核失败错误码：

```java
CONTENT_MODERATION_FAILED(1001, "内容包含违规信息，请修改后重试")
```

## 需要修改的DTO

| DTO | 字段 | 审核场景 |
|-----|------|----------|
| CreateFamilyDTO | name | FAMILY_NAME |
| CreateFamilyDTO | description | DESCRIPTION |
| UpdateFamilyDTO | name | FAMILY_NAME |
| UpdateFamilyDTO | description | DESCRIPTION |
| UserProfileUpdateDTO | nickname | NICKNAME |
| AddBillDTO | name | TITLE |
| AddBillDTO | remark | DESCRIPTION |
| UpdateBillDTO | name | TITLE |
| UpdateBillDTO | remark | DESCRIPTION |

## 使用示例

```java
@Data
public class CreateFamilyDTO {

    @NotBlank(message = "家庭名称不能为空")
    @Size(max = 20, message = "家庭名称长度不能超过20个字符")
    @TextModeration(scene = ModerationScene.FAMILY_NAME)
    private String name;

    @Size(max = 200, message = "家庭描述长度不能超过200个字符")
    @TextModeration(scene = ModerationScene.DESCRIPTION)
    private String description;
}
```

## 缓存策略

- **Key格式**：`content:moderation:text:{md5(文本内容)}`
- **Value**：`true`（通过）/ `false`（不通过）
- **过期时间**：14天
- **特点**：与用户无关，同一文本全局命中

## 异常处理

审核不通过时抛出 `BusinessException`，由 `GlobalExceptionHandler` 统一处理：

```json
{
    "code": 1001,
    "message": "内容包含违规信息，请修改后重试",
    "data": null,
    "timestamp": 1705737600000
}
```

## 依赖项

```xml
<!-- 阿里云内容安全SDK -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>green20220302</artifactId>
    <version>3.3.3</version>
</dependency>
```

## 环境变量

```env
ALIBABA_CLOUD_ACCESS_KEY_ID=xxx
ALIBABA_CLOUD_ACCESS_KEY_SECRET=xxx
```
