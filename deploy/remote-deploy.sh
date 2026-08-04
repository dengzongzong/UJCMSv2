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
SERVER_DOMAIN="zgrlosta.org.cn"
MYSQL_DB="exam_platform"
MYSQL_USER="root"
MYSQL_PASS="${MYSQL_PASS:-修改成你的数据库root密码}"
JWT_SECRET="${JWT_SECRET:-修改成你的随机密钥字符串至少40个字符}"
# SSL 证书路径(需提前上传到服务器)
SSL_CERT="/etc/nginx/ssl/zgrlosta.org.cn_bundle.crt"
SSL_KEY="/etc/nginx/ssl/zgrlosta.org.cn.key"
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
  access-prefix: https://${SERVER_DOMAIN}/api/uploads/

async:
  task:
    retention-days: 7
    zombie-hours: 2

# 直播配置(从环境变量注入, 未配置时使用默认值)
live:
  provider: ${LIVE_PROVIDER:-tencent}
  app-name: live
  push-host: ${LIVE_PUSH_HOST:-push.zgrlosta.org.cn}
  play-host: ${LIVE_PLAY_HOST:-play.zgrlosta.org.cn}
  callback-secret: "${CALLBACK_SECRET:-}"
  tencent:
    push-key: "${TENCENT_PUSH_KEY:-}"
    play-key: "${TENCENT_PLAY_KEY:-}"
    push-valid-seconds: 86400
    play-valid-seconds: 172800
  aliyun:
    auth-key: "${ALIYUN_AUTH_KEY:-}"
    valid-seconds: 86400

# 课程支付配置(从环境变量注入, 未配置时关闭对应通道)
pay:
  channel: ${PAY_CHANNEL:-both}
  callback-base: https://${SERVER_DOMAIN}/api
  wechat:
    mch-id: "${WECHAT_MCH_ID:-}"
    app-id: "${WECHAT_APP_ID:-}"
    api-v3-key: "${WECHAT_API_V3_KEY:-}"
    merchant-serial-no: "${WECHAT_MERCHANT_SERIAL_NO:-}"
    merchant-private-key: "${WECHAT_MERCHANT_PRIVATE_KEY:-}"
    platform-public-key: "${WECHAT_PLATFORM_PUBLIC_KEY:-}"
  alipay:
    app-id: "${ALIPAY_APP_ID:-}"
    private-key: "${ALIPAY_PRIVATE_KEY:-}"
    alipay-public-key: "${ALIPAY_PUBLIC_KEY:-}"
    gateway: https://openapi.alipay.com/gateway.do

logging:
  level:
    com.exam: info
  file:
    name: /opt/exam-platform/logs/exam-platform.log
EOF
echo "  配置文件已生成: $DEPLOY_DIR/application-prod.yml"

# 6. 执行 SQL 升级 (仅DDL: 补字段/建表,幂等可重复执行)
echo "[6/8] 执行 SQL 升级..."
if [ -f "upgrade_all.sql" ]; then
    mysql -u${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} --default-character-set=utf8mb4 --force < upgrade_all.sql 2>&1 | grep -v "Using a password"
    echo "  SQL 升级完成"
else
    echo "  警告: upgrade_all.sql 不存在,跳过"
fi

# 以下一次性数据修复SQL已全部执行完毕,不再每次升级重复执行
# 如需重新执行,请在服务器上手动 mysql < xxx.sql
# - fix_all_cert_issues.sql       (证书类型/模板绑定修复)
# - fix_publish_time.sql          (文章发布时间填充)
# - fix_publish_time_v2.sql       (文章发布时间分散)
# - fix_id_card_empty.sql         (身份证空字符串修复)
# - fix_zy_scores.sql             (职业能力证书成绩回填)
# - fix_issue_date_v2.sql         (证书颁发日期修复)
# - import_missing_certs.sql      (缺失证书导入)
# - fix_dup_certs.sql             (证书编号重复修复)

# 一次性数据恢复/修复脚本已全部执行完毕,已从部署流程中移除
# 包括: recover_certificate_full.py, fix_recovered_fields.py, 删除空证书类型
# 如需重新执行,请在服务器上手动操作

# 7. 配置 Nginx (HTTPS 模式,带域名和SSL证书)
echo "[7/8] 配置 Nginx (HTTPS)..."
# 先备份当前配置
cp /etc/nginx/conf.d/exam-platform.conf /etc/nginx/conf.d/exam-platform.conf.bak 2>/dev/null || true

# 检查 SSL 证书是否存在,不存在则回退到 HTTP 模式
if [ -f "${SSL_CERT}" ] && [ -f "${SSL_KEY}" ]; then
cat > /etc/nginx/conf.d/exam-platform.conf << NGINX_EOF
# HTTP -> HTTPS 自动跳转
server {
    listen 80;
    server_name ${SERVER_DOMAIN} www.${SERVER_DOMAIN};
    return 301 https://\$host\$request_uri;
}

# HTTPS 主配置
server {
    listen 443 ssl;
    server_name ${SERVER_DOMAIN} www.${SERVER_DOMAIN};

    ssl_certificate     ${SSL_CERT};
    ssl_certificate_key ${SSL_KEY};
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers  on;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;

    # 管理后台 admin-web
    location /admin/ {
        alias /opt/exam-platform/admin-web/dist/;
        try_files \$uri \$uri/ /admin/index.html;
        index index.html;
    }

    # 学员端 user-web (根路径直接访问 user-web)
    location / {
        root /opt/exam-platform/user-web/dist/;
        try_files \$uri \$uri/ /index.html;
        index index.html;
    }

    # entry html no-cache (always load latest js after deploy)
    location = /admin/index.html {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }
    location = /index.html {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        # WebSocket 支持(直播间聊天)
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
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
    echo "  Nginx HTTPS 配置已写入"
else
    echo "  警告: SSL 证书不存在 (${SSL_CERT}),回退到 HTTP 模式"
cat > /etc/nginx/conf.d/exam-platform.conf << 'NGINX_EOF'
server {
    listen 80;
    server_name 43.162.107.232;

    location /admin/ {
        alias /opt/exam-platform/admin-web/dist/;
        try_files $uri $uri/ /admin/index.html;
        index index.html;
    }

    location / {
        root /opt/exam-platform/user-web/dist/;
        try_files $uri $uri/ /index.html;
        index index.html;
    }

    # entry html no-cache (always load latest js after deploy)
    location = /admin/index.html {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }
    location = /index.html {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # WebSocket 支持(直播间聊天)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        client_max_body_size 1500m;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080/api/uploads/;
    }

    location /static/ {
        proxy_pass http://127.0.0.1:8080/api/static/;
    }
}
NGINX_EOF
fi

if nginx -t 2>/dev/null; then
    systemctl reload nginx || true
    echo "  Nginx 配置已更新并重新加载"
else
    echo "  警告: Nginx 配置测试失败,恢复上次备份并重新加载"
    # 恢复旧配置并重新校验/加载,确保站点不因配置问题失效
    cp /etc/nginx/conf.d/exam-platform.conf.bak /etc/nginx/conf.d/exam-platform.conf 2>/dev/null || true
    if nginx -t 2>/dev/null; then
        systemctl reload nginx || true
        echo "  已恢复备份配置并重新加载"
    else
        echo "  错误: 备份配置也无法通过校验,请人工检查 /etc/nginx/conf.d/exam-platform.conf"
    fi
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

# 9. 安装每日备份脚本和定时任务
echo "[9/9] 安装每日备份定时任务..."
if [ -f "${SCRIPT_DIR}/daily-backup.sh" ]; then
    cp "${SCRIPT_DIR}/daily-backup.sh" "${DEPLOY_DIR}/daily-backup.sh"
    chmod +x "${DEPLOY_DIR}/daily-backup.sh"
    # 设置数据库密码
    sed -i "s/MYSQL_PASS=\"Root@123456\"/MYSQL_PASS=\"${MYSQL_PASS}\"/" "${DEPLOY_DIR}/daily-backup.sh"
    # 创建备份目录
    mkdir -p "${DEPLOY_DIR}/backups"
    # 添加 crontab (每天0点执行),先清除旧的再添加避免重复
    (crontab -l 2>/dev/null | grep -v "daily-backup.sh"; echo "0 0 * * * ${DEPLOY_DIR}/daily-backup.sh >> ${DEPLOY_DIR}/backups/backup.log 2>&1") | crontab -
    echo "  每日备份定时任务已安装 (每天0点执行)"
    echo "  备份脚本: ${DEPLOY_DIR}/daily-backup.sh"
    echo "  备份目录: ${DEPLOY_DIR}/backups/"
    echo "  保留最近5个备份"
else
    echo "  警告: daily-backup.sh 不存在,跳过"
fi

echo ""
echo "========================================"
echo "  部署完成!"
echo "========================================"
echo ""
echo "  管理后台: https://${SERVER_DOMAIN}/admin/"
echo "  学员端:   https://${SERVER_DOMAIN}/"
echo "  (HTTP 自动跳转 HTTPS)"
echo ""
echo "  查看日志: journalctl -u exam-platform -f"
echo ""
