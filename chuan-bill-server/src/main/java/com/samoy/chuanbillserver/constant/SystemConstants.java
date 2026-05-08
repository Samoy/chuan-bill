package com.samoy.chuanbillserver.constant;

public class SystemConstants {

    /**
     * 验证码长度
     */
    public static final Integer CODE_LENGTH = 6;

    /**
     * 验证码缓存键前缀
     */
    public static final String CODE_CACHE_KEY_PREFIX = "sms:code:";

    /**
     * 验证码缓存过期时间(秒) --- 5分钟
     */
    public static final Integer CODE_EXPIRE_TIME = 5 * 60;

    /**
     * 用户正常状态
     */
    public static final Object USER_STATUS_NORMAL = 1;

    /**
     * 用户性别未知
     */
    public static final Byte USER_GENDER_UNKNOWN = 0;

    /**
     * 临时文件上传目录
     */
    public static final String TEMP_FILE_UPLOAD_DIR = "temp/upload/";

    /**
     * 普通用户每日AI分析次数限制
     */
    public static final int AI_DAILY_LIMIT_NORMAL = 5;

    /**
     * 同意家庭成员申请
     */
    public static final int AGREE_FAMILY_MEMBER_APPLY = 1;

    /**
     * 拒绝家庭成员申请
     */
    public static final int REFUSE_FAMILY_MEMBER_APPLY = 2;

    /**
     * 用户账单分析
     */
    public static final int USER_ANALYSIS_TYPE = 1;
    /**
     * 家庭账单分析
     */
    public static final int FAMILY_ANALYSIS_TYPE = 2;

    /**
     * 七牛上传token过期时间 - 5分钟
     */
    public static final long QINIU_UPLOAD_TOKEN_EXPIRE_TIME = 5 * 60;
}
