---
name: alova-api-fix
description: Fix Alova API generated type definitions for Chuan Bill project. Use when working with src/api/globals.d.ts after running pnpm alova-gen to fix (1) amount/money fields from number to string type to match backend @JsonFormat annotation, (2) DTO field duplications in params where @ModelAttribute expands DTO to flat query parameters, (3) binary response types where void-returning file download endpoints incorrectly generate null instead of ArrayBuffer. Must be used immediately after pnpm alova-gen generates new API definitions.
---

# Alova API Fix

修复 Alova 生成的 API 类型定义，使其符合小川记账项目的后端规范。

## 背景问题

### 问题 1: 金额字段类型错误
后端使用 `@JsonFormat(shape = JsonFormat.Shape.STRING)` 将金额字段序列化为字符串，但 alova-gen 工具无法识别此注解，导致生成的 TypeScript 类型中金额字段为 `number` 而非正确的 `string`。

**涉及字段**: `amount`, `minAmount`, `maxAmount`, `balance`, `income`, `expense`, `budget`, `price`, `money`, `totalAmount`, `totalIncome`, `totalExpense`

### 问题 2: 二进制响应类型错误
后端文件下载接口（如 `exportBill`）返回类型为 `void`，实际通过 `HttpServletResponse` 写入二进制流。alova-gen 为 `void` 生成 `null` 泛型，但前端需要 `ArrayBuffer` 才能正确处理文件下载。

**涉及接口**: `bill.exportBill`（可通过 `BINARY_RESPONSE_METHODS` 列表扩展）

**示例**:
```typescript
// 修复前（alova-gen 生成）
exportBill<Config extends Alova2MethodConfig<null> & { ... }>(
  config: Config
): Alova2Method<null, 'bill.exportBill', Config>;

// 修复后（正确）
exportBill<Config extends Alova2MethodConfig<ArrayBuffer> & { ... }>(
  config: Config
): Alova2Method<ArrayBuffer, 'bill.exportBill', Config>;
```

### 问题 3: DTO 参数重复
后端使用 `@ModelAttribute` 注解将 DTO 字段展开为 query 参数。虽然 `alova.config.ts` 中的 `handleApi` 函数已配置为展开 DTO 字段，但生成的代码仍然保留了原始的 DTO 对象字段，导致 params 中同时存在 DTO 对象和展开的扁平字段。

**示例**:
```typescript
// 修复前（有问题）
params: {
  billListDTO?: BillListDTO;  // ❌ 需要删除
  startDate?: string;         // ✅ 已展开，保留
  endDate?: string;           // ✅ 已展开，保留
  // ... 其他展开字段
}

// 修复后（正确）
params: {
  startDate?: string;         // ✅ 保留
  endDate?: string;           // ✅ 保留
  // ... 其他展开字段（没有 DTO 对象）
}
```

## 自动修复流程（推荐）

### 步骤 1: 生成接口定义

```bash
cd chuan-bill-app
pnpm alova-gen
```

### 步骤 2: 运行自动修复脚本

```bash
# 使用整合脚本自动修复所有问题
python .claude/skills/alova-api-fix/scripts/fix_alova_api.py src/api/globals.d.ts
```

脚本会自动：
1. 提取所有 DTO 接口定义
2. 将所有金额字段从 `number` 改为 `string`
3. 删除 params 中重复的 DTO 对象字段（仅当字段已展开时才删除）
4. 修复文件下载接口的二进制响应类型（`null` → `ArrayBuffer`）
5. 自动创建带时间戳的备份文件

### 步骤 3: 验证修复结果

```bash
pnpm type-check
```

## 手动修复（备选）

如果自动脚本无法解决问题，可以手动修复：

### 手动修复二进制响应类型

在 `globals.d.ts` 中搜索 `Alova2MethodConfig<null>` 和 `Alova2Method<null,`，找到文件下载相关的接口（如 `exportBill`），将 `null` 替换为 `ArrayBuffer`。

**匹配模式**: `Alova2MethodConfig<null>` → `Alova2MethodConfig<ArrayBuffer>`，`Alova2Method<null,` → `Alova2Method<ArrayBuffer,`

### 手动修复金额字段

全局搜索以下字段，将 `: number` 改为 `: string`：

| 字段名 | 说明 |
|--------|------|
| `amount` | 金额 |
| `minAmount` | 最小金额 |
| `maxAmount` | 最大金额 |
| `balance` | 余额 |
| `income` | 收入 |
| `expense` | 支出 |
| `budget` | 预算 |
| `price` | 价格 |
| `money` | 金额（通用） |
| `totalAmount` | 总金额 |
| `totalIncome` | 总收入 |
| `totalExpense` | 总支出 |

**匹配模式**: `{field}?: number` → `{field}?: string`

### 手动修复 DTO 参数

在 `params` 对象中查找 DTO 字段（如 `xxxDTO?: XxxDTO`），如果该 DTO 的字段已经被平铺展开，则删除 DTO 对象字段。

## 修复检查清单

修复完成后，确认：

- [ ] 运行 `python .claude/skills/alova-api-fix/scripts/fix_alova_api.py` 成功
- [ ] 所有金额相关字段为 `string` 类型
- [ ] 二进制响应接口（如 `exportBill`）的泛型为 `ArrayBuffer` 而非 `null`
- [ ] Query 参数中的 DTO 对象已删除，保留展开的扁平字段
- [ ] Body 参数中的 DTO 对象已保留（不受影响）
- [ ] 运行 `pnpm type-check` 无错误

## 配置文件说明

### alova.config.ts

项目中的 `alova.config.ts` 已配置 `handleApi` 函数来处理 `@ModelAttribute` 展开：

```typescript
handleApi: (apiDescriptor) => {
  const parameters = apiDescriptor.parameters

  // 查找 DTO 参数
  const dtoParam = parameters?.find(item => item.name.match(/dto$/i))

  if (dtoParam) {
    // 提取 DTO 的 properties
    const properties = dtoParam.schema.properties
    const dtoParameters = Object.keys(properties).map((key) => {
      const schema = properties[key]
      return {
        name: key,
        in: 'query',
        schema,
      }
    })

    // 将展开的字段添加到 parameters
    apiDescriptor.parameters = [...parameters, ...dtoParameters]
  }

  return apiDescriptor
}
```

此配置确保 DTO 字段被展开为独立的 query 参数，但仍需要修复脚本来删除原始的 DTO 对象字段。

## 参考

- 后端金额格式化：`@JsonFormat(shape = JsonFormat.Shape.STRING)`
- 后端 DTO 展开：`@ModelAttribute` 注解
- 后端文件导出：`BillController.exportBill()` 使用 `HttpServletResponse` 写入二进制流
- 备份文件：生成前自动备份到 `src/api/globals.d.ts.bak.{timestamp}`
