package com.exam.util.pay;

import lombok.Data;

/**
 * 下单结果(含支付二维码)
 */
@Data
public class PayCreateResult {
    private String orderNo;
    private String channel;
    /** 二维码内容(微信code_url/支付宝qr_code) */
    private String qrCode;
    /** 二维码图片(data:image/png;base64) */
    private String qrImage;
}
