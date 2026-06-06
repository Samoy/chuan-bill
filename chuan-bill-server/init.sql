-- ===============================
-- 小川记账数据库初始化脚本
-- 数据库名称：chuan_bill
-- 字符集：utf8mb4
-- ===============================
CREATE DATABASE IF NOT EXISTS `chuan_bill` DEFAULT CHARACTER
SET
  utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `chuan_bill`;
SET NAMES utf8mb4;

-- ===============================
-- 用户表(t_user)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_user` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '用户ID',
    `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
    `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信 openid',
    `password` VARCHAR(128) DEFAULT NULL COMMENT '密码',
    `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像',
    `gender` TINYINT (2) NOT NULL DEFAULT 0 COMMENT '性别，0未知，1男，2女',
    `status` TINYINT (1) NOT NULL DEFAULT 1 COMMENT '状态，0禁用，1启用',
    `is_vip` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否VIP，0否，1是',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_phone: 手机号登录查询
    -- 2. idx_openid: 微信 openid 登录查询，唯一索引
    -- 3. idx_create_time: 用户注册时间排序/统计
    KEY `idx_phone` (`phone`),
    UNIQUE KEY `idx_openid` (`openid`),
    KEY `idx_create_time` (`create_time`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ===============================
-- 类目表(t_category)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_category` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '类目ID',
    `name` VARCHAR(64) NOT NULL COMMENT '类目名称',
    `icon` VARCHAR(512) NOT NULL COMMENT '类目图标',
    `type` VARCHAR(20) NOT NULL COMMENT '类目类型，income-收入，expense-支出',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '类目排序，越小越靠前',
    `is_default` TINYINT (1) NOT NULL DEFAULT 1 COMMENT '是否默认类目，0否，1是',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户ID, 为空表示系统预设类目',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_user_type_sort: 查询用户某类型类目并按排序展示（最常用场景）
    -- 2. idx_user_id: 查询用户的所有类目
    KEY `idx_user_type_sort` (`user_id`, `type`, `sort_order`),
    KEY `idx_user_id` (`user_id`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '类目表';

-- ===============================
-- 支付方式表(t_payment_method)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_payment_method` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '支付方式ID',
    `name` VARCHAR(64) NOT NULL COMMENT '支付方式名称',
    `icon` VARCHAR(512) NOT NULL COMMENT '支付方式图标',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '支付方式排序，越小越靠前',
    `is_default` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否默认支付方式，0否，1是',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户ID, 为空表示系统预设支付方式',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_user_sort: 查询用户支付方式并按排序展示（最常用场景）
    -- 2. idx_user_id: 查询用户的所有支付方式
    KEY `idx_user_sort` (`user_id`, `sort_order`),
    KEY `idx_user_id` (`user_id`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付方式表';

-- ===============================
-- 家庭表(t_family)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_family` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '家庭ID',
    `name` VARCHAR(64) NOT NULL COMMENT '家庭名称',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '家庭图标',
    `owner_id` VARCHAR(64) NOT NULL COMMENT '家庭户主ID',
    `invite_code` VARCHAR(64) NOT NULL COMMENT '邀请码，用于加入家庭',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '家庭描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_invite_code: 通过邀请码查询家庭，唯一索引
    -- 2. idx_owner_id: 查询用户创建的家庭（MyBatis-Plus软删除会自动附加deleted条件）
    UNIQUE KEY `idx_invite_code` (`invite_code`),
    KEY `idx_owner_id` (`owner_id`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '家庭表';

-- ===============================
-- 家庭成员表(t_family_member)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_family_member` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '家庭成员ID',
    `family_id` VARCHAR(64) NOT NULL COMMENT '家庭ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `nickname` VARCHAR(64) NOT NULL COMMENT '家庭成员昵称',
    `is_owner` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否是户主，0否，1是',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_family_user: 查询某用户是否在某家庭中，唯一索引
    -- 2. idx_family_owner: 查询家庭的户主（高频操作：验证权限）
    -- 3. idx_user_id: 查询用户加入的所有家庭
    UNIQUE KEY `idx_family_user` (`family_id`, `user_id`),
    KEY `idx_family_owner` (`family_id`, `is_owner`),
    KEY `idx_user_id` (`user_id`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '家庭成员表';

-- ===============================
-- 家庭加入申请表(t_family_join_apply)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_family_join_apply` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '家庭加入申请ID',
    `family_id` VARCHAR(64) NOT NULL COMMENT '家庭ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `remark` VARCHAR(256) NOT NULL COMMENT '申请备注',
    `status` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '申请状态，0待处理，1同意，2拒绝',
    `handle_user_id` VARCHAR(64) NOT NULL COMMENT '处理申请的用户ID',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_family_status: 查询某家庭的待处理申请（高频：户主查看申请列表）
    -- 2. idx_user_id: 查询用户发出的所有申请
    -- 3. idx_create_time: 申请列表按时间排序
    KEY `idx_family_status` (`family_id`, `status`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '家庭加入申请表';

-- ===============================
-- 账单表(t_bill)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_bill` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '账单ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `family_id` VARCHAR(64) DEFAULT NULL COMMENT '家庭ID, 共享账单时填写',
    `name` VARCHAR(64) NOT NULL COMMENT '账单名称',
    `category_id` VARCHAR(64) NOT NULL COMMENT '类目ID',
    `payment_method_id` VARCHAR(64) NOT NULL COMMENT '支付方式ID',
    `type` VARCHAR(20) NOT NULL COMMENT '账单类型，income-收入，expense-支出',
    `amount` DECIMAL(12, 2) NOT NULL COMMENT '金额',
    `time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账单日期, 默认当前时间',
    `remark` VARCHAR(256) NOT NULL COMMENT '账单备注',
    `source` VARCHAR(64) NOT NULL COMMENT '账单来源，manual-手动添加，ocr-OCR识别, voice-语音输入，import-导入',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_user_type_time: 查询用户某类型账单的时间范围（最高频：统计收支）
    -- 2. idx_user_time: 查询用户账单按时间排序/筛选
    -- 3. idx_family_type_time: 查询家庭某类型账单的时间范围（家庭统计）
    -- 4. idx_family_time: 查询家庭账单按时间排序/筛选
    -- 5. idx_category_id: 按类目统计（如查看某类支出总额）
    -- 6. idx_payment_method_id: 按支付方式统计
    -- 7. idx_create_time: 账单列表按创建时间排序
    KEY `idx_user_type_time` (`user_id`, `type`, `time`),
    KEY `idx_user_time` (`user_id`, `time`),
    KEY `idx_family_type_time` (`family_id`, `type`, `time`),
    KEY `idx_family_time` (`family_id`, `time`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_payment_method_id` (`payment_method_id`),
    KEY `idx_create_time` (`create_time`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账单表';

-- ===============================
-- 预算表(t_budget)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_budget` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '预算ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `family_id` VARCHAR(64) DEFAULT NULL COMMENT '家庭ID, 共享预算时填写',
    `month` DATE NOT NULL COMMENT '预算月份(储存为当月第一天)',
    `amount` DECIMAL(12, 2) NOT NULL COMMENT '预算金额',
    `use_amount` DECIMAL(12, 2) NOT NULL COMMENT '已使用金额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_user_month: 查询用户某月预算，唯一索引（用户每月只有一个预算）
    -- 2. idx_family_month: 查询家庭某月预算，唯一索引（家庭每月只有一个预算）
    -- 注意：单字段idx_user_id/idx_family_id冗余，复合索引前缀已覆盖
    UNIQUE KEY `idx_user_month` (`user_id`, `month`),
    UNIQUE KEY `idx_family_month` (`family_id`, `month`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预算表';

-- ===============================
-- 消息表(t_message)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_message` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '消息ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `title` VARCHAR(64) NOT NULL COMMENT '消息标题',
    `content` VARCHAR(256) NOT NULL COMMENT '消息内容',
    `type` VARCHAR(20) NOT NULL COMMENT '消息类型，system-系统消息，family-家庭相关消息，bill-账单相关消息，budget-预算相关消息',
    `status` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '消息状态，0未读，1已读',
    `related_id` VARCHAR(64) DEFAULT NULL COMMENT '相关ID，根据消息类型不同而不同(如家庭id，账单id，预算id等)',
    `related_type` VARCHAR(20) DEFAULT NULL COMMENT '相关类型，根据消息类型不同而不同: family-家庭，bill-账单，budget-预算',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_user_status_time: 查询用户未读消息并按时间排序（最高频：消息列表）
    -- 2. idx_user_id: 查询用户的所有消息
    -- 3. idx_related: 根据相关ID查询消息（如查看某账单的所有消息）
    -- 4. idx_create_time: 消息列表按时间排序
    KEY `idx_user_status_time` (`user_id`, `status`, `create_time`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_related` (`related_id`, `related_type`),
    KEY `idx_create_time` (`create_time`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息表';

-- ===============================
-- 用户偏好设置表(t_user_preference)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_user_preference` (
    `id`          VARCHAR(64)  PRIMARY KEY NOT NULL COMMENT '主键ID',
    `user_id`     VARCHAR(64)  NOT NULL             COMMENT '用户ID',
    `pref_key`    VARCHAR(100) NOT NULL             COMMENT '偏好键名',
    `pref_value`  TEXT                              COMMENT '偏好值（JSON字符串）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_key` (`user_id`, `pref_key`),
    KEY `idx_user_id` (`user_id`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户偏好设置表';

-- ===============================
-- AI分析建议表(t_ai_suggestion)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_ai_suggestion`
(
  `id`            VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '主键ID',
  `user_id`       VARCHAR(64)             NOT NULL COMMENT '生成者ID；个人建议为用户ID，家庭建议为户主ID',
  `month`         VARCHAR(7)              NOT NULL COMMENT '月份，格式YYYY-MM',
  `analysis_type` TINYINT                 NOT NULL COMMENT '分析类型：1-个人，2-家庭',
  `target_id`     VARCHAR(64)             NOT NULL COMMENT '目标ID：个人为user_id，家庭为family_id',
  `content`       TEXT                    NOT NULL COMMENT 'AI分析内容',
  `create_time`   DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT(1)              NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',

  UNIQUE KEY `uk_user_month_target` (`user_id`, `month`, `analysis_type`, `target_id`),
  KEY `idx_user_month` (`user_id`, `month`),
  KEY `idx_target_month` (`analysis_type`, `target_id`, `month`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI分析建议表';

-- ===============================
-- AI使用次数统计表(t_ai_usage)
-- ===============================
CREATE TABLE
  IF NOT EXISTS `t_ai_usage` (
    `id` VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `usage_date` DATE NOT NULL COMMENT '使用日期',
    `analysis_count` INT NOT NULL DEFAULT 0 COMMENT '当日AI分析调用次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT (1) NOT NULL DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    -- 索引优化说明：
    -- 1. idx_user_date: 查询用户某日的AI使用次数，唯一索引（支持ON DUPLICATE KEY UPDATE原子递增）
    UNIQUE KEY `idx_user_date` (`user_id`, `usage_date`)
  ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI使用次数统计表';

-- ===============================
-- 初始化系统预设类目
-- ===============================
-- 支出类目
INSERT INTO
  `t_category` (
    `id`,
    `name`,
    `icon`,
    `type`,
    `sort_order`,
    `is_default`
  )
VALUES
  ('cat_exp_001', '餐饮', 'i-icon-park-outline:chopsticks-fork', 'expense', 1, 1),
  (
    'cat_exp_002',
    '购物',
    'i-mingcute:shopping-bag-1-line',
    'expense',
    2,
    1
  ),
  (
    'cat_exp_003',
    '交通',
    'i-mingcute:car-3-line',
    'expense',
    3,
    1
  ),
  ('cat_exp_004', '娱乐', 'i-mingcute:movie-line', 'expense', 4, 1),
  (
    'cat_exp_005',
    '居住',
    'i-lucide:house-plug',
    'expense',
    5,
    1
  ),
  (
    'cat_exp_006',
    '通讯',
    'i-lucide:smartphone-charging',
    'expense',
    6,
    1
  ),
  (
    'cat_exp_007',
    '医疗',
    'i-lucide:hospital',
    'expense',
    7,
    1
  ),(
    'cat_exp_008',
    '教育',
    'i-lucide:book-open',
    'expense',
    8,
    1
  ),(
    'cat_exp_009',
    '其他支出',
    'i-icon-park-outline:expenses',
    'expense',
    9,
    1
  );

-- 收入类目
INSERT INTO
  `t_category` (
    `id`,
    `name`,
    `icon`,
    `type`,
    `sort_order`,
    `is_default`
  )
VALUES
  (
    'cat_inc_001',
    '工资',
    'i-mingcute:currency-dollar-line',
    'income',
    1,
    1
  ),
  ('cat_inc_002', '奖金', 'i-material-symbols:money-bag-outline-rounded', 'income', 2, 1),
  (
    'cat_inc_003',
    '投资',
    'i-carbon:financial-assets',
    'income',
    3,
    1
  ),
  (
    'cat_inc_004',
    '兼职',
    'i-material-symbols:deployed-code-account-outline-rounded',
    'income',
    4,
    1
  ),
  ('cat_inc_005', '礼金', 'i-mingcute:gift-line', 'income', 5, 1),
  (
    'cat_inc_006',
    '退款',
    'i-mingcute:card-refund-line',
    'income',
    6,
    1
  ),
  (
    'cat_inc_007',
    '其他收入',
    'i-icon-park-outline:income',
    'income',
    7,
    1
  );

-- ===============================
-- 初始化系统预设支付方式
-- ===============================
INSERT INTO
  `t_payment_method` (`id`, `name`, `icon`, `sort_order`, `is_default`)
VALUES
  ('pay_001', '微信', 'i-mingcute:wechat-pay-line text-[#07c160]', 1, 1),
  ('pay_002', '支付宝', 'i-mingcute:alipay-line text-[#1677ff]', 2, 1),
  ('pay_003', '现金', 'i-mingcute:cash-line text-[#f1c305]', 3, 1),
  ('pay_004', '银行卡', 'i-mingcute:bank-card-line text-[#50f4ff]', 4, 1),
  ('pay_005', '信用卡', 'i-lucide:credit-card text-[#8ba6c5]', 5, 1),
  ('pay_006', '其他', 'i-icon-park-outline:payment-method text-[#ff4d4f]', 6, 1);
