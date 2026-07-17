# 考试平台域名绑定 & HTTPS 证书部署指南

## 架构说明

```
                    ┌──────────────────────────────────────────┐
                    │            Nginx (443/80)                │
                    │                                          │
   用户访问          │  https://你的域名/          → 用户端前端  │
   https://         │  https://你的域名/admin/    → 管理后台    │
                   │  https://你的域名/api/      → 反代后端8080│
                   │  https://你的域名/uploads/  → 反代后端8080│
                   └─────────────────┬────────────────────────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │   Spring Boot (端口 8080)      │
                    │   context-path: /api            │
                    │   MySQL / 上传文件              │
                    └────────────────────────────────┘
```

**只需要一个域名 + 一张证书**，管理后台通过 `/admin/` 路径访问。

---

## 第一步：域名解析

登录腾讯云控制台 → DNS 解析 DNSPod → 你的域名 → 添加记录：

| 记录类型 | 主机记录 | 记录类型 | 记录值 |
|----------|----------|----------|--------|
| A | `@` | A | 你的服务器公网IP |
| A | `www` | A | 你的服务器公网IP |

添加后等几分钟生效。可以用 `ping 你的域名` 验证是否解析到你的服务器IP。

---

## 第二步：服务器环境准备

在服务器上安装 Java、MySQL、Nginx：

```bash
# CentOS
yum install -y java-1.8.0-openjdk-devel mysql-server nginx
systemctl start mysqld nginx
systemctl enable mysqld nginx

# Ubuntu/Debian
apt update
apt install -y openjdk-8-jdk mysql-server nginx
systemctl start mysql nginx
systemctl enable mysql nginx
```

创建数据库：

```bash
mysql -u root -p
```

```sql
CREATE DATABASE exam_platform DEFAULT CHARACTER SET utf8mb4;
CREATE USER 'exam_user'@'localhost' IDENTIFIED BY '你的数据库密码';
GRANT ALL PRIVILEGES ON exam_platform.* TO 'exam_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

导入初始数据库：

```bash
mysql -u exam_user -p exam_platform < backend/sql/schema.sql
```

---

## 第三步：构建并上传项目

### 3.1 在开发机上构建

```bash
# 进入项目目录
cd V1exam-platform—code

# 构建后端
cd backend
mvn clean package -DskipTests
# 产物: target/exam-platform-1.0.0.jar

# 构建管理后台前端
cd ../admin-web
npm install
npm run build
# 产物: admin-web/dist/

# 构建用户端前端
cd ../user-web
npm install
npm run build
# 产物: user-web/dist/
```

### 3.2 上传到服务器

```bash
# 在服务器创建目录
mkdir -p /data/exam-platform/{admin-web/dist,user-web/dist,uploads,logs}

# 上传后端 jar 包
scp backend/target/*.jar root@服务器IP:/data/exam-platform/exam-platform.jar

# 上传管理后台前端
scp -r admin-web/dist/* root@服务器IP:/data/exam-platform/admin-web/dist/

# 上传用户端前端
scp -r user-web/dist/* root@服务器IP:/data/exam-platform/user-web/dist/

# 上传 Nginx 配置
scp deploy/nginx-exam-platform.conf root@服务器IP:/etc/nginx/conf.d/exam-platform.conf
```

---

## 第四步：配置后端

在服务器上创建生产配置文件：

```bash
vi /data/exam-platform/application-prod.yml
```

内容如下（修改数据库密码和域名为你的实际值）：

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
    password: 你的数据库密码       # ← 改成实际密码
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
  secret: 改成你的随机密钥字符串至少40个字符  # ← 改成随机字符串
  expiration: 86400000

upload:
  path: /data/exam-platform/uploads
  access-prefix: https://你的域名/api/uploads/   # ← 改成你的域名

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

---

## 第五步：配置 Nginx

### 5.1 修改 Nginx 配置

```bash
vi /etc/nginx/conf.d/exam-platform.conf
```

把配置中所有的 `example.com` 替换成你的域名：

```bash
sed -i 's/example.com/你的域名/g' /etc/nginx/conf.d/exam-platform.conf
```

### 5.2 上传 HTTPS 证书

1. 登录腾讯云控制台 → SSL 证书
2. 找到已申请的证书，点击「下载」
3. 选择 **Nginx** 类型下载
4. 解压后得到两个文件：
   - `你的域名_bundle.crt`（证书文件）
   - `你的域名.key`（私钥文件）

上传到服务器：

```bash
# 创建证书目录
mkdir -p /etc/nginx/ssl/exam-platform

# 上传证书（在本地执行）
scp 你的域名_bundle.crt root@服务器IP:/etc/nginx/ssl/exam-platform/你的域名_bundle.crt
scp 你的域名.key root@服务器IP:/etc/nginx/ssl/exam-platform/你的域名.key
```

**注意：** 证书文件名必须与 Nginx 配置中的路径一致。配置中写的是：
```
ssl_certificate     /etc/nginx/ssl/exam-platform/example.com_bundle.crt;
ssl_certificate_key /etc/nginx/ssl/exam-platform/example.com.key;
```
执行了 `sed` 替换后会自动变成你的域名，所以确保上传的文件名匹配。

### 5.3 测试并重载 Nginx

```bash
nginx -t
nginx -s reload
```

---

## 第六步：启动后端

### 6.1 创建系统服务

```bash
vi /etc/systemd/system/exam-platform.service
```

内容：

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

### 6.2 启动

```bash
systemctl daemon-reload
systemctl enable exam-platform
systemctl start exam-platform

# 查看状态
systemctl status exam-platform

# 查看日志
tail -f /data/exam-platform/logs/exam-platform.log
```

---

## 第七步：验证

| 验证项 | 访问地址 | 预期结果 |
|--------|----------|----------|
| 用户端 | `https://你的域名/` | 显示用户端首页 |
| 管理后台 | `https://你的域名/admin/` | 显示管理后台登录页 |
| API接口 | `https://你的域名/api/public/cooperation/list` | 返回JSON数据 |
| HTTP跳转 | `http://你的域名/` | 自动跳转到HTTPS |
| 证书 | 浏览器地址栏 | 显示锁图标 |

---

## 常见问题

### Q: 访问管理后台白屏

管理后台部署在 `/admin/` 路径，确保构建时 `publicPath` 为 `/admin/`。检查 `admin-web/dist/index.html` 中的资源路径是否以 `/admin/` 开头。

### Q: 证书不生效

1. 确认证书文件名与 Nginx 配置一致
2. `nginx -t` 检查配置语法
3. 确认域名解析已生效：`ping 你的域名`
4. 腾讯云证书需要下载 **Nginx** 版本

### Q: 后端启动失败

```bash
# 查看详细日志
journalctl -u exam-platform -f

# 常见原因：
# 1. 数据库连接失败 → 检查密码、数据库名
# 2. 端口被占用 → netstat -tlnp | grep 8080
# 3. Java路径不对 → which java，修改service文件中的路径
```

### Q: 上传文件无法访问

确认 Nginx 配置中 `/uploads/` 反代到后端，且后端 `upload.path` 目录存在且有权限：

```bash
ls -la /data/exam-platform/uploads/
chown -R root:root /data/exam-platform/uploads/
```

### Q: 重启后端

```bash
systemctl restart exam-platform
```
