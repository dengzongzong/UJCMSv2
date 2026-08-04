package com.exam.util.provider;

import cn.hutool.crypto.digest.DigestUtil;
import com.exam.config.LiveConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 阿里云直播 Provider
 * 地址鉴权算法(A类鉴权): auth_key = timestamp-rand-uid-md5hash
 * md5hash = md5(鉴权Key + 访问路径(不含query) + timestamp + rand + uid + 0/1)
 * - 推流(rtmp): 路径=/appName/streamName, 末尾标识 1
 * - 播放(hls):  路径=/appName/streamName.m3u8, 末尾标识 0
 */
@Component
@ConditionalOnProperty(name = "live.provider", havingValue = "aliyun")
public class AliyunLiveProvider implements LiveProvider {

    private final LiveConfig config;

    public AliyunLiveProvider(LiveConfig config) {
        this.config = config;
    }

    @Override
    public String getProviderName() {
        return "aliyun";
    }

    @Override
    public String genStreamName() {
        return "live_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(10000, 100000);
    }

    @Override
    public String buildPushUrl(String streamName) {
        long ts = System.currentTimeMillis() / 1000;
        String rand = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
        String path = "/" + config.getAppName() + "/" + streamName;
        String md5hash = DigestUtil.md5Hex(config.getAliyun().getAuthKey() + path + ts + rand + "0" + "1");
        return "rtmp://" + config.getPushHost() + path
                + "?auth_key=" + ts + "-" + rand + "-0-" + md5hash;
    }

    @Override
    public String buildPlayUrl(String streamName) {
        long ts = System.currentTimeMillis() / 1000;
        String rand = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
        String path = "/" + config.getAppName() + "/" + streamName + ".m3u8";
        String md5hash = DigestUtil.md5Hex(config.getAliyun().getAuthKey() + path + ts + rand + "0" + "0");
        return "https://" + config.getPlayHost() + path
                + "?auth_key=" + ts + "-" + rand + "-0-" + md5hash;
    }
}
