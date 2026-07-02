#!/usr/bin/env python3
"""
修复 Alova 生成的 API 类型定义中的 DTO 参数重复问题。
删除 params 中的 DTO 对象字段（因为后端 @ModelAttribute 已将其展开为扁平字段）。
"""

import re
import sys
from pathlib import Path


def extract_dto_fields(content: str, dto_name: str) -> list:
    """从 DTO 接口定义中提取字段名列表"""
    # 查找 DTO 接口定义
    pattern = rf'export interface {dto_name} \{{([^}}]+)\}}'
    match = re.search(pattern, content, re.DOTALL)

    if not match:
        return []

    dto_body = match.group(1)
    fields = []

    # 提取字段名（支持可选标记 ?）
    field_pattern = r'(\w+)\??:'
    for field_match in re.finditer(field_pattern, dto_body):
        fields.append(field_match.group(1))

    return fields


def fix_dto_params(file_path: str):
    """修复 globals.d.ts 中的 DTO 参数重复问题"""
    path = Path(file_path)

    if not path.exists():
        print(f"错误: 文件不存在 {file_path}")
        sys.exit(1)

    content = path.read_text(encoding='utf-8')
    original = content

    # 查找所有 DTO 接口名称
    dto_interfaces = re.findall(r'export interface (\w+DTO)', content)

    if not dto_interfaces:
        print(f"未找到 DTO 接口定义: {file_path}")
        return

    print(f"发现 DTO 接口: {', '.join(dto_interfaces)}")

    # 统计修改
    removed_dtos = []

    # 处理每个 DTO
    for dto_name in dto_interfaces:
        # 查找 DTO 字段名（通常首字母小写）
        dto_field_variants = [
            dto_name[0].lower() + dto_name[1:],  # BillListDTO -> billListDTO
            dto_name,  # 可能直接使用原名
        ]

        for dto_field in dto_field_variants:
            # 在 params 中查找 DTO 字段定义
            # 匹配模式：dtoField?: DtoType 或 dtoField: DtoType
            dto_param_pattern = rf'({dto_field}\??\s*:\s*{dto_name}\s*;?)'

            matches = list(re.finditer(dto_param_pattern, content))

            for match in matches:
                # 检查是否在 params 上下文中
                start_pos = match.start()
                context = content[max(0, start_pos - 200):min(len(content), start_pos + 50)]

                # 确认在 params 对象中
                if 'params:' in context or 'params :' in context:
                    # 删除 DTO 字段定义
                    field_def = match.group(1)
                    content = content.replace(field_def, '', 1)
                    removed_dtos.append(f"{dto_field}: {dto_name}")
                    print(f"  已删除 params 中的 DTO 字段: {dto_field}: {dto_name}")
                    break

    # 清理可能的空行和多余空格
    content = re.sub(r'\n\s*\n\s*\n', '\n\n', content)

    # 写回文件（如果内容有变化）
    if content != original:
        backup_path = path.with_suffix('.d.ts.dto.bak')
        if not backup_path.exists():
            path.rename(backup_path)
            print(f"  备份: {backup_path}")

        path.write_text(content, encoding='utf-8')
        print(f"✓ 已修复: {file_path}")

        if removed_dtos:
            print(f"  删除的 DTO 字段: {', '.join(set(removed_dtos))}")
    else:
        print(f"无需修复 DTO 参数: {file_path}")


if __name__ == '__main__':
    if len(sys.argv) < 2:
        default_path = Path('src/api/globals.d.ts')
        if default_path.exists():
            fix_dto_params(str(default_path))
        else:
            print("用法: python fix_dto_params.py <path/to/globals.d.ts>")
            sys.exit(1)
    else:
        fix_dto_params(sys.argv[1])
