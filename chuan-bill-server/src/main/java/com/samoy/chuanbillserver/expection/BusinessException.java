package com.samoy.chuanbillserver.expection;

import com.samoy.chuanbillserver.result.ResultEnum;
import lombok.Getter;

public class BusinessException extends RuntimeException {

    @Getter
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultEnum.ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultEnum resultEnum) {
        super(resultEnum.getMessage());
        this.code = resultEnum.getCode();
    }

    public BusinessException(ResultEnum resultEnum, String message) {
        super(message);
        this.code = resultEnum.getCode();
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }


}
