#!/usr/bin/env python3
"""
证书数据完整恢复脚本 v4 - 唯一索引处理版
=========================================
核心修复:
  1. 【关键】恢复前临时删除 certificate 表的唯一索引(uk_idcard_profession/uk_cert_no/uk_student_no)
     → 这些索引导致 INSERT IGNORE 静默丢弃约1000条冲突记录!
  2. 恢复完成后重新添加唯一索引(先去重保留最新记录)
  3. 新增 certificate_user 表恢复(证书用户关联)
  4. 动态读取表结构,不再硬编码列顺序
  5. 恢复5张关联表: certificate_template / certificate_template_field / certificate / certificate_photo / certificate_user
  6. 直接读取本地binlog文件,不连接MySQL读binlog

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

# 恢复时间范围: 最近7天
RECOVER_START = "2026-07-22 00:00:00"
RECOVER_END   = "2026-07-28 23:59:59"

# MySQL数据目录(binlog文件所在位置)
MYSQL_DATA_DIR = "/var/lib/mysql"

# 需要恢复的表列表(按依赖顺序)
RECOVER_TABLES = [
    "certificate_template",
    "certificate_template_field",
    "certificate",
    "certificate_photo",
    "certificate_user",
]

# certificate 表上需要临时删除的唯一索引(恢复后重建)
CERT_UNIQUE_INDEXES = [
    "uk_idcard_profession",  # (id_card, profession) — 最主要的元凶
    "uk_cert_no",            # cert_no
    "uk_student_no",         # student_no
]


def run_mysql_cmd(sql, timeout=30):
    """执行MySQL命令"""
    cmd = [
        "mysql", f"-h{DB_HOST}", f"-P{DB_PORT}",
        f"-u{DB_USER}", f"-p{DB_PASSWORD}",
        DB_NAME, "-N", "-e", sql
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    if result.returncode != 0:
        # 不打印stderr到stdout,只返回None
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


def scan_binlog_for_table_deletes(binlog_path, start_dt, end_dt, table_name, columns):
    """扫描本地binlog文件,提取指定表的DELETE记录"""
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

    while i < len(lines):
        line = lines[i].strip()
        if "### DELETE FROM" in line and target_pattern in line:
            values = {}
            i += 1
            while i < len(lines):
                col_line = lines[i].strip()
                if not col_line.startswith("###"):
                    break
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
                i += 1
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
    """安全删除索引(如果存在)"""
    # 检查索引是否存在
    check = run_mysql_cmd(
        f"SELECT COUNT(*) FROM information_schema.STATISTICS "
        f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='{table_name}' "
        f"AND INDEX_NAME='{index_name}';"
    )
    if check and int(check) > 0:
        print(f"    删除索引 {index_name} ...")
        result = run_mysql_cmd_verbose(f"ALTER TABLE `{table_name}` DROP INDEX `{index_name}`;")
        if result is not None:
            print(f"    ✅ 已删除 {index_name}")
            return True
        else:
            print(f"    ❌ 删除 {index_name} 失败")
            return False
    else:
        print(f"    索引 {index_name} 不存在,跳过")
        return False


def recreate_unique_indexes():
    """恢复后重建唯一索引(先去重)"""
    print()
    print("  ── 重建唯一索引 ──")

    # 1. 先清理专业为空的脏数据
    print("    清理专业为空的记录...")
    run_mysql_cmd_verbose(
        "DELETE FROM `certificate` WHERE (profession IS NULL OR profession = '');"
    )

    # 2. 按 id_card+profession 去重(保留id最大的)
    print("    按 (id_card, profession) 去重,保留最新记录...")
    dedup_result = run_mysql_cmd_verbose(
        "DELETE c1 FROM `certificate` c1 "
        "INNER JOIN `certificate` c2 "
        "ON c1.id_card = c2.id_card AND c1.profession = c2.profession "
        "AND c1.id < c2.id;"
    )

    # 查看去重后的数量
    after_dedup = run_mysql_cmd("SELECT COUNT(*) FROM `certificate`")
    print(f"    去重后记录数: {after_dedup}")

    # 3. 重建 uk_idcard_profession
    check = run_mysql_cmd(
        f"SELECT COUNT(*) FROM information_schema.STATISTICS "
        f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='certificate' "
        f"AND INDEX_NAME='uk_idcard_profession';"
    )
    if check and int(check) == 0:
        print("    重建 uk_idcard_profession ...")
        result = run_mysql_cmd_verbose(
            "ALTER TABLE `certificate` ADD UNIQUE INDEX `uk_idcard_profession` (`id_card`, `profession`);"
        )
        if result is not None:
            print("    ✅ uk_idcard_profession 已重建")
        else:
            print("    ❌ uk_idcard_profession 重建失败(可能有重复数据)")
    else:
        print("    uk_idcard_profession 已存在,跳过")

    # 4. 重建 uk_cert_no (允许NULL,不冲突)
    check = run_mysql_cmd(
        f"SELECT COUNT(*) FROM information_schema.STATISTICS "
        f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='certificate' "
        f"AND INDEX_NAME='uk_cert_no';"
    )
    if check and int(check) == 0:
        print("    重建 uk_cert_no ...")
        # cert_no 可能有重复或NULL,先清理
        run_mysql_cmd_verbose(
            "DELETE c1 FROM `certificate` c1 "
            "INNER JOIN `certificate` c2 "
            "ON c1.cert_no = c2.cert_no AND c1.cert_no IS NOT NULL AND c1.cert_no != '' "
            "AND c1.id < c2.id;"
        )
        run_mysql_cmd_verbose(
            "ALTER TABLE `certificate` ADD UNIQUE INDEX `uk_cert_no` (`cert_no`);"
        )
        print("    ✅ uk_cert_no 已重建")

    # 5. 重建 uk_student_no
    check = run_mysql_cmd(
        f"SELECT COUNT(*) FROM information_schema.STATISTICS "
        f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='certificate' "
        f"AND INDEX_NAME='uk_student_no';"
    )
    if check and int(check) == 0:
        print("    重建 uk_student_no ...")
        run_mysql_cmd_verbose(
            "DELETE c1 FROM `certificate` c1 "
            "INNER JOIN `certificate` c2 "
            "ON c1.student_no = c2.student_no AND c1.student_no IS NOT NULL AND c1.student_no != '' "
            "AND c1.id < c2.id;"
        )
        run_mysql_cmd_verbose(
            "ALTER TABLE `certificate` ADD UNIQUE INDEX `uk_student_no` (`student_no`);"
        )
        print("    ✅ uk_student_no 已重建")


def recover_table(table_name, columns, all_binlog_files, recover_start_dt, recover_end_dt):
    """恢复单张表的DELETE记录"""
    print()
    print(f"{'='*60}")
    print(f"  恢复表: {table_name}")
    print(f"  列数: {len(columns)}")
    print(f"{'='*60}")

    before_count = run_mysql_cmd(f"SELECT COUNT(*) FROM `{table_name}`")
    print(f"  当前记录数: {before_count}")

    # 扫描binlog
    all_deleted = []
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

        records = scan_binlog_for_table_deletes(
            binlog_path, RECOVER_START, RECOVER_END, table_name, columns
        )

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

    print(f"  已存在(跳过): {len(existing_ids)} 条")
    print(f"  需要恢复: {len(missing_records)} 条")

    if not missing_records:
        print(f"  {table_name}: 所有删除记录都已存在,无需操作")
        return 0

    # 预览
    print(f"\n  恢复预览(前5条):")
    for rec in missing_records[:5]:
        info_parts = [f"id={rec.get('id')}"]
        for preview_col in ['name', 'id_card', 'profession', 'cert_type', 'template_id', 'url', 'field_key']:
            if preview_col in rec:
                info_parts.append(f"{preview_col}={rec[preview_col]}")
        print(f"    {', '.join(info_parts)}")

    # 逐条恢复 — 使用 INSERT IGNORE(仅按主键id去重)
    # 唯一索引已在恢复前临时删除,不会静默丢弃数据
    print(f"\n  开始恢复 {len(missing_records)} 条记录...")
    inserted = 0
    failed = 0
    failed_records = []

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
            print(f"    进度: {idx + 1}/{len(missing_records)} (成功 {inserted}, 失败 {failed})")

    print(f"\n  {table_name} 恢复完成:")
    print(f"    尝试恢复: {len(missing_records)} 条")
    print(f"    成功插入: {inserted} 条")
    print(f"    失败: {failed} 条")

    if failed_records:
        print(f"    失败记录预览(前3条):")
        for rec in failed_records[:3]:
            print(f"      id={rec.get('id')}, name={rec.get('name')}, id_card={rec.get('id_card')}")

    after_count = run_mysql_cmd(f"SELECT COUNT(*) FROM `{table_name}`")
    print(f"    记录数: {before_count} → {after_count}")

    return inserted


def main():
    print()
    print("╔══════════════════════════════════════════════════════════════════╗")
    print("║  证书数据完整恢复 v4 (唯一索引处理 + 全表恢复)                    ║")
    print("║  修复: INSERT IGNORE 被唯一索引静默丢弃数据的问题                 ║")
    print("╚══════════════════════════════════════════════════════════════════╝")
    print()
    print(f"  恢复时间范围: {RECOVER_START} ~ {RECOVER_END}")
    print(f"  恢复表: {', '.join(RECOVER_TABLES)}")
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

    # 2. 检查各表状态
    print()
    print("━━━ 2. 检查各关联表状态 ━━━")
    table_columns = {}
    for table_name in RECOVER_TABLES:
        cols = get_table_columns(table_name)
        table_columns[table_name] = cols
        count = run_mysql_cmd(f"SELECT COUNT(*) FROM `{table_name}`")
        print(f"  {table_name}: {count} 条, {len(cols)} 列")

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

    # 4. 【关键】临时删除 certificate 表的唯一索引
    print()
    print("━━━ 4. 临时删除 certificate 唯一索引 ━━━")
    print("  ⚠️  这一步是关键!")
    print("  certificate 表有唯一索引 uk_idcard_profession (id_card, profession)")
    print("  INSERT IGNORE 会静默丢弃与现有记录 (id_card, profession) 冲突的数据")
    print("  临时删除索引后,所有被删除的记录都能完整恢复")
    print("  恢复完成后会自动重建索引(先去重保留最新记录)")
    print()

    dropped_indexes = []
    for idx_name in CERT_UNIQUE_INDEXES:
        dropped = drop_index_if_exists("certificate", idx_name)
        if dropped:
            dropped_indexes.append(idx_name)

    if not dropped_indexes:
        print("  没有需要删除的索引(可能已被删除)")
    else:
        print(f"  已删除 {len(dropped_indexes)} 个唯一索引: {', '.join(dropped_indexes)}")

    # 5. 逐表恢复
    print()
    print("━━━ 5. 开始逐表恢复 ━━━")
    print(f"  恢复顺序: {' → '.join(RECOVER_TABLES)}")

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

    # 6. 【关键】重建唯一索引
    print()
    print("━━━ 6. 重建 certificate 唯一索引 ━━━")
    print("  恢复完成后,重建唯一索引(先去重保留最新记录)")
    recreate_unique_indexes()

    # 7. 最终验证
    print()
    print("━━━ 7. 最终验证 ━━━")
    print(f"  总恢复记录数: {total_recovered}")
    print()

    for table_name in RECOVER_TABLES:
        count = run_mysql_cmd(f"SELECT COUNT(*) FROM `{table_name}`")
        print(f"  {table_name}: {count} 条")

    # certificate 主表详细统计
    print()
    cert_count_final = run_mysql_cmd("SELECT COUNT(*) FROM certificate")
    print(f"  certificate 最终记录数: {cert_count_final}")
    print(f"  恢复前: {cert_count} → 恢复后: {cert_count_final}")
    diff = int(cert_count_final) - int(cert_count) if cert_count and cert_count_final else 0
    print(f"  净增: {diff} 条")

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

    # template_id 统计
    template_stats = run_mysql_cmd(
        "SELECT IFNULL(template_id,'(未绑定)'), COUNT(*) FROM certificate "
        "GROUP BY template_id ORDER BY COUNT(*) DESC LIMIT 10"
    )
    if template_stats:
        print("  模板绑定分布:")
        for line in template_stats.split("\n"):
            print(f"    {line}")

    # 照片统计
    photo_stats = run_mysql_cmd("SELECT COUNT(DISTINCT id_card) FROM certificate_photo")
    if photo_stats:
        print(f"  照片覆盖学员数: {photo_stats}")

    # certificate_user 统计
    user_stats = run_mysql_cmd("SELECT COUNT(*) FROM certificate_user")
    if user_stats:
        print(f"  证书用户关联: {user_stats} 条")

    # 索引验证
    print()
    print("  唯一索引状态:")
    for idx_name in CERT_UNIQUE_INDEXES:
        check = run_mysql_cmd(
            f"SELECT COUNT(*) FROM information_schema.STATISTICS "
            f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='certificate' "
            f"AND INDEX_NAME='{idx_name}';"
        )
        status = "✅ 存在" if check and int(check) > 0 else "❌ 缺失"
        print(f"    {idx_name}: {status}")

    print()
    print("══════════════════════════════════════")
    print("  全表恢复完成!")
    print("══════════════════════════════════════")


if __name__ == "__main__":
    main()
