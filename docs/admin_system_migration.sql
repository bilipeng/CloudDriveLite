/*
 CloudDriveLite 管理员系统数据库迁移脚本
 Date: 2025-11-29
 
 功能：
 1. 用户角色管理（USER/ADMIN）
 2. 用户存储空间限制
 3. 登录日志记录
 4. 系统监控数据（实时计算，无需单独表）
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 修改 users 表：添加角色和存储空间字段
-- ============================================
ALTER TABLE `users` 
ADD COLUMN `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER' COMMENT '用户角色：USER-普通用户，ADMIN-管理员' AFTER `status`,
ADD COLUMN `max_storage` bigint NOT NULL DEFAULT 10737418240 COMMENT '最大存储空间（字节），默认10GB' AFTER `role`;

-- 为现有用户设置默认值
UPDATE `users` SET `role` = 'USER', `max_storage` = 10737418240 WHERE `role` IS NULL OR `role` = '';

-- 创建索引（可选，用于按角色查询）
CREATE INDEX `idx_users_role` ON `users`(`role`);

-- ============================================
-- 2. 创建登录日志表
-- ============================================
DROP TABLE IF EXISTS `login_log`;
CREATE TABLE `login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账号（冗余字段，便于查询）',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名（冗余字段）',
  `login_time` datetime(6) NOT NULL COMMENT '登录时间',
  `ip_address` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP地址（支持IPv6）',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户代理（浏览器信息）',
  `login_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '登录状态：SUCCESS-成功，FAILED-失败',
  `failure_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因（仅失败时记录）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_login_log_user_id`(`user_id`) USING BTREE,
  INDEX `idx_login_log_login_time`(`login_time`) USING BTREE,
  INDEX `idx_login_log_status`(`login_status`) USING BTREE,
  INDEX `idx_login_log_user_time`(`user_id`, `login_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '登录日志表' ROW_FORMAT = Dynamic;

-- ============================================
-- 3. 创建系统配置表（可选，用于存储系统级配置）
-- ============================================
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置值',
  `config_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'STRING' COMMENT '配置类型：STRING, NUMBER, BOOLEAN, JSON',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置说明',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- 插入默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('default_user_storage', '10737418240', 'NUMBER', '新用户默认存储空间（字节），默认10GB'),
('max_file_size', '104857600', 'NUMBER', '单文件最大上传大小（字节），默认100MB'),
('system_total_storage', '0', 'NUMBER', '系统总存储空间（字节），0表示不限制'),
('enable_user_registration', 'true', 'BOOLEAN', '是否允许用户注册');

-- ============================================
-- 4. 创建管理员账号（可选，手动执行）
-- ============================================
-- 注意：需要先手动生成密码的 BCrypt 哈希值
-- 示例：将某个现有用户设置为管理员
-- UPDATE `users` SET `role` = 'ADMIN' WHERE `id` = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 5. 数据验证查询
-- ============================================
-- 查看用户角色分布
-- SELECT role, COUNT(*) as count FROM users GROUP BY role;

-- 查看用户存储空间设置
-- SELECT id, username, user_number, role, max_storage / 1024 / 1024 / 1024 as max_storage_gb FROM users;

-- 查看最近的登录日志
-- SELECT * FROM login_log ORDER BY login_time DESC LIMIT 10;

