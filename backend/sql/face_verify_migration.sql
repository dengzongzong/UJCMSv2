-- ============================================================
-- 考前人脸识别功能 数据库迁移脚本
-- 在已有生产数据库上执行本脚本即可，无需重建整个库
-- 执行方式: mysql -u用户名 -p exam_platform < face_verify_migration.sql
-- ============================================================

USE `exam_platform`;

-- 1. exam_record 表新增人脸验证相关字段
-- 先检查字段是否存在，不存在才新增(避免重复执行报错)
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'exam_platform' AND TABLE_NAME = 'exam_record'
      AND COLUMN_NAME = 'face_verify_status');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `exam_record`
        ADD COLUMN `face_verify_status` tinyint DEFAULT 0 COMMENT ''0-未验证 1-验证通过 2-验证失败 3-无需验证'',
        ADD COLUMN `face_verify_time` datetime DEFAULT NULL COMMENT ''人脸验证时间'',
        ADD COLUMN `face_similarity` decimal(5,4) DEFAULT NULL COMMENT ''人脸相似度(欧式距离，越小越相似)'',
        ADD KEY `idx_face_verify` (`face_verify_status`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 人脸验证日志表
CREATE TABLE IF NOT EXISTS `face_verify_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `exam_id` bigint NOT NULL COMMENT '考试ID',
  `record_id` bigint DEFAULT NULL COMMENT '考试记录ID',
  `verify_result` tinyint NOT NULL COMMENT '0-失败 1-成功',
  `similarity` decimal(5,4) DEFAULT NULL COMMENT '相似度(欧式距离)',
  `retry_count` int DEFAULT 0 COMMENT '重试次数',
  `id_photo_url` varchar(500) DEFAULT NULL COMMENT '证件照URL',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `device_info` varchar(255) DEFAULT NULL COMMENT '设备信息',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_exam_record` (`exam_id`, `record_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸验证日志表';

-- 3. system_setting 表插入人脸验证配置(使用 INSERT IGNORE 防止重复)
INSERT IGNORE INTO `system_setting` (`setting_key`, `setting_value`, `remark`) VALUES
('face_verify_enabled', '0', '考前人脸识别开关：0-关闭 1-开启'),
('face_verify_threshold', '0.6', '人脸比对阈值(欧式距离，越小越严格，建议0.4-0.6)'),
('face_verify_max_retries', '3', '人脸验证最大重试次数');
