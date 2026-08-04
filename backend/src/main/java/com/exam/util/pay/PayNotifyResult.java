package com.exam.util.pay;

import lombok.Data;

/**
 * 支付回调解析结果
 */
@Data
public class PayNotifyResult {
    /** 验签/解析是否成功 */
    private boolean success;
    /** 业务订单号 */
    private String orderNo;
    /** 第三方交易号 */
    private String transactionId;
    /** 支付金额(分) */
    private Integer amountFen;
    /** 失败原因 */
    private String message;
}
