package com.exam.util.pay;

import com.exam.entity.CourseOrder;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付通道抽象: 下单 + 回调验签解析
 */
public interface PaymentProvider {

    /** 通道标识: wechat / alipay */
    String getChannel();

    /** 统一下单, 返回支付二维码 */
    PayCreateResult createOrder(CourseOrder order, String notifyUrl);

    /** 解析并验签支付回调 */
    PayNotifyResult verifyNotify(HttpServletRequest request);
}
