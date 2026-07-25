-- =====================================================================
-- 修复 student 表 id_card 空字符串导致唯一约束冲突
-- ---------------------------------------------------------------------
-- 问题: student 表有 uk_id_card 唯一索引, 当不填身份证时插入空串 '',
--       多条空串记录冲突, 导致新增学生报 500 错误。
-- 修复: 将所有空字符串 id_card 统一转为 NULL (NULL 不受唯一约束限制)。
-- 幂等: 可重复执行, 只影响空字符串记录。
-- =====================================================================

-- 1. 将 student 表中空字符串 id_card 转为 NULL
UPDATE student SET id_card = NULL WHERE id_card = '';

-- 2. 确保 uk_id_card 唯一索引存在 (幂等, 用 information_schema 判断)
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'student' AND index_name = 'uk_id_card');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE student ADD UNIQUE INDEX uk_id_card (id_card)',
    'SELECT ''uk_id_card already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
