package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.entity.UserPreference;
import java.util.Map;

/**
 * <p>
 * 用户偏好设置 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-05-12
 */
public interface IUserPreferenceService extends IService<UserPreference> {

    /**
     * 获取单个偏好值，不存在返回 null
     */
    String getValue(String userId, String key);

    /**
     * 设置单个偏好（INSERT ON DUPLICATE UPDATE），需校验 key 白名单
     */
    void setValue(String userId, String key, String value);

    /**
     * 内部设置偏好（不校验 key，仅供服务内部调用，如定时任务）
     */
    void setValueInternal(String userId, String key, String value);

    /**
     * 获取用户所有偏好，返回 Map
     */
    Map<String, String> getAll(String userId);

    /**
     * 删除某个偏好
     */
    void deleteValue(String userId, String key);
}
