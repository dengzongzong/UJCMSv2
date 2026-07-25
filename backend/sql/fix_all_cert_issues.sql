-- =====================================================================
-- 综合修复脚本: 证书类型 + 模板绑定 + 数据导入
-- 执行方式: mysql -u root -p'Root@123456' exam_platform < fix_all_cert_issues.sql
-- 安全说明: 本脚本不会删除用户自定义的证书类型,仅使用 INSERT IGNORE 补齐
-- 重要: 本脚本可安全重复执行(幂等),不会破坏用户手动新增的证书类型
-- =====================================================================

-- ====== 1. 回填 cert_type (从 extra_json) ======
-- 仅更新 cert_type 为空的记录,不会覆盖已有的类型名
UPDATE certificate
SET cert_type = JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.cert_type'))
WHERE (cert_type IS NULL OR cert_type = '')
  AND extra_json IS NOT NULL
  AND JSON_EXTRACT(extra_json, '$.cert_type') IS NOT NULL;

-- ====== 2. 确保证书类型表有正确的默认类型(INSERT IGNORE,不删除已有类型) ======
INSERT IGNORE INTO certificate_type (name, code, sort, status) VALUES
('专项职业证书', '1', 1, 1),
('人才数据库', '2', 2, 1),
('职业能力证书', '3', 3, 1),
('能力等级证书', '4', 4, 1);

-- 注意: 不再按名称硬删除用户自定义类型(之前的 DELETE FROM certificate_type WHERE name = '新增' / '11' 已移除)
-- 注意: 不再无条件改写证书的 cert_type 字段(之前的 UPDATE certificate SET cert_type = '专项职业证书' WHERE cert_type = '专业技能证书' 等已移除)
-- 这些操作会导致用户自定义类型与证书的关联被破坏

-- ====== 3. 清理/修正证书模板(仅删除名为"11"且无证书绑定的无效模板) ======
DELETE FROM certificate_template_field WHERE template_id IN (
    SELECT id FROM certificate_template WHERE name = '11'
      AND id NOT IN (SELECT DISTINCT template_id FROM certificate WHERE template_id IS NOT NULL)
);
DELETE FROM certificate_template WHERE name = '11'
  AND id NOT IN (SELECT DISTINCT template_id FROM certificate WHERE template_id IS NOT NULL);

-- ====== 4. 按证书类型自动绑定同名模板 ======
-- 仅绑定 template_id 为空的证书,不会覆盖已有的模板绑定
UPDATE certificate c
INNER JOIN certificate_template t ON c.cert_type = t.name
SET c.template_id = t.id
WHERE c.template_id IS NULL
  AND c.cert_type IS NOT NULL
  AND c.cert_type != '';

-- ====== 5. 为没有 cert_type 的证书设置默认类型 ======
-- 如果还有 cert_type 为空的证书, 从学生表关联获取
UPDATE certificate c
INNER JOIN student s ON c.id_card = s.id_card
SET c.cert_type = s.cert_type
WHERE (c.cert_type IS NULL OR c.cert_type = '')
  AND s.cert_type IS NOT NULL
  AND s.cert_type != '';

-- 再次尝试绑定模板(仅绑定未绑定的)
UPDATE certificate c
INNER JOIN certificate_template t ON c.cert_type = t.name
SET c.template_id = t.id
WHERE c.template_id IS NULL
  AND c.cert_type IS NOT NULL
  AND c.cert_type != '';

-- ====== 6. 验证结果 ======
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
