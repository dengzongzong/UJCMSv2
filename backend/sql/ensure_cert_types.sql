-- =====================================================================
-- 修复脚本: 确保五种证书类型全部存在
-- 执行方式: mysql -u root -p'Root@123456' exam_platform < ensure_cert_types.sql
-- 安全说明: 仅使用 INSERT IGNORE 补齐缺失类型,不删除任何已有类型
-- 五种证书类型:
--   1. 专业技能证书 (数据量最大,约6941条)
--   2. 专项职业证书 (约2043条)
--   3. 职业能力证书 (约332条)
--   4. 能力等级证书 (约152条)
--   5. 人才数据库   (约2条)
-- =====================================================================

-- ====== 1. 补齐缺失的证书类型(INSERT IGNORE, 已存在的不重复插入) ======
INSERT IGNORE INTO certificate_type (name, code, sort, status) VALUES
('专业技能证书', '5', 5, 1),
('专项职业证书', '1', 1, 1),
('人才数据库', '2', 2, 1),
('职业能力证书', '3', 3, 1),
('能力等级证书', '4', 4, 1);

-- ====== 2. 验证结果 ======
SELECT '=== 当前证书类型列表 ===' AS info;
SELECT id, name, code, sort, status FROM certificate_type ORDER BY sort, id;

-- ====== 3. 验证各类型证书数量分布 ======
SELECT '=== 证书类型分布 ===' AS info;
SELECT
    COALESCE(NULLIF(cert_type, ''), '(未分类)') AS cert_type,
    COUNT(*) AS cnt
FROM certificate
GROUP BY cert_type
ORDER BY cnt DESC;
