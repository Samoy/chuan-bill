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
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
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
