package com.samoy.chuanbillserver.enums;

import lombok.Getter;

/**
 * <p>
 * 短信场景枚举类
 * </p>
 *
 * @author samoy
 * @since 2026/5/17
 */
@Getter
public enum SmsScene {
    LOGIN("登录/注册", "100001"),
    CHANGE_BIND_PHONE("修改绑定手机号", "100002"),
    RESET_PASSWORD("重置密码", "100003"),
    BIND_PHONE("绑定新手机号", "100004"),
    VERIFY_BIND_PHONE("验证绑定手机号", "100005"),
    ;

    private final String scene;
    private final String templateCode;

    SmsScene(String scene, String templateCode) {
        this.scene = scene;
        this.templateCode = templateCode;
    }

    public static SmsScene fromCode(String sceneCode) {
        for (SmsScene smsScene : SmsScene.values()) {
            if (smsScene.templateCode.equals(sceneCode)) {
                return smsScene;
            }
        }
        throw new IllegalArgumentException("Invalid scene code: " + sceneCode);
    }
}
