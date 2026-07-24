#!/bin/bash
# =====================================================================
# 远程部署脚本 (在服务器上执行)
# ---------------------------------------------------------------------
# 由 GitHub Actions 通过 SSH 调用,完成最终的部署工作:
#   1. 停止旧的后端服务
#   2. 替换 JAR 文件
#   3. 替换前端静态文件
#   4. 执行 SQL 升级
#   5. 配置 Nginx
#   6. 启动新的后端服务
#
# 参数(通过环境变量传入):
#   MYSQL_PASS   - 数据库root密码
#   JWT_SECRET   - JWT密钥
# =====================================================================

set -e

# ===== 配置区 =====
DEPLOY_DIR="/opt/exam-platform"
SERVER_IP="43.162.107.232"
MYSQL_DB="exam_platform"
MYSQL_USER="root"
MYSQL_PASS="${MYSQL_PASS:-修改成你的数据库root密码}"
JWT_SECRET="${JWT_SECRET:-修改成你的随机密钥字符串至少40个字符}"
# ================================

echo "========================================"
echo "  开始远程部署"
echo "  服务器: $SERVER_IP"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

# 当前脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 1. 停止旧的后端服务
echo "[1/8] 停止旧的后端服务..."
if systemctl is-active --quiet exam-platform 2>/dev/null; then
    systemctl stop exam-platform
    echo "  已停止 exam-platform 服务"
else
    echo "  exam-platform 服务未运行或不存在,跳过"
fi

# 2. 创建目录
echo "[2/8] 创建目录..."
mkdir -p $DEPLOY_DIR/admin-web/dist
mkdir -p $DEPLOY_DIR/user-web/dist
mkdir -p $DEPLOY_DIR/uploads
mkdir -p $DEPLOY_DIR/logs

# 3. 替换后端 JAR
echo "[3/8] 部署后端 JAR..."
if [ -f "exam-platform-1.0.0.jar" ]; then
    cp exam-platform-1.0.0.jar $DEPLOY_DIR/exam-platform-1.0.0.jar
    echo "  JAR 已复制到 $DEPLOY_DIR/exam-platform-1.0.0.jar"
else
    echo "  错误: exam-platform-1.0.0.jar 不存在!"
    exit 1
fi

# 4. 部署前端静态文件
echo "[4/8] 部署前端静态文件..."
if [ -f "admin-web-dist.tar.gz" ]; then
    rm -rf $DEPLOY_DIR/admin-web/dist/*
    tar -xzf admin-web-dist.tar.gz -C $DEPLOY_DIR/admin-web/dist/
    echo "  管理后台前端已部署"
else
    echo "  警告: admin-web-dist.tar.gz 不存在,跳过"
fi

if [ -f "user-web-dist.tar.gz" ]; then
    rm -rf $DEPLOY_DIR/user-web/dist/*
    tar -xzf user-web-dist.tar.gz -C $DEPLOY_DIR/user-web/dist/
    echo "  用户端前端已部署"
else
    echo "  警告: user-web-dist.tar.gz 不存在,跳过"
fi

# 5. 生成生产环境配置文件
echo "[5/8] 生成配置文件..."
cat > $DEPLOY_DIR/application-prod.yml << EOF
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/${MYSQL_DB}?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASS}
  servlet:
    multipart:
      max-file-size: 1500MB
      max-request-size: 1500MB
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      adjust-dates-to-context-time-zone: true

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.exam.entity
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

upload:
  path: /opt/exam-platform/uploads
  access-prefix: http://${SERVER_IP}/api/uploads/

async:
  task:
    retention-days: 7
    zombie-hours: 2

logging:
  level:
    com.exam: info
  file:
    name: /opt/exam-platform/logs/exam-platform.log
EOF
echo "  配置文件已生成: $DEPLOY_DIR/application-prod.yml"

# 6. 执行 SQL 升级 (幂等,可重复执行)
echo "[6/8] 执行 SQL 升级..."
if [ -f "upgrade_all.sql" ]; then
    mysql -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} --default-character-set=utf8mb4 --force < upgrade_all.sql 2>&1 | grep -v "Using a password"
    echo "  SQL 升级完成"
else
    echo "  警告: upgrade_all.sql 不存在,跳过"
fi

# 6.1 执行证书修复SQL (幂等,安全,不删除用户自定义类型)
if [ -f "fix_all_cert_issues.sql" ]; then
    echo "  执行证书修复SQL..."
    mysql -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} --default-character-set=utf8mb4 --force < fix_all_cert_issues.sql 2>&1 | grep -v "Using a password"
    echo "  证书修复SQL完成"
fi

# 6.2 导入缺失的证书数据 (幂等,INSERT IGNORE)
if [ -f "import_missing_certs.sql" ]; then
    echo "  导入缺失证书数据..."
    mysql -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} --default-character-set=utf8mb4 --force < import_missing_certs.sql 2>&1 | grep -v "Using a password"
    echo "  证书数据导入完成"
fi

# 7. 配置 Nginx (HTTP 模式,无域名无SSL)
echo "[7/8] 配置 Nginx..."
# 先备份当前配置
cp /etc/nginx/conf.d/exam-platform.conf /etc/nginx/conf.d/exam-platform.conf.bak 2>/dev/null || true
cat > /etc/nginx/conf.d/exam-platform.conf << 'NGINX_EOF'
server {
    listen 80;
    server_name 43.162.107.232;

    # 管理后台 admin-web
    location /admin/ {
        alias /opt/exam-platform/admin-web/dist/;
        try_files $uri $uri/ /admin/index.html;
        index index.html;
    }

    # 学员端 user-web (根路径直接访问 user-web，不跳转到 admin)
    location / {
        root /opt/exam-platform/user-web/dist/;
        try_files $uri $uri/ /index.html;
        index index.html;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 1500m;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    # 上传文件
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080/api/uploads/;
    }

    # 静态资源(旧系统导入的图片路径 /static/upload/xxx)
    location /static/ {
        proxy_pass http://127.0.0.1:8080/api/static/;
    }
}
NGINX_EOF

if nginx -t 2>/dev/null; then
    systemctl reload nginx || true
    echo "  Nginx 配置已更新并重新加载"
else
    echo "  警告: Nginx 配置测试失败,跳过重载,使用旧配置"
    # 恢复旧配置以确保 Nginx 正常运行
    cp /etc/nginx/conf.d/exam-platform.conf.bak /etc/nginx/conf.d/exam-platform.conf 2>/dev/null || true
fi

# 8. 创建并启动 systemd 服务
echo "[8/8] 启动后端服务..."
cat > /etc/systemd/system/exam-platform.service << EOF
[Unit]
Description=Exam Platform Backend
After=network.target mysqld.service

[Service]
Type=simple
User=root
WorkingDirectory=${DEPLOY_DIR}
ExecStart=/usr/bin/java -jar ${DEPLOY_DIR}/exam-platform-1.0.0.jar --spring.profiles.active=prod --spring.config.additional-location=file:${DEPLOY_DIR}/
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable exam-platform

systemctl start exam-platform
sleep 3

if systemctl is-active --quiet exam-platform; then
    echo "  exam-platform 服务启动成功"
else
    echo "  警告: exam-platform 服务启动失败,查看日志: journalctl -u exam-platform -n 20"
fi

echo ""
echo "========================================"
echo "  部署完成!"
echo "========================================"
echo ""
echo "  管理后台: http://${SERVER_IP}/admin/"
echo "  学员端:   http://${SERVER_IP}/"
echo ""
echo "  查看日志: journalctl -u exam-platform -f"
echo ""
