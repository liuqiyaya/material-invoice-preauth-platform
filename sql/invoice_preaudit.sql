/*
 Navicat Premium Dump SQL

 Source Server         : 我的本地数据库
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : invoice_preaudit

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 15/10/2026 19:58:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `nickname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for category_rule
-- ----------------------------
DROP TABLE IF EXISTS `category_rule`;
CREATE TABLE `category_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '品类名',
  `keywords` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关键词（逗号分隔）',
  `level` tinyint NOT NULL COMMENT '结论：1可以 2存疑 3不可以',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '品类长期规则' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for invoice
-- ----------------------------
DROP TABLE IF EXISTS `invoice`;
CREATE TABLE `invoice`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submitter_id` bigint NOT NULL COMMENT '提交人 id',
  `seller_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '销售方名称',
  `seller_tax_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '销售方纳税人识别号',
  `buyer_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '购买方名称',
  `buyer_tax_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '购买方税号',
  `invoice_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发票号码',
  `invoice_date` date NULL DEFAULT NULL COMMENT '开票日期',
  `total_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '价税合计',
  `submit_time` datetime NOT NULL COMMENT '提交审核时间',
  `week_start` date NULL DEFAULT NULL COMMENT '所属自然周周一',
  `qualify_status` tinyint NOT NULL DEFAULT 0 COMMENT '资质审核：0待审核 1待补凭证 2待人工处理 3通过 4不通过 5已取消',
  `finance_status` tinyint NOT NULL DEFAULT 0 COMMENT '财务提交：0未提交 1已提交',
  `reimburse_status` tinyint NOT NULL DEFAULT 0 COMMENT '最终报销：0未完成 1成功 2失败',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '不通过原因',
  `category_result` tinyint NULL DEFAULT NULL COMMENT '整体品类结论：1可以 2存疑 3不可以',
  `evidence_required` tinyint NOT NULL DEFAULT 0 COMMENT '是否被要求补凭证：0否 1是',
  `evidence_reason` tinyint NULL DEFAULT NULL COMMENT '补凭证原因：1低值品 2累计超1000',
  `need_manual` tinyint NOT NULL DEFAULT 0 COMMENT '是否需人工兜底：0否 1是',
  `manual_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人工备注',
  `source_file_id` bigint NULL DEFAULT NULL COMMENT '关联原始 PDF 文件 id',
  `ai_raw_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI 结构化原始结果 JSON',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_seller_week`(`seller_tax_no` ASC, `week_start` ASC, `qualify_status` ASC) USING BTREE,
  INDEX `idx_invoice_no`(`invoice_no` ASC) USING BTREE,
  INDEX `idx_submit_time`(`submit_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '发票主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for invoice_file
-- ----------------------------
DROP TABLE IF EXISTS `invoice_file`;
CREATE TABLE `invoice_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NULL DEFAULT NULL COMMENT '关联发票（ZIP 记录为 NULL）',
  `parent_file_id` bigint NULL DEFAULT NULL COMMENT '父文件：ZIP 解压出的 PDF 指向其 ZIP',
  `file_key` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储 Key',
  `original_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'PDF / ZIP / JPG / PNG',
  `file_size` bigint NULL DEFAULT NULL COMMENT '字节',
  `upload_batch` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '同一批次上传的批次号',
  `process_status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1处理中 2成功 3失败 4跳过',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_invoice`(`invoice_id` ASC) USING BTREE,
  INDEX `idx_batch`(`upload_batch` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_file_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '原始文件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for invoice_item
-- ----------------------------
DROP TABLE IF EXISTS `invoice_item`;
CREATE TABLE `invoice_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL COMMENT '所属发票',
  `item_name` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
  `quantity` decimal(12, 3) NULL DEFAULT NULL COMMENT '数量',
  `unit_price` decimal(12, 2) NULL DEFAULT NULL COMMENT '单价（用于判断资产/低值品）',
  `amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '金额',
  `category_level` tinyint NULL DEFAULT NULL COMMENT 'AI品类三态：1可以 2存疑 3不可以',
  `category_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '归入的品类名',
  `price_type` tinyint NULL DEFAULT NULL COMMENT '单价类别：1材料 2低值品 3资产',
  `ai_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AI判断理由',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_invoice`(`invoice_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '发票商品明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NULL DEFAULT NULL COMMENT '关联发票',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人id',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人名称',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作：AI_STRUCT/MANUAL_EDIT/STATUS_CHANGE/EVIDENCE_REVIEW等',
  `before_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '修改前值',
  `after_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '修改后值',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_invoice`(`invoice_id` ASC) USING BTREE,
  INDEX `idx_action`(`action` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 93 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for order_screenshot
-- ----------------------------
DROP TABLE IF EXISTS `order_screenshot`;
CREATE TABLE `order_screenshot`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL COMMENT '所属凭证组',
  `file_id` bigint NOT NULL COMMENT '关联 invoice_file.id',
  `original_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_group`(`group_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单截图' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for payment_evidence_group
-- ----------------------------
DROP TABLE IF EXISTS `payment_evidence_group`;
CREATE TABLE `payment_evidence_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL COMMENT '所属发票（一组只能属于一张）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '凭证组状态：0待上传 1待人工审核 2通过 3不通过',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人',
  `review_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `review_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_invoice`(`invoice_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付凭证组' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for payment_record
-- ----------------------------
DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL COMMENT '所属凭证组',
  `file_id` bigint NOT NULL COMMENT '关联 invoice_file.id',
  `original_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_group`(`group_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付记录' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
