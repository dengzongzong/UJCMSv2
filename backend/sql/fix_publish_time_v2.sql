-- =====================================================================
-- 修复脚本 v2: 信息公开/新闻/公告 时间显示全部相同的问题
-- 根因: 批量导入的记录 publish_time = create_time,全部相同
-- 方案: 对 publish_time = create_time 的记录,按 id 偏移分钟数,使每条时间不同
-- 幂等: 第一次执行后 publish_time != create_time,再次执行不会重复修改
-- =====================================================================

SET NAMES utf8mb4;

-- 1. 信息公开 (homepage_section type=2): 按 id 偏移 37 分钟
UPDATE homepage_section h
SET h.publish_time = DATE_ADD(h.create_time, INTERVAL (h.id * 37) MINUTE)
WHERE h.type = 2
  AND h.publish_time = h.create_time
  AND h.create_time IS NOT NULL;

-- 2. 政策法规 (homepage_section type=1): 按 id 偏移 41 分钟
UPDATE homepage_section h
SET h.publish_time = DATE_ADD(h.create_time, INTERVAL (h.id * 41) MINUTE)
WHERE h.type = 1
  AND h.publish_time = h.create_time
  AND h.create_time IS NOT NULL;

-- 3. 新闻动态 (news): 按 id 偏移 53 分钟
UPDATE news n
SET n.publish_time = DATE_ADD(n.create_time, INTERVAL (n.id * 53) MINUTE)
WHERE n.publish_time = n.create_time
  AND n.create_time IS NOT NULL;

-- 4. 系统公告 (announcement): 按 id 偏移 47 分钟
UPDATE announcement a
SET a.publish_time = DATE_ADD(a.create_time, INTERVAL (a.id * 47) MINUTE)
WHERE a.publish_time = a.create_time
  AND a.create_time IS NOT NULL;

-- 验证: 检查是否还有大量相同时间
SELECT '=== homepage_section type=2 publish_time 分布 ===' AS info;
SELECT publish_time, COUNT(*) AS cnt FROM homepage_section WHERE type = 2 GROUP BY publish_time HAVING cnt > 1;
SELECT '=== news publish_time 分布 ===' AS info;
SELECT publish_time, COUNT(*) AS cnt FROM news GROUP BY publish_time HAVING cnt > 1;
SELECT '=== announcement publish_time 分布 ===' AS info;
SELECT publish_time, COUNT(*) AS cnt FROM announcement GROUP BY publish_time HAVING cnt > 1;
