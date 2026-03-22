package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.entity.Category;
import com.samoy.chuanbillserver.vo.CategoryVO;
import java.util.List;

/**
 * <p>
 * 类目表 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
public interface ICategoryService extends IService<Category> {
    /**
     * 根据用户ID获取类目列表
     *
     * @param userId 用户ID
     * @param type   账单类型，可选值：income（收入）/expense（支出）
     * @return 类目列表
     */
    List<CategoryVO> getCategoryList(String userId, String type);
}
