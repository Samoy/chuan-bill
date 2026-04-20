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

    /**
     * 处理未登录异常
     *
     * @param e 异常对象
     * @return 处理结果
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.info("未登录异常: {}", e.getMessage(), e);
        return Result.error(ResultEnum.UNAUTHORIZED.getCode(), e.getMessage());
    }

    /**
     * 处理业务异常
     *
     * @param e 异常对象
     * @return 处理结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.info("业务异常: {}", e.getMessage(), e);
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

    /**
     * 处理其他异常
     *
     * @param e 异常对象
     * @return 处理结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.info("系统异常: {}", e.getMessage(), e);
        return Result.error("系统异常，请稍后再试");
    }
}
