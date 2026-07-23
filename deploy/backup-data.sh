#!/bin/bash
# ============================================================
# 数据备份脚本 - 在源系统服务器上执行
# 导出课程/视频/题库/考试/证书模板等配置数据 + 媒体文件
# 不包含: 学生数据、证书数据、考试记录、管理员账号
# ============================================================
set -e

MYSQL_DB="exam_platform"
MYSQL_USER="root"
MYSQL_PASS="${MYSQL_PASS:-Root@123456}"
# 老系统部署在 /data/exam-platform,新系统在 /opt/exam-platform
# 自动检测: 优先 /data,其次 /opt,可用 UPLOAD_DIR 环境变量覆盖
if [ -d "/data/exam-platform/uploads" ]; then
  UPLOAD_DIR="/data/exam-platform/uploads"
elif [ -d "/opt/exam-platform/uploads" ]; then
  UPLOAD_DIR="/opt/exam-platform/uploads"
else
  UPLOAD_DIR="${UPLOAD_DIR:-/data/exam-platform/uploads}"
fi
BACKUP_DIR="/tmp/exam-backup-$(date +%Y%m%d_%H%M%S)"

echo "===== 数据备份开始 ====="
echo "数据库: ${MYSQL_DB}"
echo "备份目录: ${BACKUP_DIR}"
echo ""

mkdir -p "${BACKUP_DIR}"

# --------------------------------------------------
# 1. 导出配置数据(不含用户数据)
# --------------------------------------------------
echo "[1/3] 导出数据库..."
DUMP_FILE="${BACKUP_DIR}/data.sql"

# 需要导出的表(配置类数据,不含学生/证书/考试记录等)
TABLES=(
  profession
  subject
  video_category
  question_category
  course
  course_section
  course_section_video
  video
  question
  question_option
  question_template
  question_template_item
  paper
  paper_question
  exam
  certificate_type
  certificate_field
  certificate_template
  certificate_template_field
  certificate_number_config
  certificate_url_config
  homepage_section
  news
  announcement
  banner
  banner_image
  friendly_link
  system_setting
  about_us
  site_declaration
  cooperation_setting
  course_three_image
)

mysqldump -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} \
  --default-character-set=utf8mb4 \
  --no-create-db \
  --skip-add-drop-table \
  --complete-insert \
  --where="1=1" \
  ${TABLES[@]} 2>/dev/null > "${DUMP_FILE}"

# 在 SQL 文件头部加上 CREATE TABLE IF NOT EXISTS
echo "-- 自动生成的数据备份" > "${BACKUP_DIR}/data_full.sql"
echo "SET NAMES utf8mb4;" >> "${BACKUP_DIR}/data_full.sql"
echo "SET FOREIGN_KEY_CHECKS=0;" >> "${BACKUP_DIR}/data_full.sql"
echo "" >> "${BACKUP_DIR}/data_full.sql"

# 先导出表结构
mysqldump -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} \
  --default-character-set=utf8mb4 \
  --no-data \
  --skip-add-drop-table \
  ${TABLES[@]} 2>/dev/null >> "${BACKUP_DIR}/data_full.sql"

# 再追加数据(用 INSERT IGNORE 防重复)
echo "" >> "${BACKUP_DIR}/data_full.sql"
echo "-- 数据" >> "${BACKUP_DIR}/data_full.sql"
sed 's/INSERT INTO/INSERT IGNORE INTO/g' "${DUMP_FILE}" >> "${BACKUP_DIR}/data_full.sql"

echo "SET FOREIGN_KEY_CHECKS=1;" >> "${BACKUP_DIR}/data_full.sql"
rm -f "${DUMP_FILE}"

TABLE_COUNT=$((${#TABLES[@]}))
SQL_SIZE=$(du -h "${BACKUP_DIR}/data_full.sql" | cut -f1)
echo "  导出 ${TABLE_COUNT} 张表, SQL 大小: ${SQL_SIZE}"

# --------------------------------------------------
# 2. 复制媒体文件(上传的图片/视频等)
# --------------------------------------------------
echo "[2/3] 复制媒体文件..."
if [ -d "${UPLOAD_DIR}" ]; then
  cp -r "${UPLOAD_DIR}" "${BACKUP_DIR}/uploads"
  MEDIA_SIZE=$(du -sh "${BACKUP_DIR}/uploads" | cut -f1)
  echo "  媒体文件大小: ${MEDIA_SIZE}"
else
  echo "  警告: 上传目录 ${UPLOAD_DIR} 不存在,跳过"
  mkdir -p "${BACKUP_DIR}/uploads"
fi

# --------------------------------------------------
# 3. 打包成 zip
# --------------------------------------------------
echo "[3/3] 打包..."
ZIP_FILE="/tmp/exam-backup-$(date +%Y%m%d_%H%M%S).zip"
cd /tmp
zip -r "${ZIP_FILE}" "$(basename ${BACKUP_DIR})" -q
ZIP_SIZE=$(du -h "${ZIP_FILE}" | cut -f1)
rm -rf "${BACKUP_DIR}"

echo ""
echo "===== 备份完成 ====="
echo "备份文件: ${ZIP_FILE}"
echo "文件大小: ${ZIP_SIZE}"
echo ""
echo "下一步: 将此文件传到目标服务器,执行 restore-data.sh 恢复"
echo "  scp ${ZIP_FILE} root@目标服务器IP:/tmp/"
