package com.samoy.chuanbillserver.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(data, ResultEnum.SUCCESS.getMessage());
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<T>()
                .setCode(ResultEnum.SUCCESS.getCode())
                .setMessage(message)
                .setData(data)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> error(String message) {
        return error(ResultEnum.ERROR.getCode(), message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<T>()
                .setCode(code)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> error(ResultEnum resultEnum) {
        return error(resultEnum.getCode(), resultEnum.getMessage());
    }

    public boolean isSuccess() {
        return ResultEnum.SUCCESS.getCode().equals(this.code);
    }
}
