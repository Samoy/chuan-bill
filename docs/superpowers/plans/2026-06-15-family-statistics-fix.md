# 家庭统计数据范围修复实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复家庭统计页面只显示当前用户数据的问题，使其显示全家汇总数据

**Architecture:** 修改 `BillMapper.xml` 中 3 个 SQL 查询的 WHERE 条件，使用 MyBatis `<choose>` 动态 SQL 实现：当 `familyId` 存在时按 `family_id` 过滤（全家数据），否则按 `user_id` 过滤（个人数据）

**Tech Stack:** MyBatis XML, Spring Boot, Java 17

---

## File Structure

仅修改 1 个文件：
- `chuan-bill-server/src/main/resources/mapper/BillMapper.xml` — 3 个 SQL 查询的 WHERE 条件

## Task 1: 修改 selectMonthlyStats 查询

**Files:**
- Modify: `chuan-bill-server/src/main/resources/mapper/BillMapper.xml:4-16`

- [ ] **Step 1: 修改 selectMonthlyStats 的 WHERE 条件**

将第 8-16 行的 WHERE 条件从：
```xml
WHERE user_id = #{userId}
    AND time >= #{startTime}
    AND time <= #{endTime}
    AND deleted = 0
    AND type = #{type}
<if test="familyId != null">
    AND family_id = #{familyId}
</if>
```

改为：
```xml
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

- [ ] **Step 2: 提交**

```bash
git add chuan-bill-server/src/main/resources/mapper/BillMapper.xml
git commit -m "fix(statistics): 修复家庭概览统计只显示当前用户数据的问题"
```

## Task 2: 修改 selectCategoryStats 查询

**Files:**
- Modify: `chuan-bill-server/src/main/resources/mapper/BillMapper.xml:18-37`

- [ ] **Step 1: 修改 selectCategoryStats 的 WHERE 条件**

将第 26-34 行的 WHERE 条件从：
```xml
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

改为：
```xml
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

- [ ] **Step 2: 提交**

```bash
git add chuan-bill-server/src/main/resources/mapper/BillMapper.xml
git commit -m "fix(statistics): 修复家庭分类饼图只显示当前用户数据的问题"
```

## Task 3: 修改 selectDailyTrend 查询

**Files:**
- Modify: `chuan-bill-server/src/main/resources/mapper/BillMapper.xml:39-54`

- [ ] **Step 1: 修改 selectDailyTrend 的 WHERE 条件**

将第 45-51 行的 WHERE 条件从：
```xml
WHERE user_id = #{userId}
    AND time >= #{startTime}
    AND time <= #{endTime}
    AND deleted = 0
<if test="familyId != null">
    AND family_id = #{familyId}
</if>
```

改为：
```xml
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

- [ ] **Step 2: 提交**

```bash
git add chuan-bill-server/src/main/resources/mapper/BillMapper.xml
git commit -m "fix(statistics): 修复家庭每日趋势只显示当前用户数据的问题"
```

## Task 4: 验证与合并

- [ ] **Step 1: 启动后端服务验证**

```bash
cd chuan-bill-server && mvn spring-boot:run
```

- [ ] **Step 2: 测试个人统计接口（无 familyId）**

```bash
curl -H "satoken: <token>" "http://localhost:8080/statistics/overview?month=2026-06"
```
预期：返回当前用户的个人统计数据

- [ ] **Step 3: 测试家庭统计接口（有 familyId）**

```bash
curl -H "satoken: <token>" "http://localhost:8080/statistics/overview?month=2026-06&familyId=<familyId>"
```
预期：返回全家汇总数据（包含所有成员）

- [ ] **Step 4: squash 合并提交**

```bash
git reset --soft HEAD~3
git commit -m "fix(statistics): 修复家庭统计只显示当前用户数据的问题

修改 selectMonthlyStats、selectCategoryStats、selectDailyTrend 三个查询的
WHERE 条件，使用 <choose> 动态 SQL：familyId 存在时按 family_id 过滤，
否则按 user_id 过滤。

Fixes #31"
```
