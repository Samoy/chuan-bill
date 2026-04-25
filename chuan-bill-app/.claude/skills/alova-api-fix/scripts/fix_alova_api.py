#!/usr/bin/env python3
"""
Alova API 生成文件自动修复脚本
修复两个问题：
1. 金额字段类型：将 number 转换为 string（匹配后端 @JsonFormat(shape = JsonFormat.Shape.STRING)）
2. DTO 参数重复：删除 params 中的 DTO 对象字段（后端 @ModelAttribute 已展开为扁平字段）

用法：
    python fix_alova_api.py [path/to/globals.d.ts]

默认路径：src/api/globals.d.ts
"""

import re
import sys
from pathlib import Path
from datetime import datetime

# 需要转换为 string 的金额字段名
AMOUNT_FIELDS = [
    'minAmount',     # 最小金额
    'maxAmount',     # 最大金额
    'amount',        # 金额
    'balance',       # 余额
    'income',        # 收入
    'expense',       # 支出
    'budget',        # 预算
    'price',         # 价格
    'money',         # 金额（通用）
    'totalAmount',   # 总金额
    'totalIncome',   # 总收入
    'totalExpense',  # 总支出
]

# DTO 接口名称模式
DTO_INTERFACE_PATTERN = re.compile(r'export interface (\w+DTO)\s*\{([^}]+)\}', re.DOTALL)
FIELD_PATTERN = re.compile(r'(\w+)\??:\s*([^;\n]+)')


def extract_dto_definitions(content: str) -> dict:
    """提取所有 DTO 接口定义及其字段"""
    dto_definitions = {}

    for match in DTO_INTERFACE_PATTERN.finditer(content):
        dto_name = match.group(1)
        dto_body = match.group(2)

        fields = {}
        for field_match in FIELD_PATTERN.finditer(dto_body):
            field_name = field_match.group(1)
            field_type = field_match.group(2).strip()
            fields[field_name] = field_type

        dto_definitions[dto_name] = fields

    return dto_definitions


def generate_dto_field_variants(dto_name: str) -> list:
    """生成可能的 DTO 字段名变体"""
    variants = [
        dto_name[0].lower() + dto_name[1:],  # BillListDTO -> billListDTO
        dto_name,                            # BillListDTO -> BillListDTO
    ]

    # 对于 XXXDTO 形式，也尝试 xxxDTO 和 xxxDto
    if dto_name.endswith('DTO'):
        base = dto_name[:-3]
        variants.append(base[0].lower() + base[1:] + 'DTO')  # billListDTO
        variants.append(base[0].lower() + base[1:] + 'Dto')  # billListDto
        variants.append(base + 'Dto')                         # BillListDto

    return list(set(variants))


def fix_amount_types(content: str) -> tuple[str, list]:
    """修复金额字段类型，返回修改后的内容和变更列表"""
    changes = []

    for field in AMOUNT_FIELDS:
        # 匹配多种模式：
        # 1. field?: number
        # 2. field?: number | undefined
        # 3. field: number
        # 4. field: number | undefined
        # 5. field?: number | null
        pattern = rf'({re.escape(field)}\??\s*:\s*)number(\s*\|\s*(?:undefined|null))?'

        def replace_match(match):
            prefix = match.group(1)
            suffix = match.group(2) or ''
            return f'{prefix}string{suffix}'

        new_content, count = re.subn(pattern, replace_match, content)

        if count > 0:
            changes.append(f"{field}: number -> string ({count} 处)")
            content = new_content

    return content, changes


def fix_dto_params(content: str, dto_definitions: dict) -> tuple[str, list]:
    """修复 DTO 参数重复问题，返回修改后的内容和变更列表"""
    changes = []

    # 查找所有在 params 上下文中的 DTO 字段
    for dto_name, dto_fields in dto_definitions.items():
        field_variants = generate_dto_field_variants(dto_name)

        for field_name in field_variants:
            # 在 params 中查找 DTO 字段定义
            # 匹配模式：fieldName?: DtoType 或 fieldName: DtoType
            dto_param_pattern = rf'({re.escape(field_name)}\??\s*:\s*{re.escape(dto_name)}\s*;?)'

            for match in re.finditer(dto_param_pattern, content):
                start_pos = match.start()

                # 获取上下文（前后各 300 字符）
                context_start = max(0, start_pos - 300)
                context_end = min(len(content), start_pos + 300)
                context = content[context_start:context_end]

                # 确认在 params 对象中（检查上下文）
                if re.search(r'\bparams\s*:', context):
                    # 验证 DTO 的字段是否已经被展开（存在于同一上下文中）
                    expanded_fields = list(dto_fields.keys())
                    expanded_count = sum(1 for f in expanded_fields if re.search(rf'\b{re.escape(f)}\??\s*:', context))

                    # 如果至少有一半的 DTO 字段已展开，则认为可以安全删除 DTO 对象
                    if expanded_count >= len(expanded_fields) / 2:
                        field_def = match.group(1)
                        # 删除字段定义（包括可能的尾随逗号或分号）
                        content = content[:match.start()] + content[match.end():]
                        changes.append(f"删除 params 中的 {field_name}: {dto_name} (已展开 {expanded_count}/{len(expanded_fields)} 个字段)")
                        break

    # 清理多余的空行
    content = re.sub(r'\n\s*\n\s*\n', '\n\n', content)

    return content, changes


def fix_api_definitions(file_path: str) -> tuple[str, list]:
    """修复 apiDefinitions.ts 中的重复条目"""
    path = Path(file_path)
    if not path.exists():
        return None, []

    content = path.read_text(encoding='utf-8')
    original = content
    changes = []

    # 检查是否有重复的行（相同的方法名）
    lines = content.split('\n')
    seen = {}
    duplicates = []

    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped and not stripped.startswith('//') and not stripped.startswith('/*'):
            # 提取方法名（引号内的内容）
            match = re.search(r"'([^']+)'", stripped)
            if match:
                method_name = match.group(1)
                if method_name in seen:
                    duplicates.append((i, method_name, seen[method_name]))
                else:
                    seen[method_name] = i

    # 删除重复行（保留第一个）
    for line_idx, method_name, first_idx in reversed(duplicates):
        del lines[line_idx]
        changes.append(f"删除重复的方法定义: {method_name} (第 {line_idx + 1} 行，保留第 {first_idx + 1} 行)")

    if changes:
        content = '\n'.join(lines)

    return content, changes


def main():
    # 获取文件路径
    if len(sys.argv) < 2:
        default_path = Path('src/api/globals.d.ts')
        if default_path.exists():
            globals_path = default_path
        else:
            print("用法: python fix_alova_api.py [path/to/globals.d.ts]")
            print("默认路径: src/api/globals.d.ts")
            sys.exit(1)
    else:
        globals_path = Path(sys.argv[1])

    if not globals_path.exists():
        print(f"错误: 文件不存在 {globals_path}")
        sys.exit(1)

    # 读取文件
    print(f"正在处理: {globals_path}")
    print("-" * 50)

    content = globals_path.read_text(encoding='utf-8')
    original_content = content

    # 步骤 1: 提取 DTO 定义
    print("\n[步骤 1] 提取 DTO 定义...")
    dto_definitions = extract_dto_definitions(content)
    if dto_definitions:
        print(f"  发现 {len(dto_definitions)} 个 DTO 接口:")
        for dto_name in dto_definitions:
            print(f"    - {dto_name}")
    else:
        print("  未发现 DTO 接口")

    # 步骤 2: 修复金额字段类型
    print("\n[步骤 2] 修复金额字段类型...")
    content, amount_changes = fix_amount_types(content)
    if amount_changes:
        print("  变更:")
        for change in amount_changes:
            print(f"    [OK] {change}")
    else:
        print("  无需修复金额字段")

    # 步骤 3: 修复 DTO 参数重复
    print("\n[步骤 3] 修复 DTO 参数重复...")
    content, dto_changes = fix_dto_params(content, dto_definitions)
    if dto_changes:
        print("  变更:")
        for change in dto_changes:
            print(f"    [OK] {change}")
    else:
        print("  无需修复 DTO 参数")

    # 步骤 4: 写回文件
    if content != original_content:
        # 创建备份
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        backup_path = globals_path.with_suffix(f'.d.ts.bak.{timestamp}')
        backup_path.write_text(original_content, encoding='utf-8')

        # 写入修复后的内容
        globals_path.write_text(content, encoding='utf-8')

        print("\n" + "=" * 50)
        print("[OK] 修复完成!")
        print(f"  备份文件: {backup_path}")

        total_changes = len(amount_changes) + len(dto_changes)
        print(f"  共 {total_changes} 项变更")
    else:
        print("\n" + "=" * 50)
        print("[OK] 无需修复，文件已是最新")

    # 可选：修复 apiDefinitions.ts
    api_defs_path = globals_path.parent / 'apiDefinitions.ts'
    if api_defs_path.exists():
        print(f"\n[可选] 检查 apiDefinitions.ts...")
        new_content, api_changes = fix_api_definitions(str(api_defs_path))
        if api_changes:
            # 创建备份
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            backup_path = api_defs_path.with_suffix(f'.ts.bak.{timestamp}')
            backup_path.write_text(api_defs_path.read_text(encoding='utf-8'), encoding='utf-8')

            api_defs_path.write_text(new_content, encoding='utf-8')
            print("  变更:")
            for change in api_changes:
                print(f"    [OK] {change}")
            print(f"  备份文件: {backup_path}")
        else:
            print("  无需修复")

    print("\n" + "=" * 50)
    print("提示: 请运行 'pnpm type-check' 验证修复结果")


if __name__ == '__main__':
    main()
