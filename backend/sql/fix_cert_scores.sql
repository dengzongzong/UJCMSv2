-- =====================================================================
-- 回填证书主表成绩字段: 从 extra_json 同步到 theory_score/practical_score/comprehensive_evaluation
-- 问题: 导入时成绩只写入 extra_json, 主表列为空, 导致前端列表不显示
-- 执行方式: mysql -u root -p'Root@123456' exam_platform < fix_cert_scores.sql
-- 安全说明: 本脚本可安全重复执行(幂等), 仅更新主表列为空的记录
-- =====================================================================

-- 1. 回填 theory_score (主表列为空时, 从 extra_json 取值)
UPDATE certificate
SET theory_score = JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.theoryScore'))
WHERE (theory_score IS NULL OR theory_score = '')
  AND extra_json IS NOT NULL
  AND JSON_EXTRACT(extra_json, '$.theoryScore') IS NOT NULL;

-- 2. 回填 practical_score
UPDATE certificate
SET practical_score = JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.practicalScore'))
WHERE (practical_score IS NULL OR practical_score = '')
  AND extra_json IS NOT NULL
  AND JSON_EXTRACT(extra_json, '$.practicalScore') IS NOT NULL;

-- 3. 回填 comprehensive_evaluation
UPDATE certificate
SET comprehensive_evaluation = JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.comprehensiveEvaluation'))
WHERE (comprehensive_evaluation IS NULL OR comprehensive_evaluation = '')
  AND extra_json IS NOT NULL
  AND JSON_EXTRACT(extra_json, '$.comprehensiveEvaluation') IS NOT NULL;

-- ====== 验证(查询用,不影响数据) ======
-- SELECT COUNT(*) AS total,
--        SUM(theory_score IS NOT NULL AND theory_score != '') AS has_theory,
--        SUM(practical_score IS NOT NULL AND practical_score != '') AS has_practical,
--        SUM(comprehensive_evaluation IS NOT NULL AND comprehensive_evaluation != '') AS has_eval
-- FROM certificate;
