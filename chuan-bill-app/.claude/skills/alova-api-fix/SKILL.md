---
name: alova-api-fix
description: Fix Alova API generated type definitions for Chuan Bill project. Use when working with src/api/globals.d.ts after running pnpm alova-gen to: (1) Convert amount/money fields from number to string type, (2) Handle DTO field transformations where duplicate fields need to be removed, (3) Post-process OpenAPI generated types to match backend @JsonFormat(shape = JsonFormat.Shape.STRING) annotations.
---

# Alova API Fix

修复 Alova 生成的 API 类型定义，使其符合小川记账项目的后端规范。

## 背景问题

后端使用 `@JsonFormat(shape = JsonFormat.Shape.STRING)` 将金额字段序列化为字符串，但 alova-gen 工具无法识别此注解，导致生成的 TypeScript 类型中金额字段为 `number` 而非正确的 `string`。

此外，后端使用 `@ModelAttribute` 注解将 DTO 字段展开为 query 参数，alova-gen 无法识别此模式，会产生重复字段。

## 修复流程

### 步骤 1: 生成接口定义

```bash
cd chuan-bill-app
pnpm alova-gen
```

### 步骤 2: 修复金额字段类型

将 `src/api/globals.d.ts` 中以下字段从 `number` 改为 `string`：

| 字段名 | 说明 |
|--------|------|
| `amount` | 金额 |
| `balance` | 余额 |
| `income` | 收入 |
| `expense` | 支出 |
| `budget` | 预算 |
| `price` | 价格 |
| `money` | 金额（通用） |
| `totalAmount` | 总金额 |
| `totalIncome` | 总收入 |
| `totalExpense` | 总支出 |

**操作方式**：
- 全局搜索这些字段名
- 将 `: number` 或 `: number | undefined` 改为 `: string` 或 `: string | undefined`
- 注意保留可选标记 `?` 和联合类型中的 `undefined`

### 步骤 3: 处理 DTO 字段重复（仅针对 Query 参数）

**重要区分**：
- **Query 参数**（`params`）：后端使用 `@ModelAttribute` 展开 DTO，需要删除 DTO 对象、保留展开的扁平字段
- **Body 参数**（`data`）：DTO 作为 JSON 对象传递，**保留 DTO 对象字段**

**判断方法**：
查看接口定义中 DTO 字段出现的位置：
- 在 `params` 中 → 需要展开处理
- 在 `data` 中 → 保留原样

**自动修复**（推荐）：

```bash
# 使用脚本自动删除 params 中的 DTO 字段
python .claude/skills/alova-api-fix/scripts/fix_dto_params.py src/api/globals.d.ts
```

脚本会：
1. 自动识别所有 `export interface XXXDTO` 定义
2. 在 `params` 上下文中查找对应的 DTO 字段
3. 安全删除 DTO 对象字段，保留已展开的扁平字段
4. 自动创建备份文件

**手动处理示例**：
```typescript
// 删除这个 DTO 对象字段（params 中）
billQueryDto?: {
  startDate?: string
  endDate?: string
  categoryId?: number
}

// 保留这些已展开的字段（如果存在）
startDate?: string
endDate?: string
categoryId?: number
```

**Body 参数保留示例**：
```typescript
// 保留这个 DTO 对象字段（data 中）
body: {
  billDto: {
    amount: string
    categoryId: number
    description?: string
  }
}
```

## 快速检查清单

修复完成后，确认：

- [ ] 所有金额相关字段为 `string` 类型
- [ ] Query 参数中的 DTO 对象已删除，保留展开的扁平字段
- [ ] Body 参数中的 DTO 对象已保留
- [ ] 文件无 TypeScript 语法错误
- [ ] 项目能正常通过 `pnpm type-check`

## 参考

- 后端金额格式化：`@JsonFormat(shape = JsonFormat.Shape.STRING)`
- 后端 DTO 展开：`@ModelAttribute` 注解
- 原文件备份：生成前自动备份到 `src/api/globals.d.ts.bak`