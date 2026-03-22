package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.CategoryMapper;
import com.samoy.chuanbillserver.entity.Category;
import com.samoy.chuanbillserver.service.ICategoryService;
import com.samoy.chuanbillserver.vo.CategoryVO;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 类目表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {
    @Override
    public List<CategoryVO> getCategoryList(String userId, String type) {

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(type != null && !type.isEmpty(), Category::getType, type)
                .and(i -> i.eq(Category::getUserId, userId).or(j -> j.eq(Category::getIsDefault, true)))
                .orderByAsc(Category::getType, Category::getSortOrder);

        List<Category> categories = this.list(queryWrapper);

        return categories.stream()
                .map(category -> {
                    CategoryVO vo = new CategoryVO();
                    vo.setId(category.getId());
                    vo.setName(category.getName());
                    vo.setIcon(category.getIcon());
                    vo.setType(category.getType());
                    vo.setSortOrder(category.getSortOrder());
                    vo.setIsDefault(category.getIsDefault());
                    vo.setUserId(category.getUserId());
                    return vo;
                })
                .toList();
    }
}
