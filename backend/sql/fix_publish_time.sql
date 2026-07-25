-- =====================================================================
-- 修复脚本: 文章 publish_time 为空导致日期显示为批量导入时间(2025-07-17)
-- 执行方式: mysql -u exam_user -p'Test_1234' exam_platform < fix_publish_time.sql
-- 说明: 将 publish_time 为空的记录用 create_time 填充(幂等,可重复执行)
-- =====================================================================

SET NAMES utf8mb4;

-- 新闻动态
UPDATE news SET publish_time = create_time WHERE publish_time IS NULL AND create_time IS NOT NULL;

-- 信息公开/政策法规/重大活动(首页内容板块)
UPDATE homepage_section SET publish_time = create_time WHERE publish_time IS NULL AND create_time IS NOT NULL;

-- 系统公告
UPDATE announcement SET publish_time = create_time WHERE publish_time IS NULL AND create_time IS NOT NULL;

-- 验证
SELECT '=== news publish_time 空值检查 ===' AS info;
SELECT COUNT(*) AS null_count FROM news WHERE publish_time IS NULL;
SELECT '=== homepage_section publish_time 空值检查 ===' AS info;
SELECT COUNT(*) AS null_count FROM homepage_section WHERE publish_time IS NULL;
SELECT '=== announcement publish_time 空值检查 ===' AS info;
SELECT COUNT(*) AS null_count FROM announcement WHERE publish_time IS NULL;
