package com.samoy.chuanbillserver.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户偏好设置配置
 *
 * @author Samoy
 * @since 2026-05-12
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "preference")
public class PreferenceProperties {

    /**
     * 允许用户设置的偏好键列表
     */
    private List<String> allowedKeys = new ArrayList<>();
}
