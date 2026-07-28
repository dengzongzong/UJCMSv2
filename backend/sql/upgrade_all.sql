-- =====================================================================
-- 数据库完整升级脚本 - 一次性补齐所有缺失字段和表
-- 
-- 使用方法:
--   mysql -u root -p exam_platform < upgrade_all.sql
-- 
-- 本脚本仅包含DDL(建表/加字段/加索引),幂等可重复执行
-- 所有一次性数据修复SQL(UPDATE/INSERT/DELETE)已执行完毕并移除
-- =====================================================================

-- ============================================================
-- 1. 补齐缺失的表(整张表不存在时创建)
-- ============================================================

-- student_profession 表(学生-专业关联)
CREATE TABLE IF NOT EXISTS `student_profession` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `profession_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_profession` (`student_id`, `profession_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生-专业关联表';

-- question_template 表(题目模板)
CREATE TABLE IF NOT EXISTS `question_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '模板名称',
  `type` int NOT NULL COMMENT '题型: 1单选 2多选 3填空 4判断 5简答',
  `content` text COMMENT '题干模板',
  `options` text COMMENT '选项模板(JSON)',
  `answer` text COMMENT '答案模板',
  `analysis` text COMMENT '解析模板',
  `profession_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目模板表';

-- question_template_item 表(题目模板子项)
CREATE TABLE IF NOT EXISTS `question_template_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `content` text,
  `sort` int DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目模板子项表';

-- certificate_user 表(证书用户关联)
CREATE TABLE IF NOT EXISTS `certificate_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `certificate_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `student_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_certificate_id` (`certificate_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书用户关联表';

-- ============================================================
-- 2. 补齐各表缺失字段(用存储过程确保幂等)
-- ============================================================

-- 通用: 安全添加列的存储过程
DROP PROCEDURE IF EXISTS `safe_add_column`;
DELIMITER //
CREATE PROCEDURE `safe_add_column`(
  IN tbl VARCHAR(100),
  IN col VARCHAR(100),
  IN col_def VARCHAR(500)
)
BEGIN
  SET @col_count = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col);
  IF @col_count = 0 THEN
    SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', col_def);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

-- course 表
CALL safe_add_column('course', 'sort', 'INT NOT NULL DEFAULT 0 COMMENT ''排序''');
CALL safe_add_column('course', 'base_study_count', 'INT NOT NULL DEFAULT 0 COMMENT ''基础学过人数''');
CALL safe_add_column('course', 'base_study_hours', 'INT NOT NULL DEFAULT 0 COMMENT ''基础学时''');
CALL safe_add_column('course', 'is_top', 'INT DEFAULT 0 COMMENT ''是否置顶 0-否 1-是''');
CALL safe_add_column('course', 'top_sort', 'INT DEFAULT 0 COMMENT ''置顶排序(越小越靠前)''');

-- exam 表
CALL safe_add_column('exam', 'max_attempts', 'INT DEFAULT NULL COMMENT ''最大考试次数''');
CALL safe_add_column('exam', 'base_exam_count', 'INT NOT NULL DEFAULT 0 COMMENT ''基础考过人数''');

-- video 表
CALL safe_add_column('video', 'base_study_count', 'INT NOT NULL DEFAULT 0 COMMENT ''基础学过人数''');

-- certificate 表
CALL safe_add_column('certificate', 'theory_score', 'VARCHAR(50) DEFAULT NULL COMMENT ''理论成绩''');
CALL safe_add_column('certificate', 'practical_score', 'VARCHAR(50) DEFAULT NULL COMMENT ''实操成绩''');
CALL safe_add_column('certificate', 'comprehensive_evaluation', 'VARCHAR(200) DEFAULT NULL COMMENT ''综合评价''');
CALL safe_add_column('certificate', 'template_id', 'BIGINT DEFAULT NULL COMMENT ''证书模板ID''');
CALL safe_add_column('certificate', 'cert_url', 'VARCHAR(500) DEFAULT NULL COMMENT ''证书图片URL''');
CALL safe_add_column('certificate', 'student_no', 'VARCHAR(100) DEFAULT NULL COMMENT ''学员编号''');
CALL safe_add_column('certificate', 'skill_level', 'VARCHAR(50) DEFAULT NULL COMMENT ''技能等级''');
CALL safe_add_column('certificate', 'agency', 'VARCHAR(200) DEFAULT NULL COMMENT ''发证机构''');
CALL safe_add_column('certificate', 'cert_type', 'VARCHAR(100) DEFAULT NULL COMMENT ''证书类型''');

-- certificate_photo 表
CALL safe_add_column('certificate_photo', 'certificate_id', 'BIGINT DEFAULT NULL COMMENT ''关联证书记录ID''');

-- news 表
CALL safe_add_column('news', 'publish_time', 'DATETIME DEFAULT NULL COMMENT ''发布时间''');
CALL safe_add_column('news', 'type', 'TINYINT DEFAULT 1 COMMENT ''类型: 1-新闻动态 2-重大活动''');

-- announcement 表
CALL safe_add_column('announcement', 'publish_time', 'DATETIME DEFAULT NULL COMMENT ''发布时间''');

-- exam_answer 表
CALL safe_add_column('exam_answer', 'score', 'DECIMAL(5,2) DEFAULT NULL COMMENT ''得分''');
CALL safe_add_column('exam_answer', 'analysis', 'TEXT COMMENT ''解析''');

-- exam_record 表
CALL safe_add_column('exam_record', 'pending_count', 'INT DEFAULT 0 COMMENT ''待批改题数''');
CALL safe_add_column('exam_record', 'accuracy', 'DECIMAL(5,2) DEFAULT NULL COMMENT ''正确率''');
CALL safe_add_column('exam_record', 'exam_name', 'VARCHAR(255) DEFAULT NULL COMMENT ''冗余考试名称(删除考试后仍可展示)''');
CALL safe_add_column('exam_record', 'exam_profession_id', 'BIGINT DEFAULT NULL COMMENT ''冗余考试专业ID(删除考试后仍可按专业分组)''');
CALL safe_add_column('exam_record', 'exam_cover_url', 'VARCHAR(500) DEFAULT NULL COMMENT ''冗余考试封面图(删除考试后仍可展示)''');
CALL safe_add_column('exam_record', 'exam_question_count', 'INT DEFAULT NULL COMMENT ''冗余考试题目数(删除考试后仍可展示)''');
CALL safe_add_column('exam_record', 'exam_total_score', 'DECIMAL(10,2) DEFAULT NULL COMMENT ''冗余考试总分(删除考试后仍可展示)''');
CALL safe_add_column('exam_record', 'paper_id', 'BIGINT DEFAULT NULL COMMENT ''冗余试卷ID(删除考试后仍可查看试卷题目)''');

-- certificate_template_field 表
CALL safe_add_column('certificate_template_field', 'height', 'INT DEFAULT NULL COMMENT ''高度''');

-- certificate_template 表(证书编号前缀/中段合并到模板配置)
CALL safe_add_column('certificate_template', 'cert_no_prefix', 'VARCHAR(10) DEFAULT NULL COMMENT ''证书编号前缀字母(从此模板配置)''');
CALL safe_add_column('certificate_template', 'cert_no_middle', 'VARCHAR(10) DEFAULT NULL COMMENT ''证书编号中段字母(从此模板配置)''');

-- student_course 表
CALL safe_add_column('student_course', 'course_name', 'VARCHAR(255) DEFAULT NULL COMMENT ''冗余课程名称(删除课程后仍可展示)''');

-- video_study_record 表
CALL safe_add_column('video_study_record', 'course_name', 'VARCHAR(255) DEFAULT NULL COMMENT ''冗余课程名称(删除课程后仍可展示)''');
CALL safe_add_column('video_study_record', 'video_name', 'VARCHAR(255) DEFAULT NULL COMMENT ''冗余视频名称(删除视频后仍可展示)''');

-- ============================================================
-- 3. 删除 exam_record 指向 exam 的外键约束(防止删除考试时级联删除考试记录或置空exam_id)
-- ============================================================
SET @fk_name = (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_record' AND REFERENCED_TABLE_NAME = 'exam' LIMIT 1);
SET @preparedStatement = (SELECT IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE exam_record DROP FOREIGN KEY ', @fk_name), 'SELECT 1'));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 6. 新增: 首页内容板块表(政策法规/信息公开)
-- ============================================================
CREATE TABLE IF NOT EXISTS `homepage_section` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` LONGTEXT COMMENT '富文本内容',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '1-政策法规 2-信息公开',
  `status` TINYINT DEFAULT 1 COMMENT '0-隐藏 1-显示',
  `sort` INT DEFAULT 0 COMMENT '排序(升序)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页内容板块(政策法规/信息公开)';

-- ============================================================
-- 7. 新增: 证书类型表 + 学生/证书用户表新增 cert_type 字段
-- ============================================================
CREATE TABLE IF NOT EXISTS `certificate_type` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL COMMENT '类型名称',
  `code` VARCHAR(50) DEFAULT NULL COMMENT '类型编码(mcode)',
  `sort` INT DEFAULT 0 COMMENT '排序(升序)',
  `status` TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书类型';

-- 种子数据: 默认证书类型(使用 INSERT IGNORE,不删除用户已有的类型)
-- 五种证书类型: 专业技能证书 / 专项职业证书 / 职业能力证书 / 能力等级证书 / 人才数据库
-- 学生表新增 cert_type 字段
CALL safe_add_column('student', 'cert_type', 'VARCHAR(100) DEFAULT NULL COMMENT ''证书类型''');

-- 证书用户表新增 cert_type 字段
CALL safe_add_column('certificate_user', 'cert_type', 'VARCHAR(100) DEFAULT NULL COMMENT ''证书类型''');

-- 证书用户表新增 id_card, name 字段(冗余,方便查询)
CALL safe_add_column('certificate_user', 'id_card', 'VARCHAR(20) DEFAULT NULL COMMENT ''身份证号''');
CALL safe_add_column('certificate_user', 'name', 'VARCHAR(100) DEFAULT NULL COMMENT ''姓名''');

-- ============================================================
-- 7.1 证书类型唯一索引(保留用户自定义类型,不删除任何类型)
-- ============================================================
-- 注意: 不再做同名去重DELETE(用户要求: 每次升级不允许删除证书类型)
-- 如果历史数据存在同名重复,加唯一索引会失败,但不影响其他语句(--force模式继续执行)
-- 后续 INSERT IGNORE 在有唯一索引时自动去重; 无唯一索引时也不会报错

-- 添加唯一索引防止再次重复(如果不存在且无重复数据)
SET @has_dup = (SELECT COUNT(*) FROM (
  SELECT name FROM certificate_type GROUP BY name HAVING COUNT(*) > 1
) t);
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'certificate_type' AND INDEX_NAME = 'uk_name');
-- 只有在无重复数据且索引不存在时才添加(避免因重复数据导致报错)
SET @sql = IF(@idx_exists = 0 AND @has_dup = 0,
  'ALTER TABLE certificate_type ADD UNIQUE INDEX uk_name (name)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 8. 清理存储过程(必须放在所有 CALL 之后)
-- ============================================================
DROP PROCEDURE IF EXISTS `safe_add_column`;
-- ============================================================
-- 8.1 首页横幅图片表
-- ============================================================
CREATE TABLE IF NOT EXISTS `banner_image` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) DEFAULT NULL COMMENT '标题文字',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `link_url` VARCHAR(500) DEFAULT NULL COMMENT '点击跳转链接(可选)',
  `status` TINYINT DEFAULT 1 COMMENT '0-隐藏 1-显示',
  `sort` INT DEFAULT 0 COMMENT '排序(升序)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页横幅图片';

-- ============================================================
-- 9. 合作申请表
-- ============================================================
CREATE TABLE IF NOT EXISTS `cooperation_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  -- 一、单位基本信息
  `unit_name` varchar(255) DEFAULT NULL COMMENT '单位名称',
  `credit_code` varchar(255) DEFAULT NULL COMMENT '统一社会信用代码',
  `legal_person` varchar(255) DEFAULT NULL COMMENT '法人姓名',
  `legal_person_phone` varchar(255) DEFAULT NULL COMMENT '法人联系电话',
  `legal_person_id_card` varchar(255) DEFAULT NULL COMMENT '法人身份证号',
  `legal_id_front_img` varchar(255) DEFAULT NULL COMMENT '法人身份证正面图片',
  `legal_id_back_img` varchar(255) DEFAULT NULL COMMENT '法人身份证反面图片',
  `business_license_img` varchar(255) DEFAULT NULL COMMENT '营业执照图片',
  `registered_capital` varchar(255) DEFAULT NULL COMMENT '注册资金',
  `paid_capital` varchar(255) DEFAULT NULL COMMENT '实缴资金',
  `established_date` date DEFAULT NULL COMMENT '成立日期',
  `unit_address` varchar(255) DEFAULT NULL COMMENT '单位地址',
  `filing_status` varchar(255) DEFAULT NULL COMMENT '备案情况',
  -- 二、主营业务信息
  `main_business` varchar(255) DEFAULT NULL COMMENT '主营业务',
  `emp_count` int DEFAULT NULL COMMENT '员工人数',
  `training_years` int DEFAULT NULL COMMENT '培训经验年数',
  `training_area` varchar(255) DEFAULT NULL COMMENT '培训场地面积',
  `training_facilities` varchar(255) DEFAULT NULL COMMENT '培训设施设备',
  `exp_intro` text DEFAULT NULL COMMENT '经验介绍',
  `recruit_resource` text DEFAULT NULL COMMENT '招生资源介绍',
  `other_business` text DEFAULT NULL COMMENT '其他主营业务',
  `auth_code` varchar(255) DEFAULT NULL COMMENT '授权管理编号',
  -- 三、合作意向
  `cooperation_intent` varchar(255) DEFAULT NULL COMMENT '合作意向(多选,逗号分隔)',
  -- 额外字段
  `contact_name` varchar(255) DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(255) DEFAULT NULL COMMENT '联系人电话',
  `remark` text DEFAULT NULL COMMENT '备注',
  `status` int DEFAULT 0 COMMENT '状态: 0-待审核 1-已通过 2-已拒绝',
  `auth_start_date` date DEFAULT NULL COMMENT '授权开始日期',
  `auth_expire_date` date DEFAULT NULL COMMENT '授权有效期截止日期',
  -- 审计字段
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_unit_name` (`unit_name`),
  KEY `idx_auth_code` (`auth_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合作申请表';

-- ============================================================
-- 10. 补齐 homepage_section / news 表字段(DDL only)
-- ============================================================
-- 给 homepage_section 添加 cover_url 字段
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homepage_section' AND COLUMN_NAME = 'cover_url');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE homepage_section ADD COLUMN cover_url VARCHAR(500) DEFAULT NULL COMMENT ''封面图'' AFTER content', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 给 news 添加 source, summary 字段
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'source');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE news ADD COLUMN source VARCHAR(200) DEFAULT NULL COMMENT ''来源'' AFTER content', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'summary');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE news ADD COLUMN summary VARCHAR(500) DEFAULT NULL COMMENT ''摘要'' AFTER source', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- about_us 表增加免责声明字段
-- ============================================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'about_us' AND COLUMN_NAME = 'disclaimer');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE about_us ADD COLUMN disclaimer longtext DEFAULT NULL COMMENT ''免责声明(富文本,后台配置)'' AFTER content', 'SELECT ''disclaimer already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- news/announcement 表增加置顶字段
-- ============================================================
DROP PROCEDURE IF EXISTS `safe_add_column`;
DELIMITER //
CREATE PROCEDURE `safe_add_column`(
  IN tbl VARCHAR(100),
  IN col VARCHAR(100),
  IN col_def VARCHAR(500)
)
BEGIN
  SET @col_count = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col);
  IF @col_count = 0 THEN
    SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', col_def);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;
CALL safe_add_column('news', 'is_top', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否置顶 0-否 1-是''');
CALL safe_add_column('announcement', 'is_top', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否置顶 0-否 1-是''');

-- ============================================================
-- cooperation_apply 表增加授权有效期字段
-- ============================================================
CALL safe_add_column('cooperation_apply', 'auth_start_date', 'DATE DEFAULT NULL COMMENT ''授权开始日期''');
CALL safe_add_column('cooperation_apply', 'auth_expire_date', 'DATE DEFAULT NULL COMMENT ''授权有效期截止日期''');
DROP PROCEDURE IF EXISTS `safe_add_column`;
-- 给 certificate_template.name 添加唯一索引(幂等)
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'certificate_template' AND INDEX_NAME = 'uk_name');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE certificate_template ADD UNIQUE INDEX uk_name (name)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 给 certificate_template_field 添加 (template_id, field_key) 唯一索引(幂等)
-- 用于支持 INSERT ON DUPLICATE KEY UPDATE,避免DELETE操作
SET @field_idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'certificate_template_field' AND INDEX_NAME = 'uk_tpl_field');
SET @field_has_dup = (SELECT COUNT(*) FROM (
  SELECT template_id, field_key FROM certificate_template_field GROUP BY template_id, field_key HAVING COUNT(*) > 1
) t);
SET @field_sql = IF(@field_idx_exists = 0 AND @field_has_dup = 0,
  'ALTER TABLE certificate_template_field ADD UNIQUE INDEX uk_tpl_field (template_id, field_key)',
  'SELECT 1');
PREPARE stmt FROM @field_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- ============================================================
-- 证书导出列配置表 (每个模板可自定义导出哪些列及顺序)
-- ============================================================
CREATE TABLE IF NOT EXISTS `certificate_export_column` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `template_id` BIGINT NOT NULL COMMENT '证书模板ID',
  `field_key` VARCHAR(50) NOT NULL COMMENT '字段键(引用certificate_field.field_key)',
  `column_name` VARCHAR(100) NOT NULL COMMENT 'Excel列头名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序(从0开始)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_template_field` (`template_id`, `field_key`),
  KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书导出列配置';

-- ============================================================
-- homepage_section 增加 publish_time 字段(信息公开/政策法规的发布时间)
-- ============================================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homepage_section' AND COLUMN_NAME = 'publish_time');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE homepage_section ADD COLUMN publish_time DATETIME DEFAULT NULL COMMENT ''发布时间(null=用创建时间)'' AFTER sort',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 文章相关表加索引(提升查询性能)
-- ============================================================
-- news 表索引
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND INDEX_NAME = 'idx_status_publish');
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE news ADD INDEX idx_status_publish (status, publish_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND INDEX_NAME = 'idx_type_status');
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE news ADD INDEX idx_type_status (type, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- announcement 表索引
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcement' AND INDEX_NAME = 'idx_status_publish');
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE announcement ADD INDEX idx_status_publish (status, publish_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- homepage_section 表索引
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homepage_section' AND INDEX_NAME = 'idx_type_status');
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE homepage_section ADD INDEX idx_type_status (type, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homepage_section' AND INDEX_NAME = 'idx_status_publish');
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE homepage_section ADD INDEX idx_status_publish (status, publish_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 以下一次性数据修复SQL已全部执行完毕,已从本脚本中移除
-- 如需重新执行,请在服务器上手动 mysql < xxx.sql
-- ============================================================
-- 已移除的DML操作:
--   1. 回填冗余字段(student_course/video_study_record/exam_record的name等)
--   2. 证书类型种子数据插入(certificate_type INSERT IGNORE)
--   3. 证书用户/学生cert_type回填(从extra_json提取)
--   4. 证书cert_type字段提取(从extra_json到cert_type列)
--   5. 文章数据去重(DELETE FROM announcement/news/homepage_section)
--   6. 文章数据导入(INSERT INTO announcement/news/homepage_section)
--   7. 文章图片路径修复(UPDATE content REPLACE /static/upload -> 完整URL)
--   8. 证书模板种子数据(INSERT ON DUPLICATE KEY UPDATE certificate_template)
--   9. 证书模板字段种子数据(INSERT IGNORE certificate_template_field)
--  10. 数据迁移(announcement + homepage_section -> news表)
--  11. 独立修复脚本: fix_all_cert_issues.sql, fix_publish_time.sql,
--      fix_publish_time_v2.sql, fix_id_card_empty.sql, fix_zy_scores.sql,
--      fix_issue_date_v2.sql, import_missing_certs.sql, fix_dup_certs.sql,
--      fix_cert_issue_date.sql, fix_cert_scores.sql, ensure_cert_types.sql

-- ============================================================
-- 12. 修复 certificate.profession 存储了专业ID(数字)的问题
--     将纯数字的 profession 值替换为对应的 profession.name
--     (幂等: 只处理 profession 为纯数字的行)
-- ============================================================
UPDATE `certificate` c
  INNER JOIN `profession` p ON p.id = CAST(c.profession AS UNSIGNED)
  SET c.profession = p.name
  WHERE c.profession IS NOT NULL
    AND c.profession REGEXP '^[0-9]+$';

-- ============================================================
-- 13. 清理无效证书数据: 专业为空 且 创建时间在2026-07-25之后
--     (幂等: 只删除同时满足条件的行)
-- ============================================================
DELETE FROM `certificate`
  WHERE (profession IS NULL OR profession = '')
    AND create_time > '2026-07-25 00:00:00';
