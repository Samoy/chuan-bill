-- ===============================
-- 小川记账数据库初始化脚本
-- 数据库名称：chuan_bill
-- 字符集：utf8mb4
-- ===============================

CREATE DATABASE IF NOT EXISTS `chuan_bill` DEFAULT CHARACTER
    SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `chuan_bill`;

-- ===============================
-- 用户表(t_user)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_user`
(
    `id`              VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '用户ID',
    `phone`           VARCHAR(11)             NOT NULL COMMENT '手机号',
    `password`        VARCHAR(128)                     DEFAULT NULL COMMENT '密码',
    `nickname`        VARCHAR(64)             NOT NULL COMMENT '昵称',
    `avatar`          VARCHAR(512)                     DEFAULT NULL COMMENT '头像',
    `gender`          TINYINT(1)                       DEFAULT 0 COMMENT '性别，0未知，1男，2女',
    `status`          TINYINT(1)                       DEFAULT 1 COMMENT '状态，0禁用，1启用',
    `last_login_time` DATETIME                         DEFAULT NULL COMMENT '最后登录时间',
    `create_time`     DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    UNIQUE KEY `idx_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ===============================
-- 类目表(t_category)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_category`
(
    `id`          VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '类目ID',
    `name`        VARCHAR(64)             NOT NULL COMMENT '类目名称',
    `icon`        VARCHAR(512)            NOT NULL COMMENT '类目图标',
    `type`        VARCHAR(20)             NOT NULL COMMENT '类目类型，income-收入，expense-支出',
    `sort_order`  INT                     NOT NULL DEFAULT 0 COMMENT '类目排序，越小越靠前',
    `is_default`  TINYINT(1)                       DEFAULT 0 COMMENT '是否默认类目，0否，1是',
    `user_id`     VARCHAR(64)                      DEFAULT NULL COMMENT '用户ID, 为空表示系统预设类目',
    `create_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    KEY `idx_type` (`type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='类目表';

-- ===============================
-- 支付方式表(t_payment_method)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_payment_method`
(
    `id`          VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '支付方式ID',
    `name`        VARCHAR(64)             NOT NULL COMMENT '支付方式名称',
    `icon`        VARCHAR(512)            NOT NULL COMMENT '支付方式图标',
    `sort_order`  INT                     NOT NULL DEFAULT 0 COMMENT '支付方式排序，越小越靠前',
    `is_default`  TINYINT(1)                       DEFAULT 0 COMMENT '是否默认支付方式，0否，1是',
    `user_id`     VARCHAR(64)                      DEFAULT NULL COMMENT '用户ID, 为空表示系统预设支付方式',
    `create_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='支付方式表';

-- ===============================
-- 家庭表(t_family)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_family`
(
    `id`          VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '家庭ID',
    `name`        VARCHAR(64)             NOT NULL COMMENT '家庭名称',
    `avatar`      VARCHAR(512)            NOT NULL COMMENT '家庭图标',
    `owner_id`    VARCHAR(64)             NOT NULL COMMENT '家庭户主ID',
    `invite_code` VARCHAR(64)             NOT NULL COMMENT '邀请码，用于加入家庭',
    `description` VARCHAR(256)            NOT NULL COMMENT '家庭描述',
    `create_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    UNIQUE KEY `idx_invite_code` (`invite_code`),
    KEY `idx_owner_id` (`owner_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='家庭表';

-- ===============================
-- 家庭成员表(t_family_member)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_family_member`
(
    `id`          VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '家庭成员ID',
    `family_id`   VARCHAR(64)             NOT NULL COMMENT '家庭ID',
    `user_id`     VARCHAR(64)             NOT NULL COMMENT '用户ID',
    `nickname`    VARCHAR(64)             NOT NULL COMMENT '家庭成员昵称',
    `is_owner`    TINYINT(1)                       DEFAULT 0 COMMENT '是否是户主，0否，1是',
    `join_time`   DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    UNIQUE KEY `idx_family_user` (`family_id`, `user_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_owner` (`is_owner`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='家庭成员表';


-- ===============================
-- 家庭加入申请表(t_family_join_apply)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_family_join_apply`
(
    `id`             VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '家庭加入申请ID',
    `family_id`      VARCHAR(64)             NOT NULL COMMENT '家庭ID',
    `user_id`        VARCHAR(64)             NOT NULL COMMENT '用户ID',
    `remark`         VARCHAR(256)            NOT NULL COMMENT '申请备注',
    `status`         TINYINT(1)                       DEFAULT 0 COMMENT '申请状态，0待处理，1同意，2拒绝',
    `handle_user_id` VARCHAR(64)             NOT NULL COMMENT '处理申请的用户ID',
    `handle_time`    DATETIME                         DEFAULT NULL COMMENT '处理时间',
    `create_time`    DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    KEY `idx_family_id` (`family_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='家庭加入申请表';

-- ===============================
-- 账单表(t_bill)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_bill`
(
    `id`                VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '账单ID',
    `user_id`           VARCHAR(64)             NOT NULL COMMENT '用户ID',
    `family_id`         VARCHAR(64)                      DEFAULT NULL COMMENT '家庭ID, 共享账单时填写',
    `name`              VARCHAR(64)             NOT NULL COMMENT '账单名称',
    `category_id`       VARCHAR(64)             NOT NULL COMMENT '类目ID',
    `payment_method_id` VARCHAR(64)             NOT NULL COMMENT '支付方式ID',
    `type`              VARCHAR(20)             NOT NULL COMMENT '账单类型，income-收入，expense-支出',
    `amount`            DECIMAL(12, 2)          NOT NULL COMMENT '金额',
    `time`              DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账单日期, 默认当前时间',
    `remark`            VARCHAR(256)            NOT NULL COMMENT '账单备注',
    `source`            VARCHAR(64)             NOT NULL COMMENT '账单来源，manual-手动添加，ocr-OCR识别, voice-语音输入，import-导入',
    `create_time`       DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_payment_method_id` (`payment_method_id`),
    KEY `idx_type` (`type`),
    KEY `idx_time` (`time`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_time` (`user_id`, `time`),
    KEY `idx_family_time` (`family_id`, `time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='账单表';

-- ===============================
-- 预算表(t_budget)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_budget`
(
    `id`          VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '预算ID',
    `user_id`     VARCHAR(64)             NOT NULL COMMENT '用户ID',
    `family_id`   VARCHAR(64)                      DEFAULT NULL COMMENT '家庭ID, 共享预算时填写',
    `month`       DATE                    NOT NULL COMMENT '预算月份(储存为当月第一天)',
    `amount`      DECIMAL(12, 2)          NOT NULL COMMENT '预算金额',
    `use_amount`  DECIMAL(12, 2)          NOT NULL COMMENT '已使用金额',
    `create_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id` (`family_id`),
    UNIQUE KEY `idx_user_month` (`user_id`, `month`),
    UNIQUE KEY `idx_family_month` (`family_id`, `month`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='预算表';

-- ===============================
-- 消息表(t_message)
-- ===============================
CREATE TABLE IF NOT EXISTS `t_message`
(
    `id`           VARCHAR(64) PRIMARY KEY NOT NULL COMMENT '消息ID',
    `user_id`      VARCHAR(64)             NOT NULL COMMENT '用户ID',
    `title`        VARCHAR(64)             NOT NULL COMMENT '消息标题',
    `content`      VARCHAR(256)            NOT NULL COMMENT '消息内容',
    `type`         VARCHAR(20)             NOT NULL COMMENT '消息类型，system-系统消息，family-家庭相关消息，bill-账单相关消息，budget-预算相关消息',
    `status`       TINYINT(1)                       DEFAULT 0 COMMENT '消息状态，0未读，1已读',
    `related_id`   VARCHAR(64)                      DEFAULT NULL COMMENT '相关ID，根据消息类型不同而不同(如家庭id，账单id，预算id等)',
    `related_type` VARCHAR(20)                      DEFAULT NULL COMMENT '相关类型，根据消息类型不同而不同: family-家庭，bill-账单，budget-预算',
    `create_time`  DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT(1)                       DEFAULT 0 COMMENT '是否删除，0未删除，1已删除',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='消息表';

-- ===============================
-- 初始化系统预设类目
-- ===============================

-- 支出类目
INSERT INTO `t_category` (`id`, `name`, `icon`, `type`, `sort_order`, `is_default`)
VALUES ('cat_exp_001', '餐饮', 'icon-food', 'expense', 1, 1),
       ('cat_exp_002', '购物', 'icon-shopping', 'expense', 2, 1),
       ('cat_exp_003', '交通', 'icon-transportation', 'expense', 3, 1),
       ('cat_exp_004', '娱乐', 'icon-fun', 'expense', 4, 1),
       ('cat_exp_005', '医疗', 'icon-hospital', 'expense', 5, 1),
       ('cat_exp_006', '教育', 'icon-education', 'expense', 6, 1),
       ('cat_exp_007', '其他支出', 'icon-other-expense', 'expense', 7, 1);
-- 收入类目
INSERT INTO `t_category` (`id`, `name`, `icon`, `type`, `sort_order`, `is_default`)
VALUES ('cat_inc_001', '工资', 'icon-salary', 'income', 1, 1),
       ('cat_inc_002', '奖金', 'icon-bonus', 'income', 2, 1),
       ('cat_inc_003', '投资', 'icon-investment', 'income', 3, 1),
       ('cat_inc_004', '兼职', 'icon-part-time', 'income', 4, 1),
       ('cat_inc_005', '礼金', 'icon-gift', 'income', 5, 1),
       ('cat_inc_006', '退款', 'icon-refund', 'income', 6, 1),
       ('cat_inc_007', '其他收入', 'icon-other-income', 'income', 7, 1);

-- ===============================
-- 初始化系统预设支付方式
-- ===============================
INSERT INTO `t_payment_method` (`id`, `name`, `icon`, `sort_order`, `is_default`)
VALUES ('pay_001', '微信', 'icon-wechat', 1, 1),
       ('pay_002', '支付宝', 'icon-alipay', 2, 1),
       ('pay_003', '现金', 'icon-cash', 3, 1),
       ('pay_004', '银行卡', 'icon-bank-card', 4, 1),
       ('pay_005', '信用卡', 'icon-credit-card', 5, 1),
       ('pay_006', '花呗', 'icon-huabei', 6, 1),
       ('pay_007', '其他', 'icon-other-payment', 7, 1);