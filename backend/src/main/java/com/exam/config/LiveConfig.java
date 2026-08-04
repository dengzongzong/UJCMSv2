package com.exam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 直播云配置
 * 支持腾讯云/阿里云双云切换(live.provider: tencent | aliyun)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "live")
public class LiveConfig {
    /** 云厂商: tencent | aliyun */
    private String provider = "tencent";
    /** 应用名(推拉流路径中的 AppName) */
    private String appName = "live";
    /** 推流域名 */
    private String pushHost = "push.zgrlosta.org.cn";
    /** 播放域名(建议走CDN) */
    private String playHost = "play.zgrlosta.org.cn";

    private Tencent tencent = new Tencent();
    private Aliyun aliyun = new Aliyun();

    @Data
    public static class Tencent {
        /** 推流鉴权Key(控制台获取) */
        private String pushKey = "";
        /** 播放鉴权Key(控制台获取) */
        private String playKey = "";
        /** 推流地址有效期(秒) */
        private long pushValidSeconds = 24 * 3600;
        /** 播放地址有效期(秒) */
        private long playValidSeconds = 48 * 3600;
    }

    @Data
    public static class Aliyun {
        /** 鉴权主Key(控制台-鉴权配置获取) */
        private String authKey = "";
        /** 地址有效期(秒) */
        private long validSeconds = 24 * 3600;
    }
}
