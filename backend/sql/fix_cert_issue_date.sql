-- =====================================================================
-- 修复证书办法日期字段: 模板字段 birthday→issueDate + 证书数据补全
-- 问题: 职业能力证书模板中,颁发日期位置误用了 birthday(出生日期)字段,
--       导致证书图片显示出生日期而非颁发日期
-- 执行方式: mysql -u root -p'Root@123456' exam_platform < fix_cert_issue_date.sql
-- 安全说明: 本脚本可安全重复执行(幂等)
-- =====================================================================

-- ====== 1. 修复模板字段配置: 将误用的 birthday 改为 issueDate ======
-- 策略: 对于每个模板,如果该模板有 birthday 字段但没有 issueDate 字段,
--       则将 birthday 改为 issueDate(说明 birthday 被误用为颁发日期)
UPDATE certificate_template_field ct
SET ct.field_key = 'issueDate'
WHERE ct.field_key = 'birthday'
  AND NOT EXISTS (
    SELECT 1 FROM (
      SELECT 1 FROM certificate_template_field ct2
      WHERE ct2.template_id = ct.template_id
        AND ct2.field_key = 'issueDate'
    ) tmp
  );

-- ====== 2. 确保证书的 issue_date 不为空 ======
-- 如果 issue_date 为空,则用 create_time 的日期填充
UPDATE certificate
SET issue_date = DATE(create_time)
WHERE issue_date IS NULL;

-- ====== 3. 验证修复结果(查询用,不影响数据) ======
-- 查看各模板的字段配置(确认 birthday 已改为 issueDate)
-- SELECT t.name AS template_name, ct.field_key, ct.x, ct.y
-- FROM certificate_template_field ct
-- JOIN certificate_template t ON ct.template_id = t.id
-- WHERE ct.field_key IN ('birthday', 'issueDate')
-- ORDER BY t.name, ct.field_key;

-- 查看证书的 issue_date 是否已填充
-- SELECT COUNT(*) AS total, SUM(issue_date IS NULL) AS null_count FROM certificate;
