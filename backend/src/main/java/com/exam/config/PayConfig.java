package com.exam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付配置(微信支付V3 + 支付宝双通道)
 * 密钥通过环境变量注入(GitHub Secrets -> 部署脚本)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pay")
public class PayConfig {
    /** 支付通道: wechat | alipay | both */
    private String channel = "both";
    /** 支付结果回调前缀(公网可访问, 生产为 https://域名/api) */
    private String callbackBase = "https://zgrlosta.org.cn/api";

    private Wechat wechat = new Wechat();
    private Alipay alipay = new Alipay();

    @Data
    public static class Wechat {
        /** 商户号 */
        private String mchId = "";
        /** 公众号/小程序/APP 的 AppID */
        private String appId = "";
        /** APIv3 密钥(32位) */
        private String apiV3Key = "";
        /** 商户API证书序列号 */
        private String merchantSerialNo = "";
        /** 商户API私钥(PKCS8 PEM) */
        private String merchantPrivateKey = "";
        /** 微信支付平台证书公钥(PEM, 用于回调验签, 可留空则跳过验签) */
        private String platformPublicKey = "";
    }

    @Data
    public static class Alipay {
        /** 支付宝开放平台 AppID */
        private String appId = "";
        /** 应用私钥(PKCS8 PEM) */
        private String privateKey = "";
        /** 支付宝公钥 */
        private String alipayPublicKey = "";
        /** 网关 */
        private String gateway = "https://openapi.alipay.com/gateway.do";
    }
}
