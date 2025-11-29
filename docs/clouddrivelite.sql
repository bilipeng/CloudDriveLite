/*
 Navicat Premium Data Transfer

 Source Server         : mysqllocalhost
 Source Server Type    : MySQL
 Source Server Version : 80042
 Source Host           : localhost:3306
 Source Schema         : clouddrivelite

 Target Server Type    : MySQL
 Target Server Version : 80042
 File Encoding         : 65001

 Date: 29/11/2025 20:34:53
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file_object
-- ----------------------------
DROP TABLE IF EXISTS `file_object`;
CREATE TABLE `file_object`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `file_size` bigint NOT NULL,
  `file_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `stored_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `upload_time` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint NULL DEFAULT 0,
  `is_folder` bit(1) NOT NULL,
  `updated_time` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_object_parent_id`(`parent_id`) USING BTREE,
  INDEX `idx_file_object_user_parent`(`user_id`, `parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1685 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_object
-- ----------------------------
INSERT INTO `file_object` VALUES (4, '抽象.png', 'E:\\CloudDriveLite\\storage\\user_3\\2025\\09\\11\\474adb0d554f41f3848c85ec53f38296.png', 10056, 'image/png', '2025-09-11 07:19:52', '474adb0d554f41f3848c85ec53f38296.png', '2025-09-11 07:19:51.686877', 3, 0, b'0', NULL);
INSERT INTO `file_object` VALUES (8, '11.jpg', 'E:\\CloudDriveLite\\storage\\user_3\\2025\\09\\11\\3212511f186f457b99c2d2ab602adb87.jpg', 10174, 'image/jpeg', '2025-09-11 08:26:09', '3212511f186f457b99c2d2ab602adb87.jpg', '2025-09-11 08:26:08.771932', 3, 0, b'0', '2025-09-11 15:38:32.204657');
INSERT INTO `file_object` VALUES (13, 'images (1).jpg', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\11\\69e2ab9ab7e84b5298332c6f31c4e732.jpg', 10174, 'image/jpeg', '2025-09-11 15:09:05', '69e2ab9ab7e84b5298332c6f31c4e732.jpg', '2025-09-11 15:09:05.395788', 2, 0, b'0', '2025-09-11 15:09:05.395788');
INSERT INTO `file_object` VALUES (15, 'images (1).jpg', 'E:\\CloudDriveLite\\storage\\user_3\\2025\\09\\11\\7315a13ce9b645d4a3189c0968beac0f.jpg', 10174, 'image/jpeg', '2025-09-11 15:37:29', '7315a13ce9b645d4a3189c0968beac0f.jpg', '2025-09-11 15:37:28.562271', 3, 0, b'0', '2025-09-11 15:37:28.562271');
INSERT INTO `file_object` VALUES (16, '阿迪达斯.png', 'E:\\CloudDriveLite\\storage\\user_3\\2025\\09\\11\\9594604a5dd6472f969ca6e0c7fc96ad.png', 27325, 'image/png', '2025-09-11 15:37:37', '9594604a5dd6472f969ca6e0c7fc96ad.png', '2025-09-11 15:37:37.004901', 3, 0, b'0', '2025-09-11 15:37:37.004901');
INSERT INTO `file_object` VALUES (17, '点线面.png', 'E:\\CloudDriveLite\\storage\\user_3\\2025\\09\\11\\ce00e0a1b4e84ffa8683d13f8049b95d.png', 226965, 'image/png', '2025-09-11 15:37:41', 'ce00e0a1b4e84ffa8683d13f8049b95d.png', '2025-09-11 15:37:40.980414', 3, 0, b'0', '2025-09-11 15:37:40.980414');
INSERT INTO `file_object` VALUES (18, 'test', '', 0, 'folder', '2025-09-11 15:39:24', '', '2025-09-11 15:39:23.955515', 3, 0, b'1', '2025-09-11 15:39:23.955515');
INSERT INTO `file_object` VALUES (22, 'test2', '', 0, 'folder', '2025-09-12 00:26:31', '', '2025-09-12 00:26:31.284365', 2, 0, b'1', '2025-09-12 00:26:31.284365');
INSERT INTO `file_object` VALUES (24, 'images.jpg', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\12\\13bc9f72ed6746eda1713d1f7728e959.jpg', 8821, 'image/jpeg', '2025-09-12 01:37:04', '13bc9f72ed6746eda1713d1f7728e959.jpg', '2025-09-12 01:37:03.762467', 2, 22, b'0', '2025-09-12 01:37:03.762467');
INSERT INTO `file_object` VALUES (26, 'IMG_20240415_105237.jpg', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\12\\52dd384f134743cc9509adb4a1f97b17.jpg', 6073891, 'image/jpeg', '2025-09-12 01:44:27', '52dd384f134743cc9509adb4a1f97b17.jpg', '2025-09-12 01:44:27.042636', 2, 0, b'0', '2025-09-12 01:44:27.042636');
INSERT INTO `file_object` VALUES (27, 'openjfx-21.0.3_windows-x64_bin-sdk.zip', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\12\\00258d34e25b4b39a24820754ec88e70.zip', 48397393, 'application/zip', '2025-09-12 01:45:00', '00258d34e25b4b39a24820754ec88e70.zip', '2025-09-12 01:45:00.177513', 2, 0, b'0', '2025-09-12 01:45:00.177513');
INSERT INTO `file_object` VALUES (29, 'Dism++10.1.1002.1B.zip', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\12\\dc590ebe2a0f4900a36055cf7e0099de.zip', 3767974, 'application/zip', '2025-09-12 03:24:25', 'dc590ebe2a0f4900a36055cf7e0099de.zip', '2025-09-12 03:24:24.846670', 2, 0, b'0', '2025-09-12 03:24:24.846670');
INSERT INTO `file_object` VALUES (30, 'IMG_20240311_141219.jpg', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\15\\8b3ae65837a248e0b10f8217062e73c4.jpg', 3079732, 'image/jpeg', '2025-09-15 06:39:51', '8b3ae65837a248e0b10f8217062e73c4.jpg', '2025-09-15 06:39:51.222374', 2, 0, b'0', '2025-09-15 06:39:51.222374');
INSERT INTO `file_object` VALUES (35, '点面.png', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\19\\86f7b47ccd3348baa260651d3063fe5a.png', 53246, 'image/png', '2025-09-19 01:06:20', '86f7b47ccd3348baa260651d3063fe5a.png', '2025-09-19 01:06:19.542990', 2, 22, b'0', '2025-09-19 01:06:19.542990');
INSERT INTO `file_object` VALUES (36, '点面.png', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\19\\2e481934e27d403d8dd1331a012b4094.png', 53246, 'image/png', '2025-09-19 01:07:49', '2e481934e27d403d8dd1331a012b4094.png', '2025-09-19 01:07:48.781727', 2, 0, b'0', '2025-09-19 01:07:48.781727');
INSERT INTO `file_object` VALUES (37, 'IMG_20240415_105237.jpg', 'E:\\CloudDriveLite\\storage\\user_2\\2025\\09\\24\\26c8d331a0f540ffab011df6b4924ad4.jpg', 6073891, 'image/jpeg', '2025-09-24 02:28:58', '26c8d331a0f540ffab011df6b4924ad4.jpg', '2025-09-24 02:28:57.848519', 2, 0, b'0', '2025-09-24 02:28:57.848519');
INSERT INTO `file_object` VALUES (38, 'IMG20240514143536.jpg', 'E:\\CloudDriveLite\\storage\\user_5\\2025\\10\\26\\ed90942954f04aa79ce494c78c0c80fd.jpg', 262346, 'image/jpeg', '2025-10-26 04:43:00', 'ed90942954f04aa79ce494c78c0c80fd.jpg', '2025-10-26 04:43:00.067422', 5, 0, b'0', '2025-10-26 04:43:00.067422');
INSERT INTO `file_object` VALUES (39, '微信图片_20250321151628.jpg', 'E:\\CloudDriveLite\\storage\\user_5\\2025\\10\\26\\c10e5303f0c048b28a8f5c16d6be1cc0.jpg', 439919, 'image/jpeg', '2025-10-26 04:43:38', 'c10e5303f0c048b28a8f5c16d6be1cc0.jpg', '2025-10-26 04:43:38.047609', 5, 0, b'0', '2025-10-26 04:43:38.047609');
INSERT INTO `file_object` VALUES (1680, '1.mp4', 'E:\\CloudDriveLite\\storage\\user_6\\2025\\11\\04\\8701711ad3e34ad4ad30c279d07ef497.mp4', 12911276, 'video/mp4', '2025-11-04 00:48:11', '8701711ad3e34ad4ad30c279d07ef497.mp4', '2025-11-04 00:48:11.023763', 6, 0, b'0', '2025-11-04 00:48:11.023763');
INSERT INTO `file_object` VALUES (1681, 'test.mp4', 'E:\\CloudDriveLite\\storage\\user_6\\2025\\11\\04\\cd10dd4fb22a4a7884de9a4553705699.mp4', 1114499985, 'video/mp4', '2025-11-04 00:50:39', 'cd10dd4fb22a4a7884de9a4553705699.mp4', '2025-11-04 00:50:38.890915', 6, 0, b'0', '2025-11-04 00:51:04.852446');
INSERT INTO `file_object` VALUES (1682, 'test', '', 0, 'folder', '2025-11-04 00:51:21', '', '2025-11-04 00:51:21.255598', 6, 0, b'1', '2025-11-04 00:51:21.255598');
INSERT INTO `file_object` VALUES (1683, '20251104_084742.mp4', 'E:\\CloudDriveLite\\storage\\user_6\\2025\\11\\05\\8a7b86c32b95446fa8f494c338e1747c.mp4', 52787657, 'video/mp4', '2025-11-05 03:02:44', '8a7b86c32b95446fa8f494c338e1747c.mp4', '2025-11-05 03:02:44.313518', 6, 0, b'0', '2025-11-05 03:02:44.313518');
INSERT INTO `file_object` VALUES (1684, 'test1', '', 0, 'folder', '2025-11-11 03:12:29', '', '2025-11-11 03:12:28.544814', 6, 0, b'1', '2025-11-11 03:12:28.544814');
INSERT INTO `file_object` VALUES (1685, '20251104_084742.mp4', 'E:\\CloudDriveLite\\storage\\user_6\\2025\\11\\11\\1aa4e92507a1457baafd38001a8d619e.mp4', 52787657, 'video/mp4', '2025-11-11 03:12:46', '1aa4e92507a1457baafd38001a8d619e.mp4', '2025-11-11 03:12:46.243969', 6, 0, b'0', '2025-11-11 03:12:46.243969');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `file_dir` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `phone_number` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKr43af9ap4edm43mmtq01oddj6`(`username`) USING BTREE,
  UNIQUE INDEX `UKrlrd1rrfvwha8x0nagrn8ih33`(`user_number`) USING BTREE,
  UNIQUE INDEX `UKkwds03ohobcd8p6eowkw0f5bm`(`phone_number`) USING BTREE,
  UNIQUE INDEX `UKg3brvi3cpqs10ebdf6eqh7wv9`(`user_number`) USING BTREE,
  UNIQUE INDEX `UK9q63snka3mdh91as4io72espi`(`phone_number`) USING BTREE,
  UNIQUE INDEX `UK6dotkott2kjsp8vw4d0m25fb7`(`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, '2025-09-08 16:49:53.198271', 'zhangsan@example.com', 'user_u100001_1757350193198', '$2a$10$taVCPtSkgV7TQZhch071i.1Ehpkerc4tryEyQw8oFryfgaeL50WDa', '13800138000', '2025-09-08 16:49:53.198271', 'u100001', '一坨man', 1);
INSERT INTO `users` VALUES (2, '2025-09-09 04:02:58.474999', 'test@example.com', 'user_123456_1757390578474', '$2a$10$Q3ZPfLahm..L53xKJEbHc.3HkDe7IWf8UkeNzm/7DZHmLOmnRCuDG', '13800138001', '2025-09-09 04:02:58.474999', '123456', 'testUser', 1);
INSERT INTO `users` VALUES (3, '2025-09-10 11:00:26.092279', 'penghaiyi17@gmail.com', 'user_3313079517_1757502026092', '$2a$10$.T8zl2AqJT5qZrBXo61O7uZFDYSuaLOPgRyBeBHNPdi4vlUGoL78K', '13469403033', '2025-09-10 11:00:26.092279', '3313079517', 'phy', 1);
INSERT INTO `users` VALUES (4, '2025-09-17 01:19:18.132302', '123@test.com', 'user_112211_1758071958132', '$2a$10$Uia0h9cw5LVct1OxjjLosuqwxnx2xXO0haV4WRb3vygJZvHgjbJzG', '13469403035', '2025-09-17 01:19:18.132302', '112211', 'mini', 1);
INSERT INTO `users` VALUES (5, '2025-10-26 04:42:38.081875', NULL, 'user_331307_1761453758081', '$2a$10$ErP4HpsMpOa.BZrwl45s5etrZxnlEeopXJpYJtvVejZxtfKqF8ChW', '13469403038', '2025-10-26 04:42:38.081875', '331307', 'test', 1);
INSERT INTO `users` VALUES (6, '2025-10-29 01:41:00.474260', 'penghaiyi18@jhb.edu.kg', 'user_654321_1761702060474', '$2a$10$9phuhNPyH1yHolXZrcNoJeM2Bz6N01QPo9t/vQpc6DTau8zxwmfre', '13469403039', '2025-10-29 01:41:00.474260', '654321', 'penghaiyi', 1);

SET FOREIGN_KEY_CHECKS = 1;
