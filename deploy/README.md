# 考试平台部署文件

本目录包含考试平台在服务器 (43.162.107.232) 上的所有部署配置文件。

## 目录结构

```
deploy/
├── deploy.sh                          # 一键部署脚本
├── nginx/
│   └── exam-platform.conf             # Nginx 配置
├── config/
│   ├── application-prod.yml           # 后端生产环境配置
│   └── maven-settings.xml            # Maven 阿里云镜像配置
└── systemd/
    └── exam-platform.service          # systemd 服务配置
```

## 文件说明

| 文件 | 服务器部署位置 | 说明 |
|---|---|---|
| `nginx/exam-platform.conf` | `/etc/nginx/conf.d/exam-platform.conf` | Nginx 反向代理 |
| `config/application-prod.yml` | `/opt/exam-platform/application-prod.yml` | 后端 Spring Boot 配置 |
| `config/maven-settings.xml` | `~/.m2/settings.xml` | Maven 阿里云镜像加速 |
| `systemd/exam-platform.service` | `/etc/systemd/system/exam-platform.service` | 后端服务管理 |
| `deploy.sh` | 任意位置执行 | 一键部署脚本 |

## 快速部署

```bash
# 1. 在本地编译三个模块
cd backend && mvn clean package -DskipTests
cd ../admin-web && npm install --legacy-peer-deps && npm run build
cd ../user-web && npm install --legacy-peer-deps && npm run build

# 2. 上传产物到服务器
scp backend/target/exam-platform-1.0.0.jar root@43.162.107.232:/opt/exam-platform/exam-platform.jar
scp -r admin-web/dist root@43.162.107.232:/opt/exam-platform/admin-web/
scp -r user-web/dist root@43.162.107.232:/opt/exam-platform/user-web/

# 3. 上传代码和部署脚本
scp -r . root@43.162.107.232:/root/exam-platform-code/

# 4. SSH 登录服务器执行部署
ssh root@43.162.107.232
cd /root/exam-platform-code
chmod +x deploy/deploy.sh
./deploy/deploy.sh
```

## 访问地址

| 入口 | 地址 |
|---|---|
| 管理后台 | http://43.162.107.232/admin/ |
| 学员端 | http://43.162.107.232/ |
| API | http://43.162.107.232/api/ |
| 默认账号 | admin / admin123 |

## 常用命令

```bash
# 后端
systemctl status exam-platform      # 查看状态
systemctl restart exam-platform     # 重启
journalctl -u exam-platform -f      # 查看日志

# Nginx
systemctl restart nginx             # 重启
nginx -t                            # 测试配置

# 数据库
mysql -u root -p"Root@123456" exam_platform  # 连接数据库
```
