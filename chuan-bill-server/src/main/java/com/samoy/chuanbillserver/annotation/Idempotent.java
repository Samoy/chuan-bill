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
