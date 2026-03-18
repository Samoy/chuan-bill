package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.CategoryMapper;
import com.samoy.chuanbillserver.entity.Category;
import com.samoy.chuanbillserver.service.ICategoryService;
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
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {}
