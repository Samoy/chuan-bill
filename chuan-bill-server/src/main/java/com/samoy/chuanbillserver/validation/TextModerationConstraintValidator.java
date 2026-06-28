package com.samoy.chuanbillserver.validation;

import com.samoy.chuanbillserver.enums.ModerationScene;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.utils.ModerationUtil;
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

    private final ModerationUtil moderationUtil;

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

        boolean passed = moderationUtil.checkText(value, scene);

        if (!passed) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ResultEnum.CONTENT_MODERATION_FAILED.getMessage())
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
