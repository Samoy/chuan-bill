package com.samoy.chuanbillserver.result;

import lombok.Getter;

@Getter
public enum ResultEnum {
    // 成功状态码
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "请求未授权"),
    FORBIDDEN(403, "请求被拒绝"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    UNPROCESSABLE_ENTITY(422, "请求参数校验失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 服务器错误 5xx
    ERROR(500, "服务器内部错误"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    // 业务错误码（用户相关） 1000+
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    PASSWORD_ERROR(1003, "密码错误"),
    TOKEN_INVALID(1004, "验证码错误"),
    TOKEN_EXPIRED(1005, "验证码已过期"),
    PHONE_OR_PASSWORD_MISSING(1006, "手机号或密码不能为空"),
    PHONE_MISSING(1007, "手机号不能为空"),
    PASSWORD_MISSING(1008, "密码不能为空"),
    PASSWORD_NOT_SET(1009, "密码未设置"),

    // 业务错误码（账单相关） 2000+
    BILL_NOT_FOUND(2001, "账单不存在"),
    BILL_NOT_ALLOW_VIEW(2002, "无权查看此账单"),
    BILL_NOT_ALLOW_UPDATE(2003, "无权修改此账单"),
    BILL_NOT_ALLOW_DELETE(2004, "无权删除此账单"),
    BILL_OCR_FAILED(2101, "账单OCR识别失败"),
    BILL_TEXT_FAILED(2102, "账单文本识别失败"),
    BILL_ANALYSIS_FAILED(2103, "账单分析失败"),

    // 文件错误码 3000+
    FILE_NOT_FOUND(3001, "文件不存在"),
    FILE_UPLOAD_FAILED(3002, "文件上传失败"),
    FILE_NOT_IMAGE(3003, "不是图片文件"),
    ;

    private final Integer code;
    private final String message;

    ResultEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
