# 课程支付配置指南

本文档说明如何开通微信支付（Native 扫码）和支付宝（当面付扫码），实现学员在课程详情页在线购买课程、扫码支付、支付成功后自动开通课程。

## 目录

1. [功能说明](#一功能说明)
2. [微信支付开通与配置](#二微信支付开通与配置)
3. [支付宝开通与配置](#三支付宝开通与配置)
4. [配置到 GitHub Secrets（推荐）](#四配置到-github-secrets推荐)
5. [回调地址说明](#五回调地址说明)
6. [常见问题](#六常见问题)

---

## 一、功能说明

- **支付渠道**：微信支付 Native（扫码）+ 支付宝当面付（扫码），可只开通其中一个，也可双通道。
- **购买入口**：用户端课程详情页，未开通且价格大于 0 的课程显示"立即购买"。
- **支付流程**：点击购买 → 弹出二维码 → 用户扫码支付 → 支付平台异步回调本系统 → 自动开通课程并更新订单状态。
- **订单管理**：管理后台新增"订单管理"菜单，可查看订单号、学生、课程、金额、渠道、状态。
- **我的订单**：用户个人中心新增"我的订单"，可查看历史订单、继续支付未支付订单、跳转学习已购课程。

---

## 二、微信支付开通与配置

### 1. 申请商户号

1. 访问 [微信商户平台](https://pay.weixin.qq.com/) 注册成为商户（需营业执照、对公账户）。
2. 商户开通后，在 **产品中心 → 我的产品** 中开通 **Native 支付**（扫码支付）。

### 2. 获取配置参数

| 配置项 | 获取位置 | GitHub Secret |
|---|---|---|
| 商户号 `mchId` | 商户平台首页 | `WECHAT_MCH_ID` |
| AppID `appId` | 商户平台 → 产品中心 → AppID 账号管理（关联公众号/小程序/APP） | `WECHAT_APP_ID` |
| APIv3 密钥 `apiV3Key` | 商户平台 → 账户中心 → API安全 → **设置 APIv3 密钥**（32位随机串） | `WECHAT_API_V3_KEY` |
| 商户证书序列号 `merchantSerialNo` | 账户中心 → API安全 → API证书 → 查看证书序列号 | `WECHAT_MERCHANT_SERIAL_NO` |
| 商户API私钥 `merchantPrivateKey` | API证书中下载的 `apiclient_key.pem` 内容 | `WECHAT_MERCHANT_PRIVATE_KEY` |
| 平台证书公钥 `platformPublicKey` | 下载微信支付平台证书并转换为 PEM 公钥（可选，留空则回调不验签） | `WECHAT_PLATFORM_PUBLIC_KEY` |

### 3. 配置支付回调

在商户平台 **产品中心 → Native 支付 → 开发配置** 中设置 **支付回调链接**：

```
https://zgrlosta.org.cn/api/public/pay/callback/wechat
```

> 回调链接必须为 **HTTPS 公网可访问** 的地址，且不支持带参数。

---

## 三、支付宝开通与配置

### 1. 创建应用

1. 登录 [支付宝开放平台](https://open.alipay.com/) → 控制台 → 创建**网页/移动应用**（或当面付应用）。
2. 应用创建后，在 **开发设置** 中：
   - 获取 **AppID**（对应 `ALIPAY_APP_ID`）。
   - **接口加签方式**：选择**公钥模式**，生成 **应用私钥**（PKCS8 PEM，对应 `ALIPAY_PRIVATE_KEY`）并上传公钥。
   - 记录平台生成的 **支付宝公钥**（对应 `ALIPAY_PUBLIC_KEY`）。

### 2. 签约当面付

在 [支付宝商家中心](https://b.alipay.com/) 或开放平台 **产品中心** 中搜索并签约 **当面付**（`alipay.trade.precreate` 扫码支付），签约成功后即可调用。

### 3. 配置异步通知地址

在开放平台应用 **开发设置 → 开发信息 → 接口加签方式/异步通知** 中配置：

```
https://zgrlosta.org.cn/api/public/pay/callback/alipay
```

---

## 四、配置到 GitHub Secrets（推荐）

CI 部署时会把以下 Secret 注入到服务器 `/opt/exam-platform/application-prod.yml`，无需手动 SSH 修改。

GitHub 仓库 → **Settings → Secrets and variables → Actions → New repository secret**：

| Secret 名 | 填什么 |
|---|---|
| `PAY_CHANNEL` | `wechat`、`alipay` 或 `both`（推荐 `both`） |
| `WECHAT_MCH_ID` | 微信商户号 |
| `WECHAT_APP_ID` | 微信 AppID |
| `WECHAT_API_V3_KEY` | 微信 APIv3 密钥（32位） |
| `WECHAT_MERCHANT_SERIAL_NO` | 微信商户证书序列号 |
| `WECHAT_MERCHANT_PRIVATE_KEY` | 微信商户 API 私钥（`apiclient_key.pem` 全文，含 `-----BEGIN PRIVATE KEY-----`） |
| `WECHAT_PLATFORM_PUBLIC_KEY` | 微信平台证书公钥（可选） |
| `ALIPAY_APP_ID` | 支付宝应用 AppID |
| `ALIPAY_PRIVATE_KEY` | 支付宝应用私钥（PKCS8 PEM 全文） |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 |

> **注意**：私钥为 PEM 多行文本。GitHub Secret 支持换行粘贴，粘贴时原样保留 `-----BEGIN PRIVATE KEY-----` 和 `-----END PRIVATE KEY-----` 即可。

配置完成后，**再手动触发一次部署**（GitHub Actions → Build and Deploy → Run workflow）即生效。

### 服务器端手动配置示例

如果手动修改服务器上的 `/opt/exam-platform/application-prod.yml`，`pay` 段如下：

```yaml
pay:
  channel: both
  callback-base: https://zgrlosta.org.cn/api
  wechat:
    mch-id: "微信商户号"
    app-id: "微信AppID"
    api-v3-key: "32位APIv3密钥"
    merchant-serial-no: "证书序列号"
    merchant-private-key: |
      -----BEGIN PRIVATE KEY-----
      ...商户私钥内容...
      -----END PRIVATE KEY-----
    platform-public-key: ""
  alipay:
    app-id: "支付宝AppID"
    private-key: |
      -----BEGIN PRIVATE KEY-----
      ...应用私钥内容...
      -----END PRIVATE KEY-----
    alipay-public-key: "支付宝公钥"
    gateway: https://openapi.alipay.com/gateway.do
```

改完执行 `systemctl restart exam-platform`。

---

## 五、回调地址说明

| 渠道 | 回调地址 |
|---|---|
| 微信 | `https://zgrlosta.org.cn/api/public/pay/callback/wechat` |
| 支付宝 | `https://zgrlosta.org.cn/api/public/pay/callback/alipay` |

回调为公开接口（不校验登录），由支付平台主动 POST 调用。系统收到回调后：

1. 验签（微信需配置平台证书公钥，支付宝需配置支付宝公钥；未配置时跳过验签）。
2. 校验金额与订单一致。
3. 更新订单为"已支付"，写入第三方交易号。
4. 自动写入 `student_course` 开通记录（幂等，重复回调不重复开通）。

---

## 六、常见问题

### 1. 如何用支付宝沙箱测试？

1. 支付宝开放平台 → **沙箱环境** → 生成沙箱应用，获取沙箱 AppID/应用私钥/支付宝公钥。
2. 网关改为 `https://openapi.alipaydev.com/gateway.do`（本地 `application-local.yml`）。
3. 用沙箱 App 扫码支付。

### 2. 金额单位？

系统订单金额以**分**存储（`course_order.amount`），支付平台下单/回调也以分（微信）或元（支付宝回调转分）处理，前后端展示统一转元。

### 3. 支付成功但课程没开通？

- 检查回调 URL 是否公网可达（微信/支付宝控制台查看回调日志）。
- 检查服务器日志：`journalctl -u exam-platform -f`，搜索 `支付成功自动开通` 或 `支付回调`。
- 若提示"验签失败"，检查 `platformPublicKey`（微信）或 `alipayPublicKey` 是否配置正确。

### 4. 只想开一个通道？

- GitHub Secret `PAY_CHANNEL` 设为 `wechat` 或 `alipay`。
- 前端支付弹窗仍显示两个 Tab，但请求未启用的渠道会自动回退到默认通道（后端已做渠道归一化）。

### 5. 免费课程（价格 0）？

免费课程不会走支付流程，点击购买直接自动开通。
