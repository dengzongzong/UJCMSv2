package com.exam.util.provider;

import cn.hutool.crypto.digest.DigestUtil;
import com.exam.config.LiveConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 腾讯云直播 Provider
 * 地址鉴权算法: txSecret = md5(Key + streamName + txTime)
 * - 推流: rtmp://pushHost/appName/streamName?txSecret=xx&txTime=xx
 * - 播放: https://playHost/appName/streamName.m3u8?txSecret=xx&txTime=xx
 */
@Component
@ConditionalOnProperty(name = "live.provider", havingValue = "tencent", matchIfMissing = true)
public class TencentLiveProvider implements LiveProvider {

    private final LiveConfig config;

    public TencentLiveProvider(LiveConfig config) {
        this.config = config;
    }

    @Override
    public String getProviderName() {
        return "tencent";
    }

    @Override
    public String genStreamName() {
        return "live_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(10000, 100000);
    }

    @Override
    public String buildPushUrl(String streamName) {
        long expire = System.currentTimeMillis() / 1000 + config.getTencent().getPushValidSeconds();
        String txTime = Long.toHexString(expire).toUpperCase();
        String txSecret = DigestUtil.md5Hex(config.getTencent().getPushKey() + streamName + txTime);
        return "rtmp://" + config.getPushHost() + "/" + config.getAppName() + "/" + streamName
                + "?txSecret=" + txSecret + "&txTime=" + txTime;
    }

    @Override
    public String buildPlayUrl(String streamName) {
        long expire = System.currentTimeMillis() / 1000 + config.getTencent().getPlayValidSeconds();
        String txTime = Long.toHexString(expire).toUpperCase();
        String txSecret = DigestUtil.md5Hex(config.getTencent().getPlayKey() + streamName + txTime);
        return "https://" + config.getPlayHost() + "/" + config.getAppName() + "/" + streamName + ".m3u8"
                + "?txSecret=" + txSecret + "&txTime=" + txTime;
    }
}
