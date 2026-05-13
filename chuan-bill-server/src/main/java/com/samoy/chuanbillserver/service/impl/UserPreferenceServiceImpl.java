package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.UserPreferenceMapper;
import com.samoy.chuanbillserver.entity.UserPreference;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 用户偏好设置 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-05-12
 */
@Service
public class UserPreferenceServiceImpl extends ServiceImpl<UserPreferenceMapper, UserPreference>
        implements IUserPreferenceService {

    @Value("${preference.allowed-keys:}")
    private List<String> allowedKeyList;

    private Set<String> getAllowedKeys() {
        return new HashSet<>(allowedKeyList);
    }

    @Override
    public String getValue(String userId, String key) {
        UserPreference pref = this.getOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .eq(UserPreference::getPrefKey, key));
        return pref != null ? pref.getPrefValue() : null;
    }

    @Override
    @Transactional
    public void setValue(String userId, String key, String value) {
        if (!getAllowedKeys().contains(key)) {
            throw new BusinessException(ResultEnum.BAD_REQUEST);
        }
        setValueInternal(userId, key, value);
    }

    @Override
    public void setValueInternal(String userId, String key, String value) {
        UserPreference existing = this.getOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .eq(UserPreference::getPrefKey, key));
        if (existing != null) {
            existing.setPrefValue(value);
            this.updateById(existing);
        } else {
            UserPreference pref = new UserPreference();
            pref.setUserId(userId);
            pref.setPrefKey(key);
            pref.setPrefValue(value);
            this.save(pref);
        }
    }

    @Override
    public Map<String, String> getAll(String userId) {
        List<UserPreference> list =
                this.list(new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));
        Map<String, String> map = new HashMap<>();
        for (UserPreference pref : list) {
            map.put(pref.getPrefKey(), pref.getPrefValue());
        }
        return map;
    }

    @Override
    public void deleteValue(String userId, String key) {
        if (!getAllowedKeys().contains(key)) {
            throw new BusinessException(ResultEnum.BAD_REQUEST);
        }
        this.remove(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .eq(UserPreference::getPrefKey, key));
    }
}
