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
    CAPTCHA_INVALID(1004, "验证码错误"),
    TOKEN_EXPIRED(1005, "验证码已过期"),
    PHONE_OR_PASSWORD_MISSING(1006, "手机号或密码不能为空"),
    PHONE_MISSING(1007, "手机号不能为空"),
    PASSWORD_MISSING(1008, "密码不能为空"),
    PASSWORD_NOT_SET(1009, "密码未设置"),
    LOGIN_ERROR(1010, "登录失败"),
    PARAM_VALID_ERROR(1011, "参数校验失败"),
    PHONE_NOT_FOUND(1012, "当前用户未绑定手机号"),
    PHONE_ALREADY_BOUND(1013, "当前手机号已被绑定"),
    SMS_SEND_FAILED(1014, "短信验证码发送失败"),

    // 业务错误码（账单相关） 2000+
    BILL_NOT_FOUND(2001, "账单不存在"),
    BILL_NOT_ALLOW_VIEW(2002, "无权查看此账单"),
    BILL_NOT_ALLOW_UPDATE(2003, "无权修改此账单"),
    BILL_NOT_ALLOW_DELETE(2004, "无权删除此账单"),
    BILL_OCR_FAILED(2101, "账单OCR识别失败"),
    BILL_TEXT_FAILED(2102, "账单文本识别失败"),
    BILL_ANALYSIS_FAILED(2103, "账单分析失败"),
    AI_ANALYSIS_RATE_LIMITED(2104, "今日AI分析次数已用完"),
    BILL_EXPORT_TIME_RANGE_EXCEEDED(2105, "时间范围不能超过3个月"),
    BILL_EXPORT_DATA_TOO_LARGE(2106, "数据量过大，请缩小筛选范围"),
    BILL_EXPORT_NO_DATA(2107, "无导出数据"),
    BILL_EXPORT_FAILED(2108, "导出失败"),

    // 文件错误码 3000+
    FILE_NOT_FOUND(3001, "文件不存在"),
    FILE_UPLOAD_FAILED(3002, "文件上传失败"),
    FILE_NOT_IMAGE(3003, "不是图片文件"),

    // 家庭相关错误码 4000+
    FAMILY_NOT_FOUND(4001, "家庭不存在"),
    FAMILY_NOT_OWNER(4002, "仅户主可执行此操作"),
    FAMILY_NOT_MEMBER(4003, "您不是该家庭成员"),
    FAMILY_ALREADY_MEMBER(4004, "您已是该家庭成员"),
    FAMILY_INVITE_CODE_INVALID(4005, "邀请码无效"),
    FAMILY_OWNER_CANNOT_LEAVE(4006, "户主不能退出家庭，请先转让户主"),
    FAMILY_APPLY_NOT_FOUND(4007, "加入申请不存在"),
    FAMILY_APPLY_ALREADY_PENDING(4008, "您已提交过申请，请等待审批"),
    FAMILY_APPLY_ALREADY_HANDLED(4009, "该申请已被处理"),
    FAMILY_TRANSFER_TARGET_NOT_MEMBER(4010, "目标用户不是该家庭成员"),
    FAMILY_CANNOT_REMOVE_OWNER(4011, "不能移除户主"),
    FAMILY_CREATE_LIMIT_REACHED(4012, "每个家庭最多只能创建5个家庭"),

    // 类目/支付方式相关错误码 6000+
    CATEGORY_NOT_FOUND(6001, "类目不存在"),
    CATEGORY_HAS_BILLS(6002, "该类目下存在账单，无法删除"),
    CANNOT_MODIFY_DEFAULT_CATEGORY(6003, "系统预设类目不可修改"),
    PAYMENT_METHOD_NOT_FOUND(6004, "支付方式不存在"),
    PAYMENT_METHOD_HAS_BILLS(6005, "该支付方式下存在账单，无法删除"),
    CANNOT_MODIFY_DEFAULT_PAYMENT_METHOD(6006, "系统预设支付方式不可修改"),

    // 消息相关错误码 7000+
    MESSAGE_NOT_FOUND(7001, "消息不存在"),

    // 预算相关错误码 8000+
    BUDGET_NOT_FOUND(8001, "预算不存在"),

    // 内容安全相关错误码 9000+
    CONTENT_MODERATION_FAILED(9001, "内容包含违规信息，请修改后重试"),
    ;

    private final Integer code;
    private final String message;

    ResultEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
