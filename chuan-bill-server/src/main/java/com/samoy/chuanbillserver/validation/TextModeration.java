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
    String message() default "内容违规";

    /**
     * 分组校验
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
}
