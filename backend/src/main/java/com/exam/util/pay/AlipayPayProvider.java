package com.exam.util.pay;

import cn.hutool.core.date.DateUtil;
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

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * 支付宝当面付(扫码, alipay.trade.precreate)
 */
@Slf4j
@Component
public class AlipayPayProvider implements PaymentProvider {

    private static final String METHOD_PRECREATE = "alipay.trade.precreate";

    @Autowired
    private PayConfig payConfig;

    @Override
    public String getChannel() {
        return "alipay";
    }

    @Override
    public PayCreateResult createOrder(CourseOrder order, String notifyUrl) {
        PayConfig.Alipay cfg = payConfig.getAlipay();
        if (!StringUtils.hasText(cfg.getAppId()) || !StringUtils.hasText(cfg.getPrivateKey())) {
            throw new BusinessException("支付宝支付未配置,请联系管理员");
        }
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", cfg.getAppId());
        params.put("method", METHOD_PRECREATE);
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", DateUtil.now());
        params.put("version", "1.0");
        params.put("notify_url", notifyUrl);
        JSONObject biz = JSONUtil.createObj()
                .set("out_trade_no", order.getOrderNo())
                .set("total_amount", String.format(Locale.ROOT, "%.2f", order.getAmount() / 100.0))
                .set("subject", order.getCourseName() == null ? "课程购买" : order.getCourseName());
        params.put("biz_content", biz.toString());
        params.put("sign", alipaySign(params, cfg.getPrivateKey()));

        String resp;
        try {
            Map<String, Object> form = new HashMap<>();
            form.putAll(params);
            resp = HttpRequest.post(cfg.getGateway())
                    .form(form)
                    .timeout(15000)
                    .execute()
                    .body();
        } catch (Exception e) {
            log.error("支付宝下单请求失败", e);
            throw new BusinessException("支付宝下单失败,请稍后重试");
        }
        JSONObject json = JSONUtil.parseObj(resp);
        JSONObject r = json.getJSONObject("alipay_trade_precreate_response");
        if (r == null || !"10000".equals(r.getStr("code"))) {
            log.error("支付宝下单失败: {}", resp);
            throw new BusinessException("支付宝下单失败:" + (r == null ? "响应异常" : r.getStr("sub_msg", r.getStr("msg", "未知错误"))));
        }
        PayCreateResult result = new PayCreateResult();
        result.setOrderNo(order.getOrderNo());
        result.setChannel("alipay");
        result.setQrCode(r.getStr("qr_code"));
        result.setQrImage("data:image/png;base64," + qrImageBase64(result.getQrCode()));
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
            PayConfig.Alipay cfg = payConfig.getAlipay();
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((k, v) -> {
                if (v != null && v.length > 0) {
                    params.put(k, v[0]);
                }
            });
            // 验签
            if (StringUtils.hasText(cfg.getAlipayPublicKey())) {
                String sign = params.get("sign");
                if (sign == null || !alipayVerify(params, sign, cfg.getAlipayPublicKey())) {
                    result.setSuccess(false);
                    result.setMessage("支付宝回调验签失败");
                    return result;
                }
            }
            String tradeStatus = params.get("trade_status");
            result.setOrderNo(params.get("out_trade_no"));
            result.setTransactionId(params.get("trade_no"));
            String amountStr = params.get("total_amount");
            if (amountStr != null) {
                try {
                    result.setAmountFen((int) Math.round(Double.parseDouble(amountStr) * 100));
                } catch (NumberFormatException ignore) {
                }
            }
            result.setSuccess("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus));
            if (!result.isSuccess()) {
                result.setMessage("支付状态:" + tradeStatus);
            }
            return result;
        } catch (Exception e) {
            log.error("支付宝回调解析失败", e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    /** 支付宝签名: 除sign外参数按键排序拼接 key=value&... 后 RSA2 签名 */
    private String alipaySign(Map<String, String> params, String privateKeyPem) {
        try {
            String content = buildSignContent(params);
            PrivateKey privateKey = loadPrivateKey(privateKeyPem);
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sign.sign());
        } catch (Exception e) {
            throw new BusinessException("支付宝签名失败:" + e.getMessage());
        }
    }

    /** 支付宝回调验签 */
    private boolean alipayVerify(Map<String, String> params, String sign, String alipayPublicKey) {
        try {
            String content = buildSignContent(params);
            PublicKey publicKey = loadPublicKey(alipayPublicKey);
            Signature ver = Signature.getInstance("SHA256withRSA");
            ver.initVerify(publicKey);
            ver.update(content.getBytes(StandardCharsets.UTF_8));
            return ver.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.warn("支付宝验签异常: {}", e.getMessage());
            return false;
        }
    }

    private String buildSignContent(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if ("sign".equals(k) || "sign_type".equals(k)) {
                continue;
            }
            String v = params.get(k);
            if (v == null || v.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(k).append("=").append(v);
        }
        return sb.toString();
    }

    private PrivateKey loadPrivateKey(String pem) {
        try {
            String b64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(b64)));
        } catch (Exception e) {
            throw new BusinessException("支付宝私钥解析失败:" + e.getMessage());
        }
    }

    private PublicKey loadPublicKey(String key) {
        try {
            String b64 = key.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(b64)));
        } catch (Exception e) {
            throw new BusinessException("支付宝公钥解析失败:" + e.getMessage());
        }
    }
}
