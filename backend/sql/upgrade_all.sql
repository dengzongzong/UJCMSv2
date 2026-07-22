-- =====================================================================
-- 数据库完整升级脚本 - 一次性补齐所有缺失字段和表
-- 
-- 使用方法:
--   mysql -u root -p exam_platform < upgrade_all.sql
-- 
-- 本脚本是幂等的: 字段/表已存在时跳过,不会报错,可重复执行
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
-- 4. 回填已有数据的冗余字段（幂等：仅更新为空的记录）
-- ============================================================
UPDATE student_course SET course_name = (SELECT c.name FROM course c WHERE c.id = student_course.course_id) WHERE (course_name IS NULL OR course_name = '') AND EXISTS (SELECT 1 FROM course c WHERE c.id = student_course.course_id);
UPDATE video_study_record SET course_name = (SELECT c.name FROM course c WHERE c.id = video_study_record.course_id) WHERE (course_name IS NULL OR course_name = '') AND EXISTS (SELECT 1 FROM course c WHERE c.id = video_study_record.course_id);
UPDATE video_study_record SET video_name = (SELECT v.name FROM video v WHERE v.id = video_study_record.video_id) WHERE (video_name IS NULL OR video_name = '') AND EXISTS (SELECT 1 FROM video v WHERE v.id = video_study_record.video_id);
UPDATE exam_record SET exam_name = (SELECT e.name FROM exam e WHERE e.id = exam_record.exam_id), exam_profession_id = (SELECT e.profession_id FROM exam e WHERE e.id = exam_record.exam_id), exam_cover_url = (SELECT e.cover_url FROM exam e WHERE e.id = exam_record.exam_id) WHERE (exam_name IS NULL OR exam_name = '') AND EXISTS (SELECT 1 FROM exam e WHERE e.id = exam_record.exam_id);
UPDATE exam_record SET exam_question_count = (SELECT e.question_count FROM exam e WHERE e.id = exam_record.exam_id), exam_total_score = (SELECT e.total_score FROM exam e WHERE e.id = exam_record.exam_id), paper_id = (SELECT e.paper_id FROM exam e WHERE e.id = exam_record.exam_id) WHERE (exam_question_count IS NULL) AND EXISTS (SELECT 1 FROM exam e WHERE e.id = exam_record.exam_id);

-- ============================================================
-- 5. 验证
-- ============================================================
SELECT '=== 升级完成,可重复执行 ===' AS info;

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

-- 种子数据: 默认3种证书类型
INSERT IGNORE INTO `certificate_type` (`name`, `code`, `sort`, `status`) VALUES
('职业技能等级证书', '3', 1, 1),
('职业技能等级证书含成绩', '4', 2, 1),
('岗位/专业证书', '5', 3, 1);

-- 学生表新增 cert_type 字段
CALL safe_add_column('student', 'cert_type', 'VARCHAR(100) DEFAULT NULL COMMENT ''证书类型''');

-- 证书用户表新增 cert_type 字段
CALL safe_add_column('certificate_user', 'cert_type', 'VARCHAR(100) DEFAULT NULL COMMENT ''证书类型''');

-- 证书用户表新增 id_card, name 字段(冗余,方便查询)
CALL safe_add_column('certificate_user', 'id_card', 'VARCHAR(20) DEFAULT NULL COMMENT ''身份证号''');
CALL safe_add_column('certificate_user', 'name', 'VARCHAR(100) DEFAULT NULL COMMENT ''姓名''');

-- ============================================================
-- 7.1 证书类型去重 + 数据修复
-- ============================================================
-- 去重: 每个名称只保留id最小的一条,删除多余的
DELETE t1 FROM certificate_type t1
INNER JOIN certificate_type t2
WHERE t1.name = t2.name AND t1.id > t2.id;

-- 添加唯一索引防止再次重复(如果不存在)
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'certificate_type' AND INDEX_NAME = 'uk_name');
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE certificate_type ADD UNIQUE INDEX uk_name (name)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 填充 certificate_user 的 id_card, name
UPDATE certificate_user cu
JOIN certificate c ON c.id = cu.certificate_id
SET cu.id_card = c.id_card, cu.name = c.name
WHERE cu.id_card IS NULL;

-- 填充 certificate_user.cert_type (从certificate.extra_json提取)
UPDATE certificate_user cu
JOIN certificate c ON c.id = cu.certificate_id
SET cu.cert_type = JSON_UNQUOTE(JSON_EXTRACT(c.extra_json, '$.cert_type'))
WHERE cu.cert_type IS NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(c.extra_json, '$.cert_type')) IS NOT NULL;

-- 填充 student.cert_type (从certificate.extra_json提取)
UPDATE student s
JOIN certificate c ON c.id_card = s.id_card
SET s.cert_type = JSON_UNQUOTE(JSON_EXTRACT(c.extra_json, '$.cert_type'))
WHERE s.cert_type IS NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(c.extra_json, '$.cert_type')) IS NOT NULL;

-- 证书类型名称统一更新(旧名称 → 新名称,兼容多种历史写法)
UPDATE certificate_type SET name = '专业技能证书' WHERE name IN ('职业技能等级证书');
UPDATE certificate_type SET name = '专项职业技能证书' WHERE name IN ('职业技能等级证书(含成绩)', '职业技能等级证书含成绩');
UPDATE certificate_type SET name = '人才数据入库证书' WHERE name IN ('岗位专业证书', '岗位/专业证书');

UPDATE certificate SET extra_json = JSON_SET(extra_json, '$.cert_type', '专业技能证书')
WHERE JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.cert_type')) IN ('职业技能等级证书');
UPDATE certificate SET extra_json = JSON_SET(extra_json, '$.cert_type', '专项职业技能证书')
WHERE JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.cert_type')) IN ('职业技能等级证书(含成绩)', '职业技能等级证书含成绩');
UPDATE certificate SET extra_json = JSON_SET(extra_json, '$.cert_type', '人才数据入库证书')
WHERE JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.cert_type')) IN ('岗位专业证书', '岗位/专业证书');

UPDATE certificate_user SET cert_type = '专业技能证书' WHERE cert_type IN ('职业技能等级证书');
UPDATE certificate_user SET cert_type = '专项职业技能证书' WHERE cert_type IN ('职业技能等级证书(含成绩)', '职业技能等级证书含成绩');
UPDATE certificate_user SET cert_type = '人才数据入库证书' WHERE cert_type IN ('岗位专业证书', '岗位/专业证书');

UPDATE student SET cert_type = '专业技能证书' WHERE cert_type IN ('职业技能等级证书');
UPDATE student SET cert_type = '专项职业技能证书' WHERE cert_type IN ('职业技能等级证书(含成绩)', '职业技能等级证书含成绩');
UPDATE student SET cert_type = '人才数据入库证书' WHERE cert_type IN ('岗位专业证书', '岗位/专业证书');

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
  -- 审计字段
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_unit_name` (`unit_name`),
  KEY `idx_auth_code` (`auth_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合作申请表';
