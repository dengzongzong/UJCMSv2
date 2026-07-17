# gjrccp.org.cn 域名绑定 & HTTPS 证书部署详细指南

## 整体架构

```
                    ┌──────────────────────────────────────────┐
                    │            Nginx (443/80)                │
                    │                                          │
   用户访问          │  https://gjrccp.org.cn/          → 用户端  │
                    │  https://gjrccp.org.cn/admin/    → 管理后台│
                    │  https://gjrccp.org.cn/api/     → 后端8080│
                    │  https://gjrccp.org.cn/uploads/ → 后端8080│
                    └─────────────────┬────────────────────────┘
                                      │
                    ┌────────────────▼────────────────┐
                    │   Spring Boot (端口 8080)        │
                    │   context-path: /api             │
                    │   MySQL / 上传文件                │
                    └─────────────────────────────────┘
```

访问地址：
- 用户端：`https://gjrccp.org.cn/`
- 管理后台：`https://gjrccp.org.cn/admin/`

---

## 第一步：域名解析（腾讯云 DNS）

1. 登录腾讯云控制台 → **DNS 解析 DNSPod**
2. 找到你的域名 `gjrccp.org.cn` → 点击进入解析记录
3. 添加以下两条 A 记录：

| 记录类型 | 主机记录 | 记录值 |
|----------|----------|--------|
| A | `@` | 你的服务器公网IP |
| A | `www` | 你的服务器公网IP |

4. 添加后等待 2-5 分钟生效
5. 验证：在电脑上打开命令行，执行 `ping gjrccp.org.cn`，看返回的 IP 是不是你的服务器 IP

---

## 第二步：服务器安装环境

SSH 登录你的服务器后执行：

### 2.1 安装 Java、MySQL、Nginx

```bash
# CentOS 系统
yum install -y java-1.8.0-openjdk-devel
yum install -y nginx
yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
yum install -y mysql-community-server

# Ubuntu 系统（如果用的是 Ubuntu）
# apt update
# apt install -y openjdk-8-jdk nginx mysql-server
```

### 2.2 启动 MySQL 和 Nginx

```bash
systemctl start mysqld nginx
systemctl enable mysqld nginx
```

### 2.3 获取 MySQL 初始密码并修改

```bash
# 获取 MySQL root 初始密码
grep 'temporary password' /var/log/mysqld.log
# 会输出类似: A temporary password is generated for root@localhost: xxxxxx

# 登录 MySQL
mysql -u root -p
# 输入上面获取的临时密码
```

```sql
-- 修改 root 密码
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的新Root密码';
EXIT;
```

### 2.4 创建数据库和用户

```bash
mysql -u root -p
```

```sql
-- 创建数据库
CREATE DATABASE exam_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 创建专用用户（把密码改成你自己的）
CREATE USER 'exam_user'@'localhost' IDENTIFIED BY '你的数据库密码';
GRANT ALL PRIVILEGES ON exam_platform.* TO 'exam_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

## 第三步：构建项目

在你的开发机（有 Java 和 Node.js 环境的电脑）上操作：

### 3.1 构建后端

```bash
cd V1exam-platform—code/backend
mvn clean package -DskipTests
# 产物: target/exam-platform-1.0.0.jar
```

### 3.2 构建管理后台前端

```bash
cd V1exam-platform—code/admin-web
npm install
npm run build
# 产物: admin-web/dist/ 目录
```

### 3.3 构建用户端前端

```bash
cd V1exam-platform—code/user-web
npm install
npm run build
# 产物: user-web/dist/ 目录
```

---

## 第四步：上传文件到服务器

### 4.1 在服务器上创建目录

```bash
mkdir -p /data/exam-platform/{admin-web/dist,user-web/dist,uploads,logs}
mkdir -p /etc/nginx/ssl/exam-platform
```

### 4.2 上传构建产物

在你的开发机上执行（把 `服务器IP` 换成实际 IP）：

```bash
# 上传后端 jar 包
scp backend/target/exam-platform-1.0.0.jar root@服务器IP:/data/exam-platform/exam-platform.jar

# 上传管理后台前端
scp -r admin-web/dist/* root@服务器IP:/data/exam-platform/admin-web/dist/

# 上传用户端前端
scp -r user-web/dist/* root@服务器IP:/data/exam-platform/user-web/dist/

# 上传数据库脚本
scp backend/sql/schema.sql root@服务器IP:/data/exam-platform/schema.sql
```

### 4.3 导入数据库

在服务器上执行：

```bash
mysql -u exam_user -p exam_platform < /data/exam-platform/schema.sql
```

---

## 第五步：配置后端

### 5.1 创建生产配置文件

在服务器上执行：

```bash
vi /data/exam-platform/application-prod.yml
```

粘贴以下内容，**修改两个密码**：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/exam_platform?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true
    username: exam_user
    password: 你的数据库密码          # ← 改成第二步 2.4 中设置的密码
  servlet:
    multipart:
      max-file-size: 1500MB
      max-request-size: 1500MB
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.exam.entity
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

jwt:
  secret: gjrccp_exam_platform_secret_key_2024_random_string_40chars
  expiration: 86400000

upload:
  path: /data/exam-platform/uploads
  access-prefix: https://gjrccp.org.cn/api/uploads/

async:
  task:
    retention-days: 7
    zombie-hours: 2

logging:
  level:
    com.exam: info
  file:
    name: /data/exam-platform/logs/exam-platform.log
```

按 `ESC`，输入 `:wq` 保存退出。

### 5.2 创建系统服务

```bash
vi /etc/systemd/system/exam-platform.service
```

粘贴以下内容：

```ini
[Unit]
Description=Exam Platform Backend
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/data/exam-platform
ExecStart=/usr/bin/java -jar /data/exam-platform/exam-platform.jar --spring.profiles.active=prod --spring.config.additional-location=file:/data/exam-platform/
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

按 `ESC`，输入 `:wq` 保存退出。

### 5.3 启动后端

```bash
systemctl daemon-reload
systemctl enable exam-platform
systemctl start exam-platform

# 查看是否启动成功
systemctl status exam-platform

# 查看运行日志
tail -f /data/exam-platform/logs/exam-platform.log
```

看到日志中出现 `Started ExamApplication` 字样说明启动成功。按 `Ctrl+C` 退出日志查看。

---

## 第六步：配置 Nginx

### 6.1 上传 Nginx 配置文件

在开发机上执行：

```bash
scp deploy/nginx-exam-platform.conf root@服务器IP:/etc/nginx/conf.d/exam-platform.conf
```

或者在服务器上直接创建：

```bash
vi /etc/nginx/conf.d/exam-platform.conf
```

粘贴以下内容：

```nginx
# ---------- HTTP -> HTTPS 强制跳转 ----------
server {
    listen 80;
    server_name gjrccp.org.cn www.gjrccp.org.cn;
    return 301 https://$host$request_uri;
}

# ---------- 主站(用户端 + 管理后台共用一个域名) ----------
server {
    listen 443 ssl;
    http2 on;
    server_name gjrccp.org.cn www.gjrccp.org.cn;

    # === HTTPS 证书 ===
    ssl_certificate     /etc/nginx/ssl/exam-platform/gjrccp.org.cn_bundle.crt;
    ssl_certificate_key /etc/nginx/ssl/exam-platform/gjrccp.org.cn.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;

    # === 管理后台 (通过 /admin/ 路径访问) ===
    location /admin/ {
        alias /data/exam-platform/admin-web/dist/;
        try_files $uri $uri/ /admin/index.html;
    }

    # === 用户端前端 ===
    root /data/exam-platform/user-web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # === API 接口反代到后端 Spring Boot (8080) ===
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 1500m;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    # === 上传文件访问 ===
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080/api/uploads/;
        proxy_set_header Host $host;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 6.2 此时先不要重载 Nginx

证书还没上传，先完成下一步证书操作。

---

## 第七步：下载并上传腾讯云 SSL 证书

### 7.1 下载证书

1. 登录腾讯云控制台 → **SSL 证书**
2. 找到你已申请的证书（域名 gjrccp.org.cn）
3. 点击 **下载**
4. 服务器类型选择 **Nginx**
5. 下载后解压，得到两个文件：
   - `gjrccp.org.cn_bundle.crt`（证书文件）
   - `gjrccp.org.cn.key`（私钥文件）

> 如果解压出来的文件名不完全一样，**重命名**为上面这两个名字。

### 7.2 上传证书到服务器

在开发机上执行：

```bash
# 创建证书目录（在服务器上执行）
mkdir -p /etc/nginx/ssl/exam-platform

# 上传证书文件（在开发机上执行）
scp gjrccp.org.cn_bundle.crt root@服务器IP:/etc/nginx/ssl/exam-platform/gjrccp.org.cn_bundle.crt
scp gjrccp.org.cn.key root@服务器IP:/etc/nginx/ssl/exam-platform/gjrccp.org.cn.key
```

### 7.3 设置权限

在服务器上执行：

```bash
chmod 644 /etc/nginx/ssl/exam-platform/gjrccp.org.cn_bundle.crt
chmod 600 /etc/nginx/ssl/exam-platform/gjrccp.org.cn.key
```

### 7.4 验证证书文件是否正确

```bash
# 检查证书内容
openssl x509 -in /etc/nginx/ssl/exam-platform/gjrccp.org.cn_bundle.crt -noout -subject -dates
```

输出应该显示域名 `gjrccp.org.cn` 和有效期。

---

## 第八步：重载 Nginx 并验证

### 8.1 测试 Nginx 配置

```bash
nginx -t
```

看到 `syntax is ok` 和 `test is successful` 说明配置没问题。

### 8.2 重载 Nginx

```bash
nginx -s reload
```

### 8.3 验证访问

| 验证项 | 地址 | 预期结果 |
|--------|------|----------|
| HTTP 跳转 | `http://gjrccp.org.cn` | 自动跳转到 `https://` |
| 用户端 | `https://gjrccp.org.cn/` | 显示用户端首页 |
| 管理后台 | `https://gjrccp.org.cn/admin/` | 显示管理后台登录页 |
| 证书 | 浏览器地址栏 | 显示锁图标 |

---

## 常见问题排查

### 问题1：访问网站打不开

```bash
# 检查 Nginx 是否运行
systemctl status nginx

# 检查后端是否运行
systemctl status exam-platform

# 检查 80/443 端口是否监听
netstat -tlnp | grep -E '80|443'

# 检查防火墙是否放行了 80 和 443 端口
# 腾讯云安全组: 控制台 → 云服务器 → 安全组 → 添加入站规则
# 放行 TCP 80 和 443 端口
```

### 问题2：后端启动失败

```bash
# 查看详细日志
journalctl -u exam-platform -f

# 常见原因：
# 1. 数据库密码不对 → 检查 application-prod.yml 中的 password
# 2. 数据库没创建 → 确认 exam_platform 库已创建
# 3. 8080端口被占用 → netstat -tlnp | grep 8080
```

### 问题3：证书报错

```bash
# 检查证书文件是否匹配
openssl x509 -in /etc/nginx/ssl/exam-platform/gjrccp.org.cn_bundle.crt -noout -modulus | md5sum
openssl rsa -in /etc/nginx/ssl/exam-platform/gjrccp.org.cn.key -noout -modulus | md5sum
# 两个输出的 md5 必须一致,不一致说明证书和私钥不匹配

# 检查证书域名
openssl x509 -in /etc/nginx/ssl/exam-platform/gjrccp.org.cn_bundle.crt -noout -text | grep -A1 "Subject Alternative Name"
```

### 问题4：管理后台白屏

```bash
# 检查前端文件是否上传完整
ls /data/exam-platform/admin-web/dist/
# 应该看到 index.html 和 js/ css/ 目录

# 检查 index.html 中的资源路径
cat /data/exam-platform/admin-web/dist/index.html | grep -o 'src="[^"]*"'
# 路径应该以 /admin/ 开头
```

### 问题5：上传文件无法访问

```bash
# 检查上传目录是否存在
ls -la /data/exam-platform/uploads/

# 检查后端是否正常返回文件
curl -I https://gjrccp.org.cn/api/uploads/test.jpg
```

### 问题6：重新部署后端

```bash
# 上传新的 jar 包后
systemctl restart exam-platform
# 查看日志
tail -f /data/exam-platform/logs/exam-platform.log
```

### 问题7：重新部署前端

```bash
# 上传新的 dist 文件后,不需要重启任何服务
# Nginx 直接读取静态文件,刷新浏览器即可
# 如果浏览器缓存,强制刷新: Ctrl+Shift+R
```

---

## 服务器安全组配置（腾讯云）

腾讯云控制台 → 云服务器 → 实例 → 安全组 → 添加入站规则：

| 来源 | 协议端口 | 策略 | 说明 |
|------|----------|------|------|
| 0.0.0.0/0 | TCP:80 | 允许 | HTTP |
| 0.0.0.0/0 | TCP:443 | 允许 | HTTPS |
| 0.0.0.0/0 | TCP:22 | 允许 | SSH |
| 0.0.0.0/0 | TCP:3306 | 允许 | MySQL（建议只允许本机，不开放） |

---

## 操作顺序总结

```
1. 域名解析          → 腾讯云 DNS 添加 A 记录指向服务器IP
2. 服务器装环境      → Java + MySQL + Nginx
3. 创建数据库        → exam_platform 库 + exam_user 用户
4. 构建项目          → mvn package + npm build
5. 上传到服务器      → jar + 前端dist + schema.sql
6. 导入数据库        → mysql < schema.sql
7. 配置后端          → application-prod.yml（改密码）
8. 启动后端          → systemctl start exam-platform
9. 下载 SSL 证书     → 腾讯云下载 Nginx 版
10. 上传证书         → /etc/nginx/ssl/exam-platform/
11. 配置 Nginx       → 上传 nginx 配置文件
12. 重载 Nginx       → nginx -t && nginx -s reload
13. 验证             → https://gjrccp.org.cn/
```
