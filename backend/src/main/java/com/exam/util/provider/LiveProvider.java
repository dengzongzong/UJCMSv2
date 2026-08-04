package com.exam.util.provider;

/**
 * 直播云 Provider 抽象接口
 * 双云适配: 腾讯云(TencentLiveProvider) / 阿里云(AliyunLiveProvider)
 */
public interface LiveProvider {

    /** 提供商名称: tencent / aliyun */
    String getProviderName();

    /** 生成唯一直播流名 */
    String genStreamName();

    /** 生成带鉴权的 RTMP 推流地址(仅管理端展示给讲师用OBS推流) */
    String buildPushUrl(String streamName);

    /** 生成带鉴权的 HLS 播放地址(学生端 hls.js 播放) */
    String buildPlayUrl(String streamName);
}
