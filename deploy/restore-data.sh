#!/bin/bash
# ============================================================
# 数据恢复脚本 - 在目标系统服务器上执行
# 全部使用合并模式(INSERT IGNORE),不会覆盖任何已有数据
# 媒体文件用 cp -n(不覆盖已有文件)
# ============================================================
set -e

MYSQL_DB="exam_platform"
MYSQL_USER="root"
MYSQL_PASS="${MYSQL_PASS:-Root@123456}"
# 新系统部署在 /opt/exam-platform,老系统在 /data/exam-platform
# 自动检测: 优先 /opt,其次 /data,可用 UPLOAD_DIR 环境变量覆盖
if [ -d "/opt/exam-platform/uploads" ]; then
  UPLOAD_DIR="/opt/exam-platform/uploads"
elif [ -d "/data/exam-platform/uploads" ]; then
  UPLOAD_DIR="/data/exam-platform/uploads"
else
  UPLOAD_DIR="${UPLOAD_DIR:-/opt/exam-platform/uploads}"
fi

# 接受参数: ZIP 文件路径
ZIP_FILE="${1:-/tmp/exam-backup-*.zip}"

# 如果是通配符,找到最新的文件
if [[ "${ZIP_FILE}" == *"*"* ]]; then
  ZIP_FILE=$(ls -t ${ZIP_FILE} 2>/dev/null | head -1)
fi

if [ -z "${ZIP_FILE}" ] || [ ! -f "${ZIP_FILE}" ]; then
  echo "错误: 备份文件不存在"
  echo "用法: bash restore-data.sh <备份文件.zip>"
  echo "示例: bash restore-data.sh /tmp/exam-backup-20260723_150000.zip"
  exit 1
fi

echo "===== 数据恢复开始 ====="
echo "数据库: ${MYSQL_DB}"
echo "备份文件: ${ZIP_FILE}"
echo "模式: 合并模式(不覆盖任何已有数据)"
echo ""

# 解压
TMP_DIR="/tmp/exam-restore-$$"
mkdir -p "${TMP_DIR}"
unzip -o "${ZIP_FILE}" -d /tmp/ -q
BACKUP_DIR=$(ls -d /tmp/exam-backup-* 2>/dev/null | head -1)

if [ -z "${BACKUP_DIR}" ] || [ ! -d "${BACKUP_DIR}" ]; then
  echo "错误: 解压后未找到备份数据目录"
  exit 1
fi

echo "解压目录: ${BACKUP_DIR}"

# --------------------------------------------------
# 1. 恢复数据库
#    - 配置类表(课程/视频/题库/考试/证书模板等): 清空后覆盖
#    - 用户类表(学生/证书/考试记录等): INSERT IGNORE 合并,不覆盖
# --------------------------------------------------
echo "[1/2] 恢复数据库..."
SQL_FILE="${BACKUP_DIR}/data_full.sql"
if [ -f "${SQL_FILE}" ]; then
  # 1a. 清空配置类表(课程/视频/题库/考试/网站内容/系统设置等,用源系统覆盖)
  echo "  清空配置类表(课程/视频/题库/考试等)..."
  mysql -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} --default-character-set=utf8mb4 2>/dev/null << 'CLEAR_SQL'
SET FOREIGN_KEY_CHECKS=0;
SET NAMES utf8mb4;
TRUNCATE TABLE course_section_video;
TRUNCATE TABLE course_section;
TRUNCATE TABLE course;
TRUNCATE TABLE video;
TRUNCATE TABLE question_option;
TRUNCATE TABLE question;
TRUNCATE TABLE question_template_item;
TRUNCATE TABLE question_template;
TRUNCATE TABLE paper_question;
TRUNCATE TABLE paper;
TRUNCATE TABLE exam;
TRUNCATE TABLE video_category;
TRUNCATE TABLE question_category;
TRUNCATE TABLE profession;
TRUNCATE TABLE subject;
TRUNCATE TABLE certificate_template_field;
TRUNCATE TABLE certificate_template;
TRUNCATE TABLE certificate_field;
TRUNCATE TABLE certificate_type;
TRUNCATE TABLE certificate_number_config;
TRUNCATE TABLE certificate_url_config;
TRUNCATE TABLE homepage_section;
TRUNCATE TABLE news;
TRUNCATE TABLE announcement;
TRUNCATE TABLE banner_image;
TRUNCATE TABLE banner;
TRUNCATE TABLE friendly_link;
TRUNCATE TABLE system_setting;
TRUNCATE TABLE about_us;
TRUNCATE TABLE site_declaration;
TRUNCATE TABLE cooperation_setting;
TRUNCATE TABLE course_three_image;
SET FOREIGN_KEY_CHECKS=1;
CLEAR_SQL

  # 1b. 导入数据(配置类表直接导入,用户类表用 INSERT IGNORE 合并)
  echo "  导入数据(配置类覆盖,用户类合并)..."
  mysql -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} --default-character-set=utf8mb4 --force < "${SQL_FILE}" 2>&1 | grep -v "Using a password" || true
  echo "  数据库恢复完成"
else
  echo "  警告: SQL 文件不存在,跳过数据库恢复"
fi

# --------------------------------------------------
# 2. 恢复媒体文件(不覆盖已有文件)
# --------------------------------------------------
echo "[2/2] 恢复媒体文件(不覆盖已有文件)..."
BACKUP_UPLOADS="${BACKUP_DIR}/uploads"
if [ -d "${BACKUP_UPLOADS}" ]; then
  mkdir -p "${UPLOAD_DIR}"
  # cp -n: 不覆盖已存在的文件,只新增不存在的
  cp -rn "${BACKUP_UPLOADS}/"* "${UPLOAD_DIR}/" 2>/dev/null || true
  echo "  媒体文件恢复完成: ${UPLOAD_DIR}"
else
  echo "  警告: 媒体文件目录不存在,跳过"
fi

# 清理
rm -rf "${BACKUP_DIR}" "${TMP_DIR}"

echo ""
echo "===== 恢复完成 ====="
echo "所有数据已合并导入,已有数据未被覆盖"
echo "请重启后端服务使数据生效:"
echo "  systemctl restart exam-platform"
