package com.samoy.chuanbillserver.enums;

import lombok.Getter;

/**
 * 内容安全审核场景
 */
@Getter
public enum ModerationScene {

    /** 昵称检测 */
    NICKNAME("nickname_detection_pro"),

    /** 家庭名称检测（复用昵称检测） */
    FAMILY_NAME("nickname_detection_pro"),

    /** 描述检测（评论类） */
    DESCRIPTION("comment_detection_pro"),

    /** 标题检测 */
    TITLE("nickname_detection_pro");

    /** 阿里云服务类型 */
    private final String service;

    ModerationScene(String service) {
        this.service = service;
    }
}
