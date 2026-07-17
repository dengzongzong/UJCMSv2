#!/bin/bash
# =====================================================================
# 考试平台一键部署脚本
# 域名: gjrccp.org.cn
# 
# 部署后访问方式:
#   用户端:   https://gjrccp.org.cn/
#   管理后台: https://gjrccp.org.cn/admin/
#
# 使用前修改下面的数据库密码
# =====================================================================

# ===== 配置区(按实际修改) =====
DOMAIN="gjrccp.org.cn"
MYSQL_DB="exam_platform"
MYSQL_USER="exam_user"
MYSQL_PASS="修改成你的数据库密码"
JAVA_JAR="/data/exam-platform/exam-platform.jar"
# ================================

DEPLOY_DIR="/data/exam-platform"
SSL_CERT_DIR="/etc/nginx/ssl/exam-platform"

echo "========================================"
echo "  考试平台部署脚本"
echo "  域名: $DOMAIN"
echo "========================================"

# 1. 创建目录
echo "[1/7] 创建目录..."
mkdir -p $DEPLOY_DIR/admin-web/dist
mkdir -p $DEPLOY_DIR/user-web/dist
mkdir -p $DEPLOY_DIR/uploads
mkdir -p $DEPLOY_DIR/logs
mkdir -p $SSL_CERT_DIR

# 2. 构建后端
echo "[2/7] 构建后端 JAR..."
cd backend
mvn clean package -DskipTests -q
cp target/*.jar $DEPLOY_DIR/exam-platform.jar
cd ..

# 3. 构建管理后台前端(生产环境 publicPath=/admin/)
echo "[3/7] 构建管理后台前端..."
cd admin-web
npm install --silent
NODE_ENV=production npm run build
cp -r dist/* $DEPLOY_DIR/admin-web/dist/
cd ..

# 4. 构建用户端前端
echo "[4/7] 构建用户端前端..."
cd user-web
npm install --silent
NODE_ENV=production npm run build
cp -r dist/* $DEPLOY_DIR/user-web/dist/
cd ..

# 5. 配置后端
echo "[5/7] 配置后端..."
cp backend/src/main/resources/application-prod.yml $DEPLOY_DIR/application-prod.yml
sed -i "s|exam_platform|$MYSQL_DB|g" $DEPLOY_DIR/application-prod.yml
sed -i "s|exam_user|$MYSQL_USER|g" $DEPLOY_DIR/application-prod.yml
sed -i "s|修改成你的数据库密码|$MYSQL_PASS|g" $DEPLOY_DIR/application-prod.yml

# 6. 配置 Nginx
echo "[6/7] 配置 Nginx..."
cp deploy/nginx-exam-platform.conf /etc/nginx/conf.d/exam-platform.conf
nginx -t && systemctl reload nginx

# 7. 创建并启动 systemd 服务
echo "[7/7] 启动后端服务..."
cat > /etc/systemd/system/exam-platform.service <<EOF
[Unit]
Description=Exam Platform Backend
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=$DEPLOY_DIR
ExecStart=/usr/bin/java -jar $JAVA_JAR --spring.profiles.active=prod --spring.config.additional-location=file:$DEPLOY_DIR/
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable exam-platform
systemctl restart exam-platform
sleep 3
systemctl status exam-platform --no-pager

echo ""
echo "========================================"
echo "  部署完成!"
echo "========================================"
echo ""
echo "访问地址:"
echo "  用户端:   https://$DOMAIN/"
echo "  管理后台: https://$DOMAIN/admin/"
