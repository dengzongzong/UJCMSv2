#!/bin/bash
# ============================================================
# 每日定时备份脚本 - 数据库 + uploads目录
# crontab: 0 0 * * * /opt/exam-platform/daily-backup.sh
# 保留最近3个备份文件,超过自动清理
# ============================================================
set -e

# ---- 配置 ----
MYSQL_DB="exam_platform"
MYSQL_USER="root"
MYSQL_PASS="Root@123456"

# 上传目录(自动检测)
if [ -d "/opt/exam-platform/uploads" ]; then
  UPLOAD_DIR="/opt/exam-platform/uploads"
elif [ -d "/data/exam-platform/uploads" ]; then
  UPLOAD_DIR="/data/exam-platform/uploads"
else
  UPLOAD_DIR="/opt/exam-platform/uploads"
fi

# 备份目录
BACKUP_BASE="/opt/exam-platform/backups"
MAX_BACKUPS=3
DATE_STR=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_BASE}/${DATE_STR}"

# ---- 日志函数 ----
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# ---- 开始 ----
log "===== 每日备份开始 ====="
log "数据库: ${MYSQL_DB}"
log "上传目录: ${UPLOAD_DIR}"
log "备份目录: ${BACKUP_DIR}"
log ""

mkdir -p "${BACKUP_DIR}"

# --------------------------------------------------
# 1. 备份MySQL数据库(全量,包含所有表)
# --------------------------------------------------
log "[1/3] 备份数据库..."
SQL_FILE="${BACKUP_DIR}/db_${DATE_STR}.sql.gz"

mysqldump -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --routines \
  --triggers \
  --quick \
  --lock-tables=false 2>/dev/null | gzip > "${SQL_FILE}"

if [ $? -eq 0 ] && [ -s "${SQL_FILE}" ]; then
  SQL_SIZE=$(du -h "${SQL_FILE}" | cut -f1)
  log "  数据库备份完成, 大小: ${SQL_SIZE}"
else
  log "  错误: 数据库备份失败!"
  exit 1
fi

# --------------------------------------------------
# 2. 备份uploads目录
# --------------------------------------------------
log "[2/3] 备份上传文件..."
if [ -d "${UPLOAD_DIR}" ]; then
  UPLOAD_TAR="${BACKUP_DIR}/uploads_${DATE_STR}.tar.gz"
  tar -czf "${UPLOAD_TAR}" -C "$(dirname ${UPLOAD_DIR})" "$(basename ${UPLOAD_DIR})" 2>/dev/null
  if [ $? -eq 0 ]; then
    UPLOAD_SIZE=$(du -h "${UPLOAD_TAR}" | cut -f1)
    log "  上传文件备份完成, 大小: ${UPLOAD_SIZE}"
  else
    log "  警告: 上传文件备份失败,继续..."
  fi
else
  log "  警告: 上传目录 ${UPLOAD_DIR} 不存在,跳过"
fi

# --------------------------------------------------
# 3. 清理旧备份(只保留最近 MAX_BACKUPS 个)
# --------------------------------------------------
log "[3/3] 清理旧备份(保留最近 ${MAX_BACKUPS} 个)..."
BACKUP_COUNT=$(ls -d ${BACKUP_BASE}/*/ 2>/dev/null | wc -l)
log "  当前备份数: ${BACKUP_COUNT}"

if [ ${BACKUP_COUNT} -gt ${MAX_BACKUPS} ]; then
  # 按目录名排序(目录名是时间戳,字典序=时间序),删除最旧的
  DELETE_COUNT=$((BACKUP_COUNT - MAX_BACKUPS))
  log "  需要删除 ${DELETE_COUNT} 个旧备份"
  
  ls -d ${BACKUP_BASE}/*/ 2>/dev/null | sort | head -n ${DELETE_COUNT} | while read old_dir; do
    old_dir=$(echo "${old_dir}" | sed 's:/$::')
    log "  删除: ${old_dir}"
    rm -rf "${old_dir}"
  done
  
  log "  清理完成"
else
  log "  无需清理"
fi

# ---- 统计 ----
log ""
log "===== 备份完成 ====="
log "备份位置: ${BACKUP_DIR}"
log "备份内容:"
ls -lh "${BACKUP_DIR}/" 2>/dev/null | while read line; do
  log "  ${line}"
done
log ""
log "当前所有备份:"
ls -d ${BACKUP_BASE}/*/ 2>/dev/null | while read dir; do
  dir=$(echo "${dir}" | sed 's:/$::')
  dir_size=$(du -sh "${dir}" 2>/dev/null | cut -f1)
  log "  $(basename ${dir}) - ${dir_size}"
done
