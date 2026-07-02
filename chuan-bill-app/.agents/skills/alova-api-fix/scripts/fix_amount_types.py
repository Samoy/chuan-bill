#!/usr/bin/env python3
"""
修复 Alova 生成的 API 类型定义中的金额字段类型。
将 amount, balance 等字段从 number 转换为 string 类型。
"""

import re
import sys
from pathlib import Path

# 需要转换为 string 的字段名（支持驼峰命名）
AMOUNT_FIELDS = [
    'minAmount',  # 最小金额
    'maxAmount',  # 最大金额
    'amount',       # 金额
    'balance',      # 余额
    'income',       # 收入
    'expense',      # 支出
    'budget',       # 预算
    'price',        # 价格
    'money',        # 金额（通用）
    'totalAmount',  # 总金额
    'totalIncome',  # 总收入
    'totalExpense', # 总支出
]


def fix_amount_types(file_path: str):
    """修复 globals.d.ts 中的金额字段类型"""
    path = Path(file_path)

    if not path.exists():
        print(f"错误: 文件不存在 {file_path}")
        sys.exit(1)

    content = path.read_text(encoding='utf-8')
    original = content

    # 为每个金额字段进行类型替换
    for field in AMOUNT_FIELDS:
        # 匹配字段定义，处理多种情况：
        # 1. field?: number
        # 2. field?: number | undefined
        # 3. field: number
        # 4. field: number | undefined
        pattern = rf'({field}\??\s*:\s*)number(\s*\|\s*undefined)?'
        replacement = rf'\1string\2'
        content = re.sub(pattern, replacement, content)

    # 写回文件（如果内容有变化）
    if content != original:
        # 创建备份
        backup_path = path.with_suffix('.d.ts.bak')
        path.rename(backup_path)
        path.write_text(content, encoding='utf-8')
        print(f"✓ 已修复: {file_path}")
        print(f"  备份: {backup_path}")

        # 统计修改的字段
        changes = []
        for field in AMOUNT_FIELDS:
            if re.search(rf'{field}\??\s*:\s*string', content):
                changes.append(field)

        if changes:
            print(f"  转换的字段: {', '.join(changes)}")
    else:
        print(f"无需修复: {file_path}")


if __name__ == '__main__':
    if len(sys.argv) < 2:
        # 默认路径
        default_path = Path('src/api/globals.d.ts')
        if default_path.exists():
            fix_amount_types(str(default_path))
        else:
            print("用法: python fix_amount_types.py <path/to/globals.d.ts>")
            sys.exit(1)
    else:
        fix_amount_types(sys.argv[1])
