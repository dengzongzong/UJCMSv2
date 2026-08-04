# 直播课程配置指南

本文档说明如何开通直播云服务（腾讯云 / 阿里云二选一）、配置鉴权 Key、进行 OBS 推流以及实现直播结束后的回放观看。

## 目录

1. [开通直播服务](#一开通直播服务二选一)
2. [配置鉴权 Key 到 GitHub Secrets](#二配置鉴权-key-到-github-secrets推荐)
3. [OBS 推流操作](#三obs-推流操作)
4. [直播结束后回放（事后观看）](#四直播结束后回放事后观看)
5. [常见问题](#五常见问题)

---

## 一、开通直播服务（二选一）

### 腾讯云（推荐）

1. 腾讯云控制台搜索 **云直播 CSS** → 开通服务。
2. **域名管理 → 添加域名**，添加两个域名（需已备案）：
   - 推流域名：`push.zgrlosta.org.cn` → CNAME 到 `xxx.push.liveplay.myqcloud.com`
   - 播放域名：`play.zgrlosta.org.cn` → CNAME 到 `xxx.play.livecdn.liveplay.myqcloud.com`
3. 域名添加后，去域名服务商（DNS）把 CNAME 记录配上，生效约 1~2 小时。
4. **推流域名 → 推流配置 → 鉴权配置**：打开推流鉴权，记录**主 KEY**（对应 `TENCENT_PUSH_KEY`）。
5. **播放域名 → 访问控制 → 播放鉴权**：记录**主 KEY**（对应 `TENCENT_PLAY_KEY`）。
6. **播放域名务必配置 SSL 证书**：网站为 https，播放地址若不是 https 会被浏览器拦截（证书管理可免费申请）。

### 阿里云

1. 开通 **阿里云视频直播**。
2. 添加推流域名 + 播流域名，按提示 CNAME 到阿里云提供的地址。
3. **推流域名 → 访问控制 → URL鉴权**：记录**主 Key**（对应 `ALIYUN_AUTH_KEY`）。
4. 播流域名同样开启鉴权、配置 SSL 证书。

---

## 二、配置鉴权 Key 到 GitHub Secrets（推荐）

CI 部署时会把以下 Secret 注入到服务器 `/opt/exam-platform/application-prod.yml`，无需手动 SSH 修改。

GitHub 仓库 → **Settings → Secrets and variables → Actions → New repository secret**：

| Secret 名 | 填什么 |
|---|---|
| `LIVE_PROVIDER` | `tencent` 或 `aliyun` |
| `LIVE_PUSH_HOST` | `push.zgrlosta.org.cn`（可省略，有默认值） |
| `LIVE_PLAY_HOST` | `play.zgrlosta.org.cn`（可省略，有默认值） |
| `CALLBACK_SECRET` | 录制回调校验密钥（自定义一串随机字符串，与云控制台回调 URL 中的 `key` 一致） |
| `TENCENT_PUSH_KEY` | 腾讯云推流鉴权主 KEY |
| `TENCENT_PLAY_KEY` | 腾讯云播放鉴权主 KEY |
| `ALIYUN_AUTH_KEY` | 阿里云 URL 鉴权主 Key |

配置完成后，**再手动触发一次部署**（GitHub Actions → Build and Deploy → Run workflow）即生效。

> 备选方案：SSH 上服务器直接修改 `/opt/exam-platform/application-prod.yml` 的 `live` 段，然后执行 `systemctl restart exam-platform`。
> **注意**：下次 CI 部署会用模板覆盖该文件，因此长期维护建议使用 Secrets 方式。

### 服务器端手动配置示例

如果手动修改服务器上的 `application-prod.yml`，`live` 段如下：

```yaml
live:
  provider: tencent          # tencent | aliyun
  app-name: live
  push-host: push.zgrlosta.org.cn
  play-host: play.zgrlosta.org.cn
  tencent:
    push-key: "你的腾讯云推流鉴权主KEY"
    play-key: "你的腾讯云播放鉴权主KEY"
    push-valid-seconds: 86400
    play-valid-seconds: 172800
  aliyun:
    auth-key: "你的阿里云URL鉴权主Key"
    valid-seconds: 86400
```

---

## 三、OBS 推流操作

1. 管理后台「直播管理」→ 新增直播场次（填标题、所属课程、开始时间等）。
2. 列表操作列点「详情」，复制**推流地址**（`rtmp://push...`）和**流名**。
3. 打开 OBS：
   - 设置 → 推流 → 服务选择「自定义」
   - 服务器：粘贴推流地址
   - 串流密钥：填写流名
4. 管理后台点「开始直播」→ 学生端进入直播间即可观看（仅已开通该课程的学生可看）。
5. 直播结束，管理后台点「结束直播」。

---

## 四、直播结束后回放（事后观看）

回放地址支持**全自动回填**：云直播录制完成后会通过"录制回调"主动通知平台，按流名自动匹配场次并回填回放地址，学生即可直接观看，无需管理员手动操作。

### 4.1 配置录制回调（自动回填，推荐）

先在 GitHub Secrets 配置 `CALLBACK_SECRET`（一串自定义随机字符串），然后在云控制台配置回调：

- **腾讯云**：云直播控制台 → 域名管理 → **推流域名** → 事件回调设置 → 回调类型勾选「录制」，回调 URL 填：
  ```
  https://zgrlosta.org.cn/api/public/live/callback/tencent?key=你的CALLBACK_SECRET
  ```
- **阿里云**：视频直播控制台 → **推流域名** → 访问控制/事件回调 → 开启「录制」回调，地址填：
  ```
  https://zgrlosta.org.cn/api/public/live/callback/aliyun?key=你的CALLBACK_SECRET
  ```

录制完成 → 云厂商通知平台 → 自动回填回放地址 → 学生端直播间直接显示「看回放」。

> 自动回填生效时，若场次仍处于"直播中"状态也会自动置为"已结束"。

### 4.2 手动回填（备选）

录制完成后在管理后台手动操作：

1. 管理后台「直播管理」列表 → 已结束场次 → 点「回放」。
2. 粘贴录制文件的直链地址（`.m3u8` 或 `.mp4`）。
3. 学生端直播间会自动显示「直播已结束 + 看回放」，可直接观看。

### 4.3 录制文件获取（两种方式）

- **方式 A：云直播录制（配合自动回填）**
  - 腾讯云：云直播 → 功能配置 → 录制配置，开启录制并存到 COS；回调自动回填即基于此录制。
  - 阿里云：录制配置，开启录制并存到 OSS。
- **方式 B：OBS 本地录制**
  - OBS 设置 → 输出 → 录制，结束后把录制文件上传到服务器（或任意可访问的 mp4/m3u8 地址），用 4.2 手动回填。

---

## 五、常见问题

| 问题 | 处理方式 |
|---|---|
| 学生端播放黑屏 | 检查播放域名是否配置 SSL 证书（https 页面不能加载 http 流） |
| 鉴权地址过期（推流 24h / 播放 48h） | 管理端点「开始直播」会重新生成播放地址；失效时重新开始一次 |
| 推流失败 | 确认推流地址和流名复制完整、鉴权 Key 正确、域名 CNAME 已生效 |
| 未开通课程的学生看不到直播 | 属正常现象，需管理员在课程管理里为学生开通该课程 |
| 聊天不实时 | WebSocket 不可用时自动降级为 HTTP 轮询（10 秒刷新），仍可正常聊天 |

---

## 附：数据库

直播功能数据库变更已包含在 `backend/sql/upgrade_all.sql`（第 15 节：`live_room` 表 + `live_message` 表），部署脚本会自动执行，幂等可重复运行，无需手动处理。
