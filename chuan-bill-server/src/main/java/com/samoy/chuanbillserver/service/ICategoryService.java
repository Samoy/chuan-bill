package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.AddCategoryDTO;
import com.samoy.chuanbillserver.dto.UpdateCategoryDTO;
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

    /**
     * 新增自定义类目
     */
    CategoryVO addCategory(String userId, AddCategoryDTO dto);

    /**
     * 更新自定义类目
     */
    CategoryVO updateCategory(String userId, String id, UpdateCategoryDTO dto);

    /**
     * 删除自定义类目
     */
    void deleteCategory(String userId, String id);

    /**
     * 批量更新类目排序
     */
    void sortCategories(String userId, List<String> ids);
}
