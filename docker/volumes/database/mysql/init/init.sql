/*
 Navicat Premium Data Transfer

 Source Server         : 我的美国服务器
 Source Server Type    : MySQL
 Source Server Version : 50744
 Source Host           : 156.238.252.101:3306
 Source Schema         : skyway

 Target Server Type    : MySQL
 Target Server Version : 50744
 File Encoding         : 65001

 Date: 16/02/2026 12:18:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `table_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码生成业务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `column_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `sort` int(11) NULL DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码生成业务表字段' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------

-- ----------------------------
-- Table structure for mb_customer
-- ----------------------------
DROP TABLE IF EXISTS `mb_customer`;
CREATE TABLE `mb_customer`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键(customer_id)',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '密码',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `wechat` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '微信号',
  `qq` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'QQ号',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像URL',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态(0=正常 1=停用)',
  `register_time` datetime NULL DEFAULT NULL COMMENT '注册时间',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最近登录时间',
  `last_login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最近登录IP',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '会员客户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mb_customer
-- ----------------------------
INSERT INTO `mb_customer` VALUES (1, '1111', '$2a$10$.LKhwktXVorU2JoZ6/O2XeqBGaqpLrwCm4fBEXzNOeru7O9TS9.0O', '1942152752@qq.com', '2112', '', '', '', '0', '2026-02-14 14:50:19', '2026-02-15 17:08:49', '127.0.0.1', '', 'admin', '2026-02-14 14:50:19', 'admin', '2026-02-15 09:08:51');
INSERT INTO `mb_customer` VALUES (5, 'ewrwewe', '$2a$10$Ci/Ay7skQVAicbHVMuOGTu24d3p7liDYfFd8SaS4TbQBLCPsrRYzq', 'admin@topdemo.cn', NULL, NULL, NULL, NULL, '0', '2026-02-15 15:38:59', '2026-02-16 00:29:00', '127.0.0.1', NULL, NULL, '2026-02-15 15:38:59', '', '2026-02-15 16:29:02');

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob NULL COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Blob类型的触发器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_blob_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`, `calendar_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '日历信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_calendars
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Cron类型的触发器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_cron_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint(13) NOT NULL COMMENT '触发的时间',
  `sched_time` bigint(13) NOT NULL COMMENT '定时器制定的时间',
  `priority` int(11) NOT NULL COMMENT '优先级',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`, `entry_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '已触发的触发器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_fired_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务组名',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob NULL COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务详细信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_job_details
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`, `lock_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '存储的悲观锁信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_locks
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '暂停的触发器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_paused_trigger_grps
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint(13) NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint(13) NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`, `instance_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '调度器状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_scheduler_state
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint(7) NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint(12) NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint(10) NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '简单触发器的信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_simple_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int(11) NULL DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int(11) NULL DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint(20) NULL DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint(20) NULL DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13, 4) NULL DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13, 4) NULL DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '同步机制的行锁表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_simprop_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint(13) NULL DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint(13) NULL DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int(11) NULL DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发器的类型',
  `start_time` bigint(13) NOT NULL COMMENT '开始时间',
  `end_time` bigint(13) NULL DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint(2) NULL DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob NULL COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  INDEX `sched_name`(`sched_name`, `job_name`, `job_group`) USING BTREE,
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '触发器详细信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for res_category
-- ----------------------------
DROP TABLE IF EXISTS `res_category`;
CREATE TABLE `res_category`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父ID（0为根；分类建树，节点一般为0）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '名称',
  `order_num` int(4) NULL DEFAULT 0 COMMENT '排序',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '类型（1=分类 2=节点）',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '资源分类与节点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of res_category
-- ----------------------------
INSERT INTO `res_category` VALUES (2, 0, '狐蒂云', 0, '1', '2026-02-13 15:21:16', '2026-02-13 15:21:16');
INSERT INTO `res_category` VALUES (3, 0, '搬瓦工', 0, '1', '2026-02-13 15:21:35', '2026-02-13 15:21:35');
INSERT INTO `res_category` VALUES (4, 0, 'cloudcone', 0, '1', '2026-02-13 15:21:46', '2026-02-13 15:21:46');
INSERT INTO `res_category` VALUES (5, 2, '美国', 0, '1', '2026-02-13 15:22:19', '2026-02-13 15:22:19');

-- ----------------------------
-- Table structure for res_instance
-- ----------------------------
DROP TABLE IF EXISTS `res_instance`;
CREATE TABLE `res_instance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT 'VPS名称',
  `category_id` bigint(20) NULL DEFAULT NULL COMMENT '所属分类(res_category.type=1)',
  `node_id` bigint(20) NULL DEFAULT NULL COMMENT '所属节点(res_category.type=2)',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'IP',
  `cpu` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'CPU规格',
  `memory` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内存规格',
  `disk` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '磁盘规格',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'running' COMMENT '状态(res_instance_status)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `ssh_port` int(11) NULL DEFAULT 22 COMMENT 'SSH端口',
  `ssh_username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'SSH登录账号',
  `ssh_password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'SSH登录密码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'VPS实例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of res_instance
-- ----------------------------
INSERT INTO `res_instance` VALUES (1, '美国003（狐蒂云）', 5, NULL, '38.55.36.18', '2', '2', '10', 'running', '', '2026-02-13 15:32:03', '2026-02-14 15:20:07', 22, 'root', 'wangming1114');

-- ----------------------------
-- Table structure for res_proxy_node
-- ----------------------------
DROP TABLE IF EXISTS `res_proxy_node`;
CREATE TABLE `res_proxy_node`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance_id` bigint(20) NOT NULL COMMENT 'VPS实例ID(res_instance.id)',
  `customer_id` bigint(20) NULL DEFAULT NULL COMMENT '归属客户ID(mb_customer.id)',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '节点名称/备注',
  `node_type` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点类型(VLESS-REALITY等)',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '地址(IP/域名)',
  `port` int(11) NOT NULL COMMENT '端口',
  `url` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '完整分享链接',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '协议配置参数(JSON)',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '有效期(null=永久有效)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态(0=正常 1=停用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `custom_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '自定义用户ID(可空)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_instance_id`(`instance_id`) USING BTREE,
  INDEX `idx_customer_id`(`customer_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代理节点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of res_proxy_node
-- ----------------------------
INSERT INTO `res_proxy_node` VALUES (30, 1, 1, 'VLESS-REALITY-888-1-permanent', 'VLESS-REALITY', '38.55.36.18', 888, 'vless://f299de16-4234-4540-9318-bfae9ec1f11b@38.55.36.18:888?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.paypal.com&pbk=n3ViJTh1giILqSv3-namdP-WeHFTCPUdKRITMp_51mA&fp=chrome#VLESS-REALITY-888-1-permanent', '{\"protocol\":\"vless\",\"id\":\"f299de16-4234-4540-9318-bfae9ec1f11b\",\"flow\":\"xtls-rprx-vision\",\"network\":\"tcp\",\"security\":\"reality\",\"sni\":\"www.paypal.com\",\"fingerprint\":\"chrome\",\"publicKey\":\"n3ViJTh1giILqSv3-namdP-WeHFTCPUdKRITMp_51mA\"}', NULL, '0', NULL, NULL, '2026-02-15 08:16:11', '', '2026-02-15 08:16:11', '1');

-- ----------------------------
-- Table structure for res_proxy_node_traffic
-- ----------------------------
DROP TABLE IF EXISTS `res_proxy_node_traffic`;
CREATE TABLE `res_proxy_node_traffic`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `node_id` bigint(20) NOT NULL COMMENT '节点ID(res_proxy_node.id)',
  `stat_time` datetime NOT NULL COMMENT '统计时间点',
  `rx_delta` bigint(20) NULL DEFAULT 0 COMMENT '本周期下行增量(字节)',
  `tx_delta` bigint(20) NULL DEFAULT 0 COMMENT '本周期上行增量(字节)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_node_id_stat_time`(`node_id`, `stat_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 159 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代理节点流量明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of res_proxy_node_traffic
-- ----------------------------
INSERT INTO `res_proxy_node_traffic` VALUES (1, 25, '2026-02-15 15:03:41', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (2, 25, '2026-02-15 15:04:49', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (3, 25, '2026-02-15 15:05:00', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (4, 25, '2026-02-15 15:10:00', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (5, 25, '2026-02-15 15:11:57', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (6, 25, '2026-02-15 15:15:00', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (7, 25, '2026-02-15 15:16:31', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (8, 25, '2026-02-15 15:19:59', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (9, 25, '2026-02-15 15:20:03', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (10, 25, '2026-02-15 15:21:54', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (11, 25, '2026-02-15 15:25:00', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (12, 25, '2026-02-15 15:25:42', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (13, 25, '2026-02-15 15:27:45', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (14, 25, '2026-02-15 15:29:50', 183932, 45523);
INSERT INTO `res_proxy_node_traffic` VALUES (15, 25, '2026-02-15 15:30:00', 231, 521);
INSERT INTO `res_proxy_node_traffic` VALUES (16, 25, '2026-02-15 15:30:05', 1804, 3174);
INSERT INTO `res_proxy_node_traffic` VALUES (17, 25, '2026-02-15 15:34:48', 1003498, 24711974);
INSERT INTO `res_proxy_node_traffic` VALUES (18, 25, '2026-02-15 15:35:00', 2610136, 77925398);
INSERT INTO `res_proxy_node_traffic` VALUES (19, 25, '2026-02-15 15:35:34', 88828060, 31132141);
INSERT INTO `res_proxy_node_traffic` VALUES (39, 30, '2026-02-15 16:16:24', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (40, 30, '2026-02-15 16:16:43', 54208, 105586);
INSERT INTO `res_proxy_node_traffic` VALUES (41, 30, '2026-02-15 16:18:04', 113139265, 153403974);
INSERT INTO `res_proxy_node_traffic` VALUES (42, 30, '2026-02-15 16:20:00', 23163, 46224);
INSERT INTO `res_proxy_node_traffic` VALUES (43, 30, '2026-02-15 16:25:00', 49051, 87742);
INSERT INTO `res_proxy_node_traffic` VALUES (44, 30, '2026-02-15 16:30:00', 2982991, 10171670);
INSERT INTO `res_proxy_node_traffic` VALUES (45, 30, '2026-02-15 16:35:00', 1439830, 5578984);
INSERT INTO `res_proxy_node_traffic` VALUES (46, 30, '2026-02-15 16:40:00', 148705, 822387);
INSERT INTO `res_proxy_node_traffic` VALUES (47, 30, '2026-02-15 16:45:01', 59069, 121661);
INSERT INTO `res_proxy_node_traffic` VALUES (48, 30, '2026-02-15 16:50:00', 46989, 71663);
INSERT INTO `res_proxy_node_traffic` VALUES (49, 30, '2026-02-15 16:55:00', 16554, 17343);
INSERT INTO `res_proxy_node_traffic` VALUES (50, 30, '2026-02-15 17:00:00', 31315, 768404);
INSERT INTO `res_proxy_node_traffic` VALUES (51, 30, '2026-02-15 17:05:00', 187328, 134979);
INSERT INTO `res_proxy_node_traffic` VALUES (52, 30, '2026-02-15 17:10:00', 77083, 123349);
INSERT INTO `res_proxy_node_traffic` VALUES (53, 30, '2026-02-15 17:15:00', 50812, 98250);
INSERT INTO `res_proxy_node_traffic` VALUES (54, 30, '2026-02-15 17:20:20', 258869, 402316);
INSERT INTO `res_proxy_node_traffic` VALUES (55, 30, '2026-02-15 17:25:00', 94063, 137734);
INSERT INTO `res_proxy_node_traffic` VALUES (56, 30, '2026-02-15 17:30:00', 103119, 170401);
INSERT INTO `res_proxy_node_traffic` VALUES (57, 30, '2026-02-15 17:35:00', 389856, 429962);
INSERT INTO `res_proxy_node_traffic` VALUES (59, 30, '2026-02-15 17:40:01', 251823, 2015423);
INSERT INTO `res_proxy_node_traffic` VALUES (61, 30, '2026-02-15 17:45:00', 280036, 816747);
INSERT INTO `res_proxy_node_traffic` VALUES (63, 30, '2026-02-15 17:50:00', 42331, 51203);
INSERT INTO `res_proxy_node_traffic` VALUES (65, 30, '2026-02-15 17:55:00', 572176, 1887096);
INSERT INTO `res_proxy_node_traffic` VALUES (67, 30, '2026-02-15 18:00:00', 153311, 150274);
INSERT INTO `res_proxy_node_traffic` VALUES (69, 30, '2026-02-15 18:05:00', 713167, 710869);
INSERT INTO `res_proxy_node_traffic` VALUES (71, 30, '2026-02-15 18:10:00', 148968, 128117);
INSERT INTO `res_proxy_node_traffic` VALUES (73, 30, '2026-02-15 18:15:00', 207611, 332648);
INSERT INTO `res_proxy_node_traffic` VALUES (75, 30, '2026-02-15 18:20:00', 125242, 112114);
INSERT INTO `res_proxy_node_traffic` VALUES (77, 30, '2026-02-15 18:25:00', 106073, 103754);
INSERT INTO `res_proxy_node_traffic` VALUES (79, 30, '2026-02-15 18:30:00', 151885, 146082);
INSERT INTO `res_proxy_node_traffic` VALUES (81, 30, '2026-02-15 18:35:00', 827719, 1398464);
INSERT INTO `res_proxy_node_traffic` VALUES (83, 30, '2026-02-15 18:40:00', 257547, 282939);
INSERT INTO `res_proxy_node_traffic` VALUES (85, 30, '2026-02-15 18:45:00', 256087, 413393);
INSERT INTO `res_proxy_node_traffic` VALUES (87, 30, '2026-02-15 18:50:00', 232451, 278895);
INSERT INTO `res_proxy_node_traffic` VALUES (88, 30, '2026-02-15 18:55:00', 231531, 308810);
INSERT INTO `res_proxy_node_traffic` VALUES (89, 32, '2026-02-15 18:55:00', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (90, 30, '2026-02-15 19:00:00', 2408511, 38603261);
INSERT INTO `res_proxy_node_traffic` VALUES (91, 30, '2026-02-15 19:05:00', 208051, 94338);
INSERT INTO `res_proxy_node_traffic` VALUES (92, 30, '2026-02-15 19:10:00', 114315, 181033);
INSERT INTO `res_proxy_node_traffic` VALUES (93, 30, '2026-02-15 19:15:00', 98849, 173708);
INSERT INTO `res_proxy_node_traffic` VALUES (94, 30, '2026-02-15 19:20:00', 145073, 354205);
INSERT INTO `res_proxy_node_traffic` VALUES (95, 30, '2026-02-15 19:25:00', 82886, 153423);
INSERT INTO `res_proxy_node_traffic` VALUES (96, 30, '2026-02-15 19:30:00', 144320, 224479);
INSERT INTO `res_proxy_node_traffic` VALUES (97, 30, '2026-02-15 19:35:21', 176003, 193585);
INSERT INTO `res_proxy_node_traffic` VALUES (98, 30, '2026-02-15 19:40:21', 79925, 104699);
INSERT INTO `res_proxy_node_traffic` VALUES (99, 30, '2026-02-15 19:45:21', 75487, 128505);
INSERT INTO `res_proxy_node_traffic` VALUES (100, 30, '2026-02-15 19:50:00', 120029, 196158);
INSERT INTO `res_proxy_node_traffic` VALUES (101, 30, '2026-02-15 19:55:21', 74138, 105487);
INSERT INTO `res_proxy_node_traffic` VALUES (102, 30, '2026-02-15 20:00:21', 79965, 114163);
INSERT INTO `res_proxy_node_traffic` VALUES (103, 30, '2026-02-15 20:05:00', 417710, 604391);
INSERT INTO `res_proxy_node_traffic` VALUES (104, 30, '2026-02-15 20:10:00', 476364, 1866125);
INSERT INTO `res_proxy_node_traffic` VALUES (105, 30, '2026-02-15 20:15:00', 172653, 329658);
INSERT INTO `res_proxy_node_traffic` VALUES (106, 30, '2026-02-15 20:20:00', 2735535, 26917784);
INSERT INTO `res_proxy_node_traffic` VALUES (107, 30, '2026-02-15 20:25:00', 1266555, 4393271);
INSERT INTO `res_proxy_node_traffic` VALUES (108, 30, '2026-02-15 20:30:00', 1379988, 3823523);
INSERT INTO `res_proxy_node_traffic` VALUES (109, 30, '2026-02-15 20:35:00', 989069, 1967784);
INSERT INTO `res_proxy_node_traffic` VALUES (110, 30, '2026-02-15 20:40:00', 186688, 336632);
INSERT INTO `res_proxy_node_traffic` VALUES (111, 30, '2026-02-15 20:45:00', 476469, 3751816);
INSERT INTO `res_proxy_node_traffic` VALUES (112, 30, '2026-02-15 20:50:00', 154049, 238191);
INSERT INTO `res_proxy_node_traffic` VALUES (113, 30, '2026-02-15 20:55:00', 163353, 311589);
INSERT INTO `res_proxy_node_traffic` VALUES (114, 30, '2026-02-15 21:00:00', 98615, 284677);
INSERT INTO `res_proxy_node_traffic` VALUES (115, 30, '2026-02-15 21:05:00', 265762, 199889);
INSERT INTO `res_proxy_node_traffic` VALUES (116, 30, '2026-02-15 21:10:00', 116911, 282731);
INSERT INTO `res_proxy_node_traffic` VALUES (117, 30, '2026-02-15 21:15:00', 68870, 133587);
INSERT INTO `res_proxy_node_traffic` VALUES (118, 30, '2026-02-15 21:20:00', 95279, 212394);
INSERT INTO `res_proxy_node_traffic` VALUES (119, 30, '2026-02-15 21:25:00', 145876, 297882);
INSERT INTO `res_proxy_node_traffic` VALUES (120, 30, '2026-02-15 21:30:00', 553111, 3627150);
INSERT INTO `res_proxy_node_traffic` VALUES (121, 30, '2026-02-15 21:35:00', 275649, 299761);
INSERT INTO `res_proxy_node_traffic` VALUES (122, 30, '2026-02-15 21:40:00', 86632, 167106);
INSERT INTO `res_proxy_node_traffic` VALUES (123, 30, '2026-02-15 21:45:00', 48002, 65994);
INSERT INTO `res_proxy_node_traffic` VALUES (124, 30, '2026-02-15 21:50:00', 53215, 76043);
INSERT INTO `res_proxy_node_traffic` VALUES (125, 30, '2026-02-15 21:55:00', 107629, 185569);
INSERT INTO `res_proxy_node_traffic` VALUES (126, 30, '2026-02-15 22:00:00', 61545, 86838);
INSERT INTO `res_proxy_node_traffic` VALUES (127, 30, '2026-02-15 22:05:00', 162355, 209722);
INSERT INTO `res_proxy_node_traffic` VALUES (128, 30, '2026-02-15 22:10:00', 105542, 186734);
INSERT INTO `res_proxy_node_traffic` VALUES (129, 30, '2026-02-15 22:15:00', 125774, 241085);
INSERT INTO `res_proxy_node_traffic` VALUES (130, 30, '2026-02-15 22:20:00', 33444, 51993);
INSERT INTO `res_proxy_node_traffic` VALUES (131, 30, '2026-02-15 22:25:00', 155575, 278344);
INSERT INTO `res_proxy_node_traffic` VALUES (132, 30, '2026-02-15 22:30:00', 105741, 315203);
INSERT INTO `res_proxy_node_traffic` VALUES (133, 30, '2026-02-15 22:35:00', 227887, 200668);
INSERT INTO `res_proxy_node_traffic` VALUES (134, 30, '2026-02-15 22:40:00', 170930, 298191);
INSERT INTO `res_proxy_node_traffic` VALUES (135, 30, '2026-02-15 22:45:00', 45087, 76750);
INSERT INTO `res_proxy_node_traffic` VALUES (136, 30, '2026-02-15 22:50:00', 2111845, 2872836);
INSERT INTO `res_proxy_node_traffic` VALUES (137, 30, '2026-02-15 22:55:00', 295766, 412298);
INSERT INTO `res_proxy_node_traffic` VALUES (138, 30, '2026-02-15 23:00:21', 267977, 463455);
INSERT INTO `res_proxy_node_traffic` VALUES (139, 30, '2026-02-15 23:05:00', 236092, 196144);
INSERT INTO `res_proxy_node_traffic` VALUES (140, 30, '2026-02-15 23:10:00', 179650, 292521);
INSERT INTO `res_proxy_node_traffic` VALUES (141, 30, '2026-02-15 23:15:00', 336273, 727123);
INSERT INTO `res_proxy_node_traffic` VALUES (142, 30, '2026-02-15 23:20:00', 689644, 1657931);
INSERT INTO `res_proxy_node_traffic` VALUES (143, 30, '2026-02-15 23:25:00', 175355, 383843);
INSERT INTO `res_proxy_node_traffic` VALUES (144, 30, '2026-02-15 23:30:00', 33681, 51538);
INSERT INTO `res_proxy_node_traffic` VALUES (145, 30, '2026-02-15 23:35:00', 286176, 233524);
INSERT INTO `res_proxy_node_traffic` VALUES (146, 30, '2026-02-15 23:40:00', 43113, 60800);
INSERT INTO `res_proxy_node_traffic` VALUES (147, 30, '2026-02-15 23:45:20', 169281, 348032);
INSERT INTO `res_proxy_node_traffic` VALUES (148, 30, '2026-02-15 23:50:00', 711038, 5939302);
INSERT INTO `res_proxy_node_traffic` VALUES (149, 30, '2026-02-15 23:55:00', 43177, 99989);
INSERT INTO `res_proxy_node_traffic` VALUES (150, 30, '2026-02-16 00:00:00', 37242, 174445);
INSERT INTO `res_proxy_node_traffic` VALUES (151, 30, '2026-02-16 00:05:00', 206873, 339349);
INSERT INTO `res_proxy_node_traffic` VALUES (152, 30, '2026-02-16 00:10:00', 707347, 2033920);
INSERT INTO `res_proxy_node_traffic` VALUES (153, 30, '2026-02-16 00:15:00', 5205279, 19683618);
INSERT INTO `res_proxy_node_traffic` VALUES (154, 30, '2026-02-16 00:20:39', 730032, 1037584);
INSERT INTO `res_proxy_node_traffic` VALUES (155, 30, '2026-02-16 00:25:00', 0, 7166);
INSERT INTO `res_proxy_node_traffic` VALUES (156, 30, '2026-02-16 00:30:00', 0, 21741);
INSERT INTO `res_proxy_node_traffic` VALUES (157, 30, '2026-02-16 00:35:00', 0, 0);
INSERT INTO `res_proxy_node_traffic` VALUES (158, 30, '2026-02-16 00:40:00', 0, 0);

-- ----------------------------
-- Table structure for res_proxy_node_traffic_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `res_proxy_node_traffic_snapshot`;
CREATE TABLE `res_proxy_node_traffic_snapshot`  (
  `node_id` bigint(20) NOT NULL COMMENT '节点ID(res_proxy_node.id)',
  `last_rx` bigint(20) NULL DEFAULT 0 COMMENT '上次下行累计字节',
  `last_tx` bigint(20) NULL DEFAULT 0 COMMENT '上次上行累计字节',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`node_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代理节点流量快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of res_proxy_node_traffic_snapshot
-- ----------------------------
INSERT INTO `res_proxy_node_traffic_snapshot` VALUES (25, 92627661, 133818731, '2026-02-15 15:35:34');
INSERT INTO `res_proxy_node_traffic_snapshot` VALUES (30, 1358460987, 1214391969, '2026-02-16 00:40:00');
INSERT INTO `res_proxy_node_traffic_snapshot` VALUES (32, 0, 0, '2026-02-15 18:55:00');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_id` int(5) NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2026-02-13 14:18:37', '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2026-02-13 14:18:37', '', NULL, '初始化密码 123456');
INSERT INTO `sys_config` VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2026-02-13 14:18:37', '', NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` VALUES (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', 'admin', '2026-02-13 14:18:37', '', NULL, '是否开启验证码功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', '2026-02-13 14:18:37', '', NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y', 'admin', '2026-02-13 14:18:37', '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
INSERT INTO `sys_config` VALUES (7, '用户管理-初始密码修改策略', 'sys.account.initPasswordModify', '1', 'Y', 'admin', '2026-02-13 14:18:37', '', NULL, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
INSERT INTO `sys_config` VALUES (8, '用户管理-账号密码更新周期', 'sys.account.passwordValidateDays', '0', 'Y', 'admin', '2026-02-13 14:18:37', '', NULL, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `dept_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `order_num` int(4) NULL DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 110 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (100, 0, '0', 'Skyway', 0, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (101, 100, '0,100', '深圳总公司', 1, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (102, 100, '0,100', '长沙分公司', 2, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (103, 101, '0,100,101', '研发部门', 1, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (104, 101, '0,100,101', '市场部门', 2, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (105, 101, '0,100,101', '测试部门', 3, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (106, 101, '0,100,101', '财务部门', 4, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (107, 101, '0,100,101', '运维部门', 5, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (108, 102, '0,100,102', '市场部门', 1, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);
INSERT INTO `sys_dept` VALUES (109, 102, '0,100,102', '财务部门', 2, 'Skyway', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `dict_code` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int(4) NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '性别男');
INSERT INTO `sys_dict_data` VALUES (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '性别女');
INSERT INTO `sys_dict_data` VALUES (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '性别未知');
INSERT INTO `sys_dict_data` VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '显示菜单');
INSERT INTO `sys_dict_data` VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (8, 1, '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (9, 2, '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '默认分组');
INSERT INTO `sys_dict_data` VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '系统分组');
INSERT INTO `sys_dict_data` VALUES (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '系统默认是');
INSERT INTO `sys_dict_data` VALUES (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '系统默认否');
INSERT INTO `sys_dict_data` VALUES (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '通知');
INSERT INTO `sys_dict_data` VALUES (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '公告');
INSERT INTO `sys_dict_data` VALUES (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '关闭状态');
INSERT INTO `sys_dict_data` VALUES (18, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '其他操作');
INSERT INTO `sys_dict_data` VALUES (19, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '新增操作');
INSERT INTO `sys_dict_data` VALUES (20, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '修改操作');
INSERT INTO `sys_dict_data` VALUES (21, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '删除操作');
INSERT INTO `sys_dict_data` VALUES (22, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '授权操作');
INSERT INTO `sys_dict_data` VALUES (23, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '导出操作');
INSERT INTO `sys_dict_data` VALUES (24, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '导入操作');
INSERT INTO `sys_dict_data` VALUES (25, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '强退操作');
INSERT INTO `sys_dict_data` VALUES (26, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '生成操作');
INSERT INTO `sys_dict_data` VALUES (27, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '清空操作');
INSERT INTO `sys_dict_data` VALUES (28, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (29, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (30, 1, '分类', '1', 'res_category_type', '', 'primary', 'Y', '0', 'admin', '2026-02-13 15:16:13', '', NULL, '左侧树分类');
INSERT INTO `sys_dict_data` VALUES (31, 2, '节点', '2', 'res_category_type', '', 'default', 'N', '0', 'admin', '2026-02-13 15:16:13', '', NULL, '所属节点');
INSERT INTO `sys_dict_data` VALUES (32, 1, '运行中', 'running', 'res_instance_status', '', 'success', 'Y', '0', 'admin', '2026-02-13 15:16:14', '', NULL, '');
INSERT INTO `sys_dict_data` VALUES (33, 2, '已停止', 'stopped', 'res_instance_status', '', 'info', 'N', '0', 'admin', '2026-02-13 15:16:14', '', NULL, '');
INSERT INTO `sys_dict_data` VALUES (34, 3, '异常', 'abnormal', 'res_instance_status', '', 'danger', 'N', '0', 'admin', '2026-02-13 15:16:14', '', NULL, '');
INSERT INTO `sys_dict_data` VALUES (35, 1, '正常', '0', 'res_proxy_node_status', '', 'success', 'Y', '0', 'admin', '2026-02-14 06:57:11', '', NULL, '');
INSERT INTO `sys_dict_data` VALUES (36, 2, '停用', '1', 'res_proxy_node_status', '', 'danger', 'N', '0', 'admin', '2026-02-14 06:57:12', '', NULL, '');

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `dict_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (4, '任务状态', 'sys_job_status', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '任务状态列表');
INSERT INTO `sys_dict_type` VALUES (5, '任务分组', 'sys_job_group', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '任务分组列表');
INSERT INTO `sys_dict_type` VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (10, '系统状态', 'sys_common_status', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (11, '资源分类类型', 'res_category_type', '0', 'admin', '2026-02-13 15:16:13', '', NULL, '分类/节点');
INSERT INTO `sys_dict_type` VALUES (12, 'VPS实例状态', 'res_instance_status', '0', 'admin', '2026-02-13 15:16:13', '', NULL, '运行中/已停止/异常');
INSERT INTO `sys_dict_type` VALUES (13, '代理节点状态', 'res_proxy_node_status', '0', 'admin', '2026-02-14 06:57:11', '', NULL, '正常/停用');

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`  (
  `job_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务调度表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_job
-- ----------------------------
INSERT INTO `sys_job` VALUES (1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_job` VALUES (2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_job` VALUES (3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?', '3', '1', '1', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_job` VALUES (4, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '0 */5 * * * ?', '3', '1', '0', 'admin', '2026-02-15 06:56:41', '', NULL, '节点端口流量累计采集（每5分钟）');

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`  (
  `job_log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '异常信息',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 122 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务调度日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_job_log
-- ----------------------------
INSERT INTO `sys_job_log` VALUES (7, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4149毫秒', '0', '', '2026-02-15 07:52:28');
INSERT INTO `sys_job_log` VALUES (8, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3236毫秒', '0', '', '2026-02-15 07:52:39');
INSERT INTO `sys_job_log` VALUES (9, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3380毫秒', '0', '', '2026-02-15 07:53:09');
INSERT INTO `sys_job_log` VALUES (10, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3875毫秒', '0', '', '2026-02-15 07:55:06');
INSERT INTO `sys_job_log` VALUES (11, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3793毫秒', '0', '', '2026-02-15 08:00:06');
INSERT INTO `sys_job_log` VALUES (12, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4337毫秒', '0', '', '2026-02-15 08:00:52');
INSERT INTO `sys_job_log` VALUES (13, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3762毫秒', '0', '', '2026-02-15 08:02:40');
INSERT INTO `sys_job_log` VALUES (14, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3851毫秒', '0', '', '2026-02-15 08:05:06');
INSERT INTO `sys_job_log` VALUES (15, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4260毫秒', '0', '', '2026-02-15 08:10:06');
INSERT INTO `sys_job_log` VALUES (16, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：6428毫秒', '0', '', '2026-02-15 08:12:34');
INSERT INTO `sys_job_log` VALUES (17, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3627毫秒', '0', '', '2026-02-15 08:15:05');
INSERT INTO `sys_job_log` VALUES (18, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3850毫秒', '0', '', '2026-02-15 08:16:29');
INSERT INTO `sys_job_log` VALUES (19, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3338毫秒', '0', '', '2026-02-15 08:16:48');
INSERT INTO `sys_job_log` VALUES (20, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3610毫秒', '0', '', '2026-02-15 08:18:09');
INSERT INTO `sys_job_log` VALUES (21, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3411毫秒', '0', '', '2026-02-15 08:20:05');
INSERT INTO `sys_job_log` VALUES (22, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3918毫秒', '0', '', '2026-02-15 08:25:06');
INSERT INTO `sys_job_log` VALUES (23, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3155毫秒', '0', '', '2026-02-15 08:30:05');
INSERT INTO `sys_job_log` VALUES (24, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3757毫秒', '0', '', '2026-02-15 08:35:06');
INSERT INTO `sys_job_log` VALUES (25, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4683毫秒', '0', '', '2026-02-15 08:40:07');
INSERT INTO `sys_job_log` VALUES (26, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4254毫秒', '0', '', '2026-02-15 08:45:06');
INSERT INTO `sys_job_log` VALUES (27, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4618毫秒', '0', '', '2026-02-15 08:50:06');
INSERT INTO `sys_job_log` VALUES (28, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4833毫秒', '0', '', '2026-02-15 08:55:07');
INSERT INTO `sys_job_log` VALUES (29, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4011毫秒', '0', '', '2026-02-15 09:00:06');
INSERT INTO `sys_job_log` VALUES (30, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4283毫秒', '0', '', '2026-02-15 09:05:06');
INSERT INTO `sys_job_log` VALUES (31, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4113毫秒', '0', '', '2026-02-15 09:10:06');
INSERT INTO `sys_job_log` VALUES (32, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4046毫秒', '0', '', '2026-02-15 09:15:06');
INSERT INTO `sys_job_log` VALUES (33, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：23453毫秒', '0', '', '2026-02-15 09:20:25');
INSERT INTO `sys_job_log` VALUES (34, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5092毫秒', '0', '', '2026-02-15 09:25:07');
INSERT INTO `sys_job_log` VALUES (35, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4116毫秒', '0', '', '2026-02-15 09:30:06');
INSERT INTO `sys_job_log` VALUES (36, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4445毫秒', '0', '', '2026-02-15 09:35:06');
INSERT INTO `sys_job_log` VALUES (37, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5480毫秒', '0', '', '2026-02-15 09:40:07');
INSERT INTO `sys_job_log` VALUES (38, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4938毫秒', '0', '', '2026-02-15 09:45:07');
INSERT INTO `sys_job_log` VALUES (39, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5135毫秒', '0', '', '2026-02-15 09:50:07');
INSERT INTO `sys_job_log` VALUES (40, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4955毫秒', '0', '', '2026-02-15 09:55:07');
INSERT INTO `sys_job_log` VALUES (41, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4546毫秒', '0', '', '2026-02-15 10:00:06');
INSERT INTO `sys_job_log` VALUES (42, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4380毫秒', '0', '', '2026-02-15 10:05:06');
INSERT INTO `sys_job_log` VALUES (43, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4612毫秒', '0', '', '2026-02-15 10:10:07');
INSERT INTO `sys_job_log` VALUES (44, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4855毫秒', '0', '', '2026-02-15 10:15:07');
INSERT INTO `sys_job_log` VALUES (45, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4896毫秒', '0', '', '2026-02-15 10:20:07');
INSERT INTO `sys_job_log` VALUES (46, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5014毫秒', '0', '', '2026-02-15 10:25:07');
INSERT INTO `sys_job_log` VALUES (47, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4854毫秒', '0', '', '2026-02-15 10:30:07');
INSERT INTO `sys_job_log` VALUES (48, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4792毫秒', '0', '', '2026-02-15 10:35:07');
INSERT INTO `sys_job_log` VALUES (49, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4665毫秒', '0', '', '2026-02-15 10:40:07');
INSERT INTO `sys_job_log` VALUES (50, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4990毫秒', '0', '', '2026-02-15 10:45:07');
INSERT INTO `sys_job_log` VALUES (51, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4811毫秒', '0', '', '2026-02-15 10:50:07');
INSERT INTO `sys_job_log` VALUES (52, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4349毫秒', '0', '', '2026-02-15 10:55:06');
INSERT INTO `sys_job_log` VALUES (53, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4118毫秒', '0', '', '2026-02-15 11:00:06');
INSERT INTO `sys_job_log` VALUES (54, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3967毫秒', '0', '', '2026-02-15 11:05:06');
INSERT INTO `sys_job_log` VALUES (55, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4935毫秒', '0', '', '2026-02-15 11:10:07');
INSERT INTO `sys_job_log` VALUES (56, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3745毫秒', '0', '', '2026-02-15 11:15:06');
INSERT INTO `sys_job_log` VALUES (57, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3881毫秒', '0', '', '2026-02-15 11:20:06');
INSERT INTO `sys_job_log` VALUES (58, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3877毫秒', '0', '', '2026-02-15 11:25:06');
INSERT INTO `sys_job_log` VALUES (59, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4931毫秒', '0', '', '2026-02-15 11:30:07');
INSERT INTO `sys_job_log` VALUES (60, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：25706毫秒', '0', '', '2026-02-15 11:35:28');
INSERT INTO `sys_job_log` VALUES (61, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：28689毫秒', '0', '', '2026-02-15 11:40:31');
INSERT INTO `sys_job_log` VALUES (62, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：25827毫秒', '0', '', '2026-02-15 11:45:28');
INSERT INTO `sys_job_log` VALUES (63, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4290毫秒', '0', '', '2026-02-15 11:50:06');
INSERT INTO `sys_job_log` VALUES (64, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：25528毫秒', '0', '', '2026-02-15 11:55:28');
INSERT INTO `sys_job_log` VALUES (65, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：25338毫秒', '0', '', '2026-02-15 12:00:27');
INSERT INTO `sys_job_log` VALUES (66, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4993毫秒', '0', '', '2026-02-15 12:05:07');
INSERT INTO `sys_job_log` VALUES (67, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5047毫秒', '0', '', '2026-02-15 12:10:07');
INSERT INTO `sys_job_log` VALUES (68, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4848毫秒', '0', '', '2026-02-15 12:15:07');
INSERT INTO `sys_job_log` VALUES (69, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3943毫秒', '0', '', '2026-02-15 12:20:06');
INSERT INTO `sys_job_log` VALUES (70, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5148毫秒', '0', '', '2026-02-15 12:25:07');
INSERT INTO `sys_job_log` VALUES (71, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4154毫秒', '0', '', '2026-02-15 12:30:06');
INSERT INTO `sys_job_log` VALUES (72, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5428毫秒', '0', '', '2026-02-15 12:35:08');
INSERT INTO `sys_job_log` VALUES (73, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4044毫秒', '0', '', '2026-02-15 12:40:06');
INSERT INTO `sys_job_log` VALUES (74, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4020毫秒', '0', '', '2026-02-15 12:45:06');
INSERT INTO `sys_job_log` VALUES (75, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4735毫秒', '0', '', '2026-02-15 12:50:07');
INSERT INTO `sys_job_log` VALUES (76, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4782毫秒', '0', '', '2026-02-15 12:55:07');
INSERT INTO `sys_job_log` VALUES (77, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4670毫秒', '0', '', '2026-02-15 13:00:07');
INSERT INTO `sys_job_log` VALUES (78, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5306毫秒', '0', '', '2026-02-15 13:05:07');
INSERT INTO `sys_job_log` VALUES (79, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：8480毫秒', '0', '', '2026-02-15 13:10:11');
INSERT INTO `sys_job_log` VALUES (80, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4229毫秒', '0', '', '2026-02-15 13:15:06');
INSERT INTO `sys_job_log` VALUES (81, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4202毫秒', '0', '', '2026-02-15 13:20:06');
INSERT INTO `sys_job_log` VALUES (82, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4103毫秒', '0', '', '2026-02-15 13:25:06');
INSERT INTO `sys_job_log` VALUES (83, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3809毫秒', '0', '', '2026-02-15 13:30:06');
INSERT INTO `sys_job_log` VALUES (84, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3843毫秒', '0', '', '2026-02-15 13:35:06');
INSERT INTO `sys_job_log` VALUES (85, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4083毫秒', '0', '', '2026-02-15 13:40:06');
INSERT INTO `sys_job_log` VALUES (86, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5377毫秒', '0', '', '2026-02-15 13:45:08');
INSERT INTO `sys_job_log` VALUES (87, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4911毫秒', '0', '', '2026-02-15 13:50:07');
INSERT INTO `sys_job_log` VALUES (88, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3832毫秒', '0', '', '2026-02-15 13:55:06');
INSERT INTO `sys_job_log` VALUES (89, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3877毫秒', '0', '', '2026-02-15 14:00:06');
INSERT INTO `sys_job_log` VALUES (90, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4679毫秒', '0', '', '2026-02-15 14:05:07');
INSERT INTO `sys_job_log` VALUES (91, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5546毫秒', '0', '', '2026-02-15 14:10:08');
INSERT INTO `sys_job_log` VALUES (92, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4943毫秒', '0', '', '2026-02-15 14:15:07');
INSERT INTO `sys_job_log` VALUES (93, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4204毫秒', '0', '', '2026-02-15 14:20:06');
INSERT INTO `sys_job_log` VALUES (94, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4798毫秒', '0', '', '2026-02-15 14:25:07');
INSERT INTO `sys_job_log` VALUES (95, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5629毫秒', '0', '', '2026-02-15 14:30:08');
INSERT INTO `sys_job_log` VALUES (96, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5199毫秒', '0', '', '2026-02-15 14:35:07');
INSERT INTO `sys_job_log` VALUES (97, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5400毫秒', '0', '', '2026-02-15 14:40:08');
INSERT INTO `sys_job_log` VALUES (98, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：6320毫秒', '0', '', '2026-02-15 14:45:09');
INSERT INTO `sys_job_log` VALUES (99, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3979毫秒', '0', '', '2026-02-15 14:50:06');
INSERT INTO `sys_job_log` VALUES (100, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4326毫秒', '0', '', '2026-02-15 14:55:07');
INSERT INTO `sys_job_log` VALUES (101, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：26172毫秒', '0', '', '2026-02-15 15:00:28');
INSERT INTO `sys_job_log` VALUES (102, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5250毫秒', '0', '', '2026-02-15 15:05:07');
INSERT INTO `sys_job_log` VALUES (103, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：8112毫秒', '0', '', '2026-02-15 15:10:10');
INSERT INTO `sys_job_log` VALUES (104, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4246毫秒', '0', '', '2026-02-15 15:15:07');
INSERT INTO `sys_job_log` VALUES (105, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4392毫秒', '0', '', '2026-02-15 15:20:07');
INSERT INTO `sys_job_log` VALUES (106, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5593毫秒', '0', '', '2026-02-15 15:25:08');
INSERT INTO `sys_job_log` VALUES (107, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5534毫秒', '0', '', '2026-02-15 15:30:08');
INSERT INTO `sys_job_log` VALUES (108, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5307毫秒', '0', '', '2026-02-15 15:35:08');
INSERT INTO `sys_job_log` VALUES (109, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：6467毫秒', '0', '', '2026-02-15 15:40:09');
INSERT INTO `sys_job_log` VALUES (110, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：24106毫秒', '0', '', '2026-02-15 15:45:26');
INSERT INTO `sys_job_log` VALUES (111, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：6719毫秒', '0', '', '2026-02-15 15:50:09');
INSERT INTO `sys_job_log` VALUES (112, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5862毫秒', '0', '', '2026-02-15 15:55:08');
INSERT INTO `sys_job_log` VALUES (113, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4676毫秒', '0', '', '2026-02-15 16:00:07');
INSERT INTO `sys_job_log` VALUES (114, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5443毫秒', '0', '', '2026-02-15 16:05:08');
INSERT INTO `sys_job_log` VALUES (115, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4599毫秒', '0', '', '2026-02-15 16:10:07');
INSERT INTO `sys_job_log` VALUES (116, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：6794毫秒', '0', '', '2026-02-15 16:15:09');
INSERT INTO `sys_job_log` VALUES (117, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：42735毫秒', '0', '', '2026-02-15 16:20:45');
INSERT INTO `sys_job_log` VALUES (118, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：5846毫秒', '0', '', '2026-02-15 16:25:08');
INSERT INTO `sys_job_log` VALUES (119, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4577毫秒', '0', '', '2026-02-15 16:30:07');
INSERT INTO `sys_job_log` VALUES (120, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：3774毫秒', '0', '', '2026-02-15 16:35:06');
INSERT INTO `sys_job_log` VALUES (121, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '节点流量采集 总共耗时：4046毫秒', '0', '', '2026-02-15 16:40:06');

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor`  (
  `info_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '提示消息',
  `login_time` datetime NULL DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE,
  INDEX `idx_sys_logininfor_s`(`status`) USING BTREE,
  INDEX `idx_sys_logininfor_lt`(`login_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统访问记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_logininfor
-- ----------------------------
INSERT INTO `sys_logininfor` VALUES (1, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '退出成功', '2026-02-13 14:19:49');
INSERT INTO `sys_logininfor` VALUES (2, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-13 14:19:51');
INSERT INTO `sys_logininfor` VALUES (3, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-13 15:18:32');
INSERT INTO `sys_logininfor` VALUES (4, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-13 16:20:48');
INSERT INTO `sys_logininfor` VALUES (5, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-13 16:24:53');
INSERT INTO `sys_logininfor` VALUES (6, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '退出成功', '2026-02-13 17:11:46');
INSERT INTO `sys_logininfor` VALUES (7, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-13 17:36:25');
INSERT INTO `sys_logininfor` VALUES (8, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 05:04:31');
INSERT INTO `sys_logininfor` VALUES (9, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 06:10:59');
INSERT INTO `sys_logininfor` VALUES (10, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 06:58:18');
INSERT INTO `sys_logininfor` VALUES (11, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 08:04:04');
INSERT INTO `sys_logininfor` VALUES (12, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 09:33:38');
INSERT INTO `sys_logininfor` VALUES (13, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 13:31:44');
INSERT INTO `sys_logininfor` VALUES (14, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 14:34:53');
INSERT INTO `sys_logininfor` VALUES (15, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '退出成功', '2026-02-14 16:10:14');
INSERT INTO `sys_logininfor` VALUES (16, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 16:19:58');
INSERT INTO `sys_logininfor` VALUES (17, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '1', '验证码已失效', '2026-02-14 17:11:16');
INSERT INTO `sys_logininfor` VALUES (18, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '1', '验证码错误', '2026-02-14 17:11:19');
INSERT INTO `sys_logininfor` VALUES (19, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-14 17:11:22');
INSERT INTO `sys_logininfor` VALUES (20, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 05:57:14');
INSERT INTO `sys_logininfor` VALUES (21, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 06:18:04');
INSERT INTO `sys_logininfor` VALUES (22, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 06:22:00');
INSERT INTO `sys_logininfor` VALUES (23, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 06:28:12');
INSERT INTO `sys_logininfor` VALUES (24, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 06:40:25');
INSERT INTO `sys_logininfor` VALUES (25, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 06:47:14');
INSERT INTO `sys_logininfor` VALUES (26, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 06:51:27');
INSERT INTO `sys_logininfor` VALUES (27, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 06:57:34');
INSERT INTO `sys_logininfor` VALUES (28, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:03:22');
INSERT INTO `sys_logininfor` VALUES (29, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:11:31');
INSERT INTO `sys_logininfor` VALUES (30, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:16:18');
INSERT INTO `sys_logininfor` VALUES (31, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:19:48');
INSERT INTO `sys_logininfor` VALUES (32, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:21:47');
INSERT INTO `sys_logininfor` VALUES (33, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:25:34');
INSERT INTO `sys_logininfor` VALUES (34, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:27:39');
INSERT INTO `sys_logininfor` VALUES (35, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:29:44');
INSERT INTO `sys_logininfor` VALUES (36, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:33:52');
INSERT INTO `sys_logininfor` VALUES (37, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:44:44');
INSERT INTO `sys_logininfor` VALUES (38, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:52:16');
INSERT INTO `sys_logininfor` VALUES (39, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 07:59:07');
INSERT INTO `sys_logininfor` VALUES (40, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 08:10:14');
INSERT INTO `sys_logininfor` VALUES (41, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 09:02:48');
INSERT INTO `sys_logininfor` VALUES (42, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 09:10:45');
INSERT INTO `sys_logininfor` VALUES (43, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 09:23:08');
INSERT INTO `sys_logininfor` VALUES (44, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 10:54:46');
INSERT INTO `sys_logininfor` VALUES (45, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 15:25:32');
INSERT INTO `sys_logininfor` VALUES (46, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 15:32:32');
INSERT INTO `sys_logininfor` VALUES (47, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 16:06:47');
INSERT INTO `sys_logininfor` VALUES (48, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '退出成功', '2026-02-15 16:09:04');
INSERT INTO `sys_logininfor` VALUES (49, 'admin', '127.0.0.1', '内网IP', 'Chrome 145', 'Windows10', '0', '登录成功', '2026-02-15 16:11:22');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int(4) NULL DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '路由名称',
  `is_frame` int(1) NULL DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int(1) NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1061 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 1, 'system', NULL, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', '2026-02-13 14:18:37', '', NULL, '系统管理目录');
INSERT INTO `sys_menu` VALUES (2, '系统监控', 0, 2, 'monitor', NULL, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', '2026-02-13 14:18:37', '', NULL, '系统监控目录');
INSERT INTO `sys_menu` VALUES (3, '系统工具', 0, 3, 'tool', NULL, '', '', 1, 0, 'M', '0', '0', '', 'tool', 'admin', '2026-02-13 14:18:37', '', NULL, '系统工具目录');
INSERT INTO `sys_menu` VALUES (4, 'Skyway', 0, 4, 'https://github.com/skyway/Skyway-Vue', NULL, '', '', 0, 0, 'M', '0', '0', '', 'guide', 'admin', '2026-02-13 14:18:37', '', NULL, 'Skyway');
INSERT INTO `sys_menu` VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', '2026-02-13 14:18:37', '', NULL, '用户管理菜单');
INSERT INTO `sys_menu` VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', '2026-02-13 14:18:37', '', NULL, '角色管理菜单');
INSERT INTO `sys_menu` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', '2026-02-13 14:18:37', '', NULL, '菜单管理菜单');
INSERT INTO `sys_menu` VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', '2026-02-13 14:18:37', '', NULL, '部门管理菜单');
INSERT INTO `sys_menu` VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', '2026-02-13 14:18:37', '', NULL, '岗位管理菜单');
INSERT INTO `sys_menu` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', '2026-02-13 14:18:37', '', NULL, '字典管理菜单');
INSERT INTO `sys_menu` VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', '2026-02-13 14:18:37', '', NULL, '参数设置菜单');
INSERT INTO `sys_menu` VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 'admin', '2026-02-13 14:18:37', '', NULL, '通知公告菜单');
INSERT INTO `sys_menu` VALUES (108, '日志管理', 1, 9, 'log', '', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', '2026-02-13 14:18:37', '', NULL, '日志管理菜单');
INSERT INTO `sys_menu` VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', '2026-02-13 14:18:37', '', NULL, '在线用户菜单');
INSERT INTO `sys_menu` VALUES (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job', 'admin', '2026-02-13 14:18:37', '', NULL, '定时任务菜单');
INSERT INTO `sys_menu` VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list', 'druid', 'admin', '2026-02-13 14:18:37', '', NULL, '数据监控菜单');
INSERT INTO `sys_menu` VALUES (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', '', 1, 0, 'C', '0', '0', 'monitor:server:list', 'server', 'admin', '2026-02-13 14:18:37', '', NULL, '服务监控菜单');
INSERT INTO `sys_menu` VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 'admin', '2026-02-13 14:18:37', '', NULL, '缓存监控菜单');
INSERT INTO `sys_menu` VALUES (114, '缓存列表', 2, 6, 'cacheList', 'monitor/cache/list', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis-list', 'admin', '2026-02-13 14:18:37', '', NULL, '缓存列表菜单');
INSERT INTO `sys_menu` VALUES (115, '表单构建', 3, 1, 'build', 'tool/build/index', '', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 'admin', '2026-02-13 14:18:37', '', NULL, '表单构建菜单');
INSERT INTO `sys_menu` VALUES (116, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin', '2026-02-13 14:18:37', '', NULL, '代码生成菜单');
INSERT INTO `sys_menu` VALUES (117, '系统接口', 3, 3, 'swagger', 'tool/swagger/index', '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list', 'swagger', 'admin', '2026-02-13 14:18:37', '', NULL, '系统接口菜单');
INSERT INTO `sys_menu` VALUES (200, '资源管理', 0, 5, 'resource', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'tree', 'admin', '2026-02-13 15:16:14', '', NULL, '资源管理目录');
INSERT INTO `sys_menu` VALUES (201, 'VPS管理', 200, 1, 'vps', 'resource/vps/index', NULL, '', 1, 0, 'C', '0', '0', 'resource:vps:list', 'server', 'admin', '2026-02-13 15:16:14', '', NULL, 'VPS管理菜单');
INSERT INTO `sys_menu` VALUES (202, 'VPS查询', 201, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resource:vps:query', '#', 'admin', '2026-02-13 15:16:14', '', NULL, '');
INSERT INTO `sys_menu` VALUES (203, 'VPS新增', 201, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resource:vps:add', '#', 'admin', '2026-02-13 15:16:14', '', NULL, '');
INSERT INTO `sys_menu` VALUES (204, 'VPS修改', 201, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resource:vps:edit', '#', 'admin', '2026-02-13 15:16:15', '', NULL, '');
INSERT INTO `sys_menu` VALUES (205, 'VPS删除', 201, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resource:vps:remove', '#', 'admin', '2026-02-13 15:16:15', '', NULL, '');
INSERT INTO `sys_menu` VALUES (206, 'VPS导出', 201, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resource:vps:export', '#', 'admin', '2026-02-13 15:16:15', '', NULL, '');
INSERT INTO `sys_menu` VALUES (207, '代理节点', 200, 2, 'proxyNode', 'resource/vps/proxyNode/index', NULL, '', 1, 0, 'C', '0', '0', 'resource:vps:list', 'link', 'admin', '2026-02-14 16:13:57', '', NULL, '代理节点列表菜单');
INSERT INTO `sys_menu` VALUES (210, '用户中心', 0, 6, 'member', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'peoples', 'admin', '2026-02-14 14:32:08', '', NULL, '用户中心目录');
INSERT INTO `sys_menu` VALUES (211, '客户管理', 210, 1, 'customer', 'member/customer/index', NULL, '', 1, 0, 'C', '0', '0', 'member:customer:list', 'user', 'admin', '2026-02-14 14:32:08', '', NULL, '客户管理菜单');
INSERT INTO `sys_menu` VALUES (212, '客户查询', 211, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'member:customer:query', '#', 'admin', '2026-02-14 14:32:08', '', NULL, '');
INSERT INTO `sys_menu` VALUES (213, '客户新增', 211, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'member:customer:add', '#', 'admin', '2026-02-14 14:32:09', '', NULL, '');
INSERT INTO `sys_menu` VALUES (214, '客户修改', 211, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'member:customer:edit', '#', 'admin', '2026-02-14 14:32:09', '', NULL, '');
INSERT INTO `sys_menu` VALUES (215, '客户删除', 211, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'member:customer:remove', '#', 'admin', '2026-02-14 14:32:09', '', NULL, '');
INSERT INTO `sys_menu` VALUES (216, '重置密码', 211, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'member:customer:resetPwd', '#', 'admin', '2026-02-14 14:32:09', '', NULL, '');
INSERT INTO `sys_menu` VALUES (217, '客户导出', 211, 6, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'member:customer:export', '#', 'admin', '2026-02-14 16:16:36', '', NULL, '');
INSERT INTO `sys_menu` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', '2026-02-13 14:18:37', '', NULL, '操作日志菜单');
INSERT INTO `sys_menu` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', '2026-02-13 14:18:37', '', NULL, '登录日志菜单');
INSERT INTO `sys_menu` VALUES (1000, '用户查询', 100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1001, '用户新增', 100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1002, '用户修改', 100, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1003, '用户删除', 100, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1004, '用户导出', 100, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1005, '用户导入', 100, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1006, '重置密码', 100, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1007, '角色查询', 101, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1008, '角色新增', 101, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1009, '角色修改', 101, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1010, '角色删除', 101, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1011, '角色导出', 101, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1012, '菜单查询', 102, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1013, '菜单新增', 102, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1014, '菜单修改', 102, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1015, '菜单删除', 102, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1016, '部门查询', 103, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1017, '部门新增', 103, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1018, '部门修改', 103, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1019, '部门删除', 103, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1020, '岗位查询', 104, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1021, '岗位新增', 104, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1022, '岗位修改', 104, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1023, '岗位删除', 104, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1024, '岗位导出', 104, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1025, '字典查询', 105, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1026, '字典新增', 105, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1027, '字典修改', 105, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1028, '字典删除', 105, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1029, '字典导出', 105, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1030, '参数查询', 106, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1031, '参数新增', 106, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1032, '参数修改', 106, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1033, '参数删除', 106, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1034, '参数导出', 106, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1035, '公告查询', 107, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1036, '公告新增', 107, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1037, '公告修改', 107, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1038, '公告删除', 107, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1039, '操作查询', 500, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1040, '操作删除', 500, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1041, '日志导出', 500, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1042, '登录查询', 501, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1043, '登录删除', 501, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1044, '日志导出', 501, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1045, '账户解锁', 501, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1046, '在线查询', 109, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1047, '批量强退', 109, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1048, '单条强退', 109, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1049, '任务查询', 110, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1050, '任务新增', 110, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1051, '任务修改', 110, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1052, '任务删除', 110, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1053, '状态修改', 110, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1054, '任务导出', 110, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1055, '生成查询', 116, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1056, '生成修改', 116, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1057, '生成删除', 116, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1058, '导入代码', 116, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1059, '预览代码', 116, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1060, '生成代码', 116, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 'admin', '2026-02-13 14:18:37', '', NULL, '');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `notice_id` int(4) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob NULL COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (1, '温馨提醒：Skyway 系统', '2', 0xE696B0E78988E69CACE58685E5AEB9, '0', 'admin', '2026-02-13 14:18:37', '', NULL, '管理员');
INSERT INTO `sys_notice` VALUES (2, '维护通知：Skyway 系统维护', '1', 0xE7BBB4E68AA4E58685E5AEB9, '0', 'admin', '2026-02-13 14:18:37', '', NULL, '管理员');

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `oper_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '模块标题',
  `business_type` int(2) NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求方式',
  `operator_type` int(1) NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '返回参数',
  `status` int(1) NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint(20) NULL DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`) USING BTREE,
  INDEX `idx_sys_oper_log_bt`(`business_type`) USING BTREE,
  INDEX `idx_sys_oper_log_s`(`status`) USING BTREE,
  INDEX `idx_sys_oper_log_ot`(`oper_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 94 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------
INSERT INTO `sys_oper_log` VALUES (1, 'VPS分类/节点', 1, 'com.ruoyi.web.controller.resource.VpsCategoryController.add()', 'POST', 1, 'admin', '研发部门', '/resource/vps/category', '127.0.0.1', '内网IP', '{\"children\":[],\"id\":1,\"name\":\"美国节点\",\"orderNum\":0,\"params\":{},\"parentId\":0,\"type\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-13 15:19:17', 319);
INSERT INTO `sys_oper_log` VALUES (2, 'VPS分类/节点', 1, 'com.ruoyi.web.controller.resource.VpsCategoryController.add()', 'POST', 1, 'admin', '研发部门', '/resource/vps/category', '127.0.0.1', '内网IP', '{\"children\":[],\"id\":2,\"name\":\"狐蒂云\",\"orderNum\":0,\"params\":{},\"parentId\":0,\"type\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-13 15:21:16', 491);
INSERT INTO `sys_oper_log` VALUES (3, 'VPS分类/节点', 3, 'com.ruoyi.web.controller.resource.VpsCategoryController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/category/1', '127.0.0.1', '内网IP', '1 ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-13 15:21:20', 837);
INSERT INTO `sys_oper_log` VALUES (4, 'VPS分类/节点', 1, 'com.ruoyi.web.controller.resource.VpsCategoryController.add()', 'POST', 1, 'admin', '研发部门', '/resource/vps/category', '127.0.0.1', '内网IP', '{\"children\":[],\"id\":3,\"name\":\"搬瓦工\",\"orderNum\":0,\"params\":{},\"parentId\":0,\"type\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-13 15:21:36', 294);
INSERT INTO `sys_oper_log` VALUES (5, 'VPS分类/节点', 1, 'com.ruoyi.web.controller.resource.VpsCategoryController.add()', 'POST', 1, 'admin', '研发部门', '/resource/vps/category', '127.0.0.1', '内网IP', '{\"children\":[],\"id\":4,\"name\":\"cloudcone\",\"orderNum\":0,\"params\":{},\"parentId\":0,\"type\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-13 15:21:46', 294);
INSERT INTO `sys_oper_log` VALUES (6, 'VPS分类/节点', 1, 'com.ruoyi.web.controller.resource.VpsCategoryController.add()', 'POST', 1, 'admin', '研发部门', '/resource/vps/category', '127.0.0.1', '内网IP', '{\"children\":[],\"id\":5,\"name\":\"美国\",\"orderNum\":0,\"params\":{},\"parentId\":2,\"type\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-13 15:22:19', 294);
INSERT INTO `sys_oper_log` VALUES (7, 'VPS实例', 1, 'com.ruoyi.web.controller.resource.VpsInstanceController.add()', 'POST', 1, 'admin', '研发部门', '/resource/vps/instance', '127.0.0.1', '内网IP', '{\"categoryId\":5,\"cpu\":\"2\",\"disk\":\"10\",\"id\":1,\"ip\":\"38.12.4.132\",\"memory\":\"2\",\"name\":\"美国003（狐蒂云）\",\"params\":{},\"remark\":\"\",\"sshPassword\":\"wangming1114\",\"sshPort\":22,\"sshUsername\":\"root\",\"status\":\"running\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-13 15:32:03', 479);
INSERT INTO `sys_oper_log` VALUES (8, 'VPS实例', 2, 'com.ruoyi.web.controller.resource.VpsInstanceController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/instance', '127.0.0.1', '内网IP', '{\"categoryId\":2,\"categoryName\":\"美国\",\"cpu\":\"2\",\"createTime\":\"2026-02-13 15:32:03\",\"disk\":\"10\",\"id\":1,\"ip\":\"38.12.4.132\",\"memory\":\"2\",\"name\":\"美国003（狐蒂云）\",\"params\":{},\"remark\":\"\",\"sshPassword\":\"wangming1114\",\"sshPort\":22,\"sshUsername\":\"root\",\"status\":\"running\",\"updateTime\":\"2026-02-13 15:32:03\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-13 16:34:13', 326);
INSERT INTO `sys_oper_log` VALUES (9, 'VPS实例', 2, 'com.ruoyi.web.controller.resource.VpsInstanceController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/instance', '127.0.0.1', '内网IP', '{\"categoryId\":2,\"categoryName\":\"狐蒂云\",\"cpu\":\"2\",\"createTime\":\"2026-02-13 15:32:03\",\"disk\":\"10\",\"id\":1,\"ip\":\"38.55.36.18\",\"memory\":\"2\",\"name\":\"美国003（狐蒂云）\",\"params\":{},\"remark\":\"\",\"sshPassword\":\"wangming1114\",\"sshPort\":22,\"sshUsername\":\"root\",\"status\":\"running\",\"updateTime\":\"2026-02-13 16:34:13\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 05:22:55', 464);
INSERT INTO `sys_oper_log` VALUES (10, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/3', '127.0.0.1', '内网IP', '[3] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 08:22:54', 2197);
INSERT INTO `sys_oper_log` VALUES (11, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"96999a4a-c6de-4c51-b70f-6ca7f3b58544\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.ebay.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"tUYWqMtJ-dwEUibNNrl2o9QLkJNTvHerOuVhOr4r4U4\\\"}\",\"customId\":\"21\",\"expireTime\":\"2026-02-20 00:00:00\",\"id\":13,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-501-21-20260220\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":501,\"status\":\"1\",\"updateBy\":\"admin\",\"url\":\"vless://96999a4a-c6de-4c51-b70f-6ca7f3b58544@38.55.36.18:501?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.ebay.com&pbk=tUYWqMtJ-dwEUibNNrl2o9QLkJNTvHerOuVhOr4r4U4&fp=chrome#VLESS-REALITY-501-21-20260220\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 13:38:16', 2869);
INSERT INTO `sys_oper_log` VALUES (12, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"96999a4a-c6de-4c51-b70f-6ca7f3b58544\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.ebay.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"tUYWqMtJ-dwEUibNNrl2o9QLkJNTvHerOuVhOr4r4U4\\\"}\",\"customId\":\"21\",\"expireTime\":\"2026-02-20 00:00:00\",\"id\":13,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-501-21-20260220\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":501,\"status\":\"0\",\"updateBy\":\"admin\",\"url\":\"vless://96999a4a-c6de-4c51-b70f-6ca7f3b58544@38.55.36.18:501?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.ebay.com&pbk=tUYWqMtJ-dwEUibNNrl2o9QLkJNTvHerOuVhOr4r4U4&fp=chrome#VLESS-REALITY-501-21-20260220\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 13:39:47', 3434);
INSERT INTO `sys_oper_log` VALUES (13, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"96999a4a-c6de-4c51-b70f-6ca7f3b58544\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.ebay.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"tUYWqMtJ-dwEUibNNrl2o9QLkJNTvHerOuVhOr4r4U4\\\"}\",\"customId\":\"21\",\"expireTime\":\"2026-02-20 00:00:00\",\"id\":13,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-501-21-20260220\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":501,\"status\":\"1\",\"updateBy\":\"admin\",\"url\":\"vless://96999a4a-c6de-4c51-b70f-6ca7f3b58544@38.55.36.18:501?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.ebay.com&pbk=tUYWqMtJ-dwEUibNNrl2o9QLkJNTvHerOuVhOr4r4U4&fp=chrome#VLESS-REALITY-501-21-20260220\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 13:40:18', 3156);
INSERT INTO `sys_oper_log` VALUES (14, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"d12778cd-fa6f-4de2-80d9-b0cbc1bf5680\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.cloudflare.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"ENXYXA18jtRWW8DPhp9JgXF3QXsYonmMfJsCeGGc81E\\\"}\",\"id\":15,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-333-na-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":333,\"status\":\"1\",\"updateBy\":\"admin\",\"url\":\"vless://d12778cd-fa6f-4de2-80d9-b0cbc1bf5680@38.55.36.18:333?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.cloudflare.com&pbk=ENXYXA18jtRWW8DPhp9JgXF3QXsYonmMfJsCeGGc81E&fp=chrome#VLESS-REALITY-333-na-permanent\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 13:48:25', 5430);
INSERT INTO `sys_oper_log` VALUES (15, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"d12778cd-fa6f-4de2-80d9-b0cbc1bf5680\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.cloudflare.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"ENXYXA18jtRWW8DPhp9JgXF3QXsYonmMfJsCeGGc81E\\\"}\",\"id\":15,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-333-na-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":333,\"status\":\"0\",\"updateBy\":\"admin\",\"url\":\"vless://d12778cd-fa6f-4de2-80d9-b0cbc1bf5680@38.55.36.18:333?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.cloudflare.com&pbk=ENXYXA18jtRWW8DPhp9JgXF3QXsYonmMfJsCeGGc81E&fp=chrome#VLESS-REALITY-333-na-permanent\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 13:49:06', 5289);
INSERT INTO `sys_oper_log` VALUES (16, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"67c5a530-4bc7-494c-8030-4df9d174d540\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"aws.amazon.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"XPrvKqeEE32J41Ft7dCURPuGnOqfBvd5bX_S-JImrmI\\\"}\",\"id\":16,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-3434-na-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":3434,\"status\":\"1\",\"updateBy\":\"admin\",\"url\":\"vless://67c5a530-4bc7-494c-8030-4df9d174d540@38.55.36.18:3434?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=aws.amazon.com&pbk=XPrvKqeEE32J41Ft7dCURPuGnOqfBvd5bX_S-JImrmI&fp=chrome#VLESS-REALITY-3434-na-permanent\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 13:49:50', 6932);
INSERT INTO `sys_oper_log` VALUES (17, '会员客户', 1, 'com.ruoyi.web.controller.member.CustomerController.add()', 'POST', 1, 'admin', '研发部门', '/member/customer', '127.0.0.1', '内网IP', '{\"nickname\":\"1\",\"params\":{},\"remark\":\"\",\"role\":\"\",\"status\":\"0\",\"username\":\"12\"} ', '{\"msg\":\"密码不能为空\",\"code\":500}', 0, NULL, '2026-02-14 14:39:48', 314);
INSERT INTO `sys_oper_log` VALUES (18, '会员客户', 1, 'com.ruoyi.web.controller.member.CustomerController.add()', 'POST', 1, 'admin', '研发部门', '/member/customer', '127.0.0.1', '内网IP', '{\"nickname\":\"1\",\"params\":{},\"remark\":\"\",\"role\":\"\",\"status\":\"0\",\"username\":\"12\"} ', '{\"msg\":\"密码不能为空\",\"code\":500}', 0, NULL, '2026-02-14 14:39:48', 312);
INSERT INTO `sys_oper_log` VALUES (19, '会员客户', 1, 'com.ruoyi.web.controller.member.CustomerController.add()', 'POST', 1, 'admin', '研发部门', '/member/customer', '127.0.0.1', '内网IP', '{\"nickname\":\"1\",\"params\":{},\"remark\":\"\",\"role\":\"\",\"status\":\"0\",\"username\":\"12\"} ', '{\"msg\":\"密码不能为空\",\"code\":500}', 0, NULL, '2026-02-14 14:39:54', 151);
INSERT INTO `sys_oper_log` VALUES (20, '会员客户', 1, 'com.ruoyi.web.controller.member.CustomerController.add()', 'POST', 1, 'admin', '研发部门', '/member/customer', '127.0.0.1', '内网IP', '{\"nickname\":\"2323\",\"params\":{},\"remark\":\"2323\",\"role\":\"232323\",\"status\":\"0\",\"username\":\"3223\"} ', '{\"msg\":\"密码不能为空\",\"code\":500}', 0, NULL, '2026-02-14 14:40:05', 151);
INSERT INTO `sys_oper_log` VALUES (21, '会员客户', 1, 'com.ruoyi.web.controller.member.CustomerController.add()', 'POST', 1, 'admin', '研发部门', '/member/customer', '127.0.0.1', '内网IP', '{\"avatar\":\"\",\"createBy\":\"admin\",\"email\":\"1942152752@qq.com\",\"id\":1,\"params\":{},\"phone\":\"\",\"qq\":\"\",\"remark\":\"\",\"status\":\"0\",\"username\":\"1111\",\"wechat\":\"\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 14:50:19', 568);
INSERT INTO `sys_oper_log` VALUES (22, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.edit()', 'PUT', 1, 'admin', '研发部门', '/member/customer', '127.0.0.1', '内网IP', '{\"avatar\":\"\",\"createBy\":\"admin\",\"createTime\":\"2026-02-14 14:50:19\",\"email\":\"1942152752@qq.com\",\"id\":1,\"nodeBindCount\":0,\"params\":{},\"phone\":\"2112\",\"qq\":\"\",\"registerTime\":\"2026-02-14 14:50:19\",\"remark\":\"\",\"status\":\"0\",\"updateBy\":\"admin\",\"updateTime\":\"2026-02-14 14:50:19\",\"username\":\"1111\",\"wechat\":\"\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 14:50:47', 448);
INSERT INTO `sys_oper_log` VALUES (23, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.resetPwd()', 'PUT', 1, 'admin', '研发部门', '/member/customer/resetPwd', '127.0.0.1', '内网IP', '{\"id\":1,\"params\":{}} ', '{\"msg\":\"重置成功，新密码已生效\",\"code\":200}', 0, NULL, '2026-02-14 14:50:53', 373);
INSERT INTO `sys_oper_log` VALUES (24, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.changeStatus()', 'PUT', 1, 'admin', '研发部门', '/member/customer/changeStatus', '127.0.0.1', '内网IP', '{\"id\":1,\"params\":{},\"status\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 14:50:55', 291);
INSERT INTO `sys_oper_log` VALUES (25, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.changeStatus()', 'PUT', 1, 'admin', '研发部门', '/member/customer/changeStatus', '127.0.0.1', '内网IP', '{\"id\":1,\"params\":{},\"status\":\"0\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 14:53:59', 440);
INSERT INTO `sys_oper_log` VALUES (26, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"bc6ee9d1-d5c1-4222-a489-c6b811654012\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.cloudflare.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"k4eV3rb5eQ73WGY9KBfCfXBd4xV_gRGvev3kPE_ac1s\\\"}\",\"customId\":\"1\",\"customerId\":1,\"id\":18,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-999-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":999,\"status\":\"1\",\"updateBy\":\"admin\",\"url\":\"vless://bc6ee9d1-d5c1-4222-a489-c6b811654012@38.55.36.18:999?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.cloudflare.com&pbk=k4eV3rb5eQ73WGY9KBfCfXBd4xV_gRGvev3kPE_ac1s&fp=chrome#VLESS-REALITY-999-1-permanent\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:07:36', 5755);
INSERT INTO `sys_oper_log` VALUES (27, 'VPS实例', 2, 'com.ruoyi.web.controller.resource.VpsInstanceController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/instance', '127.0.0.1', '内网IP', '{\"categoryId\":5,\"categoryName\":\"狐蒂云\",\"cpu\":\"2\",\"createTime\":\"2026-02-13 15:32:03\",\"disk\":\"10\",\"id\":1,\"ip\":\"38.55.36.18\",\"memory\":\"2\",\"name\":\"美国003（狐蒂云）\",\"params\":{},\"remark\":\"\",\"sshPassword\":\"wangming1114\",\"sshPort\":22,\"sshUsername\":\"root\",\"status\":\"running\",\"updateTime\":\"2026-02-14 05:22:54\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:20:08', 317);
INSERT INTO `sys_oper_log` VALUES (28, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"bf1a75ca-ae95-4da4-9931-9693f99e1a84\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.cloudflare.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"Ue5N2BWWlJGkutHGgwlmpHRT3wRHfND_N67CyS4GxEA\\\"}\",\"customId\":\"1\",\"customerId\":1,\"id\":19,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-111-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":111,\"status\":\"1\",\"updateBy\":\"admin\",\"url\":\"vless://bf1a75ca-ae95-4da4-9931-9693f99e1a84@38.55.36.18:111?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.cloudflare.com&pbk=Ue5N2BWWlJGkutHGgwlmpHRT3wRHfND_N67CyS4GxEA&fp=chrome#VLESS-REALITY-111-1-permanent\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:49:59', 6120);
INSERT INTO `sys_oper_log` VALUES (29, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"bf1a75ca-ae95-4da4-9931-9693f99e1a84\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.cloudflare.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"Ue5N2BWWlJGkutHGgwlmpHRT3wRHfND_N67CyS4GxEA\\\"}\",\"customId\":\"1\",\"customerId\":1,\"id\":19,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-111-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":111,\"status\":\"0\",\"updateBy\":\"admin\",\"url\":\"vless://bf1a75ca-ae95-4da4-9931-9693f99e1a84@38.55.36.18:111?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.cloudflare.com&pbk=Ue5N2BWWlJGkutHGgwlmpHRT3wRHfND_N67CyS4GxEA&fp=chrome#VLESS-REALITY-111-1-permanent\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:50:20', 5402);
INSERT INTO `sys_oper_log` VALUES (30, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/19', '127.0.0.1', '内网IP', '[19] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:51:30', 7076);
INSERT INTO `sys_oper_log` VALUES (31, '代理节点', 1, 'com.ruoyi.web.controller.resource.VpsInstanceController.addProxyNode()', 'POST', 1, 'admin', '研发部门', '/resource/vps/instance/1/proxyNode', '127.0.0.1', '内网IP', '1 {\"customerId\":1,\"port\":333} ', '{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"1ab82388-3f23-425d-97ae-09dedd29d16f\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.cloudflare.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"q-ecL3cFhiuuAenbP-04kY8Zz1npDUbsr8p1DxxRelI\\\"}\",\"createBy\":\"admin\",\"customId\":\"1\",\"customerId\":1,\"id\":20,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-333-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":333,\"status\":\"0\",\"url\":\"vless://1ab82388-3f23-425d-97ae-09dedd29d16f@38.55.36.18:333?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.cloudflare.com&pbk=q-ecL3cFhiuuAenbP-04kY8Zz1npDUbsr8p1DxxRelI&fp=chrome#VLESS-REALITY-333-1-permanent\"}}', 0, NULL, '2026-02-14 15:51:46', 6019);
INSERT INTO `sys_oper_log` VALUES (32, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/20', '127.0.0.1', '内网IP', '[20] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:52:42', 7107);
INSERT INTO `sys_oper_log` VALUES (33, '代理节点', 1, 'com.ruoyi.web.controller.resource.VpsInstanceController.addProxyNode()', 'POST', 1, 'admin', '研发部门', '/resource/vps/instance/1/proxyNode', '127.0.0.1', '内网IP', '1 {\"customerId\":1,\"port\":555} ', '{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"a7acbed6-872d-403d-a57c-a79ff0200ce5\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.ebay.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"dopOe9lV-IiC3H70gzfGg4DXZd1p_XDvJuIfAMwxnGc\\\"}\",\"createBy\":\"admin\",\"customId\":\"1\",\"customerId\":1,\"id\":21,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-555-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":555,\"status\":\"0\",\"url\":\"vless://a7acbed6-872d-403d-a57c-a79ff0200ce5@38.55.36.18:555?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.ebay.com&pbk=dopOe9lV-IiC3H70gzfGg4DXZd1p_XDvJuIfAMwxnGc&fp=chrome#VLESS-REALITY-555-1-permanent\"}}', 0, NULL, '2026-02-14 15:58:00', 6255);
INSERT INTO `sys_oper_log` VALUES (34, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/21', '127.0.0.1', '内网IP', '[21] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:58:13', 5955);
INSERT INTO `sys_oper_log` VALUES (35, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"id\":21,\"params\":{},\"status\":\"1\"} ', '{\"msg\":\"节点不存在\",\"code\":500}', 0, NULL, '2026-02-14 15:58:15', 151);
INSERT INTO `sys_oper_log` VALUES (36, '代理节点', 1, 'com.ruoyi.web.controller.resource.VpsInstanceController.addProxyNode()', 'POST', 1, 'admin', '研发部门', '/resource/vps/instance/1/proxyNode', '127.0.0.1', '内网IP', '1 {\"customerId\":1,\"port\":55} ', '{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"7818cc18-b2ed-44c1-9b7c-56ca7bcf4988\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"aws.amazon.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"WlwcX1XY0C9OPcQLCMk5zYPXcRNoPnXfbSUcbRqhVWI\\\"}\",\"createBy\":\"admin\",\"customId\":\"1\",\"customerId\":1,\"id\":22,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-55-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":55,\"status\":\"0\",\"url\":\"vless://7818cc18-b2ed-44c1-9b7c-56ca7bcf4988@38.55.36.18:55?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=aws.amazon.com&pbk=WlwcX1XY0C9OPcQLCMk5zYPXcRNoPnXfbSUcbRqhVWI&fp=chrome#VLESS-REALITY-55-1-permanent\"}}', 0, NULL, '2026-02-14 15:58:27', 6803);
INSERT INTO `sys_oper_log` VALUES (37, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"7818cc18-b2ed-44c1-9b7c-56ca7bcf4988\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"aws.amazon.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"WlwcX1XY0C9OPcQLCMk5zYPXcRNoPnXfbSUcbRqhVWI\\\"}\",\"customId\":\"1\",\"customerId\":1,\"id\":22,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-55-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":55,\"status\":\"1\",\"updateBy\":\"admin\",\"url\":\"vless://7818cc18-b2ed-44c1-9b7c-56ca7bcf4988@38.55.36.18:55?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=aws.amazon.com&pbk=WlwcX1XY0C9OPcQLCMk5zYPXcRNoPnXfbSUcbRqhVWI&fp=chrome#VLESS-REALITY-55-1-permanent\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:58:34', 4853);
INSERT INTO `sys_oper_log` VALUES (38, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/22', '127.0.0.1', '内网IP', '[22] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:58:39', 2710);
INSERT INTO `sys_oper_log` VALUES (39, '代理节点', 2, 'com.ruoyi.web.controller.resource.ProxyNodeController.edit()', 'PUT', 1, 'admin', '研发部门', '/resource/vps/proxyNode', '127.0.0.1', '内网IP', '{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"38064fb9-1f9a-4bcb-a453-79af2f308677\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.ebay.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"2xY_ITv2YwiLR2at877HkvoN12DrGpgr3M9wftI2zHk\\\"}\",\"customId\":\"1\",\"customerId\":1,\"id\":23,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-555-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":555,\"status\":\"1\",\"updateBy\":\"admin\",\"url\":\"vless://38064fb9-1f9a-4bcb-a453-79af2f308677@38.55.36.18:555?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.ebay.com&pbk=2xY_ITv2YwiLR2at877HkvoN12DrGpgr3M9wftI2zHk&fp=chrome#VLESS-REALITY-555-1-permanent\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 15:59:09', 5833);
INSERT INTO `sys_oper_log` VALUES (40, '代理节点', 1, 'com.ruoyi.web.controller.resource.VpsInstanceController.addProxyNode()', 'POST', 1, 'admin', '研发部门', '/resource/vps/instance/1/proxyNode', '127.0.0.1', '内网IP', '1 {\"customerId\":1,\"port\":1212} ', '{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"46e38759-e1a0-4494-ab05-f0901a774be8\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"aws.amazon.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"RLUWxBD_y84LqHMdTv4cUnzuB7TCXUari4HJxIl14Gs\\\"}\",\"createBy\":\"admin\",\"customId\":\"1\",\"customerId\":1,\"id\":24,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-1212-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":1212,\"status\":\"0\",\"url\":\"vless://46e38759-e1a0-4494-ab05-f0901a774be8@38.55.36.18:1212?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=aws.amazon.com&pbk=RLUWxBD_y84LqHMdTv4cUnzuB7TCXUari4HJxIl14Gs&fp=chrome#VLESS-REALITY-1212-1-permanent\"}}', 0, NULL, '2026-02-14 17:12:01', 5893);
INSERT INTO `sys_oper_log` VALUES (41, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/24', '127.0.0.1', '内网IP', '[24] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-14 17:12:20', 5699);
INSERT INTO `sys_oper_log` VALUES (42, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 06:57:49', 599);
INSERT INTO `sys_oper_log` VALUES (43, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 06:58:48', 585);
INSERT INTO `sys_oper_log` VALUES (44, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:03:43', 601);
INSERT INTO `sys_oper_log` VALUES (45, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:04:50', 574);
INSERT INTO `sys_oper_log` VALUES (46, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:11:59', 596);
INSERT INTO `sys_oper_log` VALUES (47, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:16:33', 614);
INSERT INTO `sys_oper_log` VALUES (48, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:20:01', 616);
INSERT INTO `sys_oper_log` VALUES (49, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:21:56', 605);
INSERT INTO `sys_oper_log` VALUES (50, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:25:44', 633);
INSERT INTO `sys_oper_log` VALUES (51, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:27:47', 584);
INSERT INTO `sys_oper_log` VALUES (52, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:29:52', 622);
INSERT INTO `sys_oper_log` VALUES (53, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:30:07', 584);
INSERT INTO `sys_oper_log` VALUES (54, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:34:50', 653);
INSERT INTO `sys_oper_log` VALUES (55, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:35:35', 550);
INSERT INTO `sys_oper_log` VALUES (56, '定时任务调度日志', 3, 'com.ruoyi.quartz.controller.SysJobLogController.remove()', 'DELETE', 1, 'admin', '研发部门', '/monitor/jobLog/21,20,19,18,17,16,15,14,13,12', '127.0.0.1', '内网IP', '[21,20,19,18,17,16,15,14,13,12] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:35:42', 293);
INSERT INTO `sys_oper_log` VALUES (57, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/25', '127.0.0.1', '内网IP', '[25] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:36:20', 8339);
INSERT INTO `sys_oper_log` VALUES (58, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:38:07', 580);
INSERT INTO `sys_oper_log` VALUES (59, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:38:52', 585);
INSERT INTO `sys_oper_log` VALUES (60, '调度日志', 9, 'com.ruoyi.quartz.controller.SysJobLogController.clean()', 'DELETE', 1, 'admin', '研发部门', '/monitor/jobLog/clean', '127.0.0.1', '内网IP', '', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:38:59', 304);
INSERT INTO `sys_oper_log` VALUES (61, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/26', '127.0.0.1', '内网IP', '[26] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:45:19', 8820);
INSERT INTO `sys_oper_log` VALUES (62, '代理节点', 1, 'com.ruoyi.web.controller.resource.VpsInstanceController.addProxyNode()', 'POST', 1, 'admin', '研发部门', '/resource/vps/instance/1/proxyNode', '127.0.0.1', '内网IP', '1 {\"customerId\":1,\"port\":888} ', '{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"2ec51705-ff2b-44e5-a455-184379dbcb55\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"www.amazon.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"IdEmz_DFdCnjFmig9NPkFjehZghCxKX5iz88qnxFrlM\\\"}\",\"createBy\":\"admin\",\"customId\":\"1\",\"customerId\":1,\"id\":27,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-888-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":888,\"status\":\"0\",\"url\":\"vless://2ec51705-ff2b-44e5-a455-184379dbcb55@38.55.36.18:888?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.amazon.com&pbk=IdEmz_DFdCnjFmig9NPkFjehZghCxKX5iz88qnxFrlM&fp=chrome#VLESS-REALITY-888-1-permanent\"}}', 0, NULL, '2026-02-15 07:45:33', 9011);
INSERT INTO `sys_oper_log` VALUES (63, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:47:15', 623);
INSERT INTO `sys_oper_log` VALUES (64, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:47:36', 578);
INSERT INTO `sys_oper_log` VALUES (65, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:48:59', 579);
INSERT INTO `sys_oper_log` VALUES (66, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:52:24', 591);
INSERT INTO `sys_oper_log` VALUES (67, '定时任务调度日志', 3, 'com.ruoyi.quartz.controller.SysJobLogController.remove()', 'DELETE', 1, 'admin', '研发部门', '/monitor/jobLog/6,5,4,3,2,1', '127.0.0.1', '内网IP', '[6,5,4,3,2,1] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:52:31', 288);
INSERT INTO `sys_oper_log` VALUES (68, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:52:36', 574);
INSERT INTO `sys_oper_log` VALUES (69, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:53:06', 575);
INSERT INTO `sys_oper_log` VALUES (70, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/27', '127.0.0.1', '内网IP', '[27] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 07:59:24', 9423);
INSERT INTO `sys_oper_log` VALUES (71, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 08:00:48', 624);
INSERT INTO `sys_oper_log` VALUES (72, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 08:02:37', 720);
INSERT INTO `sys_oper_log` VALUES (73, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/28', '127.0.0.1', '内网IP', '[28] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 08:10:38', 10046);
INSERT INTO `sys_oper_log` VALUES (74, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 08:12:28', 612);
INSERT INTO `sys_oper_log` VALUES (75, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/29', '127.0.0.1', '内网IP', '[29] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 08:15:45', 9668);
INSERT INTO `sys_oper_log` VALUES (76, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 08:16:26', 559);
INSERT INTO `sys_oper_log` VALUES (77, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 08:16:45', 580);
INSERT INTO `sys_oper_log` VALUES (78, '定时任务', 2, 'com.ruoyi.quartz.controller.SysJobController.run()', 'PUT', 1, 'admin', '研发部门', '/monitor/job/run', '127.0.0.1', '内网IP', '{\"jobGroup\":\"DEFAULT\",\"jobId\":4,\"misfirePolicy\":\"0\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 08:18:06', 580);
INSERT INTO `sys_oper_log` VALUES (79, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.resetPwd()', 'PUT', 1, 'admin', '研发部门', '/member/customer/resetPwd', '127.0.0.1', '内网IP', '{\"id\":1,\"params\":{}} ', '{\"msg\":\"重置成功，新密码已生效\",\"code\":200}', 0, NULL, '2026-02-15 09:03:14', 383);
INSERT INTO `sys_oper_log` VALUES (80, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.changeStatus()', 'PUT', 1, 'admin', '研发部门', '/member/customer/changeStatus', '127.0.0.1', '内网IP', '{\"id\":2,\"params\":{},\"status\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 09:23:36', 314);
INSERT INTO `sys_oper_log` VALUES (81, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.changeStatus()', 'PUT', 1, 'admin', '研发部门', '/member/customer/changeStatus', '127.0.0.1', '内网IP', '{\"id\":2,\"params\":{},\"status\":\"0\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 09:23:50', 295);
INSERT INTO `sys_oper_log` VALUES (82, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.resetPwd()', 'PUT', 1, 'admin', '研发部门', '/member/customer/resetPwd', '127.0.0.1', '内网IP', '{\"id\":2,\"params\":{}} ', '{\"msg\":\"重置成功，新密码已生效\",\"code\":200}', 0, NULL, '2026-02-15 09:24:07', 377);
INSERT INTO `sys_oper_log` VALUES (83, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.changeStatus()', 'PUT', 1, 'admin', '研发部门', '/member/customer/changeStatus', '127.0.0.1', '内网IP', '{\"id\":2,\"params\":{},\"status\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 09:24:16', 294);
INSERT INTO `sys_oper_log` VALUES (84, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.changeStatus()', 'PUT', 1, 'admin', '研发部门', '/member/customer/changeStatus', '127.0.0.1', '内网IP', '{\"id\":2,\"params\":{},\"status\":\"0\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 09:24:32', 293);
INSERT INTO `sys_oper_log` VALUES (85, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.changeStatus()', 'PUT', 1, 'admin', '研发部门', '/member/customer/changeStatus', '127.0.0.1', '内网IP', '{\"id\":2,\"params\":{},\"status\":\"1\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 09:28:07', 297);
INSERT INTO `sys_oper_log` VALUES (86, '会员客户', 2, 'com.ruoyi.web.controller.member.CustomerController.changeStatus()', 'PUT', 1, 'admin', '研发部门', '/member/customer/changeStatus', '127.0.0.1', '内网IP', '{\"id\":2,\"params\":{},\"status\":\"0\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 09:28:18', 294);
INSERT INTO `sys_oper_log` VALUES (87, '会员客户', 3, 'com.ruoyi.web.controller.member.CustomerController.remove()', 'DELETE', 1, 'admin', '研发部门', '/member/customer/2', '127.0.0.1', '内网IP', '[2] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 09:28:29', 306);
INSERT INTO `sys_oper_log` VALUES (88, '代理节点', 1, 'com.ruoyi.web.controller.resource.VpsInstanceController.addProxyNode()', 'POST', 1, 'admin', '研发部门', '/resource/vps/instance/1/proxyNode', '127.0.0.1', '内网IP', '1 {\"customerId\":1,\"port\":30000} ', '{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"9c42d26a-8392-4210-b31a-46da28728b90\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"dash.cloudflare.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"BNZ1kQKiqzcWFBrM7zP8L_B2iem2WSc9CD42trgjcQU\\\"}\",\"createBy\":\"admin\",\"customId\":\"1\",\"customerId\":1,\"id\":31,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-30000-1-permanent\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":30000,\"status\":\"0\",\"url\":\"vless://9c42d26a-8392-4210-b31a-46da28728b90@38.55.36.18:30000?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=dash.cloudflare.com&pbk=BNZ1kQKiqzcWFBrM7zP8L_B2iem2WSc9CD42trgjcQU&fp=chrome#VLESS-REALITY-30000-1-permanent\"}}', 0, NULL, '2026-02-15 09:38:56', 7883);
INSERT INTO `sys_oper_log` VALUES (89, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/31', '127.0.0.1', '内网IP', '[31] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 09:39:09', 8145);
INSERT INTO `sys_oper_log` VALUES (90, '代理节点', 1, 'com.ruoyi.web.controller.resource.VpsInstanceController.addProxyNode()', 'POST', 1, 'admin', '研发部门', '/resource/vps/instance/1/proxyNode', '127.0.0.1', '内网IP', '1 {\"customerId\":1,\"port\":30000,\"expireTime\":\"2026-02-16 00:00:00\"} ', '{\"msg\":\"操作成功\",\"code\":200,\"data\":{\"address\":\"38.55.36.18\",\"configJson\":\"{\\\"protocol\\\":\\\"vless\\\",\\\"id\\\":\\\"2b48d86d-1c25-4682-b79a-a44d24b7812d\\\",\\\"flow\\\":\\\"xtls-rprx-vision\\\",\\\"network\\\":\\\"tcp\\\",\\\"security\\\":\\\"reality\\\",\\\"sni\\\":\\\"dash.cloudflare.com\\\",\\\"fingerprint\\\":\\\"chrome\\\",\\\"publicKey\\\":\\\"97yw_3lXYu7e8teRuTwFRrzdMorbAeNY4HArf1gWFEg\\\"}\",\"createBy\":\"admin\",\"customId\":\"1\",\"customerId\":1,\"expireTime\":\"2026-02-16 00:00:00\",\"id\":32,\"instanceId\":1,\"nodeName\":\"VLESS-REALITY-30000-1-20260216\",\"nodeType\":\"VLESS-REALITY\",\"params\":{},\"port\":30000,\"status\":\"0\",\"url\":\"vless://2b48d86d-1c25-4682-b79a-a44d24b7812d@38.55.36.18:30000?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=dash.cloudflare.com&pbk=97yw_3lXYu7e8teRuTwFRrzdMorbAeNY4HArf1gWFEg&fp=chrome#VLESS-REALITY-30000-1-20260216\"}}', 0, NULL, '2026-02-15 09:39:27', 8233);
INSERT INTO `sys_oper_log` VALUES (91, '代理节点', 3, 'com.ruoyi.web.controller.resource.ProxyNodeController.remove()', 'DELETE', 1, 'admin', '研发部门', '/resource/vps/proxyNode/32', '127.0.0.1', '内网IP', '[32] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 10:55:04', 9125);
INSERT INTO `sys_oper_log` VALUES (92, '会员客户', 3, 'com.ruoyi.web.controller.member.CustomerController.remove()', 'DELETE', 1, 'admin', '研发部门', '/member/customer/3', '127.0.0.1', '内网IP', '[3] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 15:32:38', 328);
INSERT INTO `sys_oper_log` VALUES (93, '会员客户', 3, 'com.ruoyi.web.controller.member.CustomerController.remove()', 'DELETE', 1, 'admin', '研发部门', '/member/customer/4', '127.0.0.1', '内网IP', '[4] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-02-15 15:35:25', 298);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `post_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int(4) NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '岗位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, 'ceo', '董事长', 1, '0', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_post` VALUES (2, 'se', '项目经理', 2, '0', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_post` VALUES (3, 'hr', '人力资源', 3, '0', 'admin', '2026-02-13 14:18:37', '', NULL, '');
INSERT INTO `sys_post` VALUES (4, 'user', '普通员工', 4, '0', 'admin', '2026-02-13 14:18:37', '', NULL, '');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int(4) NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '超级管理员');
INSERT INTO `sys_role` VALUES (2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', '2026-02-13 14:18:37', '', NULL, '普通角色');

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色和部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO `sys_role_dept` VALUES (2, 100);
INSERT INTO `sys_role_dept` VALUES (2, 101);
INSERT INTO `sys_role_dept` VALUES (2, 105);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (2, 1);
INSERT INTO `sys_role_menu` VALUES (2, 2);
INSERT INTO `sys_role_menu` VALUES (2, 3);
INSERT INTO `sys_role_menu` VALUES (2, 4);
INSERT INTO `sys_role_menu` VALUES (2, 100);
INSERT INTO `sys_role_menu` VALUES (2, 101);
INSERT INTO `sys_role_menu` VALUES (2, 102);
INSERT INTO `sys_role_menu` VALUES (2, 103);
INSERT INTO `sys_role_menu` VALUES (2, 104);
INSERT INTO `sys_role_menu` VALUES (2, 105);
INSERT INTO `sys_role_menu` VALUES (2, 106);
INSERT INTO `sys_role_menu` VALUES (2, 107);
INSERT INTO `sys_role_menu` VALUES (2, 108);
INSERT INTO `sys_role_menu` VALUES (2, 109);
INSERT INTO `sys_role_menu` VALUES (2, 110);
INSERT INTO `sys_role_menu` VALUES (2, 111);
INSERT INTO `sys_role_menu` VALUES (2, 112);
INSERT INTO `sys_role_menu` VALUES (2, 113);
INSERT INTO `sys_role_menu` VALUES (2, 114);
INSERT INTO `sys_role_menu` VALUES (2, 115);
INSERT INTO `sys_role_menu` VALUES (2, 116);
INSERT INTO `sys_role_menu` VALUES (2, 117);
INSERT INTO `sys_role_menu` VALUES (2, 500);
INSERT INTO `sys_role_menu` VALUES (2, 501);
INSERT INTO `sys_role_menu` VALUES (2, 1000);
INSERT INTO `sys_role_menu` VALUES (2, 1001);
INSERT INTO `sys_role_menu` VALUES (2, 1002);
INSERT INTO `sys_role_menu` VALUES (2, 1003);
INSERT INTO `sys_role_menu` VALUES (2, 1004);
INSERT INTO `sys_role_menu` VALUES (2, 1005);
INSERT INTO `sys_role_menu` VALUES (2, 1006);
INSERT INTO `sys_role_menu` VALUES (2, 1007);
INSERT INTO `sys_role_menu` VALUES (2, 1008);
INSERT INTO `sys_role_menu` VALUES (2, 1009);
INSERT INTO `sys_role_menu` VALUES (2, 1010);
INSERT INTO `sys_role_menu` VALUES (2, 1011);
INSERT INTO `sys_role_menu` VALUES (2, 1012);
INSERT INTO `sys_role_menu` VALUES (2, 1013);
INSERT INTO `sys_role_menu` VALUES (2, 1014);
INSERT INTO `sys_role_menu` VALUES (2, 1015);
INSERT INTO `sys_role_menu` VALUES (2, 1016);
INSERT INTO `sys_role_menu` VALUES (2, 1017);
INSERT INTO `sys_role_menu` VALUES (2, 1018);
INSERT INTO `sys_role_menu` VALUES (2, 1019);
INSERT INTO `sys_role_menu` VALUES (2, 1020);
INSERT INTO `sys_role_menu` VALUES (2, 1021);
INSERT INTO `sys_role_menu` VALUES (2, 1022);
INSERT INTO `sys_role_menu` VALUES (2, 1023);
INSERT INTO `sys_role_menu` VALUES (2, 1024);
INSERT INTO `sys_role_menu` VALUES (2, 1025);
INSERT INTO `sys_role_menu` VALUES (2, 1026);
INSERT INTO `sys_role_menu` VALUES (2, 1027);
INSERT INTO `sys_role_menu` VALUES (2, 1028);
INSERT INTO `sys_role_menu` VALUES (2, 1029);
INSERT INTO `sys_role_menu` VALUES (2, 1030);
INSERT INTO `sys_role_menu` VALUES (2, 1031);
INSERT INTO `sys_role_menu` VALUES (2, 1032);
INSERT INTO `sys_role_menu` VALUES (2, 1033);
INSERT INTO `sys_role_menu` VALUES (2, 1034);
INSERT INTO `sys_role_menu` VALUES (2, 1035);
INSERT INTO `sys_role_menu` VALUES (2, 1036);
INSERT INTO `sys_role_menu` VALUES (2, 1037);
INSERT INTO `sys_role_menu` VALUES (2, 1038);
INSERT INTO `sys_role_menu` VALUES (2, 1039);
INSERT INTO `sys_role_menu` VALUES (2, 1040);
INSERT INTO `sys_role_menu` VALUES (2, 1041);
INSERT INTO `sys_role_menu` VALUES (2, 1042);
INSERT INTO `sys_role_menu` VALUES (2, 1043);
INSERT INTO `sys_role_menu` VALUES (2, 1044);
INSERT INTO `sys_role_menu` VALUES (2, 1045);
INSERT INTO `sys_role_menu` VALUES (2, 1046);
INSERT INTO `sys_role_menu` VALUES (2, 1047);
INSERT INTO `sys_role_menu` VALUES (2, 1048);
INSERT INTO `sys_role_menu` VALUES (2, 1049);
INSERT INTO `sys_role_menu` VALUES (2, 1050);
INSERT INTO `sys_role_menu` VALUES (2, 1051);
INSERT INTO `sys_role_menu` VALUES (2, 1052);
INSERT INTO `sys_role_menu` VALUES (2, 1053);
INSERT INTO `sys_role_menu` VALUES (2, 1054);
INSERT INTO `sys_role_menu` VALUES (2, 1055);
INSERT INTO `sys_role_menu` VALUES (2, 1056);
INSERT INTO `sys_role_menu` VALUES (2, 1057);
INSERT INTO `sys_role_menu` VALUES (2, 1058);
INSERT INTO `sys_role_menu` VALUES (2, 1059);
INSERT INTO `sys_role_menu` VALUES (2, 1060);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint(20) NULL DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime NULL DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 103, 'admin', 'Skyway', '00', 'ry@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-02-16 00:11:20', '2026-02-13 14:18:37', 'admin', '2026-02-13 14:18:37', '', NULL, '管理员');
INSERT INTO `sys_user` VALUES (2, 105, 'ry', 'Skyway', '00', 'ry@qq.com', '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-02-13 14:18:37', '2026-02-13 14:18:37', 'admin', '2026-02-13 14:18:37', '', NULL, '测试员');

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户与岗位关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES (1, 1);
INSERT INTO `sys_user_post` VALUES (2, 2);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2);

SET FOREIGN_KEY_CHECKS = 1;
