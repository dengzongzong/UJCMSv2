#!/bin/bash
# =====================================================================
# 考试平台一键部署脚本
# 服务器: 43.162.107.232 (无域名, IP直接访问)
# 数据库: root 账号
# 
# 使用前提:
#   1. 后端 JAR 已上传到 /opt/exam-platform/exam-platform.jar
#   2. admin-web 已上传到 /opt/exam-platform/admin-web/dist/
#   3. user-web 已上传到 /opt/exam-platform/user-web/dist/
#   4. SQL 文件已上传到 /tmp/
#
# 使用方法:
#   chmod +x deploy.sh
#   ./deploy.sh
# =====================================================================
set -e

echo "============================================"
echo "  考试平台部署脚本"
echo "  服务器: 43.162.107.232"
echo "  数据库: root 账号"
echo "============================================"

# ====== 配置项 ======
MYSQL_ROOT_PASSWORD="Root@123456"
DEPLOY_DIR="/opt/exam-platform"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# ====================

# 1. 安装基础环境
echo "[1/7] 检查并安装基础环境..."

if ! command -v java &>/dev/null; then
    echo "  安装 Java 11..."
    yum install -y java-11-openjdk-devel
fi
java -version 2>&1 | head -1

if ! command -v nginx &>/dev/null; then
    echo "  安装 Nginx..."
    yum install -y nginx
fi
systemctl enable --now nginx

if ! command -v mysql &>/dev/null; then
    echo "  安装 MySQL..."
    yum install -y mysql-server --disableexcludes=all 2>/dev/null || \
    yum install -y mysql-server --nobest 2>/dev/null || \
    yum install -y mariadb-server
fi
systemctl enable --now mysqld 2>/dev/null || systemctl enable --now mariadb 2>/dev/null || true

# 2. 配置 MySQL
echo "[2/7] 配置 MySQL..."

# 设置 root 密码(如果是新安装)
mysqladmin -u root password "$MYSQL_ROOT_PASSWORD" 2>/dev/null || true

# 创建数据库
mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS exam_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>/dev/null

# 3. 导入 SQL
echo "[3/7] 导入数据库..."
SQL_DIR="$SCRIPT_DIR/backend/sql"
if [ -f "$SQL_DIR/schema.sql" ]; then
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" exam_platform < "$SQL_DIR/schema.sql" 2>/dev/null
    echo "  schema.sql 导入完成"
elif [ -f "/tmp/schema.sql" ]; then
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" exam_platform < /tmp/schema.sql 2>/dev/null
    echo "  /tmp/schema.sql 导入完成"
fi

if [ -f "$SQL_DIR/upgrade_all.sql" ]; then
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" exam_platform < "$SQL_DIR/upgrade_all.sql" 2>/dev/null
    echo "  upgrade_all.sql 导入完成"
elif [ -f "/tmp/upgrade_all.sql" ]; then
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" exam_platform < /tmp/upgrade_all.sql 2>/dev/null
    echo "  /tmp/upgrade_all.sql 导入完成"
fi

# 4. 创建部署目录
echo "[4/7] 创建部署目录..."
mkdir -p "$DEPLOY_DIR/uploads" "$DEPLOY_DIR/logs"

# 5. 复制配置文件
echo "[5/7] 部署配置文件..."

# 后端配置
if [ -f "$SCRIPT_DIR/deploy/config/application-prod.yml" ]; then
    cp "$SCRIPT_DIR/deploy/config/application-prod.yml" "$DEPLOY_DIR/application-prod.yml"
    # 替换数据库密码
    sed -i "s/Root@123456/$MYSQL_ROOT_PASSWORD/g" "$DEPLOY_DIR/application-prod.yml"
fi

# Nginx 配置
if [ -f "$SCRIPT_DIR/deploy/nginx/exam-platform.conf" ]; then
    cp "$SCRIPT_DIR/deploy/nginx/exam-platform.conf" /etc/nginx/conf.d/exam-platform.conf
fi

# systemd 服务
if [ -f "$SCRIPT_DIR/deploy/systemd/exam-platform.service" ]; then
    cp "$SCRIPT_DIR/deploy/systemd/exam-platform.service" /etc/systemd/system/exam-platform.service"
fi

# 6. 修复权限并启动
echo "[6/7] 配置权限并启动服务..."
chmod -R 755 "$DEPLOY_DIR"
chown -R nginx:nginx "$DEPLOY_DIR" 2>/dev/null || true

# 关闭 SELinux (避免 Nginx 403)
setenforce 0 2>/dev/null || true
sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config 2>/dev/null || true

# 防火墙
firewall-cmd --permanent --add-port=80/tcp 2>/dev/null || true
firewall-cmd --reload 2>/dev/null || true

# 启动 Nginx
nginx -t && systemctl restart nginx

# 启动后端
systemctl daemon-reload
systemctl enable exam-platform
systemctl restart exam-platform

# 7. 验证
echo "[7/7] 验证部署..."
sleep 5

echo ""
echo "============================================"
echo "  部署完成!"
echo "============================================"
echo ""
echo "  管理后台:  http://43.162.107.232/admin/"
echo "  学员端:    http://43.162.107.232/"
echo "  API:       http://43.162.107.232/api/"
echo ""
echo "  默认账号:  admin / admin123"
echo ""
echo "  常用命令:"
echo "    查看后端状态: systemctl status exam-platform"
echo "    查看后端日志: journalctl -u exam-platform -f"
echo "    重启后端:     systemctl restart exam-platform"
echo "    重启 Nginx:   systemctl restart nginx"
echo ""
