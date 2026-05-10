# 小川记账 × NL2SQL 方案 — 百炼平台接入设计

> NL2SQL：将用户的自然语言问题自动转换为 SQL 查询，直接从数据库获取答案。

---

## 1. 为什么选 NL2SQL

| 维度 | Function Calling（方案B） | NL2SQL |
|------|--------------------------|--------|
| 灵活性 | 需预定义每个 API | 自然语言直接转 SQL，无限灵活 |
| 开发量 | 需新增后端接口 | 只需暴露只读数据库连接 |
| 维护成本 | 新需求 = 新接口 | 新问题 = 自动适配 |
| 覆盖范围 | 只能回答预设问题 | 能回答任何数据可回答的问题 |
| 安全性 | 代码层控制 | SQL 拦截 + 只读账号 + 行级过滤 |

**典型问题覆盖**：
- ✅ "我最近早餐花销大吗？"
- ✅ "这个月我在交通上花了多少？"
- ✅ "上个月和这个月比，餐饮支出多了多少？"
- ✅ "我最贵的一笔消费是什么？"
- ✅ "家庭账单里谁花得最多？"
- ✅ "我一般用什么方式付款？"
- ✅ "最近一周有超预算吗？"

---

## 2. 整体架构

```
┌──────────────────────────────────────────────────────────┐
│                        用户                                │
│                  "我最近早餐花销大吗？"                      │
└─────────────────────────┬────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────┐
│                   百炼平台 工作流                           │
│                                                          │
│  ┌──────────┐    ┌──────────┐    ┌───────────────────┐  │
│  │ Gate Node │──▶│ NL2SQL   │──▶│ SQL 安全审核节点    │  │
│  │ 安全过滤  │    │ 自然语言  │    │ · 只允许 SELECT   │  │
│  │          │    │ → SQL    │    │ · 必须有 user_id  │  │
│  └──────────┘    └──────────┘    │ · 禁止 DROP/ALTER │  │
│                                  │ · 行数限制 ≤100   │  │
│                                  └───────┬───────────┘  │
│                                          │               │
│                                          ▼               │
│                                  ┌───────────────────┐  │
│                                  │ 只读数据库连接      │  │
│                                  │ 执行 SQL 查询       │  │
│                                  └───────┬───────────┘  │
│                                          │               │
│                                          ▼               │
│                                  ┌───────────────────┐  │
│                                  │ LLM 生成自然语言   │  │
│                                  │ 回答（基于查询结果） │  │
│                                  └───────────────────┘  │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
                    自然语言回答：
                    "最近7天您的早餐消费共 ¥156，
                     日均 ¥22.3，比上周略高一些 😊"
```

---

## 3. 数据库 Schema（NL2SQL 模型输入）

> 以下 Schema 文档将作为百炼 NL2SQL 节点的上下文输入。

```sql
-- ============================================
-- 小川记账 数据库 Schema 说明
-- 所有表都有 deleted 字段(0=正常, 1=已删除)
-- 所有表主键为 String 类型（雪花ID）
-- 所有表都有 create_time, update_time 字段
-- ============================================

-- 用户表
-- 注意: phone 是敏感字段，查询结果中不得返回
-- 注意: password, openid 不得返回
CREATE TABLE t_user (
    id          VARCHAR(32) PRIMARY KEY COMMENT '用户ID',
    nickname    VARCHAR(50)  COMMENT '昵称',
    avatar      VARCHAR(255) COMMENT '头像URL',
    gender      TINYINT      COMMENT '性别: 0-未知 1-男 2-女',
    is_vip      TINYINT      COMMENT '是否VIP: 0-否 1-是',
    deleted     TINYINT      COMMENT '是否删除: 0-正常 1-已删除'
);

-- 账单表（核心表）
CREATE TABLE t_bill (
    id                VARCHAR(32)   PRIMARY KEY COMMENT '账单ID',
    user_id           VARCHAR(32)   COMMENT '用户ID（必须过滤条件）',
    family_id         VARCHAR(32)   COMMENT '家庭ID（可为空，表示个人账单）',
    name              VARCHAR(100)  COMMENT '账单名称/描述，如"早餐"、"地铁"',
    category_id       VARCHAR(32)   COMMENT '分类ID',
    payment_method_id VARCHAR(32)   COMMENT '支付方式ID',
    type              VARCHAR(10)   COMMENT '类型: expense-支出 income-收入',
    amount            DECIMAL(10,2) COMMENT '金额（元）',
    time              DATETIME      COMMENT '账单日期时间',
    remark            VARCHAR(500)  COMMENT '备注',
    source            VARCHAR(10)   COMMENT '来源: manual-手动 ocr-拍照 voice-语音 import-导入',
    deleted           TINYINT       COMMENT '是否删除: 0-正常 1-已删除'
);

-- 分类表
CREATE TABLE t_category (
    id        VARCHAR(32)  PRIMARY KEY COMMENT '分类ID',
    name      VARCHAR(50)  COMMENT '分类名称，如: 餐饮、交通、购物、工资、奖金',
    type      VARCHAR(10)  COMMENT '类型: expense-支出 income-收入',
    user_id   VARCHAR(32)  COMMENT '为NULL表示系统预设，非NULL表示用户自定义',
    deleted   TINYINT      COMMENT '是否删除: 0-正常 1-已删除'
);

-- 支付方式表
CREATE TABLE t_payment_method (
    id      VARCHAR(32)  PRIMARY KEY COMMENT '支付方式ID',
    name    VARCHAR(50)  COMMENT '名称，如: 现金、微信、支付宝、银行卡',
    user_id VARCHAR(32)  COMMENT '为NULL表示系统预设',
    deleted TINYINT      COMMENT '是否删除: 0-正常 1-已删除'
);

-- 家庭表
CREATE TABLE t_family (
    id          VARCHAR(32)  PRIMARY KEY COMMENT '家庭ID',
    name        VARCHAR(50)  COMMENT '家庭名称',
    owner_id    VARCHAR(32)  COMMENT '户主用户ID',
    description VARCHAR(200) COMMENT '家庭描述',
    deleted     TINYINT      COMMENT '是否删除: 0-正常 1-已删除'
);

-- 家庭成员表
CREATE TABLE t_family_member (
    id        VARCHAR(32) PRIMARY KEY COMMENT '记录ID',
    family_id VARCHAR(32) COMMENT '家庭ID',
    user_id   VARCHAR(32) COMMENT '用户ID',
    nickname  VARCHAR(50) COMMENT '成员在家庭中的昵称',
    is_owner  TINYINT     COMMENT '是否户主: 0-否 1-是',
    deleted   TINYINT     COMMENT '是否删除: 0-正常 1-已删除'
);

-- 预算表
CREATE TABLE t_budget (
    id         VARCHAR(32)   PRIMARY KEY COMMENT '预算ID',
    user_id    VARCHAR(32)   COMMENT '用户ID',
    family_id  VARCHAR(32)   COMMENT '家庭ID（可为空=个人预算）',
    month      DATE          COMMENT '月份（存储为该月1号）',
    amount     DECIMAL(10,2) COMMENT '预算金额',
    use_amount DECIMAL(10,2) COMMENT '已使用金额',
    deleted    TINYINT       COMMENT '是否删除: 0-正常 1-已删除'
);

-- 消息表
CREATE TABLE t_message (
    id           VARCHAR(32)  PRIMARY KEY COMMENT '消息ID',
    user_id      VARCHAR(32)  COMMENT '接收用户ID',
    title        VARCHAR(100) COMMENT '标题',
    content      VARCHAR(500) COMMENT '内容',
    type         VARCHAR(20)  COMMENT '类型: system-系统 family-家庭 bill-账单 budget-预算',
    status       TINYINT      COMMENT '状态: 0-未读 1-已读',
    deleted      TINYINT      COMMENT '是否删除: 0-正常 1-已删除'
);
```

---

## 4. 安全设计（核心）

### 4.1 SQL 安全审核规则

百炼工作流中在 NL2SQL 节点后增加一个 **SQL 审核节点**，规则如下：

```
┌─────────────────────────────────────────────────┐
│              SQL 安全审核规则                      │
│                                                 │
│  ✅ 必须满足:                                    │
│  1. 只允许 SELECT 语句                           │
│  2. WHERE 条件必须包含 user_id = '${user_id}'    │
│  3. WHERE 条件必须包含 deleted = 0               │
│  4. 不允许返回 phone、password、openid 字段       │
│  5. 不允许子查询嵌套超过 2 层                     │
│  6. LIMIT 不超过 100                             │
│  7. 不允许 UNION、INTO、LOAD_FILE 等危险关键字    │
│                                                 │
│  ❌ 禁止操作:                                    │
│  INSERT / UPDATE / DELETE / DROP / ALTER         │
│  CREATE / TRUNCATE / GRANT / REVOKE             │
│  EXEC / EXECUTE / xp_ / sp_                     │
│                                                 │
│  🔄 自动注入:                                    │
│  user_id = '${current_user_id}'                 │
│  deleted = 0                                    │
│  审核不通过 → 拒绝执行，返回友好提示               │
└─────────────────────────────────────────────────┘
```

### 4.2 只读数据库账号

创建一个专门的 **只读数据库用户**，用于百炼 NL2SQL 连接：

```sql
-- 创建只读用户
CREATE USER 'chuan_bill_agent'@'%' IDENTIFIED BY 'strong_password_here';

-- 只授予 SELECT 权限
GRANT SELECT ON chuan_bill_db.t_bill TO 'chuan_bill_agent'@'%';
GRANT SELECT ON chuan_bill_db.t_category TO 'chuan_bill_agent'@'%';
GRANT SELECT ON chuan_bill_db.t_payment_method TO 'chuan_bill_agent'@'%';
GRANT SELECT ON chuan_bill_db.t_family TO 'chuan_bill_agent'@'%';
GRANT SELECT ON chuan_bill_db.t_family_member TO 'chuan_bill_agent'@'%';
GRANT SELECT ON chuan_bill_db.t_budget TO 'chuan_bill_agent'@'%';
GRANT SELECT ON chuan_bill_db.t_message TO 'chuan_bill_agent'@'%';

-- 不授予 t_user 的 SELECT（防止泄露手机号等）
-- 不授予任何写入权限
FLUSH PRIVILEGES;
```

### 4.3 数据隔离层级

```
层级 1: 数据库账号 — 只读，只能 SELECT
层级 2: 表级权限 — 不暴露 t_user（手机号、密码等）
层级 3: SQL 审核 — 强制 user_id 过滤，防止越权
层级 4: 字段过滤 — 结果中移除敏感字段
层级 5: 行数限制 — 最多返回 100 行
```

### 4.4 用户身份传递

```
App 端调用百炼 Agent 时:
├── 传入 session_id（百炼会话ID）
├── 传入 user_id（当前登录用户ID）
│
百炼工作流中:
├── NL2SQL 节点自动注入: user_id = '${传入的user_id}'
├── SQL 审核节点校验: WHERE 子句必须包含 user_id
│
数据库查询:
├── 只返回该用户的数据
└── 结果经过 LLM 生成自然语言回答
```

---

## 5. 百炼平台配置

### 5.1 工作流节点

```
[开始] 
  │  输入: user_question, user_id
  ▼
[LLM: Gate] ── 安全过滤 + 意图分类
  │
  ├── OFF_TOPIC / BLOCKED → [回复节点] → 结束
  │
  ├── 知识类问题 → [知识库 RAG + LLM] → 回复（产品FAQ等）
  │
  └── 数据查询类问题 → [NL2SQL 节点]
                          │
                          ▼
                    [SQL 审核节点]
                          │
                          ├── 不通过 → [回复节点: 安全拒绝] → 结束
                          │
                          └── 通过 → [数据库查询节点]
                                        │
                                        ▼
                                  [LLM: 回答生成]
                                        │
                                        ▼
                                    [输出节点] → 结束
```

### 5.2 NL2SQL 节点配置

**百炼控制台 → 工作流编辑器 → 添加 NL2SQL 节点**：

| 配置项 | 值 |
|--------|-----|
| 数据源类型 | MySQL |
| 连接地址 | 数据库内网地址:3306 |
| 数据库名 | chuan_bill_db |
| 用户名 | chuan_bill_agent（只读账号） |
| 密码 | （强密码） |
| Schema 文档 | 见第3节 |
| 模型 | qwen-max（NL2SQL 需要较强推理能力） |

### 5.3 NL2SQL Prompt 配置

```markdown
你是小川记账的 SQL 查询生成器。根据用户的自然语言问题，生成安全的 MySQL 查询。

## 规则
1. 只生成 SELECT 语句
2. WHERE 条件必须包含: user_id = '${user_id}' AND deleted = 0
3. 不返回 phone、password、openid 等敏感字段
4. 不使用 INSERT/UPDATE/DELETE/DROP 等写操作
5. LIMIT 不超过 100
6. 账单表 t_bill 是核心表，通过 category_id 关联 t_category 获取分类名称
7. 金额字段 amount 使用 DECIMAL(10,2)
8. 时间字段 time 是 DATETIME 类型，支持 DATE_SUB 等日期函数
9. 用户说"最近"默认指最近7天，说"这个月"指当月1号至今
10. 用户说的"早餐"、"午餐"等对应 t_bill.name 字段的模糊匹配
11. 用户说的"餐饮"、"交通"等对应 t_category.name 字段
12. 收入/支出对应 t_bill.type: 'income'/'expense'

## 常见分类名称
支出类: 餐饮、交通、购物、娱乐、住房、医疗、教育、日用、通讯、服饰
收入类: 工资、奖金、投资收益、兼职、红包、退款

## 常见支付方式
现金、微信、支付宝、银行卡、信用卡

## 示例
用户: "我这个月餐饮花了多少？"
SQL: SELECT SUM(b.amount) as total, COUNT(*) as count 
     FROM t_bill b 
     JOIN t_category c ON b.category_id = c.id 
     WHERE b.user_id = '${user_id}' 
     AND b.deleted = 0 
     AND c.name = '餐饮' 
     AND b.type = 'expense'
     AND b.time >= DATE_FORMAT(NOW(), '%Y-%m-01');

用户: "最近一周我最贵的消费是什么？"
SQL: SELECT b.name, b.amount, b.time 
     FROM t_bill b 
     WHERE b.user_id = '${user_id}' 
     AND b.deleted = 0 
     AND b.type = 'expense'
     AND b.time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
     ORDER BY b.amount DESC 
     LIMIT 1;
```

### 5.4 回答生成 Prompt

```markdown
你是小川助手，基于查询结果用自然语言回答用户的账单问题。

## 规则
1. 用友好、简洁的中文回答
2. 适当使用 emoji
3. 金额保留2位小数，带 ¥ 符号
4. 如果查询结果为空，告知用户该时间段没有相关账单
5. 可以给出简单的分析（如趋势、对比），但不要编造数据
6. 不要暴露 SQL 查询过程或数据库结构
7. 如果用户的问题超出数据范围，引导使用 App 功能

## 示例
查询结果: {"total": 156.50, "count": 7, "avg": 22.36}
回答: "最近7天您的早餐消费共 ¥156.50，一共7笔，日均约 ¥22.36 😊"

查询结果: []（空）
回答: "这个时间段内没有找到相关账单记录哦～可能还没有记过这类账单 📝"
```

---

## 6. 示例对话

### 示例 1: 早餐花销
```
用户: 我最近早餐花销大吗？

[NL2SQL 生成]:
SELECT SUM(b.amount) as total, COUNT(*) as count,
       AVG(b.amount) as avg_per_meal
FROM t_bill b
WHERE b.user_id = '${user_id}'
AND b.deleted = 0
AND b.name LIKE '%早餐%'
AND b.time >= DATE_SUB(NOW(), INTERVAL 30 DAY);

[查询结果]: {"total": 380.00, "count": 15, "avg_per_meal": 25.33}

[回答]: 最近30天您的早餐消费共 ¥380.00，共15笔，
       平均每顿 ¥25.33。如果按月来看，日均早餐花费约 ¥12.67，
       这个水平属于正常范围哦 😊 如果想控制开支，
       可以考虑设置预算提醒～
```

### 示例 2: 月度对比
```
用户: 这个月和上个月比，餐饮支出多了多少？

[NL2SQL 生成]:
SELECT 
  SUM(CASE WHEN b.time >= DATE_FORMAT(NOW(), '%Y-%m-01') 
      THEN b.amount ELSE 0 END) as this_month,
  SUM(CASE WHEN b.time >= DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 MONTH), '%Y-%m-01')
      AND b.time < DATE_FORMAT(NOW(), '%Y-%m-01')
      THEN b.amount ELSE 0 END) as last_month
FROM t_bill b
JOIN t_category c ON b.category_id = c.id
WHERE b.user_id = '${user_id}'
AND b.deleted = 0
AND c.name = '餐饮'
AND b.type = 'expense';

[查询结果]: {"this_month": 1580.00, "last_month": 1280.00}

[回答]: 这个月餐饮支出 ¥1,580.00，上个月是 ¥1,280.00，
       多花了 ¥300.00，环比增长 23.4% 📈
       建议关注一下外卖和聚餐的频率哦～
```

### 示例 3: 家庭统计
```
用户: 家庭账单里谁花得最多？

[NL2SQL 生成]:
SELECT fm.nickname, SUM(b.amount) as total, COUNT(*) as count
FROM t_bill b
JOIN t_family_member fm ON b.user_id = fm.user_id
JOIN t_family f ON b.family_id = f.id AND fm.family_id = f.id
WHERE f.owner_id = '${user_id}'  -- 或通过 family_id 过滤
AND b.deleted = 0
AND fm.deleted = 0
AND b.family_id IS NOT NULL
AND b.time >= DATE_FORMAT(NOW(), '%Y-%m-01')
GROUP BY fm.nickname
ORDER BY total DESC;

[查询结果]: [
  {"nickname": "爸爸", "total": 3200.00, "count": 45},
  {"nickname": "妈妈", "total": 2800.00, "count": 38},
  {"nickname": "小明", "total": 580.00, "count": 12}
]

[回答]: 这个月家庭账单中：
       👨 爸爸: ¥3,200.00（45笔）
       👩 妈妈: ¥2,800.00（38笔）
       👦 小明: ¥580.00（12笔）
       
       爸爸这个月花得最多哦，主要是哪些方面的支出呢？
       可以查看家庭统计页面了解更详细的分类～
```

---

## 7. 安全风险与对策

| 风险 | 等级 | 对策 |
|------|------|------|
| SQL 注入 | 🔴 高 | 只读账号 + SQL审核 + 参数化查询 |
| 数据越权 | 🔴 高 | 强制 user_id 过滤 + 行级校验 |
| 敏感字段泄露 | 🟡 中 | 不暴露 t_user 表 + 字段黑名单 |
| 数据量泄露 | 🟡 中 | LIMIT 100 + 结果脱敏 |
| NL2SQL 生成错误 SQL | 🟡 中 | SQL 语法校验 + 审核节点拦截 |
| 恶意构造问题 | 🟢 低 | Gate Node 安全过滤 |

---

## 8. 成本估算

| 项目 | 单价 | 月调用量 | 月成本 |
|------|------|---------|--------|
| qwen-plus (NL2SQL) | ~¥0.004/千token | 5,000次 | ¥20-40 |
| qwen-plus (回答生成) | ~¥0.004/千token | 5,000次 | ¥20-40 |
| 只读数据库 | 已有MySQL实例 | - | ¥0 |
| 百炼工作流 | 包含在百炼套餐 | - | ¥0 |
| **总计** | | | **¥40-80/月** |

---

## 9. 实施步骤

### Step 1: 数据库准备
```sql
-- 1. 创建只读账号
-- 2. 授权 SELECT（不含 t_user）
-- 3. 确认数据库可从百炼平台访问（内网/公网）
```

### Step 2: 百炼平台配置
```
1. 创建智能体应用
2. 配置 NL2SQL 节点（连接信息 + Schema）
3. 配置 SQL 审核节点
4. 配置 LLM 回答生成节点
5. 配置工作流路由（知识类 vs 数据查询类）
```

### Step 3: 测试
```
1. 测试基本查询："我花了多少钱"
2. 测试分类查询："餐饮花了多少"
3. 测试时间查询："上个月的支出"
4. 测试对比查询："这个月和上个月比"
5. 测试边界："查询所有用户的数据" → 应被拦截
6. 测试注入："删除所有数据" → 应被拦截
```

### Step 4: App 端集成
```
1. 在「帮助与反馈」页面嵌入百炼对话组件
2. 传递 user_id 到百炼 Agent
3. 处理流式回答展示
```
