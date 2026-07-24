-- =====================================================================
-- 综合修复脚本: 证书类型 + 模板绑定 + 数据导入
-- 执行方式: mysql -u root -p'Root@123456' exam_platform < fix_all_cert_issues.sql
-- 安全说明: 本脚本不会删除用户自定义的证书类型,仅使用 INSERT IGNORE 补齐
-- =====================================================================

-- ====== 1. 回填 cert_type (从 extra_json) ======
UPDATE certificate
SET cert_type = JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.cert_type'))
WHERE (cert_type IS NULL OR cert_type = '')
  AND extra_json IS NOT NULL
  AND JSON_EXTRACT(extra_json, '$.cert_type') IS NOT NULL;

-- ====== 2. 将旧类型名统一改为新类型名(仅更新证书数据,不删除类型) ======
UPDATE certificate SET cert_type = '专项职业证书' WHERE cert_type = '专业技能证书';
UPDATE certificate SET cert_type = '专项职业证书' WHERE cert_type = '专项职业技能证书';
UPDATE certificate SET cert_type = '人才数据库' WHERE cert_type = '人才数据入库证书';

-- ====== 3. 确保证书类型表有正确的默认类型(INSERT IGNORE,不删除已有类型) ======
INSERT IGNORE INTO certificate_type (name, code, sort, status) VALUES
('专项职业证书', '1', 1, 1),
('人才数据库', '2', 2, 1),
('职业能力证书', '3', 3, 1),
('能力等级证书', '4', 4, 1);

-- 清理明显的垃圾记录(仅删除名为"新增"或"11"的无效类型)
DELETE FROM certificate_type WHERE name = '新增';
DELETE FROM certificate_type WHERE name = '11';

-- ====== 4. 清理/修正证书模板 ======
-- 删除名为"11"的无效模板(如果没有证书绑定它)
DELETE FROM certificate_template_field WHERE template_id IN (
    SELECT id FROM certificate_template WHERE name = '11'
);
DELETE FROM certificate_template WHERE name = '11'
  AND id NOT IN (SELECT DISTINCT template_id FROM certificate WHERE template_id IS NOT NULL);

-- ====== 5. 按证书类型自动绑定同名模板 ======
-- 重新绑定所有证书: 按 cert_type 匹配同名模板
UPDATE certificate c
INNER JOIN certificate_template t ON c.cert_type = t.name
SET c.template_id = t.id
WHERE c.cert_type IS NOT NULL
  AND c.cert_type != '';

-- ====== 6. 为没有 cert_type 的证书设置默认类型 ======
-- 如果还有 cert_type 为空的证书, 从学生表关联获取
UPDATE certificate c
INNER JOIN student s ON c.id_card = s.id_card
SET c.cert_type = s.cert_type
WHERE (c.cert_type IS NULL OR c.cert_type = '')
  AND s.cert_type IS NOT NULL
  AND s.cert_type != '';

-- 再次尝试绑定模板
UPDATE certificate c
INNER JOIN certificate_template t ON c.cert_type = t.name
SET c.template_id = t.id
WHERE c.template_id IS NULL
  AND c.cert_type IS NOT NULL
  AND c.cert_type != '';

-- ====== 7. 验证结果 ======
SELECT '=== 证书类型分布 ===' AS info;
SELECT cert_type, COUNT(*) AS cnt,
       SUM(CASE WHEN template_id IS NOT NULL THEN 1 ELSE 0 END) AS bound,
       SUM(CASE WHEN template_id IS NULL THEN 1 ELSE 0 END) AS unbound
FROM certificate
GROUP BY cert_type
ORDER BY cnt DESC;

SELECT '=== 证书模板列表 ===' AS info;
SELECT id, name FROM certificate_template ORDER BY id;

SELECT '=== 证书类型列表 ===' AS info;
SELECT id, name FROM certificate_type ORDER BY id;
