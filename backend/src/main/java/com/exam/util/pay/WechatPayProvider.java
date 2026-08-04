package com.exam.util.pay;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.exam.common.BusinessException;
import com.exam.config.PayConfig;
import com.exam.entity.CourseOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 微信支付 Native 扫码(API V3)
 */
@Slf4j
@Component
public class WechatPayProvider implements PaymentProvider {

    private static final String GATEWAY = "https://api.mch.weixin.qq.com";
    private static final String NATIVE_PATH = "/v3/pay/transactions/native";

    @Autowired
    private PayConfig payConfig;

    @Override
    public String getChannel() {
        return "wechat";
    }

    @Override
    public PayCreateResult createOrder(CourseOrder order, String notifyUrl) {
        PayConfig.Wechat cfg = payConfig.getWechat();
        if (!StringUtils.hasText(cfg.getMchId()) || !StringUtils.hasText(cfg.getMerchantPrivateKey())) {
            throw new BusinessException("微信支付未配置,请联系管理员");
        }
        JSONObject body = JSONUtil.createObj()
                .set("appid", cfg.getAppId())
                .set("mchid", cfg.getMchId())
                .set("description", order.getCourseName() == null ? "课程购买" : order.getCourseName())
                .set("out_trade_no", order.getOrderNo())
                .set("notify_url", notifyUrl)
                .set("amount", JSONUtil.createObj()
                        .set("total", order.getAmount())
                        .set("currency", "CNY"));
        String bodyStr = body.toString();

        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = RandomUtil.randomString(32);
        String message = "POST\n" + NATIVE_PATH + "\n" + timestamp + "\n" + nonce + "\n" + bodyStr + "\n";
        String signature = rsaSign(message, loadPrivateKey(cfg.getMerchantPrivateKey()));

        String auth = "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + cfg.getMchId() + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + cfg.getMerchantSerialNo() + "\"";

        String resp;
        try {
            resp = HttpRequest.post(GATEWAY + NATIVE_PATH)
                    .header("Authorization", auth)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "exam-platform wxpay v3")
                    .body(bodyStr)
                    .timeout(15000)
                    .execute()
                    .body();
        } catch (Exception e) {
            log.error("微信下单请求失败", e);
            throw new BusinessException("微信下单失败,请稍后重试");
        }
        JSONObject json = JSONUtil.parseObj(resp);
        String codeUrl = json.getStr("code_url");
        if (codeUrl == null) {
            log.error("微信下单失败: {}", resp);
            throw new BusinessException("微信下单失败:" + json.getStr("message", "未知错误"));
        }
        PayCreateResult result = new PayCreateResult();
        result.setOrderNo(order.getOrderNo());
        result.setChannel("wechat");
        result.setQrCode(codeUrl);
        result.setQrImage("data:image/png;base64," + qrImageBase64(codeUrl));
        return result;
    }

    /** 生成二维码 PNG 的 base64 字符串(不含 data URI 前缀) */
    private String qrImageBase64(String content) {
        try {
            BufferedImage image = cn.hutool.extra.qrcode.QrCodeUtil.generate(content, new QrConfig(280, 280));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("生成支付二维码失败:" + e.getMessage());
        }
    }

    @Override
    public PayNotifyResult verifyNotify(HttpServletRequest request) {
        PayNotifyResult result = new PayNotifyResult();
        try {
            PayConfig.Wechat cfg = payConfig.getWechat();
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String signature = request.getHeader("Wechatpay-Signature");
            String serial = request.getHeader("Wechatpay-Serial");
            String body = readBody(request);

            // 回调验签(平台公钥配置后生效)
            if (StringUtils.hasText(cfg.getPlatformPublicKey())) {
                String message = timestamp + "\n" + nonce + "\n" + body + "\n";
                if (!rsaVerify(message, loadPublicKey(cfg.getPlatformPublicKey()), signature)) {
                    result.setSuccess(false);
                    result.setMessage("微信回调验签失败(serial=" + serial + ")");
                    return result;
                }
            }
            // AES-GCM 解密 resource
            JSONObject resource = JSONUtil.parseObj(body).getJSONObject("resource");
            if (resource == null) {
                result.setSuccess(false);
                result.setMessage("微信回调缺少resource");
                return result;
            }
            String plain = aesGcmDecrypt(cfg.getApiV3Key(),
                    resource.getStr("nonce"),
                    resource.getStr("associated_data"),
                    resource.getStr("ciphertext"));
            JSONObject data = JSONUtil.parseObj(plain);
            result.setOrderNo(data.getStr("out_trade_no"));
            result.setTransactionId(data.getStr("transaction_id"));
            JSONObject amount = data.getJSONObject("amount");
            if (amount != null) {
                result.setAmountFen(amount.getInt("total"));
            }
            result.setSuccess("SUCCESS".equals(data.getStr("trade_state")));
            if (!result.isSuccess()) {
                result.setMessage("支付状态:" + data.getStr("trade_state"));
            }
            return result;
        } catch (Exception e) {
            log.error("微信回调解析失败", e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    /** SHA256withRSA 签名, Base64 输出 */
    private String rsaSign(String message, PrivateKey privateKey) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sign.sign());
        } catch (Exception e) {
            throw new BusinessException("微信签名失败:" + e.getMessage());
        }
    }

    /** 使用公钥验签 */
    private boolean rsaVerify(String message, PublicKey publicKey, String signature) {
        try {
            Signature ver = Signature.getInstance("SHA256withRSA");
            ver.initVerify(publicKey);
            ver.update(message.getBytes(StandardCharsets.UTF_8));
            return ver.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.warn("微信回调验签异常: {}", e.getMessage());
            return false;
        }
    }

    /** AES-256-GCM 解密 */
    private String aesGcmDecrypt(String key, String nonce, String aad, String ciphertext) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);
            if (aad != null && !aad.isEmpty()) {
                cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("微信回调解密失败:" + e.getMessage());
        }
    }

    /** 解析 PEM PKCS8 私钥 */
    private PrivateKey loadPrivateKey(String pem) {
        try {
            String b64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(b64)));
        } catch (Exception e) {
            throw new BusinessException("微信商户私钥解析失败:" + e.getMessage());
        }
    }

    /** 解析 PEM 平台证书公钥(兼容证书文件与纯公钥) */
    private PublicKey loadPublicKey(String pem) {
        try {
            if (pem.contains("-----BEGIN")) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                try (InputStream in = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))) {
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                    return cert.getPublicKey();
                }
            }
            String b64 = pem.replaceAll("\\s", "");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(b64)));
        } catch (Exception e) {
            throw new BusinessException("微信平台公钥解析失败:" + e.getMessage());
        }
    }

    private String readBody(HttpServletRequest request) {
        try {
            java.io.BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException("读取微信回调失败:" + e.getMessage());
        }
    }
}
