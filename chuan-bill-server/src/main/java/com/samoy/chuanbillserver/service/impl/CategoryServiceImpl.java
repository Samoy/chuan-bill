package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dao.CategoryMapper;
import com.samoy.chuanbillserver.dto.AddCategoryDTO;
import com.samoy.chuanbillserver.dto.UpdateCategoryDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.entity.Category;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.ICategoryService;
import com.samoy.chuanbillserver.vo.CategoryVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    private BillMapper billMapper;

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

    @Override
    @Transactional
    public CategoryVO addCategory(String userId, AddCategoryDTO dto) {
        LambdaQueryWrapper<Category> maxQuery = new LambdaQueryWrapper<>();
        maxQuery.eq(Category::getType, dto.getType())
                .and(i -> i.eq(Category::getUserId, userId).or(j -> j.eq(Category::getIsDefault, true)));
        List<Category> existing = this.list(maxQuery);
        int maxSortOrder =
                existing.stream().mapToInt(Category::getSortOrder).max().orElse(0);

        Category category = new Category();
        category.setId(IdUtil.fastSimpleUUID());
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setType(dto.getType());
        category.setSortOrder(maxSortOrder + 1);
        category.setIsDefault(false);
        category.setUserId(userId);
        category.setDeleted(false);
        this.save(category);

        return toVO(category);
    }

    @Override
    @Transactional
    public CategoryVO updateCategory(String userId, String id, UpdateCategoryDTO dto) {
        Category category = this.getById(id);
        if (category == null || Boolean.TRUE.equals(category.getDeleted())) {
            throw new BusinessException(ResultEnum.CATEGORY_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(category.getIsDefault())) {
            throw new BusinessException(ResultEnum.CANNOT_MODIFY_DEFAULT_CATEGORY);
        }
        if (!userId.equals(category.getUserId())) {
            throw new BusinessException(ResultEnum.CATEGORY_NOT_FOUND);
        }

        if (dto.getName() != null) {
            category.setName(dto.getName());
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon());
        }
        this.updateById(category);

        return toVO(category);
    }

    @Override
    @Transactional
    public void deleteCategory(String userId, String id) {
        Category category = this.getById(id);
        if (category == null || Boolean.TRUE.equals(category.getDeleted())) {
            throw new BusinessException(ResultEnum.CATEGORY_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(category.getIsDefault())) {
            throw new BusinessException(ResultEnum.CANNOT_MODIFY_DEFAULT_CATEGORY);
        }
        if (!userId.equals(category.getUserId())) {
            throw new BusinessException(ResultEnum.CATEGORY_NOT_FOUND);
        }

        LambdaQueryWrapper<Bill> billQuery = new LambdaQueryWrapper<>();
        billQuery.eq(Bill::getCategoryId, id).last("LIMIT 1");
        if (billMapper.selectCount(billQuery) > 0) {
            throw new BusinessException(ResultEnum.CATEGORY_HAS_BILLS);
        }

        this.removeById(id);
    }

    @Override
    @Transactional
    public void sortCategories(String userId, List<String> ids) {
        int sortOrder = 1;
        for (String id : ids) {
            Category category = this.getById(id);
            if (category != null
                    && userId.equals(category.getUserId())
                    && Boolean.FALSE.equals(category.getIsDefault())) {
                category.setSortOrder(sortOrder++);
                this.updateById(category);
            }
        }
    }

    private CategoryVO toVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        vo.setType(category.getType());
        vo.setSortOrder(category.getSortOrder());
        vo.setIsDefault(category.getIsDefault());
        vo.setUserId(category.getUserId());
        return vo;
    }
}
