#!/usr/bin/env python3
"""
证书恢复数据修复脚本 — 补全 photoUrl / certType / templateId / 成绩字段
=====================================================================
背景:
  binlog 恢复只能恢复 DELETE 事件中记录的列值。如果 cert_type、theory_score
  等列是在数据被删除之后由 upgrade_all.sql 新增的,则 binlog 中不包含这些列
  的值,恢复后这些字段为 NULL。

  但 extra_json 列在删除前就已存在,其中通常包含 cert_type、theoryScore、
  practicalScore、comprehensiveEvaluation 等键值。本脚本从 extra_json 中
  提取这些值,回写到独立列,并重新绑定模板。

修复内容:
  1. 从 extra_json 提取 cert_type → 回写 certificate.cert_type
  2. 从 extra_json 提取 theoryScore/practicalScore/comprehensiveEvaluation
     → 回写 certificate 独立列
  3. 按 cert_type 匹配 certificate_template.name → 回写 certificate.template_id
  4. 验证 certificate_photo 关联: 按 id_card 回填 certificate_id

用法: python3 fix_recovered_fields.py
"""

import subprocess
import sys
import os
import json
import re

# ===== 配置 =====
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = os.environ.get("DB_PORT", "3306")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASSWORD = os.environ.get("MYSQL_PASS", os.environ.get("DB_PASSWORD", "Root@123456"))
DB_NAME = os.environ.get("DB_NAME", "exam_platform")


def run_mysql(sql, timeout=60):
    """执行MySQL命令,返回stdout"""
    cmd = [
        "mysql", f"-h{DB_HOST}", f"-P{DB_PORT}",
        f"-u{DB_USER}", f"-p{DB_PASSWORD}",
        DB_NAME, "-N", "-e", sql
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    if result.returncode != 0:
        print(f"  MySQL错误: {result.stderr.strip()[:500]}")
        return None
    return result.stdout.strip()


def run_mysql_silent(sql, timeout=30):
    """执行MySQL命令,不打印错误"""
    cmd = [
        "mysql", f"-h{DB_HOST}", f"-P{DB_PORT}",
        f"-u{DB_USER}", f"-p{DB_PASSWORD}",
        DB_NAME, "-N", "-e", sql
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    return result.returncode == 0


def escape_sql(val):
    if val is None:
        return "NULL"
    val = str(val).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{val}'"


def fix_cert_type_from_extra_json():
    """从 extra_json 提取 cert_type,回写到 cert_type 列"""
    print("\n━━━ 1. 从 extra_json 提取 cert_type ━━━")

    # 查询 cert_type 为 NULL 但 extra_json 中有 cert_type 的记录
    rows = run_mysql(
        "SELECT id, extra_json FROM certificate "
        "WHERE (cert_type IS NULL OR cert_type = '') "
        "AND extra_json IS NOT NULL AND extra_json != '' "
        "AND (extra_json LIKE '%cert_type%' OR extra_json LIKE '%certType%');"
    )
    if not rows:
        print("  无需修复: 所有记录的 cert_type 已有值或 extra_json 中无 cert_type")
        return 0

    records = []
    for line in rows.split("\n"):
        line = line.strip()
        if not line:
            continue
        # 格式: id\textra_json
        parts = line.split("\t", 1)
        if len(parts) < 2:
            continue
        cert_id = parts[0]
        extra_json_str = parts[1]
        try:
            extra = json.loads(extra_json_str)
            cert_type = None
            # 按优先级查找
            for key in ["cert_type", "certType", "cert_type_name"]:
                if key in extra and extra[key]:
                    cert_type = str(extra[key]).strip()
                    break
            if cert_type:
                records.append((cert_id, cert_type))
        except (json.JSONDecodeError, TypeError):
            continue

    print(f"  找到 {len(records)} 条需要修复 cert_type 的记录")

    fixed = 0
    for cert_id, cert_type in records:
        sql = f"UPDATE certificate SET cert_type = {escape_sql(cert_type)}, update_time = NOW() WHERE id = {cert_id};"
        if run_mysql_silent(sql):
            fixed += 1

    print(f"  ✅ 成功修复 cert_type: {fixed} 条")
    return fixed


def fix_scores_from_extra_json():
    """从 extra_json 提取成绩字段,回写到独立列"""
    print("\n━━━ 2. 从 extra_json 提取成绩字段 ━━━")

    # 查询成绩字段为 NULL 但 extra_json 中有成绩的记录
    rows = run_mysql(
        "SELECT id, extra_json FROM certificate "
        "WHERE (theory_score IS NULL OR practical_score IS NULL OR comprehensive_evaluation IS NULL) "
        "AND extra_json IS NOT NULL AND extra_json != '' "
        "AND (extra_json LIKE '%theoryScore%' OR extra_json LIKE '%practicalScore%' "
        "OR extra_json LIKE '%comprehensiveEvaluation%' OR extra_json LIKE '%theory_score%' "
        "OR extra_json LIKE '%practical_score%' OR extra_json LIKE '%comprehensive_score%' "
        "OR extra_json LIKE '%ext_llzscjc%' OR extra_json LIKE '%ext_czjncjc%' "
        "OR extra_json LIKE '%ext_zhpjcj%');"
    )
    if not rows:
        print("  无需修复: 所有记录的成绩字段已有值或 extra_json 中无成绩")
        return 0

    records = []
    for line in rows.split("\n"):
        line = line.strip()
        if not line:
            continue
        parts = line.split("\t", 1)
        if len(parts) < 2:
            continue
        cert_id = parts[0]
        extra_json_str = parts[1]
        try:
            extra = json.loads(extra_json_str)

            # 理论成绩: 优先用标准键,回退到旧键名
            theory = None
            for key in ["theoryScore", "theory_score", "ext_llzscjc", "ext_llzscjd"]:
                if key in extra and extra[key] is not None and str(extra[key]).strip():
                    theory = str(extra[key]).strip()
                    break

            # 实操成绩
            practical = None
            for key in ["practicalScore", "practical_score", "skill_score", "ext_czjncjc", "ext_czjncjd"]:
                if key in extra and extra[key] is not None and str(extra[key]).strip():
                    practical = str(extra[key]).strip()
                    break

            # 综合测评
            comprehensive = None
            for key in ["comprehensiveEvaluation", "comprehensive_score", "ext_zhpjcj", "ext_zhpjcjd"]:
                if key in extra and extra[key] is not None and str(extra[key]).strip():
                    comprehensive = str(extra[key]).strip()
                    break

            if theory or practical or comprehensive:
                records.append((cert_id, theory, practical, comprehensive))
        except (json.JSONDecodeError, TypeError):
            continue

    print(f"  找到 {len(records)} 条需要修复成绩的记录")

    fixed = 0
    for cert_id, theory, practical, comprehensive in records:
        sets = []
        if theory:
            sets.append(f"theory_score = {escape_sql(theory)}")
        if practical:
            sets.append(f"practical_score = {escape_sql(practical)}")
        if comprehensive:
            sets.append(f"comprehensive_evaluation = {escape_sql(comprehensive)}")
        sets.append("update_time = NOW()")
        sql = f"UPDATE certificate SET {', '.join(sets)} WHERE id = {cert_id};"
        if run_mysql_silent(sql):
            fixed += 1

    print(f"  ✅ 成功修复成绩字段: {fixed} 条")
    return fixed


def fix_template_id_by_cert_type():
    """按 cert_type 匹配 certificate_template.name,回写 template_id"""
    print("\n━━━ 3. 按 cert_type 重新绑定证书模板 ━━━")

    # 获取所有模板的 name -> id 映射
    rows = run_mysql("SELECT id, name FROM certificate_template WHERE name IS NOT NULL;")
    if not rows:
        print("  ⚠️ certificate_template 表为空,无法绑定模板")
        return 0

    template_map = {}
    for line in rows.split("\n"):
        parts = line.strip().split("\t", 1)
        if len(parts) == 2:
            template_map[parts[1].strip()] = parts[0]

    print(f"  可用模板: {len(template_map)} 个")
    for name, tid in template_map.items():
        print(f"    - {name} (id={tid})")

    # 查询 template_id 为 NULL 但 cert_type 有值的记录
    rows = run_mysql(
        "SELECT id, cert_type FROM certificate "
        "WHERE template_id IS NULL AND cert_type IS NOT NULL AND cert_type != '';"
    )
    if not rows:
        print("  无需修复: 所有记录的 template_id 已有值或 cert_type 为空")
        return 0

    records = []
    for line in rows.split("\n"):
        parts = line.strip().split("\t", 1)
        if len(parts) == 2:
            cert_id = parts[0]
            cert_type = parts[1].strip()
            if cert_type in template_map:
                records.append((cert_id, template_map[cert_type]))

    print(f"  找到 {len(records)} 条可以绑定模板的记录")

    fixed = 0
    for cert_id, template_id in records:
        sql = f"UPDATE certificate SET template_id = {template_id}, update_time = NOW() WHERE id = {cert_id};"
        if run_mysql_silent(sql):
            fixed += 1

    print(f"  ✅ 成功绑定模板: {fixed} 条")
    return fixed


def fix_photo_associations():
    """验证并修复 certificate_photo 的 certificate_id 关联"""
    print("\n━━━ 4. 修复照片关联(certificate_photo.certificate_id) ━━━")

    # 统计当前照片关联状态
    total_photos = run_mysql("SELECT COUNT(*) FROM certificate_photo;") or "0"
    photos_with_cert_id = run_mysql(
        "SELECT COUNT(*) FROM certificate_photo WHERE certificate_id IS NOT NULL;"
    ) or "0"
    photos_without_cert_id = run_mysql(
        "SELECT COUNT(*) FROM certificate_photo WHERE certificate_id IS NULL;"
    ) or "0"
    orphan_photos = run_mysql(
        "SELECT COUNT(*) FROM certificate_photo cp "
        "WHERE cp.certificate_id IS NOT NULL "
        "AND cp.certificate_id NOT IN (SELECT id FROM certificate);"
    ) or "0"

    print(f"  照片总数: {total_photos}")
    print(f"  有 certificate_id 的: {photos_with_cert_id}")
    print(f"  无 certificate_id 的: {photos_without_cert_id}")
    print(f"  certificate_id 指向不存在证书的: {orphan_photos}")

    # 修复: 为 certificate_id 为 NULL 的照片,按 id_card 匹配证书
    rows = run_mysql(
        "SELECT cp.id, cp.id_card FROM certificate_photo cp "
        "WHERE (cp.certificate_id IS NULL "
        "OR cp.certificate_id NOT IN (SELECT id FROM certificate)) "
        "AND cp.id_card IS NOT NULL AND cp.id_card != '';"
    )
    if not rows:
        print("  无需修复: 所有照片关联正常")
        return 0

    fixed = 0
    for line in rows.split("\n"):
        parts = line.strip().split("\t", 1)
        if len(parts) != 2:
            continue
        photo_id = parts[0]
        id_card = parts[1].strip()
        # 查找该身份证号对应的证书记录(取最新的一条)
        cert_id = run_mysql(
            f"SELECT id FROM certificate WHERE id_card = {escape_sql(id_card)} "
            f"ORDER BY id DESC LIMIT 1;"
        )
        if cert_id:
            sql = f"UPDATE certificate_photo SET certificate_id = {cert_id} WHERE id = {photo_id};"
            if run_mysql_silent(sql):
                fixed += 1

    print(f"  ✅ 成功修复照片关联: {fixed} 条")
    return fixed


def verify_recovery():
    """验证修复结果"""
    print("\n━━━ 5. 验证修复结果 ━━━")

    total = run_mysql("SELECT COUNT(*) FROM certificate;") or "0"
    has_cert_type = run_mysql(
        "SELECT COUNT(*) FROM certificate WHERE cert_type IS NOT NULL AND cert_type != '';"
    ) or "0"
    has_template = run_mysql(
        "SELECT COUNT(*) FROM certificate WHERE template_id IS NOT NULL;"
    ) or "0"
    has_theory = run_mysql(
        "SELECT COUNT(*) FROM certificate WHERE theory_score IS NOT NULL AND theory_score != '';"
    ) or "0"
    has_practical = run_mysql(
        "SELECT COUNT(*) FROM certificate WHERE practical_score IS NOT NULL AND practical_score != '';"
    ) or "0"
    has_photo = run_mysql(
        "SELECT COUNT(*) FROM certificate c "
        "WHERE EXISTS (SELECT 1 FROM certificate_photo cp WHERE cp.certificate_id = c.id) "
        "OR EXISTS (SELECT 1 FROM certificate_photo cp WHERE cp.id_card = c.id_card);"
    ) or "0"

    print(f"  证书总数: {total}")
    print(f"  有证书类型(cert_type): {has_cert_type}")
    print(f"  有模板(template_id): {has_template}")
    print(f"  有理论成绩: {has_theory}")
    print(f"  有实操成绩: {has_practical}")
    print(f"  有照片(photoUrl可查): {has_photo}")


def main():
    print()
    print("╔══════════════════════════════════════════════════════════════╗")
    print("║  证书恢复数据修复 — 补全 certType/templateId/成绩/照片关联   ║")
    print("║  原理: 从 extra_json 提取字段值,回写独立列 + 重新绑定模板   ║")
    print("╚══════════════════════════════════════════════════════════════╝")
    print()

    # 检查MySQL连接
    check = run_mysql("SELECT 1")
    if check is None:
        print("❌ MySQL连接失败!")
        return

    print(f"  ✅ MySQL连接正常")

    # 执行修复
    fix_cert_type_from_extra_json()
    fix_scores_from_extra_json()
    fix_template_id_by_cert_type()
    fix_photo_associations()

    # 验证
    verify_recovery()

    print()
    print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    print("  修复完成!")
    print("  如果某些字段仍为空,说明原始数据中(extra_json)也未保存这些值,")
    print("  这种情况下数据已无法从 binlog 恢复,需要从其他来源(如导入文件)补录。")
    print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")


if __name__ == "__main__":
    main()
