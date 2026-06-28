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
