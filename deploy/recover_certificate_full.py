#!/usr/bin/env python3
"""
证书数据完整恢复脚本 v6 - 修复关键字段丢失问题
===================================================
核心修复(相比v5):
  1. 【关键】检查 binlog_row_image 设置
     → 如果是 MINIMAL,DELETE 事件只记录主键列,其他列值不在 binlog 中
     → 如果是 FULL(默认),DELETE 事件记录所有列,可以完整恢复
  2. 【关键】对已存在的记录用 UPDATE 补全缺失字段(不再跳过)
     → v5 的 bug: 用 INSERT IGNORE + check_existing_ids 跳过已存在记录
     → 如果之前恢复时插入了残缺数据(只有 id/name/id_card,没有 cert_type/scores),
       v5 会跳过这些记录,导致字段永远无法补全
     → v6: 对已存在的记录执行 UPDATE,用 binlog 中的完整数据覆盖残缺数据
  3. 新增 binlog 原始内容采样诊断
     → 打印第一条 DELETE 事件的原始 @N=value 内容,方便确认哪些列有值
  4. 直接读取本地 binlog 文件,不连接 MySQL 读 binlog

用法: python3 recover_certificate_full.py

在生产服务器(43.162.107.232)上运行
"""

import subprocess
import sys
import os
import re
import shutil
from datetime import datetime

# ===== 配置(支持环境变量覆盖,便于部署脚本调用) =====
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = os.environ.get("DB_PORT", "3306")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASSWORD = os.environ.get("MYSQL_PASS", os.environ.get("DB_PASSWORD", "Root@123456"))
DB_NAME = os.environ.get("DB_NAME", "exam_platform")

# 恢复时间范围: 今天12点到14点
RECOVER_START = "2026-07-28 12:00:00"
RECOVER_END   = "2026-07-28 14:00:00"

# MySQL数据目录(binlog文件所在位置)
MYSQL_DATA_DIR = "/var/lib/mysql"

# 需要恢复的表列表(按依赖顺序:先字典/模板,再主表,最后关联表)
RECOVER_TABLES = [
    "certificate_type",            # 证书类型字典(先恢复,主表依赖类型名)
    "certificate_template",        # 证书模板
    "certificate_template_field",  # 模板字段位置
    "certificate_export_column",   # 导出列配置
    "certificate",                 # 证书主表
    "certificate_photo",           # 学员照片(依赖 certificate.id)
    "certificate_user",            # 证书用户关联
]

# certificate 表上需要永久删除的唯一索引(不再重建)
CERT_UNIQUE_INDEXES = [
    "uk_idcard_profession",  # (id_card, profession) — 主要元凶,导致约1000条数据被静默丢弃
    "uk_cert_no",            # cert_no
    "uk_student_no",         # student_no
]

# 诊断模式: 打印第一条 DELETE 事件的原始内容
DIAGNOSTIC_SAMPLE = True


def run_mysql_cmd(sql, timeout=30):
    """执行MySQL命令"""
    cmd = [
        "mysql", f"-h{DB_HOST}", f"-P{DB_PORT}",
        f"-u{DB_USER}", f"-p{DB_PASSWORD}",
        DB_NAME, "-N", "-e", sql
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    if result.returncode != 0:
        return None
    return result.stdout.strip()


def run_mysql_cmd_verbose(sql, timeout=60):
    """执行MySQL命令(带错误输出)"""
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


def get_table_columns(table_name):
    """动态获取表的列名列表(按实际物理顺序)"""
    result = run_mysql_cmd(
        f"SELECT COLUMN_NAME FROM information_schema.COLUMNS "
        f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='{table_name}' "
        f"ORDER BY ORDINAL_POSITION;"
    )
    if not result:
        return []
    return [line.strip() for line in result.split("\n") if line.strip()]


def get_binlog_files_list():
    """获取binlog文件列表"""
    cmd = [
        "mysql", f"-h{DB_HOST}", f"-P{DB_PORT}",
        f"-u{DB_USER}", f"-p{DB_PASSWORD}",
        DB_NAME, "-N", "-e", "SHOW BINARY LOGS;"
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        print(f"  获取binlog列表失败: {result.stderr}")
        return []

    files = []
    for line in result.stdout.strip().split("\n"):
        parts = line.split()
        if parts:
            files.append({
                "name": parts[0],
                "size": int(parts[1]) if len(parts) > 1 else 0
            })
    return files


def find_local_binlog(binlog_name):
    """在本地文件系统中查找binlog文件"""
    possible_paths = [
        os.path.join(MYSQL_DATA_DIR, binlog_name),
        f"/var/lib/mysql/{binlog_name}",
        f"/var/log/mysql/{binlog_name}",
        f"/data/mysql/{binlog_name}",
    ]
    for path in possible_paths:
        if os.path.exists(path):
            return path
    try:
        result = subprocess.run(
            ["find", "/", "-name", binlog_name, "-type", "f", "-not", "-path", "*/proc/*"],
            capture_output=True, text=True, timeout=30
        )
        if result.stdout.strip():
            return result.stdout.strip().split("\n")[0]
    except:
        pass
    return None


def get_binlog_time_range_local(binlog_path):
    """读取本地binlog文件的起始时间"""
    cmd = [
        "mysqlbinlog", "--base64-output=DECODE-ROWS", "-v",
        f"--start-position=4", "--stop-position=2000",
        binlog_path
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    for line in result.stdout.split("\n"):
        if "#" in line and "server id" in line:
            match = re.search(r'#(\d{6}\s+\d{2}:\d{2}:\d{2})', line)
            if match:
                try:
                    return datetime.strptime("20" + match.group(1), "%Y%m%d %H:%M:%S")
                except:
                    pass
            break
    return None


def scan_binlog_for_table_deletes(binlog_path, start_dt, end_dt, table_name, columns, diagnostic=False):
    """扫描本地binlog文件,提取指定表的DELETE记录

    diagnostic=True 时,打印第一条 DELETE 事件的原始内容用于诊断
    """
    cmd = [
        "mysqlbinlog", "--base64-output=DECODE-ROWS", "-v",
        f"--start-datetime={start_dt}",
        f"--stop-datetime={end_dt}",
        binlog_path
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
    if result.returncode != 0:
        print(f"    mysqlbinlog错误: {result.stderr[:300]}")
        return []

    output = result.stdout
    deleted_records = []
    target_pattern = f"`{DB_NAME}`.`{table_name}`"
    lines = output.split("\n")
    i = 0
    first_delete_printed = False

    while i < len(lines):
        line = lines[i].strip()
        if "### DELETE FROM" in line and target_pattern in line:
            values = {}
            raw_lines = []  # 保存原始行用于诊断
            i += 1
            while i < len(lines):
                col_line = lines[i].strip()
                if not col_line.startswith("###"):
                    break
                raw_lines.append(col_line)
                match = re.match(r'###\s+@(\d+)=(.*)', col_line)
                if match:
                    col_idx = int(match.group(1))
                    col_val = match.group(2).strip()
                    if 1 <= col_idx <= len(columns):
                        col_name = columns[col_idx - 1]
                        if col_val == 'NULL':
                            values[col_name] = None
                        else:
                            col_val = col_val.strip()
                            if col_val.startswith("'") and col_val.endswith("'"):
                                col_val = col_val[1:-1]
                                col_val = col_val.replace("\\'", "'").replace("\\\\", "\\")
                            values[col_name] = col_val
                    else:
                        # @N 超出列数范围 — 说明 binlog 中的列数与当前表结构不匹配
                        if diagnostic and not first_delete_printed:
                            print(f"    ⚠️  @N={col_idx} 超出当前列数({len(columns)}),可能表结构不匹配")
                i += 1

            # 诊断: 打印第一条 DELETE 事件的原始内容
            if diagnostic and not first_delete_printed and raw_lines:
                first_delete_printed = True
                print(f"\n    📋 诊断 - {table_name} 第一条DELETE事件原始内容:")
                print(f"    DELETE FROM {target_pattern}")
                for rl in raw_lines:
                    # 截断过长的行
                    display = rl if len(rl) <= 200 else rl[:200] + "..."
                    print(f"    {display}")
                print(f"    解析到的列: {len(values)} / {len(columns)}")
                # 检查关键字段是否存在
                key_fields = ['cert_type', 'template_id', 'theory_score',
                              'practical_score', 'comprehensive_evaluation',
                              'extra_json', 'photo_url', 'url']
                present_fields = [f for f in key_fields if f in values and values[f] is not None]
                missing_fields = [f for f in key_fields if f not in values or values[f] is None]
                print(f"    关键字段有值: {present_fields if present_fields else '(无)'}")
                print(f"    关键字段缺失: {missing_fields if missing_fields else '(无)'}")
                print()

            if 'id' in values and values['id'] is not None:
                deleted_records.append(values)
        else:
            i += 1
    return deleted_records


def check_existing_ids(table_name, ids):
    """检查哪些ID已存在"""
    if not ids:
        return set()
    existing = set()
    batch_size = 500
    for i in range(0, len(ids), batch_size):
        batch = ids[i:i+batch_size]
        id_list = ",".join(str(x) for x in batch)
        result = run_mysql_cmd(f"SELECT id FROM `{table_name}` WHERE id IN ({id_list})")
        if result:
            for x in result.split("\n"):
                if x.strip():
                    existing.add(int(x))
    return existing


def escape_sql_val(val):
    """转义SQL值"""
    if val is None:
        return "NULL"
    val = str(val).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{val}'"


def drop_index_if_exists(table_name, index_name):
    """永久删除索引(如果存在)"""
    check = run_mysql_cmd(
        f"SELECT COUNT(*) FROM information_schema.STATISTICS "
        f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='{table_name}' "
        f"AND INDEX_NAME='{index_name}';"
    )
    if check and int(check) > 0:
        print(f"    删除索引 {index_name} ...")
        result = run_mysql_cmd_verbose(f"ALTER TABLE `{table_name}` DROP INDEX `{index_name}`;")
        if result is not None:
            print(f"    ✅ 已永久删除 {index_name}")
            return True
        else:
            print(f"    ❌ 删除 {index_name} 失败")
            return False
    else:
        print(f"    索引 {index_name} 不存在,跳过")
        return False


def recover_table(table_name, columns, all_binlog_files, recover_start_dt, recover_end_dt):
    """恢复单张表的DELETE记录

    v6 关键改进: 对已存在的记录执行 UPDATE 补全缺失字段,不再跳过
    """
    print()
    print(f"{'='*60}")
    print(f"  恢复表: {table_name}")
    print(f"  列数: {len(columns)}")
    print(f"{'='*60}")

    before_count = run_mysql_cmd(f"SELECT COUNT(*) FROM `{table_name}`")
    print(f"  当前记录数: {before_count}")

    # 扫描binlog
    all_deleted = []
    is_first_binlog = True
    for bf in all_binlog_files:
        binlog_name = bf['name']
        print(f"\n  扫描 {binlog_name} ({bf['size']/1024/1024:.1f} MB) ...")

        binlog_path = find_local_binlog(binlog_name)
        if not binlog_path:
            print(f"    ⚠️  本地未找到 {binlog_name},跳过")
            continue

        print(f"    文件路径: {binlog_path}")

        binlog_start = get_binlog_time_range_local(binlog_path)
        if binlog_start:
            print(f"    binlog起始时间: {binlog_start}")
            if binlog_start > recover_end_dt:
                print(f"    跳过(晚于恢复结束时间)")
                continue

        # 只在第一个找到DELETE记录的binlog上打印诊断信息
        records = scan_binlog_for_table_deletes(
            binlog_path, RECOVER_START, RECOVER_END, table_name, columns,
            diagnostic=(DIAGNOSTIC_SAMPLE and is_first_binlog)
        )
        is_first_binlog = False

        if records:
            print(f"    ✅ 找到 {len(records)} 条 {table_name} DELETE 记录")
            all_deleted.extend(records)
        else:
            print(f"    未找到 DELETE 记录")

    if not all_deleted:
        print(f"\n  {table_name}: 未找到可恢复的DELETE记录")
        return 0

    # 去重(按id)
    seen_ids = set()
    unique_records = []
    for rec in all_deleted:
        rec_id = rec.get('id')
        if rec_id and rec_id not in seen_ids:
            seen_ids.add(rec_id)
            unique_records.append(rec)

    print(f"\n  {table_name}: 提取 {len(all_deleted)} 条,去重后 {len(unique_records)} 条唯一记录")

    # 检查已存在的
    all_ids = [int(r['id']) for r in unique_records if r.get('id')]
    existing_ids = check_existing_ids(table_name, all_ids)
    missing_records = [r for r in unique_records if int(r['id']) not in existing_ids]
    existing_records = [r for r in unique_records if int(r['id']) in existing_ids]

    print(f"  已存在(需UPDATE补全): {len(existing_records)} 条")
    print(f"  不存在(需INSERT): {len(missing_records)} 条")

    # === 预览 ===
    print(f"\n  恢复预览(前5条):")
    for rec in (missing_records + existing_records)[:5]:
        info_parts = [f"id={rec.get('id')}"]
        for preview_col in ['name', 'id_card', 'profession', 'cert_type', 'template_id',
                            'theory_score', 'practical_score', 'comprehensive_evaluation',
                            'url', 'field_key', 'code']:
            if preview_col in rec:
                val = rec[preview_col]
                if val is not None:
                    val_str = str(val)[:50]
                    info_parts.append(f"{preview_col}={val_str}")
                else:
                    info_parts.append(f"{preview_col}=NULL")
        print(f"    {', '.join(info_parts)}")

    # === v6 关键改进: 对已存在的记录执行 UPDATE 补全缺失字段 ===
    updated = 0
    update_failed = 0
    if existing_records:
        print(f"\n  开始 UPDATE {len(existing_records)} 条已存在记录(补全缺失字段)...")
        for idx, rec in enumerate(existing_records):
            # 只 UPDATE 非NULL的字段(不覆盖已有值)
            set_parts = []
            for col in columns:
                if col in rec and rec[col] is not None and col != 'id':
                    set_parts.append(f"`{col}` = {escape_sql_val(rec[col])}")
            if not set_parts:
                continue
            set_parts.append("`update_time` = NOW()")
            sql = f"UPDATE `{table_name}` SET {', '.join(set_parts)} WHERE `id` = {rec['id']};"
            result = run_mysql_cmd(sql, timeout=10)
            if result is not None:
                updated += 1
            else:
                update_failed += 1
            if (idx + 1) % 200 == 0:
                print(f"    UPDATE进度: {idx + 1}/{len(existing_records)} (成功 {updated}, 失败 {update_failed})")

        print(f"  UPDATE完成: 成功 {updated}, 失败 {update_failed}")

    # === INSERT 不存在的记录 ===
    inserted = 0
    failed = 0
    failed_records = []
    if missing_records:
        print(f"\n  开始 INSERT {len(missing_records)} 条新记录...")
        for idx, rec in enumerate(missing_records):
            col_list = []
            val_list = []
            for col in columns:
                if col in rec:
                    col_list.append(f"`{col}`")
                    val_list.append(escape_sql_val(rec[col]))

            if not col_list:
                failed += 1
                continue

            sql = f"INSERT IGNORE INTO `{table_name}` ({', '.join(col_list)}) VALUES ({', '.join(val_list)})"
            result = run_mysql_cmd(sql, timeout=10)

            if result is not None:
                inserted += 1
            else:
                failed += 1
                failed_records.append(rec)

            if (idx + 1) % 200 == 0:
                print(f"    INSERT进度: {idx + 1}/{len(missing_records)} (成功 {inserted}, 失败 {failed})")

    print(f"\n  {table_name} 恢复完成:")
    print(f"    INSERT: 尝试 {len(missing_records)} 条, 成功 {inserted} 条, 失败 {failed} 条")
    print(f"    UPDATE: 尝试 {len(existing_records)} 条, 成功 {updated} 条, 失败 {update_failed} 条")

    if failed_records:
        print(f"    INSERT失败记录预览(前3条):")
        for rec in failed_records[:3]:
            print(f"      id={rec.get('id')}, name={rec.get('name')}, id_card={rec.get('id_card')}")

    after_count = run_mysql_cmd(f"SELECT COUNT(*) FROM `{table_name}`")
    print(f"    记录数: {before_count} → {after_count}")

    return inserted + updated


def main():
    print()
    print("╔══════════════════════════════════════════════════════════════════════╗")
    print("║  证书数据完整恢复 v6 (UPDATE补全 + binlog诊断 + 7表全恢复)          ║")
    print("║  修复: INSERT IGNORE跳过已存在记录 → 改用UPDATE补全缺失字段        ║")
    print("╚══════════════════════════════════════════════════════════════════════╝")
    print()
    print(f"  恢复时间范围: {RECOVER_START} ~ {RECOVER_END}")
    print(f"  恢复表({len(RECOVER_TABLES)}张): {', '.join(RECOVER_TABLES)}")
    print(f"  读取方式: 本地binlog文件(不连接MySQL读binlog)")
    print()

    # 1. 检查MySQL
    print("━━━ 1. 检查MySQL连接 ━━━")
    cert_count = run_mysql_cmd("SELECT COUNT(*) FROM certificate")
    if cert_count is None:
        print("  ❌ MySQL连接失败!")
        print("  紧急步骤: systemctl restart mysqld")
        return
    print(f"  ✅ MySQL连接正常")
    print(f"  当前 certificate 表记录数: {cert_count}")

    # 1.5 【关键】检查 binlog_row_image 设置
    print()
    print("━━━ 1.5 检查 binlog_row_image 设置(决定DELETE事件是否包含全部列) ━━━")
    row_image = run_mysql_cmd("SHOW VARIABLES LIKE 'binlog_row_image'")
    print(f"  binlog_row_image = {row_image}")
    if row_image and "MINIMAL" in str(row_image).upper():
        print("  ⚠️⚠️⚠️ 严重警告: binlog_row_image = MINIMAL")
        print("  这意味着 DELETE 事件只记录主键列,其他列值(cert_type/scores/template_id等)不在 binlog 中!")
        print("  恢复的数据将只包含主键,其他字段全部为 NULL")
        print("  解决方案:")
        print("    1. 这些字段无法从 binlog 恢复")
        print("    2. 只能从 extra_json(如果有的话)或其他来源补录")
        print("    3. 建议修改 MySQL 配置: binlog_row_image = FULL,然后重启 MySQL")
        print("    4. 继续执行恢复,但字段补全依赖 fix_recovered_fields.py")
    elif row_image and "FULL" in str(row_image).upper():
        print("  ✅ binlog_row_image = FULL,DELETE 事件包含所有列值,可以完整恢复")
    else:
        print(f"  ℹ️  binlog_row_image 未明确设置,MySQL 默认为 FULL(8.0+),应该可以完整恢复")

    # 也检查 binlog_format
    binlog_format = run_mysql_cmd("SHOW VARIABLES LIKE 'binlog_format'")
    print(f"  binlog_format = {binlog_format}")
    if binlog_format and "STATEMENT" in str(binlog_format).upper():
        print("  ⚠️ 警告: binlog_format = STATEMENT,DELETE 事件不记录行数据!")
        print("  需要 binlog_format = ROW 或 MIXED 才能从 binlog 恢复 DELETE 数据")

    # 2. 检查各表状态
    print()
    print("━━━ 2. 检查各关联表状态 ━━━")
    table_columns = {}
    for table_name in RECOVER_TABLES:
        cols = get_table_columns(table_name)
        table_columns[table_name] = cols
        count = run_mysql_cmd(f"SELECT COUNT(*) FROM `{table_name}`")
        print(f"  {table_name}: {count} 条, {len(cols)} 列")
        if table_name == "certificate" and cols:
            print(f"    列名: {', '.join(cols)}")

    # 3. 检查binlog
    print()
    print("━━━ 3. 检查binlog状态 ━━━")
    log_bin = run_mysql_cmd("SHOW VARIABLES LIKE 'log_bin'")
    if "ON" not in str(log_bin).upper():
        print("  ❌ binlog未开启!")
        return

    binlog_files = get_binlog_files_list()
    print(f"  可用binlog文件: {len(binlog_files)} 个")
    for f in binlog_files:
        print(f"    - {f['name']} ({f['size']/1024/1024:.1f} MB)")

    # 4. 【关键】永久删除 certificate 表的唯一索引
    print()
    print("━━━ 4. 永久删除 certificate 唯一索引 ━━━")
    print("  ⚠️  用户要求: 不再添加唯一索引")

    for idx_name in CERT_UNIQUE_INDEXES:
        drop_index_if_exists("certificate", idx_name)

    # 5. 逐表恢复
    print()
    print("━━━ 5. 开始逐表恢复 ━━━")
    print(f"  恢复顺序: {' → '.join(RECOVER_TABLES)}")
    print(f"  诊断模式: {'开启(打印第一条DELETE原始内容)' if DIAGNOSTIC_SAMPLE else '关闭'}")

    recover_start_dt = datetime.strptime(RECOVER_START, "%Y-%m-%d %H:%M:%S")
    recover_end_dt = datetime.strptime(RECOVER_END, "%Y-%m-%d %H:%M:%S")

    total_recovered = 0
    for table_name in RECOVER_TABLES:
        cols = table_columns.get(table_name, [])
        if not cols:
            print(f"\n  ⚠️  无法获取 {table_name} 的列信息,跳过")
            continue
        recovered = recover_table(table_name, cols, binlog_files, recover_start_dt, recover_end_dt)
        total_recovered += recovered

    # 6. 最终验证
    print()
    print("━━━ 6. 最终验证 ━━━")
    print(f"  总恢复记录数(INSERT+UPDATE): {total_recovered}")
    print()

    for table_name in RECOVER_TABLES:
        count = run_mysql_cmd(f"SELECT COUNT(*) FROM `{table_name}`")
        print(f"  {table_name}: {count} 条")

    # certificate 主表详细统计
    print()
    cert_count_final = run_mysql_cmd("SELECT COUNT(*) FROM certificate")
    print(f"  certificate 最终记录数: {cert_count_final}")
    print(f"  恢复前: {cert_count} → 恢复后: {cert_count_final}")

    # cert_type 统计
    print()
    cert_type_stats = run_mysql_cmd(
        "SELECT IFNULL(cert_type,'(空)'), COUNT(*) FROM certificate "
        "GROUP BY cert_type ORDER BY COUNT(*) DESC LIMIT 10"
    )
    if cert_type_stats:
        print("  证书类型分布:")
        for line in cert_type_stats.split("\n"):
            print(f"    {line}")

    # 关键字段统计
    print()
    has_cert_type = run_mysql_cmd(
        "SELECT COUNT(*) FROM certificate WHERE cert_type IS NOT NULL AND cert_type != ''"
    ) or "?"
    has_template = run_mysql_cmd(
        "SELECT COUNT(*) FROM certificate WHERE template_id IS NOT NULL"
    ) or "?"
    has_theory = run_mysql_cmd(
        "SELECT COUNT(*) FROM certificate WHERE theory_score IS NOT NULL AND theory_score != ''"
    ) or "?"
    has_practical = run_mysql_cmd(
        "SELECT COUNT(*) FROM certificate WHERE practical_score IS NOT NULL AND practical_score != ''"
    ) or "?"
    has_extra_json = run_mysql_cmd(
        "SELECT COUNT(*) FROM certificate WHERE extra_json IS NOT NULL AND extra_json != ''"
    ) or "?"
    has_photo = run_mysql_cmd(
        "SELECT COUNT(DISTINCT c.id) FROM certificate c "
        "INNER JOIN certificate_photo cp ON cp.certificate_id = c.id"
    ) or "?"

    print(f"  关键字段统计:")
    print(f"    有 cert_type:      {has_cert_type} / {cert_count_final}")
    print(f"    有 template_id:    {has_template} / {cert_count_final}")
    print(f"    有 theory_score:   {has_theory} / {cert_count_final}")
    print(f"    有 practical_score:{has_practical} / {cert_count_final}")
    print(f"    有 extra_json:     {has_extra_json} / {cert_count_final}")
    print(f"    有照片关联:        {has_photo} / {cert_count_final}")

    # 照片统计
    print()
    photo_total = run_mysql_cmd("SELECT COUNT(*) FROM certificate_photo")
    photo_with_cert = run_mysql_cmd(
        "SELECT COUNT(*) FROM certificate_photo cp "
        "INNER JOIN certificate c ON cp.certificate_id = c.id"
    )
    photo_orphan = run_mysql_cmd(
        "SELECT COUNT(*) FROM certificate_photo cp "
        "WHERE cp.certificate_id IS NULL "
        "OR cp.certificate_id NOT IN (SELECT id FROM certificate)"
    )
    print(f"  照片总数: {photo_total}")
    print(f"  照片成功关联证书: {photo_with_cert}")
    print(f"  孤儿照片(需修复关联): {photo_orphan}")

    # 索引验证
    print()
    print("  唯一索引状态(应全部不存在):")
    for idx_name in CERT_UNIQUE_INDEXES:
        check = run_mysql_cmd(
            f"SELECT COUNT(*) FROM information_schema.STATISTICS "
            f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='certificate' "
            f"AND INDEX_NAME='{idx_name}';"
        )
        status = "❌ 仍存在" if check and int(check) > 0 else "✅ 已删除"
        print(f"    {idx_name}: {status}")

    print()
    print("═══════════════════════════════════════════════════════════════")
    print("  恢复完成!")
    print("  如果关键字段(cert_type/scores/template_id)仍为空:")
    print("    1. 检查上方诊断输出中 DELETE 事件是否包含这些列")
    print("    2. 如果 binlog_row_image=MINIMAL,这些列不在 binlog 中")
    print("    3. 执行 fix_recovered_fields.py 从 extra_json 补全")
    print("═══════════════════════════════════════════════════════════════")


if __name__ == "__main__":
    main()
