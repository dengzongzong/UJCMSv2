-- ============================================================
-- 中国人力资源专业技能人才评价中心 数据库建表脚本
-- 技术栈: SpringBoot + Vue2 + MySQL
-- ============================================================

CREATE DATABASE IF NOT EXISTS `exam_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `exam_platform`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 管理员表 (含超级管理员与子管理员)
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '密码(BCrypt)',
  `role_name` varchar(50) DEFAULT NULL COMMENT '角色名称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `is_super` tinyint NOT NULL DEFAULT 0 COMMENT '0-子管理员 1-超级管理员',
  `permissions` text DEFAULT NULL COMMENT '功能权限JSON数组',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  `last_login_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ----------------------------
-- 2. 学生表
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL COMMENT '学生姓名',
  `student_no` varchar(50) DEFAULT NULL COMMENT '学号',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号(选填)',
  `password` varchar(100) NOT NULL COMMENT '密码(BCrypt)',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `id_card` varchar(32) DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `profession_id` bigint DEFAULT NULL COMMENT '当前选择专业',
  `subject_id` bigint DEFAULT NULL COMMENT '当前选择科目',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0-冻结 1-正常',
  `last_login_time` datetime DEFAULT NULL,
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- ----------------------------
-- 3. 专业表
-- ----------------------------
DROP TABLE IF EXISTS `profession`;
CREATE TABLE `profession` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '专业名称',
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业表';

-- ----------------------------
-- 4. 科目表
-- ----------------------------
DROP TABLE IF EXISTS `subject`;
CREATE TABLE `subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `profession_id` bigint NOT NULL COMMENT '所属专业',
  `name` varchar(100) NOT NULL COMMENT '科目名称',
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_profession` (`profession_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科目表';

-- ----------------------------
-- 5. 轮播图表
-- ----------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) DEFAULT NULL,
  `image_url` varchar(500) NOT NULL COMMENT '图片地址',
  `link_type` tinyint NOT NULL DEFAULT 0 COMMENT '0-纯展示 1-跳转试卷 2-跳转课程',
  `link_id` bigint DEFAULT NULL COMMENT '跳转目标ID',
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- ----------------------------
-- 6. 视频分类表
-- ----------------------------
DROP TABLE IF EXISTS `video_category`;
CREATE TABLE `video_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分类表';

-- ----------------------------
-- 7. 视频表
-- ----------------------------
DROP TABLE IF EXISTS `video`;
CREATE TABLE `video` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '视频名称',
  `category_id` bigint DEFAULT NULL COMMENT '视频分类',
  `course_id` bigint DEFAULT NULL COMMENT '所属课程ID',
  `profession_id` bigint DEFAULT NULL COMMENT '所属专业ID',
  `url` varchar(500) NOT NULL COMMENT '视频地址',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面',
  `duration` int NOT NULL DEFAULT 0 COMMENT '时长(秒)',
  `size` bigint NOT NULL DEFAULT 0 COMMENT '大小(字节)',
  `play_count` int NOT NULL DEFAULT 0 COMMENT '播放量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_profession` (`profession_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频表';

-- ----------------------------
-- 8. 课程表
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '课程名称',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '课程缩略图',
  `intro` text DEFAULT NULL COMMENT '课程文本介绍',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格(仅展示)',
  `tag` varchar(50) DEFAULT NULL COMMENT '课程标签',
  `total_duration` int NOT NULL DEFAULT 0 COMMENT '课程总时长(秒)',
  `section_count` int NOT NULL DEFAULT 0 COMMENT '小节数量',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-未上架 1-已上架',
  `profession_id` bigint DEFAULT NULL,
  `subject_id` bigint DEFAULT NULL,
  `category_id` bigint(20) DEFAULT NULL COMMENT '课程分类ID(关联video_category表)',
  `base_study_count` int NOT NULL DEFAULT 0 COMMENT '基础学过人数(后台手工设置, 展示=基数+实际开通人数)',
  `base_study_hours` int NOT NULL DEFAULT 0 COMMENT '基础学时(小时, 后台手工设置, 无单位)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ----------------------------
-- 8.1 课程支付订单表
-- ----------------------------
DROP TABLE IF EXISTS `course_order`;
CREATE TABLE `course_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(40) NOT NULL COMMENT '业务订单号',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `course_name` varchar(255) DEFAULT NULL COMMENT '冗余课程名称',
  `amount` int NOT NULL DEFAULT 0 COMMENT '金额(分)',
  `channel` varchar(20) NOT NULL DEFAULT 'wechat' COMMENT '支付渠道: wechat/alipay',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已关闭',
  `transaction_id` varchar(64) DEFAULT NULL COMMENT '第三方交易号',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_student` (`student_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程支付订单表';

-- ----------------------------
-- 9. 课程小节(目录)表
-- ----------------------------
DROP TABLE IF EXISTS `course_section`;
CREATE TABLE `course_section` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '所属课程',
  `name` varchar(200) NOT NULL COMMENT '小节名称',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程小节表';

-- ----------------------------
-- 10. 小节视频关联表
-- ----------------------------
DROP TABLE IF EXISTS `course_section_video`;
CREATE TABLE `course_section_video` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `section_id` bigint NOT NULL COMMENT '所属小节',
  `video_id` bigint NOT NULL COMMENT '视频ID',
  `sort` int NOT NULL DEFAULT 0,
  `view_permission` tinyint NOT NULL DEFAULT 0 COMMENT '查看权限 0-所有已开通 1-需指定权限',
  PRIMARY KEY (`id`),
  KEY `idx_section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小节视频关联表';

-- ----------------------------
-- 11. 学生课程开通表
-- ----------------------------
DROP TABLE IF EXISTS `student_course`;
CREATE TABLE `student_course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course` (`student_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生课程开通表';

-- ----------------------------
-- 12. 视频学习记录表 (记录学生观看视频进度)
-- ----------------------------
DROP TABLE IF EXISTS `video_study_record`;
CREATE TABLE `video_study_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `video_id` bigint NOT NULL,
  `progress` int NOT NULL DEFAULT 0 COMMENT '播放进度(秒)',
  `watched_duration` int NOT NULL DEFAULT 0 COMMENT '已观看时长(秒)',
  `last_watch_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_video` (`student_id`, `video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频学习记录表';

-- ----------------------------
-- 13. 题目分类表
-- ----------------------------
DROP TABLE IF EXISTS `question_category`;
CREATE TABLE `question_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目分类表';

-- ----------------------------
-- 14. 题目表
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` tinyint NOT NULL DEFAULT 1 COMMENT '1-单选 2-多选 3-填空 4-判断 5-简答',
  `category_id` bigint DEFAULT NULL COMMENT '题目分类',
  `profession_id` bigint DEFAULT NULL COMMENT '所属专业ID',
  `content` text NOT NULL COMMENT '题干(支持HTML图文)',
  `analysis` text DEFAULT NULL COMMENT '答案解析',
  `correct_answer` varchar(500) DEFAULT NULL COMMENT '填空题正确答案',
  `score` decimal(5,1) NOT NULL DEFAULT 2.0 COMMENT '题目分值',
  `has_image` tinyint NOT NULL DEFAULT 0 COMMENT '是否含图片',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否可用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_category` (`category_id`),
  KEY `idx_profession` (`profession_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- ----------------------------
-- 15. 题目选项表
-- ----------------------------
DROP TABLE IF EXISTS `question_option`;
CREATE TABLE `question_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `label` varchar(10) NOT NULL COMMENT '选项标识 A/B/C/D',
  `content` text NOT NULL COMMENT '选项内容(支持HTML图文)',
  `is_correct` tinyint NOT NULL DEFAULT 0 COMMENT '0-错误 1-正确',
  `sort` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';

-- ----------------------------
-- 16. 考试(试卷)表
-- ----------------------------
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '考试名称',
  `category` varchar(100) DEFAULT NULL COMMENT '考试分类(用于考试中心按分类分组)',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '考试封面图',
  `intro` text DEFAULT NULL COMMENT '考试介绍',
  `total_score` decimal(6,1) NOT NULL DEFAULT 100.0 COMMENT '考试总分',
  `duration` int NOT NULL DEFAULT 60 COMMENT '考试时长(分钟)',
  `question_count` int NOT NULL DEFAULT 0 COMMENT '题目数量',
  `start_time` datetime DEFAULT NULL COMMENT '考试开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '考试结束时间',
  `allow_retry` tinyint NOT NULL DEFAULT 1 COMMENT '是否允许重新作答',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-未上架 1-已上架',
  `profession_id` bigint DEFAULT NULL,
  `subject_id` bigint DEFAULT NULL,
  `paper_id` bigint DEFAULT NULL COMMENT '关联试卷ID',
  `base_exam_count` int NOT NULL DEFAULT 0 COMMENT '基础考过人数(后台手工设置, 展示=基数+实际开通权限人数)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试表';

-- ----------------------------
-- 17. 考试题目关联表
-- ----------------------------
DROP TABLE IF EXISTS `exam_question`;
CREATE TABLE `exam_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_exam` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试题目关联表';

-- ----------------------------
-- 17.1 试卷表
-- ----------------------------
DROP TABLE IF EXISTS `paper`;
CREATE TABLE `paper` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '试卷名称',
  `description` text DEFAULT NULL COMMENT '试卷描述',
  `total_score` decimal(6,1) NOT NULL DEFAULT 100.0 COMMENT '试卷总分',
  `question_count` int NOT NULL DEFAULT 0 COMMENT '题目数量',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-未发布 1-已发布',
  `profession_id` BIGINT DEFAULT NULL COMMENT '专业ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- ----------------------------
-- 17.2 试卷题目关联表
-- ----------------------------
DROP TABLE IF EXISTS `paper_question`;
CREATE TABLE `paper_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_paper` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- ----------------------------
-- 18. 学生考试开通表
-- ----------------------------
DROP TABLE IF EXISTS `student_exam`;
CREATE TABLE `student_exam` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `exam_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_exam` (`student_id`, `exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生考试开通表';

-- ----------------------------
-- 19. 考试记录表
-- ----------------------------
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `exam_id` bigint NOT NULL,
  `score` decimal(6,1) NOT NULL DEFAULT 0.0 COMMENT '考试分数',
  `correct_count` int NOT NULL DEFAULT 0 COMMENT '正确题数',
  `wrong_count` int NOT NULL DEFAULT 0 COMMENT '错误题数',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总题数',
  `pending_count` int NOT NULL DEFAULT 0 COMMENT '待批改题数(简答题)',
  `accuracy` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '正确率',
  `duration` int NOT NULL DEFAULT 0 COMMENT '答题用时(秒)',
  `submit_status` tinyint NOT NULL DEFAULT 0 COMMENT '0-未提交 1-已提交 2-自动交卷',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `has_certificate` tinyint NOT NULL DEFAULT 0 COMMENT '是否有证书',
  `face_verify_status` tinyint DEFAULT 0 COMMENT '0-未验证 1-验证通过 2-验证失败 3-无需验证',
  `face_verify_time` datetime DEFAULT NULL COMMENT '人脸验证时间',
  `face_similarity` decimal(5,4) DEFAULT NULL COMMENT '人脸相似度(欧式距离，越小越相似)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_exam` (`exam_id`),
  KEY `idx_face_verify` (`face_verify_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- ----------------------------
-- 20. 考试答题记录表
-- ----------------------------
DROP TABLE IF EXISTS `exam_answer`;
CREATE TABLE `exam_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL COMMENT '考试记录ID',
  `question_id` bigint NOT NULL,
  `student_answer` varchar(500) DEFAULT NULL COMMENT '学生答案(选项label逗号分隔或文本答案)',
  `is_correct` tinyint NOT NULL DEFAULT 0 COMMENT '0-错误 1-正确 2-待批改',
  `sort` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试答题记录表';

-- ----------------------------
-- 21. 错题本表
-- ----------------------------
DROP TABLE IF EXISTS `wrong_question`;
CREATE TABLE `wrong_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `exam_id` bigint DEFAULT NULL,
  `student_answer` varchar(500) DEFAULT NULL,
  `mastered` tinyint NOT NULL DEFAULT 0 COMMENT '0=未掌握(默认) 1=已掌握',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_question` (`student_id`, `question_id`),
  KEY `idx_mastered` (`student_id`, `mastered`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- ----------------------------
-- 22. 关于我们设置表
-- ----------------------------
DROP TABLE IF EXISTS `about_us`;
CREATE TABLE `about_us` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `service_phone` varchar(50) DEFAULT NULL COMMENT '客服电话',
  `service_qrcode` varchar(500) DEFAULT NULL COMMENT '客服二维码',
  `qrcode_link` varchar(500) DEFAULT NULL COMMENT '关于我们右下角二维码所指向的链接(后台配置)',
  `content` longtext DEFAULT NULL COMMENT '平台介绍(富文本)',
  `disclaimer` longtext DEFAULT NULL COMMENT '免责声明(富文本,后台配置)',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关于我们设置表';

-- ----------------------------
-- 23. 系统设置表 (默认密码等)
-- ----------------------------
DROP TABLE IF EXISTS `system_setting`;
CREATE TABLE `system_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `setting_key` varchar(100) NOT NULL,
  `setting_value` varchar(500) DEFAULT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

-- ----------------------------
-- 24. 人脸验证日志表
-- ----------------------------
DROP TABLE IF EXISTS `face_verify_log`;
CREATE TABLE `face_verify_log` (
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

-- ----------------------------
-- 24.1 直播场次表
-- ----------------------------
DROP TABLE IF EXISTS `live_room`;
CREATE TABLE `live_room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '所属课程ID',
  `title` varchar(200) NOT NULL COMMENT '直播标题',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图',
  `anchor_name` varchar(100) DEFAULT NULL COMMENT '讲师姓名',
  `intro` text COMMENT '直播简介',
  `start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-未开始 1-直播中 2-已结束 3-已取消',
  `stream_name` varchar(100) DEFAULT NULL COMMENT '直播流名(唯一,生成推拉流地址用)',
  `push_url` varchar(600) DEFAULT NULL COMMENT 'RTMP推流地址(含鉴权,仅管理端可见)',
  `play_url` varchar(600) DEFAULT NULL COMMENT 'HLS播放地址(直播流)',
  `replay_url` varchar(600) DEFAULT NULL COMMENT '回放地址(结束后回填,可复看)',
  `max_online` int DEFAULT 0 COMMENT '最高同时在线',
  `view_count` int DEFAULT 0 COMMENT '累计观看人次',
  `sort` int DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_status_time` (`status`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播场次表';

-- ----------------------------
-- 24.2 直播聊天消息表
-- ----------------------------
DROP TABLE IF EXISTS `live_message`;
CREATE TABLE `live_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `live_id` bigint NOT NULL COMMENT '直播ID',
  `student_id` bigint DEFAULT NULL COMMENT '发言学生ID(游客为空)',
  `nickname` varchar(100) DEFAULT NULL COMMENT '昵称',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_live_time` (`live_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播聊天消息表';

-- ----------------------------
-- 25. 视频开通记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `student_video` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `video_id` BIGINT NOT NULL COMMENT '视频ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_student_video` (`student_id`, `video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频开通记录';

-- ----------------------------
-- 25. 系统公告表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT COMMENT '内容',
  `status` TINYINT DEFAULT 1 COMMENT '0-隐藏 1-显示',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告';

-- ============================================================
-- 种子数据
-- ============================================================

-- 超级管理员 (密码: admin123, BCrypt加密)
INSERT INTO `admin` (`username`, `password`, `role_name`, `is_super`, `permissions`, `status`) VALUES
('admin', '$2a$10$1G2SWI8orTWPY3WPxBEOcuRlgM995ngR9.j.GMmvgaNFXqlyun/PS', '超级管理员', 1, '["all"]', 1);

-- 系统设置
INSERT INTO `system_setting` (`setting_key`, `setting_value`, `remark`) VALUES
('default_password', '123456', '学生默认密码'),
('agreement_required', '1', '登录是否需勾选协议'),
('face_verify_enabled', '0', '考前人脸识别开关：0-关闭 1-开启'),
('face_verify_threshold', '0.6', '人脸比对阈值(欧式距离，越小越严格，建议0.4-0.6)'),
('face_verify_max_retries', '3', '人脸验证最大重试次数');

-- 关于我们
INSERT INTO `about_us` (`service_phone`, `service_qrcode`, `content`) VALUES
('400-888-8888', NULL, '<p>欢迎使用中国人力资源专业技能人才评价中心，致力于提供权威的职业能力评价与人才评测服务。</p>');

-- 专业与科目
INSERT INTO `profession` (`name`, `sort`) VALUES
('建筑工程', 1),
('注册会计师（CPA）', 2),
('财会金融', 3),
('基金从业', 4),
('考公考研', 5);

INSERT INTO `subject` (`profession_id`, `name`, `sort`) VALUES
(1, '建筑工程管理与实务', 1),
(1, '建设工程法规及相关知识', 2),
(2, '会计', 1),
(2, '审计', 2),
(2, '税法', 3),
(3, '金融市场基础知识', 1),
(3, '证券市场基本法律法规', 2),
(4, '基金法律法规', 1),
(4, '证券投资基金基础知识', 2),
(5, '政治', 1),
(5, '英语', 2),
(5, '数学', 3);

-- 视频分类
INSERT INTO `video_category` (`name`, `sort`) VALUES
('基础精讲', 1),
('冲刺串讲', 2),
('真题解析', 3),
('模考讲解', 4);

-- 题目分类
INSERT INTO `question_category` (`name`, `sort`) VALUES
('第一章 概述', 1),
('第二章 基础知识', 2),
('第三章 实务操作', 3),
('第四章 综合应用', 4);

-- 轮播图
INSERT INTO `banner` (`title`, `image_url`, `link_type`, `link_id`, `sort`) VALUES
('欢迎来到中国人力资源专业技能人才评价中心', '/static/banner1.jpg', 0, NULL, 1),
('建筑工程精品课程', '/static/banner2.jpg', 2, 1, 2),
('CPA考试模拟测试', '/static/banner3.jpg', 1, 1, 3);

-- 视频
INSERT INTO `video` (`name`, `category_id`, `url`, `cover_url`, `duration`, `size`, `play_count`) VALUES
('建筑工程概论-第1讲', 1, '/static/video1.mp4', '/static/video_cover1.jpg', 3600, 524288000, 128),
('建筑工程概论-第2讲', 1, '/static/video2.mp4', '/static/video_cover2.jpg', 3300, 480000000, 96),
('法规知识精讲-第1讲', 1, '/static/video3.mp4', '/static/video_cover3.jpg', 4200, 600000000, 256),
('冲刺串讲-重点回顾', 2, '/static/video4.mp4', '/static/video_cover4.jpg', 3000, 450000000, 312),
('真题解析-2024年真题', 3, '/static/video5.mp4', '/static/video_cover5.jpg', 5400, 780000000, 456);

-- 课程
INSERT INTO `course` (`name`, `cover_url`, `intro`, `price`, `tag`, `total_duration`, `section_count`, `status`, `profession_id`, `subject_id`) VALUES
('建筑工程管理与实务全程班', '/static/course1.jpg', '本课程全面讲解建筑工程管理与实务的核心知识点，适合备考学员系统学习。', 299.00, '热门', 16100, 2, 1, 1, 1),
('建设工程法规精讲班', '/static/course2.jpg', '系统讲解建设工程法规及相关知识，涵盖全部考点。', 199.00, '推荐', 7500, 1, 1, 1, 2);

-- 课程小节
INSERT INTO `course_section` (`course_id`, `name`, `remark`, `sort`) VALUES
(1, '第一章 建筑工程概论', '基础入门章节', 1),
(1, '第二章 法规知识', '重点章节', 2),
(2, '第一章 法规体系', NULL, 1);

-- 小节视频关联
INSERT INTO `course_section_video` (`section_id`, `video_id`, `sort`) VALUES
(1, 1, 1),
(1, 2, 2),
(2, 3, 1),
(3, 3, 1),
(3, 4, 2);

-- 题目
INSERT INTO `question` (`type`, `category_id`, `content`, `analysis`, `score`, `has_image`, `enabled`) VALUES
(1, 1, '建筑工程项目的首要任务是？', '建筑工程项目的首要任务是确保工程质量与安全。', 2.0, 0, 1),
(1, 1, '下列哪项不属于建设工程法规体系？', '建设工程法规体系包括法律、行政法规、部门规章等。', 2.0, 0, 1),
(2, 2, '以下属于建筑工程质量验收标准的有？', '建筑工程质量验收标准包括主控项目和一般项目。', 3.0, 0, 1),
(1, 2, '施工图设计文件审查的主要依据是？', '施工图审查依据国家工程建设强制性标准。', 2.0, 0, 1),
(2, 3, '下列属于安全生产"三宝"的是？', '安全生产三宝指安全帽、安全带、安全网。', 3.0, 0, 1);

-- 新增题型示例：填空题(type=3)、判断题(type=4)、简答题(type=5)
INSERT INTO `question` (`type`, `category_id`, `content`, `analysis`, `correct_answer`, `score`, `has_image`, `enabled`) VALUES
(3, 1, '建筑工程的三大目标是质量、___和___。', '建筑工程的三大目标是质量、安全和进度。', '安全,进度', 3.0, 0, 1),
(4, 2, '施工现场必须设置安全警示标志。', '根据安全生产相关规定，施工现场必须设置安全警示标志。', NULL, 2.0, 0, 1),
(5, 3, '简述建筑工程质量管理的基本原则。', '参考答案：1.质量第一原则；2.预防为主原则；3.全员参与原则；4.过程控制原则；5.持续改进原则。', NULL, 5.0, 0, 1);

-- 题目选项
INSERT INTO `question_option` (`question_id`, `label`, `content`, `is_correct`, `sort`) VALUES
(1, 'A', '确保工程质量与安全', 1, 1),
(1, 'B', '控制工程成本', 0, 2),
(1, 'C', '加快工程进度', 0, 3),
(1, 'D', '美化工程外观', 0, 4),
(2, 'A', '《建筑法》', 0, 1),
(2, 'B', '《招标投标法》', 0, 2),
(2, 'C', '《公司法》', 1, 3),
(2, 'D', '《安全生产法》', 0, 4),
(3, 'A', '主控项目', 1, 1),
(3, 'B', '一般项目', 1, 2),
(3, 'C', '附属项目', 0, 3),
(3, 'D', '参考项目', 0, 4),
(4, 'A', '国家工程建设强制性标准', 1, 1),
(4, 'B', '企业内部标准', 0, 2),
(4, 'C', '地方习惯做法', 0, 3),
(4, 'D', '设计师个人意见', 0, 4),
(5, 'A', '安全帽', 1, 1),
(5, 'B', '安全带', 1, 2),
(5, 'C', '安全网', 1, 3),
(5, 'D', '安全鞋', 0, 4),
-- 判断题(type=4, question_id=7)选项固定为"正确"和"错误"
(7, 'A', '正确', 1, 1),
(7, 'B', '错误', 0, 2);

-- 试卷
INSERT INTO `paper` (`name`, `description`, `total_score`, `question_count`, `status`, `profession_id`) VALUES
('建筑工程管理与实务试卷', '涵盖建筑工程管理与实务全部考点，共8题（含填空、判断、简答）。', 22.0, 8, 1, 1),
('建设工程法规专项试卷', '建设工程法规专项试卷，检验法规知识掌握程度。', 12.0, 5, 1, 1);

-- 试卷题目关联
INSERT INTO `paper_question` (`paper_id`, `question_id`, `sort`) VALUES
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
(1, 4, 4),
(1, 5, 5),
(1, 6, 6),
(1, 7, 7),
(1, 8, 8),
(2, 1, 1),
(2, 2, 2),
(2, 3, 3),
(2, 4, 4),
(2, 5, 5);

-- 考试（关联试卷）
INSERT INTO `exam` (`name`, `cover_url`, `intro`, `total_score`, `duration`, `question_count`, `start_time`, `end_time`, `allow_retry`, `status`, `profession_id`, `subject_id`, `paper_id`) VALUES
('建筑工程管理与实务模拟考试', '/static/exam1.jpg', '本考试涵盖建筑工程管理与实务全部考点，共8题（含填空、判断、简答），满分22分。', 22.0, 30, 8, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1, 1, 1, 1, 1),
('建设工程法规专项测试', '/static/exam2.jpg', '建设工程法规专项测试，检验法规知识掌握程度。', 12.0, 20, 5, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1, 1, 1, 2, 2);

-- 考试题目关联（历史数据，保留兼容）
INSERT INTO `exam_question` (`exam_id`, `question_id`, `sort`) VALUES
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
(1, 4, 4),
(1, 5, 5),
(1, 6, 6),
(1, 7, 7),
(1, 8, 8),
(2, 1, 1),
(2, 2, 2),
(2, 3, 3),
(2, 4, 4),
(2, 5, 5);

-- 测试学生 (密码: 123456)
INSERT INTO `student` (`name`, `student_no`, `phone`, `password`, `nickname`, `profession_id`, `subject_id`, `status`) VALUES
('测试学员', 'STU20260001', '13800138000', '$2a$10$sPePS4M0bEdlv5MgYlz6cu7vLNK1d.hjGwCmb2rnXh6oCDj3uJqC.', '测试学员', 1, 1, 1);

-- 为测试学生开通课程和考试
INSERT INTO `student_course` (`student_id`, `course_id`) VALUES (1, 1), (1, 2);
INSERT INTO `student_exam` (`student_id`, `exam_id`) VALUES (1, 1), (1, 2);

-- 系统公告测试数据
INSERT INTO `announcement` (`title`, `content`, `status`, `sort`) VALUES
('欢迎使用中国人力资源专业技能人才评价中心', '欢迎各位学员使用中国人力资源专业技能人才评价中心，平台提供权威的职业能力评价、课程学习与在线考试服务，祝您学习愉快、考试顺利！', 1, 1),
('系统维护通知', '平台将于本周日凌晨2:00-4:00进行系统维护升级，期间部分功能可能无法使用，请提前做好安排。', 1, 2);

-- 新闻表
CREATE TABLE IF NOT EXISTS `news` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT COMMENT '内容',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图',
  `status` TINYINT DEFAULT 1 COMMENT '0-隐藏 1-显示',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻';

-- 新闻测试数据
INSERT INTO `news` (`title`, `content`, `status`, `sort`) VALUES
('平台全新升级', '中国人力资源专业技能人才评价中心全新升级，带来更好的评测与学习体验！', 1, 1),
('新春优惠活动', '新年新气象，全场课程8折优惠，快来报名吧！', 1, 2);

-- ============================================================
-- 证书管理模块(5 张表)
-- ============================================================

-- 证书字段定义(系统内置 + 用户自定义)
DROP TABLE IF EXISTS `certificate_field`;
CREATE TABLE `certificate_field` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `field_key` VARCHAR(50) NOT NULL COMMENT '字段键(英文唯一,系统内置不可改)',
  `field_name` VARCHAR(100) NOT NULL COMMENT '字段显示名',
  `field_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1-文本 2-数字 3-日期 4-选择项 5-图片',
  `required` TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `default_value` VARCHAR(200) DEFAULT NULL COMMENT '默认值',
  `options` VARCHAR(1000) DEFAULT NULL COMMENT '选择项(逗号分隔,field_type=4时使用)',
  `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '1-系统内置(不可删除/不可改key) 0-用户自定义',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_field_key` (`field_key`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书字段定义';

-- 预置系统字段(对应 Excel 的核心列)
INSERT INTO `certificate_field` (`field_key`, `field_name`, `field_type`, `required`, `sort`, `is_system`, `options`) VALUES
('name',         '姓名',                1, 1,  1, 1, NULL),
('idCard',       '证件号码',            1, 1,  2, 1, NULL),
('gender',       '性别',                1, 0,  3, 1, '男,女'),
('profession',   '职业名称',            1, 1,  4, 1, NULL),
('skillLevel',   '技能等级',            4, 0,  5, 1, '五级/初级,四级/中级,三级/高级,二级/技师,一级/高级技师'),
('issueDate',    '颁发日期',            3, 0, 60, 1, NULL),
('issueYear',    '颁发日期(年)',        1, 0, 61, 1, NULL),
('issueMonth',   '颁发日期(月)',        1, 0, 62, 1, NULL),
('issueDay',     '颁发日期(日)',        1, 0, 63, 1, NULL),
('certNo',       '证书编号',            1, 0,  7, 1, NULL),
('studentNo',    '学员编号',            1, 0,  8, 1, NULL),
('agency',       '报单机构',            1, 1,  9, 1, NULL),
('agencyFee',    '报单机构费用',        2, 0, 10, 1, NULL),
('qr1',          '证书二维码1',         1, 0, 11, 1, NULL),
('qr2',          '证书二维码2',         1, 0, 12, 1, NULL),
('qr3',          '证书二维码3',         1, 0, 13, 1, NULL),
('examQr',       '学员考试二维码',      1, 0, 14, 1, NULL),
('photo',        '学员照片',            5, 0, 15, 1, NULL),
('birthday',                 '出生日期',    3, 0, 16, 1, NULL),
('theoryScore',              '理论成绩',    1, 0, 20, 1, NULL),
('practicalScore',           '实操成绩',    1, 0, 21, 1, NULL),
('comprehensiveEvaluation',  '综合测评',    1, 0, 22, 1, NULL),
('trainingMajor',            '培训专业',    1, 0, 30, 1, NULL),
('trainingHours',            '培训学时',    1, 0, 31, 1, NULL),
('trainingDate',             '培训日期',    3, 0, 32, 1, NULL),
('examTime',                 '考试时间',    3, 0, 33, 1, NULL),
('phone',                    '手机号码',    1, 0, 40, 1, NULL);

-- 证书主表
DROP TABLE IF EXISTS `certificate`;
CREATE TABLE `certificate` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `cert_no` VARCHAR(50) DEFAULT NULL COMMENT '证书编号(系统生成,唯一,绑定模板时生成)',
  `student_no` VARCHAR(50) DEFAULT NULL COMMENT '学员编号(系统生成,唯一)',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `id_card` VARCHAR(30) NOT NULL COMMENT '身份证号',
  `gender` TINYINT DEFAULT NULL COMMENT '1-男 2-女',
  `profession` VARCHAR(100) DEFAULT NULL COMMENT '职业名称',
  `skill_level` VARCHAR(50) DEFAULT NULL COMMENT '技能等级',
  `issue_date` DATE DEFAULT NULL COMMENT '颁发日期',
  `cert_no_prefix` VARCHAR(10) DEFAULT NULL COMMENT '证书编号前缀字母',
  `cert_no_middle` VARCHAR(10) DEFAULT NULL COMMENT '证书编号中段字母',
  `student_no_prefix` VARCHAR(10) DEFAULT NULL COMMENT '学员编号前缀字母',
  `student_no_middle` VARCHAR(10) DEFAULT NULL COMMENT '学员编号中段字母',
  `agency` VARCHAR(200) DEFAULT NULL COMMENT '报单机构',
  `agency_fee` DECIMAL(10,2) DEFAULT NULL COMMENT '报单机构费用',
  `qr_url1` VARCHAR(500) DEFAULT NULL COMMENT '证书二维码1 URL',
  `qr_url2` VARCHAR(500) DEFAULT NULL COMMENT '证书二维码2 URL',
  `qr_url3` VARCHAR(500) DEFAULT NULL COMMENT '证书二维码3 URL',
  `exam_qr_url` VARCHAR(500) DEFAULT NULL COMMENT '学员考试二维码 URL',
  `exam_qr_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '0-关 1-开(考试二维码是否启用)',
  `extra_json` TEXT COMMENT '自定义字段值,JSON格式',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `template_id` BIGINT DEFAULT NULL COMMENT '已绑定的证书模板ID(走『模板绑定』菜单写入)',
  `upload_time` DATETIME DEFAULT NULL COMMENT '证书导入/上传时间(由导入或单条上传动作写入;与createTime区分场景)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_cert_no` (`cert_no`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_id_card` (`id_card`),
  KEY `idx_name` (`name`),
  KEY `idx_issue_date` (`issue_date`),
  KEY `idx_agency` (`agency`),
  KEY `idx_profession` (`profession`),
  KEY `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书';

-- 证书编号配置(前缀/中段字母,生成证书编号时使用)
DROP TABLE IF EXISTS `certificate_number_config`;
CREATE TABLE `certificate_number_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cert_no_prefix` VARCHAR(10) DEFAULT NULL COMMENT '证书编号前缀字母(如ZGZH)',
  `cert_no_middle` VARCHAR(10) DEFAULT NULL COMMENT '证书编号中段字母(如M)',
  `student_no_prefix` VARCHAR(10) DEFAULT NULL COMMENT '学员编号前缀字母(如RCCP)',
  `student_no_middle` VARCHAR(10) DEFAULT NULL COMMENT '学员编号中段字母(如B)',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书编号配置';

-- 默认配置
INSERT INTO `certificate_number_config` (`cert_no_prefix`, `cert_no_middle`, `student_no_prefix`, `student_no_middle`)
SELECT 'ZGZH', 'M', 'RCCP', 'B'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `certificate_number_config`);

-- 证书二维码 URL 生成规则配置(配置二维码1/2/3的跳转链接生成规则:常量+证书属性占位符拼接)
DROP TABLE IF EXISTS `certificate_url_config`;
CREATE TABLE `certificate_url_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `qr1_template` VARCHAR(1000) DEFAULT NULL COMMENT '证书二维码1的URL生成规则(留空回退使用证书qr_url1)',
  `qr2_template` VARCHAR(1000) DEFAULT NULL COMMENT '证书二维码2的URL生成规则(留空回退使用证书qr_url2)',
  `qr3_template` VARCHAR(1000) DEFAULT NULL COMMENT '证书二维码3的URL生成规则(留空回退使用证书qr_url3)',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书二维码URL生成规则配置表';

-- 课程关联三图(视频下方三张宣传图,可点击跳转)
DROP TABLE IF EXISTS `course_three_image`;
CREATE TABLE `course_three_image` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID(NULL=全站通用)',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '图片标题/Alt',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述文字',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `link_type` TINYINT NOT NULL DEFAULT 0 COMMENT '0-不跳转 1-跳转外链 2-跳转试卷 3-跳转课程',
  `link_url` VARCHAR(500) DEFAULT NULL COMMENT '跳转URL(link_type=1 时使用)',
  `link_id` BIGINT DEFAULT NULL COMMENT '跳转目标ID(link_type=2/3 时使用)',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '显示顺序(小的在前)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_course_id` (`course_id`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程关联三图(视频下方宣传图,可点击跳转)';

-- 友情链接(页脚展示)
DROP TABLE IF EXISTS `friendly_link`;
CREATE TABLE `friendly_link` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL COMMENT '链接名称',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图标URL',
  `link_url` VARCHAR(500) NOT NULL COMMENT '跳转URL',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='友情链接';

-- 证书模板
DROP TABLE IF EXISTS `certificate_template`;
CREATE TABLE `certificate_template` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `bg_image_url` VARCHAR(500) DEFAULT NULL COMMENT '背景图 URL',
  `bg_width` INT DEFAULT NULL COMMENT '背景图宽度(像素)',
  `bg_height` INT DEFAULT NULL COMMENT '背景图高度(像素)',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认模板',
  `stamp_url` VARCHAR(500) DEFAULT NULL COMMENT '钢印图片URL(透明背景PNG)',
  `stamp_x` INT DEFAULT NULL COMMENT '钢印X坐标',
  `stamp_y` INT DEFAULT NULL COMMENT '钢印Y坐标',
  `stamp_width` INT DEFAULT NULL COMMENT '钢印宽度(0=原尺寸)',
  `stamp_rotation` DOUBLE DEFAULT NULL COMMENT '钢印旋转角度(0-360)',
  `stamp_opacity` FLOAT DEFAULT NULL COMMENT '钢印透明度(0-1,默认0.8)',
  `cert_no_prefix` VARCHAR(10) DEFAULT NULL COMMENT '证书编号前缀字母(从此模板配置,生成证书编号时优先使用)',
  `cert_no_middle` VARCHAR(10) DEFAULT NULL COMMENT '证书编号中段字母(从此模板配置,生成证书编号时优先使用)',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书模板';

-- 证书模板字段位置
DROP TABLE IF EXISTS `certificate_template_field`;
CREATE TABLE `certificate_template_field` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `field_key` VARCHAR(50) NOT NULL COMMENT '引用 certificate_field.field_key',
  `x` INT NOT NULL DEFAULT 0 COMMENT 'X坐标(相对背景图,像素)',
  `y` INT NOT NULL DEFAULT 0 COMMENT 'Y坐标',
  `font_size` INT NOT NULL DEFAULT 24 COMMENT '字号',
  `color` VARCHAR(20) DEFAULT '#000000' COMMENT '颜色 #RRGGBB',
  `font_weight` TINYINT NOT NULL DEFAULT 1 COMMENT '1-常规 2-粗体',
  `align` TINYINT NOT NULL DEFAULT 1 COMMENT '1-左 2-居中 3-右',
  `width` INT DEFAULT NULL COMMENT '文本框宽度(用于多行,像素)',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_template_id` (`template_id`),
  KEY `idx_field_key` (`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书模板字段位置';

-- 学员照片
DROP TABLE IF EXISTS `certificate_photo`;
CREATE TABLE `certificate_photo` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `id_card` VARCHAR(30) NOT NULL COMMENT '身份证号(同一人可多张照片)',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '姓名(冗余便于管理)',
  `url` VARCHAR(500) NOT NULL COMMENT '照片 URL',
  `certificate_id` BIGINT DEFAULT NULL COMMENT '关联的证书记录ID(支持同一个人不同证书设置不同照片,为空时按身份证号匹配最新照片)',
  `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_id_card` (`id_card`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_certificate_id` (`certificate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员照片';

-- 异步任务(持久化)
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `task_id` VARCHAR(64) NOT NULL COMMENT 'UUID,业务主键',
  `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型: certificate-import / certificate-batch-generate / exam-qr-batch',
  `biz_name` VARCHAR(200) DEFAULT NULL COMMENT '业务名称(展示用)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed/cancelled',
  `progress` TINYINT NOT NULL DEFAULT 0 COMMENT '进度 0-100',
  `processed` INT NOT NULL DEFAULT 0 COMMENT '已处理数量',
  `total` INT NOT NULL DEFAULT 0 COMMENT '总数量',
  `success_count` INT NOT NULL DEFAULT 0,
  `fail_count` INT NOT NULL DEFAULT 0,
  `error_message` VARCHAR(2000) DEFAULT NULL,
  `result_file_path` VARCHAR(500) DEFAULT NULL COMMENT '结果文件磁盘路径',
  `result_file_name` VARCHAR(200) DEFAULT NULL COMMENT '结果文件名(下载用)',
  `extra_json` TEXT DEFAULT NULL COMMENT '扩展参数(任务上下文)',
  `created_by` VARCHAR(64) DEFAULT NULL,
  `start_time` DATETIME DEFAULT NULL COMMENT '实际开始执行时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_status` (`status`),
  KEY `idx_biz_type` (`biz_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务(持久化)';

-- ============================================================
-- 合作咨询/网站声明/投诉建议 相关表
-- ============================================================

-- 合作咨询配置(单条记录)
DROP TABLE IF EXISTS `cooperation_setting`;
CREATE TABLE `cooperation_setting` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `phone1` VARCHAR(50) DEFAULT NULL COMMENT '联系电话1',
  `phone2` VARCHAR(50) DEFAULT NULL COMMENT '联系电话2',
  `email1` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱1',
  `email2` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱2',
  `process_desc` LONGTEXT COMMENT '合作流程说明(富文本)',
  `intro` LONGTEXT COMMENT '合作咨询左栏-单位背景介绍(富文本)',
  `attachment_name` VARCHAR(200) DEFAULT NULL COMMENT '意向表下载-附件名',
  `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '意向表下载-附件URL',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合作咨询配置';

-- 初始化一条默认配置
INSERT INTO `cooperation_setting` (id, phone1, phone2, email1, email2, process_desc, intro)
VALUES (1, '010-67758599', '010-53397379', 'hezuo@example.com', 'fuwu@example.com',
        '1. 提交合作意向<br/>2. 初步沟通与需求评估<br/>3. 签订合作协议<br/>4. 项目实施与培训<br/>5. 持续运营支持',
        '我们致力于为合作机构提供专业的人才测评服务,合作机构覆盖全国多个省市。');

-- 网站声明(单条记录)
DROP TABLE IF EXISTS `site_declaration`;
CREATE TABLE `site_declaration` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL DEFAULT '网站声明' COMMENT '声明标题',
  `content` LONGTEXT NOT NULL COMMENT '声明内容(富文本)',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站声明';

INSERT INTO `site_declaration` (id, title, content) VALUES (1, '网站声明',
'<p>一、本网站所提供的信息仅供参考,本网站不对信息的准确性、完整性、合法性、可靠性、可用性做任何保证。</p><p>二、未经本网站书面授权,任何单位及个人不得以任何方式复制、镜像、传播、修改或以其他方式使用本网站内容。</p><p>三、本网站保留随时修改或更新本声明的权利。</p><p>四、本网站尊重并保护所有用户的个人隐私权。</p>');

-- 合作咨询/投诉建议 留言表
DROP TABLE IF EXISTS `feedback_message`;
CREATE TABLE `feedback_message` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `type` VARCHAR(20) NOT NULL DEFAULT 'cooperation' COMMENT '类型: cooperation-合作咨询 complaint-投诉建议 declaration-网站声明',
  `org_name` VARCHAR(200) DEFAULT NULL COMMENT '单位名称',
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `content` TEXT NOT NULL COMMENT '留言内容/合作意向/投诉建议',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '提交IP',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未处理 1-已处理',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '管理员备注/处理意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合作咨询/投诉建议留言表';

-- ----------------------------
-- 合作申请表
-- ----------------------------
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
  `auth_expire_date` date DEFAULT NULL COMMENT '授权有效期截止日期',
  -- 证书相关字段
  `cert_image_url` varchar(500) DEFAULT NULL COMMENT '证书背景图片URL',
  `cert_rich_text` longtext COMMENT '覆盖在证书图片上的富文本HTML',
  `cert_bg_scale` int DEFAULT 100 COMMENT '证书背景图缩放比例(30-100)',
  `cert_editor_width` int DEFAULT NULL COMMENT '编辑证书时编辑区文本宽度(像素)',
  -- 审计字段
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_unit_name` (`unit_name`),
  KEY `idx_auth_code` (`auth_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合作申请表';

SET FOREIGN_KEY_CHECKS = 1;
