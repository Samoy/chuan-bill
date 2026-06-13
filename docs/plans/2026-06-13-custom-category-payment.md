# 自定义账单类目和支付方式 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将添加/编辑账单时的类目和支付方式选择器从 wd-picker 升级为 wd-action-sheet + 胶囊网格选择器，支持用户自定义类目和支付方式的增删改及拖拽排序。

**Architecture:** 后端在 BillController 中新增 8 个 CRUD + 排序端点，复用已有的 Category/PaymentMethod 实体和 Mapper；前端创建 GridPickerPopup 可复用组件封装 ActionSheet + 胶囊网格 + 编辑模式，通过 billStore 中的 CRUD 方法与后端交互。

**Tech Stack:** Spring Boot 3 / MyBatis-Plus / Sa-Token (backend), Vue 3 / uni-app / wot-design-uni / Alova.js / Pinia (frontend)

**Spec:** `docs/specs/2026-06-13-custom-category-payment-design.md`

---

## File Structure

### Backend (chuan-bill-server)

| File | Action | Responsibility |
|------|--------|---------------|
| `src/main/java/com/samoy/chuanbillserver/dto/AddCategoryDTO.java` | Create | 新增类目请求体 |
| `src/main/java/com/samoy/chuanbillserver/dto/UpdateCategoryDTO.java` | Create | 更新类目请求体 |
| `src/main/java/com/samoy/chuanbillserver/dto/AddPaymentMethodDTO.java` | Create | 新增支付方式请求体 |
| `src/main/java/com/samoy/chuanbillserver/dto/UpdatePaymentMethodDTO.java` | Create | 更新支付方式请求体 |
| `src/main/java/com/samoy/chuanbillserver/dto/SortDTO.java` | Create | 排序请求体 |
| `src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java` | Modify | 新增类目/支付方式错误码 (6000+) |
| `src/main/java/com/samoy/chuanbillserver/service/ICategoryService.java` | Modify | 新增 addCategory, updateCategory, deleteCategory, sortCategories 方法签名 |
| `src/main/java/com/samoy/chuanbillserver/service/impl/CategoryServiceImpl.java` | Modify | 实现 CRUD + 排序方法 |
| `src/main/java/com/samoy/chuanbillserver/service/IPaymentMethodService.java` | Modify | 新增 addPaymentMethod, updatePaymentMethod, deletePaymentMethod, sortPaymentMethods 方法签名 |
| `src/main/java/com/samoy/chuanbillserver/service/impl/PaymentMethodServiceImpl.java` | Modify | 实现 CRUD + 排序方法 |
| `src/main/java/com/samoy/chuanbillserver/controller/BillController.java` | Modify | 新增 8 个端点 |

### Frontend (chuan-bill-app)

| File | Action | Responsibility |
|------|--------|---------------|
| `src/api/apiDefinitions.ts` | Modify | 新增 8 个 API 端点定义 |
| `src/api/globals.d.ts` | Modify | alova-gen 后修复类型定义 |
| `src/store/billStore.ts` | Modify | 新增 CRUD 方法 + filter getter |
| `src/constant/icons.ts` | Create | UnoCSS 图标常量列表 |
| `src/pages/bill/components/GridPickerPopup.vue` | Create | 核心可复用组件 |
| `src/pages/bill/components/ManualEdit.vue` | Modify | 替换 wd-picker 为 GridPickerPopup |
| `src/pages/bill/components/FilterModal.vue` | Modify | 使用 store 的 filter getter |

---

### Task 1: Backend - DTO 和错误码

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/AddCategoryDTO.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UpdateCategoryDTO.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/AddPaymentMethodDTO.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UpdatePaymentMethodDTO.java`
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/SortDTO.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java`

- [ ] **Step 1: 创建 AddCategoryDTO.java**

```java
package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCategoryDTO {

    @NotBlank(message = "类目名称不能为空")
    private String name;

    @NotBlank(message = "类目图标不能为空")
    private String icon;

    @NotBlank(message = "类目类型不能为空")
    private String type;
}
```

- [ ] **Step 2: 创建 UpdateCategoryDTO.java**

```java
package com.samoy.chuanbillserver.dto;

import lombok.Data;

@Data
public class UpdateCategoryDTO {

    private String name;

    private String icon;
}
```

- [ ] **Step 3: 创建 AddPaymentMethodDTO.java**

```java
package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddPaymentMethodDTO {

    @NotBlank(message = "支付方式名称不能为空")
    private String name;

    @NotBlank(message = "支付方式图标不能为空")
    private String icon;
}
```

- [ ] **Step 4: 创建 UpdatePaymentMethodDTO.java**

```java
package com.samoy.chuanbillserver.dto;

import lombok.Data;

@Data
public class UpdatePaymentMethodDTO {

    private String name;

    private String icon;
}
```

- [ ] **Step 5: 创建 SortDTO.java**

```java
package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SortDTO {

    @NotEmpty(message = "排序列表不能为空")
    private List<String> ids;
}
```

- [ ] **Step 6: 在 ResultEnum.java 中添加错误码**

在 `BUDGET_NOT_FOUND(5001, "预算不存在"),` 之后、`;` 之前添加：

```java
    // 类目/支付方式相关错误码 6000+
    CATEGORY_NOT_FOUND(6001, "类目不存在"),
    CATEGORY_HAS_BILLS(6002, "该类目下存在账单，无法删除"),
    CANNOT_MODIFY_DEFAULT_CATEGORY(6003, "系统预设类目不可修改"),
    PAYMENT_METHOD_NOT_FOUND(6004, "支付方式不存在"),
    PAYMENT_METHOD_HAS_BILLS(6005, "该支付方式下存在账单，无法删除"),
    CANNOT_MODIFY_DEFAULT_PAYMENT_METHOD(6006, "系统预设支付方式不可修改"),
```

- [ ] **Step 7: 验证编译**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/AddCategoryDTO.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UpdateCategoryDTO.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/AddPaymentMethodDTO.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/UpdatePaymentMethodDTO.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/SortDTO.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java
git commit -m "feat(category): 添加类目和支付方式 CRUD 的 DTO 和错误码"
```

---

### Task 2: Backend - 类目 Service CRUD

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/ICategoryService.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/CategoryServiceImpl.java`

- [ ] **Step 1: 在 ICategoryService.java 中添加方法签名**

在 `List<CategoryVO> getCategoryList(String userId, String type);` 之后添加：

```java
    /**
     * 新增自定义类目
     *
     * @param userId 用户ID
     * @param dto    新增类目信息
     * @return 新增的类目VO
     */
    CategoryVO addCategory(String userId, AddCategoryDTO dto);

    /**
     * 更新自定义类目
     *
     * @param userId 用户ID
     * @param id     类目ID
     * @param dto    更新信息
     * @return 更新后的类目VO
     */
    CategoryVO updateCategory(String userId, String id, UpdateCategoryDTO dto);

    /**
     * 删除自定义类目
     *
     * @param userId 用户ID
     * @param id     类目ID
     */
    void deleteCategory(String userId, String id);

    /**
     * 批量更新类目排序
     *
     * @param userId 用户ID
     * @param ids    排序后的ID列表
     */
    void sortCategories(String userId, List<String> ids);
```

添加 import: `import com.samoy.chuanbillserver.dto.AddCategoryDTO;` `import com.samoy.chuanbillserver.dto.UpdateCategoryDTO;`

- [ ] **Step 2: 在 CategoryServiceImpl.java 中实现 CRUD 方法**

在 `getCategoryList` 方法之后添加以下方法：

```java
    @Override
    @Transactional
    public CategoryVO addCategory(String userId, AddCategoryDTO dto) {
        // 计算新排序值：用户自定义类目的最大 sortOrder + 1
        LambdaQueryWrapper<Category> maxQuery = new LambdaQueryWrapper<>();
        maxQuery.eq(Category::getUserId, userId)
                .eq(Category::getType, dto.getType());
        List<Category> existing = this.list(maxQuery);
        int maxSortOrder = existing.stream()
                .mapToInt(Category::getSortOrder)
                .max()
                .orElse(0);

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

        // 检查是否有关联账单
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
            if (category != null && userId.equals(category.getUserId())
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
```

- [ ] **Step 3: 添加必要的 import 和依赖注入**

在 CategoryServiceImpl.java 的 import 区域添加：

```java
import cn.hutool.core.util.IdUtil;
import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dto.AddCategoryDTO;
import com.samoy.chuanbillserver.dto.UpdateCategoryDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
```

在类内部添加字段注入：

```java
    @Resource
    private BillMapper billMapper;
```

- [ ] **Step 4: 验证编译**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/ICategoryService.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/CategoryServiceImpl.java
git commit -m "feat(category): 实现类目 CRUD 和排序服务方法"
```

---

### Task 3: Backend - 支付方式 Service CRUD

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IPaymentMethodService.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/PaymentMethodServiceImpl.java`

- [ ] **Step 1: 在 IPaymentMethodService.java 中添加方法签名**

在 `List<PaymentMethodVO> getPaymentMethods(String userId);` 之后添加：

```java
    /**
     * 新增自定义支付方式
     *
     * @param userId 用户ID
     * @param dto    新增支付方式信息
     * @return 新增的支付方式VO
     */
    PaymentMethodVO addPaymentMethod(String userId, AddPaymentMethodDTO dto);

    /**
     * 更新自定义支付方式
     *
     * @param userId 用户ID
     * @param id     支付方式ID
     * @param dto    更新信息
     * @return 更新后的支付方式VO
     */
    PaymentMethodVO updatePaymentMethod(String userId, String id, UpdatePaymentMethodDTO dto);

    /**
     * 删除自定义支付方式
     *
     * @param userId 用户ID
     * @param id     支付方式ID
     */
    void deletePaymentMethod(String userId, String id);

    /**
     * 批量更新支付方式排序
     *
     * @param userId 用户ID
     * @param ids    排序后的ID列表
     */
    void sortPaymentMethods(String userId, List<String> ids);
```

添加 import: `import com.samoy.chuanbillserver.dto.AddPaymentMethodDTO;` `import com.samoy.chuanbillserver.dto.UpdatePaymentMethodDTO;`

- [ ] **Step 2: 在 PaymentMethodServiceImpl.java 中实现 CRUD 方法**

在 `getPaymentMethods` 方法之后添加以下方法：

```java
    @Override
    @Transactional
    public PaymentMethodVO addPaymentMethod(String userId, AddPaymentMethodDTO dto) {
        // 计算新排序值：用户自定义支付方式的最大 sortOrder + 1
        LambdaQueryWrapper<PaymentMethod> maxQuery = new LambdaQueryWrapper<>();
        maxQuery.eq(PaymentMethod::getUserId, userId);
        List<PaymentMethod> existing = this.list(maxQuery);
        int maxSortOrder = existing.stream()
                .mapToInt(PaymentMethod::getSortOrder)
                .max()
                .orElse(0);

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(IdUtil.fastSimpleUUID());
        paymentMethod.setName(dto.getName());
        paymentMethod.setIcon(dto.getIcon());
        paymentMethod.setSortOrder(maxSortOrder + 1);
        paymentMethod.setIsDefault(false);
        paymentMethod.setUserId(userId);
        paymentMethod.setDeleted(false);
        this.save(paymentMethod);

        return toVO(paymentMethod);
    }

    @Override
    @Transactional
    public PaymentMethodVO updatePaymentMethod(String userId, String id, UpdatePaymentMethodDTO dto) {
        PaymentMethod paymentMethod = this.getById(id);
        if (paymentMethod == null || Boolean.TRUE.equals(paymentMethod.getDeleted())) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(paymentMethod.getIsDefault())) {
            throw new BusinessException(ResultEnum.CANNOT_MODIFY_DEFAULT_PAYMENT_METHOD);
        }
        if (!userId.equals(paymentMethod.getUserId())) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_NOT_FOUND);
        }

        if (dto.getName() != null) {
            paymentMethod.setName(dto.getName());
        }
        if (dto.getIcon() != null) {
            paymentMethod.setIcon(dto.getIcon());
        }
        this.updateById(paymentMethod);

        return toVO(paymentMethod);
    }

    @Override
    @Transactional
    public void deletePaymentMethod(String userId, String id) {
        PaymentMethod paymentMethod = this.getById(id);
        if (paymentMethod == null || Boolean.TRUE.equals(paymentMethod.getDeleted())) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(paymentMethod.getIsDefault())) {
            throw new BusinessException(ResultEnum.CANNOT_MODIFY_DEFAULT_PAYMENT_METHOD);
        }
        if (!userId.equals(paymentMethod.getUserId())) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_NOT_FOUND);
        }

        // 检查是否有关联账单
        LambdaQueryWrapper<Bill> billQuery = new LambdaQueryWrapper<>();
        billQuery.eq(Bill::getPaymentMethodId, id).last("LIMIT 1");
        if (billMapper.selectCount(billQuery) > 0) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_HAS_BILLS);
        }

        this.removeById(id);
    }

    @Override
    @Transactional
    public void sortPaymentMethods(String userId, List<String> ids) {
        int sortOrder = 1;
        for (String id : ids) {
            PaymentMethod paymentMethod = this.getById(id);
            if (paymentMethod != null && userId.equals(paymentMethod.getUserId())
                    && Boolean.FALSE.equals(paymentMethod.getIsDefault())) {
                paymentMethod.setSortOrder(sortOrder++);
                this.updateById(paymentMethod);
            }
        }
    }

    private PaymentMethodVO toVO(PaymentMethod paymentMethod) {
        PaymentMethodVO vo = new PaymentMethodVO();
        vo.setId(paymentMethod.getId());
        vo.setName(paymentMethod.getName());
        vo.setIcon(paymentMethod.getIcon());
        vo.setSortOrder(paymentMethod.getSortOrder());
        vo.setIsDefault(paymentMethod.getIsDefault());
        vo.setUserId(paymentMethod.getUserId());
        return vo;
    }
```

- [ ] **Step 3: 添加必要的 import 和依赖注入**

在 PaymentMethodServiceImpl.java 的 import 区域添加：

```java
import cn.hutool.core.util.IdUtil;
import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dto.AddPaymentMethodDTO;
import com.samoy.chuanbillserver.dto.UpdatePaymentMethodDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
```

在类内部添加字段注入：

```java
    @Resource
    private BillMapper billMapper;
```

- [ ] **Step 4: 验证编译**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IPaymentMethodService.java \
  chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/PaymentMethodServiceImpl.java
git commit -m "feat(payment): 实现支付方式 CRUD 和排序服务方法"
```

---

### Task 4: Backend - Controller 端点

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java`

- [ ] **Step 1: 在 BillController.java 中添加类目 CRUD 端点**

在 `getCategories` 方法之后、`getPaymentMethods` 方法之前添加：

```java
    @PostMapping("/categories")
    @Operation(summary = "新增自定义类目", description = "用户新增自定义类目")
    public Result<CategoryVO> addCategory(@Validated @RequestBody AddCategoryDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(categoryService.addCategory(userId, dto));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "更新自定义类目", description = "用户更新自定义类目名称和图标")
    public Result<CategoryVO> updateCategory(
            @Parameter(description = "类目 ID", required = true) @PathVariable String id,
            @Validated @RequestBody UpdateCategoryDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(categoryService.updateCategory(userId, id, dto));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "删除自定义类目", description = "用户删除自定义类目，有关联账单时不可删除")
    public Result<Boolean> deleteCategory(
            @Parameter(description = "类目 ID", required = true) @PathVariable String id) {
        String userId = StpUtil.getLoginIdAsString();
        categoryService.deleteCategory(userId, id);
        return Result.success(true);
    }

    @PutMapping("/categories/sort")
    @Operation(summary = "批量更新类目排序", description = "用户批量更新自定义类目的排序")
    public Result<Boolean> sortCategories(@Validated @RequestBody SortDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        categoryService.sortCategories(userId, dto.getIds());
        return Result.success(true);
    }
```

- [ ] **Step 2: 在 BillController.java 中添加支付方式 CRUD 端点**

在 `getPaymentMethods` 方法之后、`getMonthlyStats` 方法之前添加：

```java
    @PostMapping("/payment-methods")
    @Operation(summary = "新增自定义支付方式", description = "用户新增自定义支付方式")
    public Result<PaymentMethodVO> addPaymentMethod(@Validated @RequestBody AddPaymentMethodDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(paymentMethodService.addPaymentMethod(userId, dto));
    }

    @PutMapping("/payment-methods/{id}")
    @Operation(summary = "更新自定义支付方式", description = "用户更新自定义支付方式名称和图标")
    public Result<PaymentMethodVO> updatePaymentMethod(
            @Parameter(description = "支付方式 ID", required = true) @PathVariable String id,
            @Validated @RequestBody UpdatePaymentMethodDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(paymentMethodService.updatePaymentMethod(userId, id, dto));
    }

    @DeleteMapping("/payment-methods/{id}")
    @Operation(summary = "删除自定义支付方式", description = "用户删除自定义支付方式，有关联账单时不可删除")
    public Result<Boolean> deletePaymentMethod(
            @Parameter(description = "支付方式 ID", required = true) @PathVariable String id) {
        String userId = StpUtil.getLoginIdAsString();
        paymentMethodService.deletePaymentMethod(userId, id);
        return Result.success(true);
    }

    @PutMapping("/payment-methods/sort")
    @Operation(summary = "批量更新支付方式排序", description = "用户批量更新自定义支付方式的排序")
    public Result<Boolean> sortPaymentMethods(@Validated @RequestBody SortDTO dto) {
        String userId = StpUtil.getLoginIdAsString();
        paymentMethodService.sortPaymentMethods(userId, dto.getIds());
        return Result.success(true);
    }
```

- [ ] **Step 3: 添加 import**

在 BillController.java 的 import 区域添加：

```java
import com.samoy.chuanbillserver.dto.AddCategoryDTO;
import com.samoy.chuanbillserver.dto.AddPaymentMethodDTO;
import com.samoy.chuanbillserver.dto.SortDTO;
import com.samoy.chuanbillserver.dto.UpdateCategoryDTO;
import com.samoy.chuanbillserver.dto.UpdatePaymentMethodDTO;
```

- [ ] **Step 4: 验证编译**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java
git commit -m "feat(bill): 添加类目和支付方式的 CRUD 和排序端点"
```

---

### Task 5: Frontend - API 定义和类型

**Files:**
- Modify: `chuan-bill-app/src/api/apiDefinitions.ts`
- Modify: `chuan-bill-app/src/api/globals.d.ts`

- [ ] **Step 1: 在 apiDefinitions.ts 中添加新端点**

在 `'bill.getCategories': ['GET', '/bill/categories'],` 之后添加：

```
  'bill.addCategory': ['POST', '/bill/categories'],
  'bill.updateCategory': ['PUT', '/bill/categories/{id}'],
  'bill.deleteCategory': ['DELETE', '/bill/categories/{id}'],
  'bill.sortCategories': ['PUT', '/bill/categories/sort'],
```

在 `'bill.getPaymentMethods': ['GET', '/bill/payment-methods'],` 之后添加：

```
  'bill.addPaymentMethod': ['POST', '/bill/payment-methods'],
  'bill.updatePaymentMethod': ['PUT', '/bill/payment-methods/{id}'],
  'bill.deletePaymentMethod': ['DELETE', '/bill/payment-methods/{id}'],
  'bill.sortPaymentMethods': ['PUT', '/bill/payment-methods/sort'],
```

- [ ] **Step 2: 运行 alova-gen 生成类型**

Run: `cd chuan-bill-app && pnpm alova-gen`
Expected: globals.d.ts 更新

- [ ] **Step 3: 修复 alova-gen 生成的类型定义**

运行 alova-gen 后，检查 `globals.d.ts` 中新生成的类型。根据项目的 `alova-api-fix` skill 约定，需要检查：
1. amount/money 字段是否为 string 类型（后端 @JsonFormat 注解）
2. DTO 字段是否在 params 中重复展开
3. 二进制响应类型是否正确

如果新生成的 CRUD 端点类型有上述问题，手动修复。

- [ ] **Step 4: 提交**

```bash
git add chuan-bill-app/src/api/apiDefinitions.ts chuan-bill-app/src/api/globals.d.ts
git commit -m "feat(api): 添加类目和支付方式 CRUD 的前端 API 定义"
```

---

### Task 6: Frontend - billStore CRUD 方法

**Files:**
- Modify: `chuan-bill-app/src/store/billStore.ts`

- [ ] **Step 1: 在 billStore.ts 中添加 CRUD 方法**

在 `getPaymentMethodList` 函数之后、`addLocalBill` 函数之前添加以下方法：

```ts
  // ==================== 类目 CRUD ====================

  async function addCategory(data: { name: string, icon: string, type: string }) {
    const res = await Apis.bill.addCategory({ data })
    if (res.success) {
      await fetchCategoryList()
    }
    return res
  }

  async function updateCategory(id: string, data: { name?: string, icon?: string }) {
    const res = await Apis.bill.updateCategory({ pathParams: { id }, data })
    if (res.success) {
      await fetchCategoryList()
    }
    return res
  }

  async function deleteCategory(id: string) {
    const res = await Apis.bill.deleteCategory({ pathParams: { id } })
    if (res.success) {
      await fetchCategoryList()
    }
    return res
  }

  async function sortCategories(ids: string[]) {
    const res = await Apis.bill.sortCategories({ data: { ids } })
    if (res.success) {
      await fetchCategoryList()
    }
    return res
  }

  // ==================== 支付方式 CRUD ====================

  async function addPaymentMethod(data: { name: string, icon: string }) {
    const res = await Apis.bill.addPaymentMethod({ data })
    if (res.success) {
      await fetchPaymentMethodList()
    }
    return res
  }

  async function updatePaymentMethod(id: string, data: { name?: string, icon?: string }) {
    const res = await Apis.bill.updatePaymentMethod({ pathParams: { id }, data })
    if (res.success) {
      await fetchPaymentMethodList()
    }
    return res
  }

  async function deletePaymentMethod(id: string) {
    const res = await Apis.bill.deletePaymentMethod({ pathParams: { id } })
    if (res.success) {
      await fetchPaymentMethodList()
    }
    return res
  }

  async function sortPaymentMethods(ids: string[]) {
    const res = await Apis.bill.sortPaymentMethods({ data: { ids } })
    if (res.success) {
      await fetchPaymentMethodList()
    }
    return res
  }

  // ==================== Filter 排序 Getter ====================

  function isOtherItem(item: { name?: string, isDefault?: boolean }) {
    return item.isDefault && item.name?.includes('其他')
  }

  function getCategoryListForFilter(type?: string) {
    const list = getCategoryList(type)
    const others = list.filter(item => isOtherItem(item))
    const nonOthers = list.filter(item => !isOtherItem(item))
    return [...nonOthers, ...others]
  }

  function getPaymentMethodListForFilter() {
    const list = paymentMethodList.value
    const others = list.filter(item => isOtherItem(item))
    const nonOthers = list.filter(item => !isOtherItem(item))
    return [...nonOthers, ...others]
  }
```

- [ ] **Step 2: 在 return 语句中导出新方法**

在 `return {` 块中添加新方法：

```ts
    // 类目 CRUD
    addCategory,
    updateCategory,
    deleteCategory,
    sortCategories,
    // 支付方式 CRUD
    addPaymentMethod,
    updatePaymentMethod,
    deletePaymentMethod,
    sortPaymentMethods,
    // Filter getter
    getCategoryListForFilter,
    getPaymentMethodListForFilter,
```

- [ ] **Step 3: 验证类型检查**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 无类型错误（或仅有已存在的无关错误）

- [ ] **Step 4: 提交**

```bash
git add chuan-bill-app/src/store/billStore.ts
git commit -m "feat(store): 添加类目和支付方式的 CRUD 方法及 filter 排序 getter"
```

---

### Task 7: Frontend - 图标常量

**Files:**
- Create: `chuan-bill-app/src/constant/icons.ts`

- [ ] **Step 1: 创建图标常量文件**

```ts
/**
 * UnoCSS 图标常量列表
 * 用于自定义类目/支付方式的图标选择器
 * 使用 i-lucide:* 图标体系
 */
export const ICON_LIST = [
  // 餐饮美食
  'i-lucide:utensils',
  'i-lucide:coffee',
  'i-lucide:beer',
  'i-lucide:cake',
  'i-lucide:egg',
  'i-lucide:apple',
  'i-lucide:cherry',
  'i-lucide:grape',
  'i-lucide:pizza',
  'i-lucide:soup',
  // 购物消费
  'i-lucide:shopping-bag',
  'i-lucide:shopping-cart',
  'i-lucide:gift',
  'i-lucide:tag',
  'i-lucide:receipt',
  'i-lucide:store',
  'i-lucide:package',
  // 交通出行
  'i-lucide:car',
  'i-lucide:bus',
  'i-lucide:train-front',
  'i-lucide:plane',
  'i-lucide:bike',
  'i-lucide:ship',
  'i-lucide:taxi',
  'i-lucide:fuel',
  // 居住生活
  'i-lucide:house',
  'i-lucide:lamp',
  'i-lucide:sofa',
  'i-lucide:bath',
  'i-lucide:bed',
  'i-lucide:washer',
  'i-lucide:refrigerator',
  // 通讯数码
  'i-lucide:smartphone',
  'i-lucide:laptop',
  'i-lucide:monitor',
  'i-lucide:headphones',
  'i-lucide:camera',
  'i-lucide:wifi',
  'i-lucide:bluetooth',
  // 医疗健康
  'i-lucide:hospital',
  'i-lucide:pill',
  'i-lucide:heart-pulse',
  'i-lucide:stethoscope',
  'i-lucide:thermometer',
  // 教育学习
  'i-lucide:book-open',
  'i-lucide:graduation-cap',
  'i-lucide:pen',
  'i-lucide:pencil',
  'i-lucide:calculator',
  // 娱乐休闲
  'i-lucide:music',
  'i-lucide:gamepad-2',
  'i-lucide:film',
  'i-lucide:ticket',
  'i-lucide:dice-5',
  'i-lucide:tent',
  // 运动健身
  'i-lucide:dumbbell',
  'i-lucide:trophy',
  'i-lucide:medal',
  'i-lucide:bike',
  // 工作收入
  'i-lucide:briefcase',
  'i-lucide:landmark',
  'i-lucide:credit-card',
  'i-lucide:wallet',
  'i-lucide:banknote',
  'i-lucide:coins',
  'i-lucide:piggy-bank',
  'i-lucide:trending-up',
  // 其他通用
  'i-lucide:star',
  'i-lucide:heart',
  'i-lucide:home',
  'i-lucide:user',
  'i-lucide:users',
  'i-lucide:baby',
  'i-lucide:paw-print',
  'i-lucide:leaf',
  'i-lucide:sun',
  'i-lucide:moon',
  'i-lucide:zap',
  'i-lucide:flame',
  'i-lucide:droplets',
  'i-lucide:scissors',
  'i-lucide:wrench',
  'i-lucide:hammer',
  'i-lucide:phone',
  'i-lucide:mail',
  'i-lucide:globe',
  'i-lucide:compass',
  'i-lucide:map-pin',
] as const

export type IconClass = (typeof ICON_LIST)[number]
```

- [ ] **Step 2: 提交**

```bash
git add chuan-bill-app/src/constant/icons.ts
git commit -m "feat(constant): 添加 UnoCSS 图标常量列表"
```

---

### Task 8: Frontend - GridPickerPopup 组件

**Files:**
- Create: `chuan-bill-app/src/pages/bill/components/GridPickerPopup.vue`

- [ ] **Step 1: 创建 GridPickerPopup.vue 组件**

```vue
<script setup lang="ts">
import type { CategoryVO, PaymentMethodVO } from '@/api/globals'
import { ICON_LIST } from '@/constant/icons'
import { GlobalMessage, GlobalToast } from '@/utils'

defineOptions({
  name: 'GridPickerPopup',
  options: {
    virtualHost: true,
    styleIsolation: 'shared',
  },
})

type GridPickerItem = CategoryVO | PaymentMethodVO

const props = withDefaults(defineProps<{
  modelValue?: string
  title: string
  items: GridPickerItem[]
  type?: 'expense' | 'income'
  entity: 'category' | 'paymentMethod'
  showOthers?: boolean
}>(), {
  showOthers: false,
  modelValue: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'change': [value: string]
  'itemsUpdated': []
}>()

const billStore = useBillStore()
const user = useUserStore()

const visible = defineModel<boolean>('visible', { default: false })
const editMode = ref(false)
const showForm = ref(false)
const editingItem = ref<GridPickerItem | null>(null)
const formName = ref('')
const formIcon = ref('')
const saving = ref(false)

// 过滤后的列表（隐藏"其他"）
const filteredItems = computed(() => {
  if (props.showOthers) {
    return props.items
  }
  return props.items.filter(item => !(item.isDefault && item.name?.includes('其他')))
})

// 分组：预设 + 自定义
const presetItems = computed(() => filteredItems.value.filter(item => item.isDefault))
const customItems = computed(() => filteredItems.value.filter(item => !item.isDefault))

// 显示文本
const displayText = computed(() => {
  const selected = props.items.find(item => item.id === props.modelValue)
  return selected?.name || '请选择'
})

function handleSelect(id: string) {
  if (editMode.value) return
  emit('update:modelValue', id)
  emit('change', id)
  visible.value = false
}

function enterEditMode() {
  editMode.value = true
}

function exitEditMode() {
  editMode.value = false
}

function openAddForm() {
  editingItem.value = null
  formName.value = ''
  formIcon.value = ''
  showForm.value = true
}

function openEditForm(item: GridPickerItem) {
  editingItem.value = item
  formName.value = item.name || ''
  formIcon.value = item.icon || ''
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingItem.value = null
  formName.value = ''
  formIcon.value = ''
}

async function handleSave() {
  if (!formName.value.trim()) {
    GlobalToast('请输入名称')
    return
  }
  if (!formIcon.value) {
    GlobalToast('请选择图标')
    return
  }

  saving.value = true
  try {
    if (props.entity === 'category') {
      if (editingItem.value) {
        await billStore.updateCategory(editingItem.value.id!, {
          name: formName.value.trim(),
          icon: formIcon.value,
        })
      } else {
        await billStore.addCategory({
          name: formName.value.trim(),
          icon: formIcon.value,
          type: props.type || 'expense',
        })
      }
    } else {
      if (editingItem.value) {
        await billStore.updatePaymentMethod(editingItem.value.id!, {
          name: formName.value.trim(),
          icon: formIcon.value,
        })
      } else {
        await billStore.addPaymentMethod({
          name: formName.value.trim(),
          icon: formIcon.value,
        })
      }
    }
    emit('itemsUpdated')
    closeForm()
    GlobalToast(editingItem.value ? '修改成功' : '添加成功')
  } catch (error: any) {
    GlobalMessage(error.message || '操作失败', 'error')
  } finally {
    saving.value = false
  }
}

async function handleDelete(item: GridPickerItem) {
  try {
    if (props.entity === 'category') {
      await billStore.deleteCategory(item.id!)
    } else {
      await billStore.deletePaymentMethod(item.id!)
    }
    // 如果删除的是当前选中项，清除选中
    if (props.modelValue === item.id) {
      emit('update:modelValue', '')
      emit('change', '')
    }
    emit('itemsUpdated')
    GlobalToast('删除成功')
  } catch (error: any) {
    GlobalMessage(error.message || '删除失败', 'error')
  }
}

function confirmDelete(item: GridPickerItem) {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除"${item.name}"吗？`,
    success: (res) => {
      if (res.confirm) {
        handleDelete(item)
      }
    },
  })
}

function handleClose() {
  if (editMode.value) {
    editMode.value = false
  }
  visible.value = false
}

// 获取选中项的图标
const selectedIcon = computed(() => {
  const selected = props.items.find(item => item.id === props.modelValue)
  return selected?.icon || ''
})
</script>

<template>
  <!-- 触发器 -->
  <view
    class="flex items-center justify-between rounded-xl bg-gray-50/80 px-3 py-2 dark:bg-black/30"
    @click="visible = true"
  >
    <view class="flex items-center gap-2 overflow-hidden">
      <text v-if="selectedIcon" :class="selectedIcon" class="text-lg" />
      <text class="truncate text-sm" :class="modelValue ? 'text-gray-800 dark:text-gray-200' : 'text-gray-400'">
        {{ displayText }}
      </text>
    </view>
    <text class="i-lucide:chevron-right text-gray-400" />
  </view>

  <!-- ActionSheet -->
  <wd-action-sheet
    v-model="visible"
    :title="showForm ? (editingItem ? `编辑${title.replace('选择', '')}` : `新增${title.replace('选择', '')}`) : (editMode ? `编辑${title.replace('选择', '')}` : title)"
    position="bottom"
    :closable="!showForm"
    safe-area-inset-bottom
    custom-class="rounded-tl-2xl rounded-tr-2xl"
    :z-index="999"
    @close="handleClose"
  >
    <view class="min-h-[40vh] px-4 pb-4">
      <!-- 网格视图 -->
      <view v-if="!showForm">
        <!-- 标题栏操作按钮 -->
        <view v-if="user.isLoggedIn" class="mb-3 flex items-center justify-end gap-3">
          <view
            v-if="!editMode"
            class="flex items-center gap-1 text-sm text-primary"
            @click="enterEditMode"
          >
            <text class="i-lucide:pencil" />
            <text>编辑</text>
          </view>
          <view
            v-else
            class="flex items-center gap-1 text-sm text-primary"
            @click="exitEditMode"
          >
            <text class="i-lucide:check" />
            <text>完成</text>
          </view>
        </view>

        <!-- 预设项 -->
        <view v-if="presetItems.length > 0" class="grid grid-cols-4 gap-3">
          <view
            v-for="item in presetItems"
            :key="item.id"
            class="flex flex-col items-center justify-center rounded-xl py-3 transition-colors"
            :class="[
              modelValue === item.id && !editMode ? 'bg-primary/10 text-primary' : 'bg-gray-50 dark:bg-white/5',
            ]"
            @click="handleSelect(item.id!)"
          >
            <view class="relative">
              <text :class="item.icon" class="text-2xl" />
              <text
                v-if="editMode"
                class="i-lucide:lock absolute -right-1 -top-1 text-xs text-gray-400"
              />
            </view>
            <text class="mt-1 max-w-full truncate text-xs">{{ item.name }}</text>
          </view>
        </view>

        <!-- 自定义项分隔线 -->
        <view v-if="customItems.length > 0 || (user.isLoggedIn && !editMode)" class="my-3 flex items-center gap-2">
          <view class="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
          <text class="text-xs text-gray-400">自定义{{ title.replace('选择', '') }}</text>
          <view class="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
        </view>

        <!-- 自定义项 -->
        <view class="grid grid-cols-4 gap-3">
          <view
            v-for="item in customItems"
            :key="item.id"
            class="flex flex-col items-center justify-center rounded-xl py-3 transition-colors"
            :class="[
              modelValue === item.id && !editMode ? 'bg-primary/10 text-primary' : 'bg-gray-50 dark:bg-white/5',
            ]"
            @click="editMode ? undefined : handleSelect(item.id!)"
          >
            <view class="relative">
              <text :class="item.icon" class="text-2xl" />
              <!-- 编辑/删除按钮 -->
              <view v-if="editMode" class="absolute -right-2 -top-2 flex gap-0.5">
                <view
                  class="flex h-4 w-4 items-center justify-center rounded-full bg-blue-500"
                  @click.stop="openEditForm(item)"
                >
                  <text class="i-lucide:pencil text-[10px] text-white" />
                </view>
                <view
                  class="flex h-4 w-4 items-center justify-center rounded-full bg-red-500"
                  @click.stop="confirmDelete(item)"
                >
                  <text class="i-lucide:x text-[10px] text-white" />
                </view>
              </view>
            </view>
            <text class="mt-1 max-w-full truncate text-xs">{{ item.name }}</text>
          </view>

          <!-- 新增按钮 -->
          <view
            v-if="user.isLoggedIn"
            class="flex flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 py-3 dark:border-gray-600"
            @click="openAddForm"
          >
            <text class="i-lucide:plus text-2xl text-gray-400" />
            <text class="mt-1 text-xs text-gray-400">新增</text>
          </view>
        </view>

        <!-- 未登录提示 -->
        <view v-if="!user.isLoggedIn" class="mt-4 text-center text-xs text-gray-400">
          登录后可自定义{{ title.replace('选择', '') }}
        </view>
      </view>

      <!-- 表单视图 -->
      <view v-else>
        <!-- 名称输入 -->
        <view class="mb-4">
          <text class="mb-2 block text-sm text-gray-600 dark:text-gray-400">名称</text>
          <wd-input
            v-model="formName"
            :maxlength="8"
            placeholder="请输入名称（最多8个字）"
            show-word-limit
            no-border
            custom-class="bg-gray-50 rounded-xl dark:bg-black/30"
          />
        </view>

        <!-- 图标选择 -->
        <view class="mb-4">
          <text class="mb-2 block text-sm text-gray-600 dark:text-gray-400">图标</text>
          <scroll-view scroll-y class="h-[30vh]">
            <view class="grid grid-cols-6 gap-3">
              <view
                v-for="icon in ICON_LIST"
                :key="icon"
                class="flex h-10 w-10 items-center justify-center rounded-lg transition-colors"
                :class="formIcon === icon ? 'bg-primary/10 text-primary' : 'bg-gray-50 dark:bg-white/5'"
                @click="formIcon = icon"
              >
                <text :class="icon" class="text-xl" />
              </view>
            </view>
          </scroll-view>
        </view>

        <!-- 操作按钮 -->
        <view class="flex gap-3">
          <wd-button plain block @click="closeForm">
            取消
          </wd-button>
          <wd-button type="primary" block :loading="saving" @click="handleSave">
            保存
          </wd-button>
        </view>
      </view>
    </view>
  </wd-action-sheet>
</template>
```

- [ ] **Step 2: 验证类型检查**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 无新增类型错误

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/pages/bill/components/GridPickerPopup.vue
git commit -m "feat(component): 创建 GridPickerPopup 胶囊网格选择器组件"
```

---

### Task 9: Frontend - ManualEdit 集成

**Files:**
- Modify: `chuan-bill-app/src/pages/bill/components/ManualEdit.vue`

- [ ] **Step 1: 替换类目 wd-picker 为 GridPickerPopup**

将 ManualEdit.vue 中的类目选择器部分（约第 105-113 行）：

```html
      <view class="flex-1">
        <view class="text-xs text-gray-500">
          <text class="i-lucide:tag mr-2" />类目
        </view>
        <wd-picker
          v-model="formData.categoryId" :columns="categoryOptions" title="请选择账单类目" placeholder="请选择" custom-class="mt-2" prop="categoryId"
          :rules="[{ required: true, message: '请选择账单类目' }]"
        />
      </view>
```

替换为：

```html
      <view class="flex-1">
        <view class="text-xs text-gray-500">
          <text class="i-lucide:tag mr-2" />类目
        </view>
        <GridPickerPopup
          v-model="formData.categoryId"
          :items="categoryItems"
          title="选择类目"
          entity="category"
          :type="formData.type"
        />
      </view>
```

- [ ] **Step 2: 替换支付方式 wd-picker 为 GridPickerPopup**

将 ManualEdit.vue 中的支付方式选择器部分（约第 115-123 行）：

```html
      <view class="flex-1">
        <view class="text-xs text-gray-500">
          <text class="i-lucide:credit-card mr-2" />支付方式
        </view>
        <wd-picker
          v-model="formData.paymentMethodId" :columns="paymentMethodOptions" title="请选择支付方式" placeholder="请选择" custom-class="mt-2" prop="paymentMethodId"
          :rules="[{ required: true, message: '请选择支付方式' }]"
        />
      </view>
```

替换为：

```html
      <view class="flex-1">
        <view class="text-xs text-gray-500">
          <text class="i-lucide:credit-card mr-2" />支付方式
        </view>
        <GridPickerPopup
          v-model="formData.paymentMethodId"
          :items="paymentMethodItems"
          title="选择支付方式"
          entity="paymentMethod"
        />
      </view>
```

- [ ] **Step 3: 更新 computed 属性**

将 ManualEdit.vue 中的 `categoryOptions` 和 `paymentMethodOptions` computed（约第 29-30 行）：

```ts
const categoryOptions = computed<PickerOption[]>(() => billStore.getCategoryList(formData.value.type).map(category => ({ label: category.name, value: category.id })))
const paymentMethodOptions = computed<PickerOption[]>(() => billStore.getPaymentMethodList().map(paymentMethod => ({ label: paymentMethod.name, value: paymentMethod.id })))
```

替换为：

```ts
const categoryItems = computed(() => billStore.getCategoryList(formData.value.type))
const paymentMethodItems = computed(() => billStore.getPaymentMethodList())
```

同时可以移除不再需要的 `PickerOption` 接口（如果其他地方没有使用）和 `familyOptions` 保持不变。

- [ ] **Step 4: 验证类型检查**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 无新增类型错误

- [ ] **Step 5: 提交**

```bash
git add chuan-bill-app/src/pages/bill/components/ManualEdit.vue
git commit -m "feat(manual-edit): 替换类目和支付方式选择器为 GridPickerPopup"
```

---

### Task 10: Frontend - FilterModal "其他"排序

**Files:**
- Modify: `chuan-bill-app/src/pages/bill/components/FilterModal.vue`

- [ ] **Step 1: 修改 FilterModal 的数据源**

将 FilterModal.vue 中的数据源（约第 48-49 行）：

```ts
const categoryList = computed(() => billStore.getCategoryList(formData.value.type))
const paymentMethodList = computed(() => billStore.getPaymentMethodList())
```

替换为：

```ts
const categoryList = computed(() => billStore.getCategoryListForFilter(formData.value.type))
const paymentMethodList = computed(() => billStore.getPaymentMethodListForFilter())
```

- [ ] **Step 2: 验证类型检查**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 无新增类型错误

- [ ] **Step 3: 提交**

```bash
git add chuan-bill-app/src/pages/bill/components/FilterModal.vue
git commit -m "feat(filter): 筛选弹框使用排序 getter，其他项放在末尾"
```

---

### Task 11: 端到端验证

- [ ] **Step 1: 后端编译检查**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端类型检查**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 无新增类型错误

- [ ] **Step 3: 前端 lint 检查**

Run: `cd chuan-bill-app && pnpm lint`
Expected: 无新增 lint 错误

- [ ] **Step 4: 后端格式检查**

Run: `cd chuan-bill-server && mvn spotless:check`
Expected: 格式正确（如有问题运行 `mvn spotless:apply`）

- [ ] **Step 5: 最终提交（如有格式修复）**

```bash
git add -A
git commit -m "chore: 代码格式修复"
```
