# 家庭统计数据范围修复设计

**Issue**: [#31](https://github.com/Samoy/chuan-bill/issues/31) - 家庭统计页面分类统计的饼图无法显示其他家庭成员自定义的类目

## 问题概述

家庭统计页面的所有数据（概览、分类饼图、每日趋势）只显示当前用户的数据，而非全家汇总数据。原因是 SQL 查询中同时过滤了 `user_id` 和 `family_id`。

## 修改范围

仅修改 `chuan-bill-server/src/main/resources/mapper/BillMapper.xml` 中的 3 个查询：

### 1. `selectMonthlyStats`（概览数据）

**修改前**:
```sql
WHERE user_id = #{userId}
    AND time >= #{startTime}
    AND time <= #{endTime}
    AND deleted = 0
    AND type = #{type}
<if test="familyId != null">
    AND family_id = #{familyId}
</if>
```

**修改后**:
```sql
WHERE time >= #{startTime}
    AND time <= #{endTime}
    AND deleted = 0
    AND type = #{type}
<choose>
    <when test="familyId != null">
        AND family_id = #{familyId}
    </when>
    <otherwise>
        AND user_id = #{userId}
    </otherwise>
</choose>
```

### 2. `selectCategoryStats`（分类饼图）

**修改前**:
```sql
WHERE b.user_id = #{userId}
    AND b.time >= #{startTime}
    AND b.time <= #{endTime}
    AND b.deleted = 0
    AND b.type = #{type}
    AND b.category_id IS NOT NULL
<if test="familyId != null">
    AND b.family_id = #{familyId}
</if>
```

**修改后**:
```sql
WHERE b.time >= #{startTime}
    AND b.time <= #{endTime}
    AND b.deleted = 0
    AND b.type = #{type}
    AND b.category_id IS NOT NULL
<choose>
    <when test="familyId != null">
        AND b.family_id = #{familyId}
    </when>
    <otherwise>
        AND b.user_id = #{userId}
    </otherwise>
</choose>
```

### 3. `selectDailyTrend`（每日趋势）

**修改前**:
```sql
WHERE user_id = #{userId}
    AND time >= #{startTime}
    AND time <= #{endTime}
    AND deleted = 0
<if test="familyId != null">
    AND family_id = #{familyId}
</if>
```

**修改后**:
```sql
WHERE time >= #{startTime}
    AND time <= #{endTime}
    AND deleted = 0
<choose>
    <when test="familyId != null">
        AND family_id = #{familyId}
    </when>
    <otherwise>
        AND user_id = #{userId}
    </otherwise>
</choose>
```

## 不需要修改的部分

- `selectMemberStats` — 已正确按 `family_id` 过滤
- `StatisticsServiceImpl.java` — 无需修改，已正确传参
- `StatisticsController.java` — 无需修改
- 前端代码 — 无需修改，已正确传递 `familyId`

## 设计决策

- **使用 `<choose>` 而非 `<if>`**: 确保个人统计和家庭统计互斥，避免同时按两个条件过滤
- **不新增 Mapper 方法**: 复用现有方法，减少代码重复
- **不联表查询家庭成员表**: 直接按 `family_id` 过滤账单表即可，无需额外 JOIN
