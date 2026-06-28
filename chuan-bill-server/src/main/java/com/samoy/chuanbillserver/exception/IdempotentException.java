package com.samoy.chuanbillserver.exception;

import com.samoy.chuanbillserver.result.ResultEnum;
import lombok.Getter;

@Getter
public class IdempotentException extends RuntimeException {

    private final Integer code;

    public IdempotentException(String message) {
        super(message);
        this.code = ResultEnum.REPEAT_SUBMIT.getCode();
    }

    public IdempotentException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
