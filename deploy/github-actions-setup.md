# GitHub 自动部署完整配置指南

## 整体流程

```
你改代码 → push 到 GitHub → GitHub Actions 自动构建 → SSH 推送到服务器 → 自动部署 → 网站更新
         (在这里)           (云端构建JAR+前端)        (上传到服务器)      (重启服务)
```

## 前置条件

- [x] 代码已推送到 GitHub 仓库
- [x] 服务器已安装: Java 11、MySQL、Nginx、Node.js(可选,服务器上不需要)
- [x] 服务器已配置好 SSL 证书 (gjrccp.org.cn)

---

## 第一步: 在服务器上配置 SSH 密钥

GitHub Actions 需要通过 SSH 登录你的服务器,所以要在服务器上生成密钥对。

**登录到你的服务器执行:**

```bash
# 1. 生成 SSH 密钥对(一路回车,不要设密码)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions_key

# 2. 把公钥添加到 authorized_keys
cat ~/.ssh/github_actions_key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 3. 查看私钥内容(后面要用,全部复制包括 BEGIN 和 END 行)
cat ~/.ssh/github_actions_key
```

私钥输出类似:
```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaCFCrXRp...
...
...你的私钥内容...
...
-----END OPENSSH PRIVATE KEY-----
```

**把这个私钥的完整内容复制下来,下一步要用。**

---

## 第二步: 在 GitHub 仓库配置 Secrets

打开你的 GitHub 仓库页面:

```
Settings → Secrets and variables → Actions → New repository secret
```

逐个添加以下 Secrets(共 5 个):

| Secret 名称 | 值 | 说明 |
|---|---|---|
| `SERVER_HOST` | `你的服务器IP` | 如 `123.45.67.89` |
| `SERVER_USER` | `root` | SSH 用户名 |
| `SSH_PRIVATE_KEY` | 上一步复制的私钥 | 完整的私钥内容(含 BEGIN/END 行) |
| `MYSQL_PASS` | `你的数据库密码` | MySQL exam_user 的密码 |
| `JWT_SECRET` | `随机字符串` | 至少40字符,如 `x7k9m2n5p8q3w6e1r4t0y8u2i5o7a9s3d6f1g4h7j2k3l6` |

可选 Secret:
| Secret 名称 | 值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `22` | SSH 端口,默认22,改过才需要配置 |

---

## 第三步: 把代码推送到 GitHub

```bash
# 在项目根目录执行
cd /workspace/exam-platform-patch-v53/V1exam-platform—code

# 初始化 Git 仓库(如果还没有)
git init
git add .
git commit -m "feat: 证书类型管理 + GitHub Actions 自动部署"

# 添加远程仓库(替换成你的仓库地址)
git remote add origin https://github.com/你的用户名/你的仓库名.git

# 推送到 main 分支
git branch -M main
git push -u origin main
```

**推送后,GitHub Actions 会自动触发构建和部署。**

---

## 第四步: 查看部署状态

### 在 GitHub 上查看
```
仓库页面 → Actions 标签页 → 点击正在运行的 workflow → 查看实时日志
```

### 在服务器上查看

```bash
# 查看后端服务状态
systemctl status exam-platform

# 查看后端实时日志
tail -f /data/exam-platform/logs/exam-platform.log

# 查看 GitHub Actions 部署日志
journalctl -u exam-platform --no-pager -n 50

# 查看部署产物
ls -la /data/exam-platform/
# 应该看到:
#   exam-platform.jar      (后端)
#   admin-web/dist/        (管理后台前端)
#   user-web/dist/         (用户端前端)
#   application-prod.yml   (生产配置)
#   uploads/               (上传文件)
#   logs/                  (日志)
```

---

## 后续日常使用

### 场景1: 改了代码,想自动部署
```bash
# 在 TRAE 里改完代码后
git add .
git commit -m "修改说明"
git push
# GitHub Actions 自动触发,约 5-10 分钟后网站更新
```

### 场景2: 手动触发部署
```
GitHub 仓库 → Actions → Build and Deploy → Run workflow → 选择分支 → Run
```

### 场景3: 部署失败,需要回滚
```bash
# 登录服务器,用旧 JAR 重启(部署前会自动备份)
cd /data/exam-platform
ls -lt *.jar.bak*    # 查看备份
cp exam-platform.jar.bak_20260717 exam-platform.jar
systemctl restart exam-platform
```

---

## 文件说明

| 文件 | 作用 |
|---|---|
| `.github/workflows/deploy.yml` | GitHub Actions 工作流,在云端构建并部署 |
| `deploy/remote-deploy.sh` | 服务器端部署脚本,由 GitHub Actions 通过 SSH 调用 |
| `deploy/nginx-exam-platform.conf` | Nginx 配置 |
| `.gitignore` | Git 忽略规则(node_modules/target/dist等不提交) |

---

## 常见问题

### Q: 构建失败,提示 npm install 报错?
A: workflow 中已用 `--legacy-peer-deps` 参数,如果还报错,检查 package.json 依赖版本。

### Q: SSH 连接失败?
A: 检查: 1) SERVER_HOST 和 SERVER_PORT 是否正确; 2) SSH_PRIVATE_KEY 是否完整(含 BEGIN/END 行); 3) 服务器防火墙是否放行 SSH 端口。

### Q: 后端启动失败?
A: 登录服务器执行 `journalctl -u exam-platform --no-pager -n 50` 查看错误日志,常见原因: 数据库密码不对、MySQL 未启动、端口被占用。

### Q: 只改了前端,不想等后端构建?
A: 可以在 deploy.yml 的 steps 中注释掉 "Build Backend" 步骤,只构建前端。或者在 commit message 中加 `[skip ci]` 跳过本次部署。

### Q: 服务器 Java 版本不对?
A: workflow 中用的是 JDK 11,服务器也需要 JDK 11。在服务器上执行 `java -version` 确认,需要时安装: `yum install java-11-openjdk-devel` 或 `apt install openjdk-11-jdk`。
