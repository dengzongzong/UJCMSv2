package com.exam.util.pay;

import com.exam.config.PayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付通道工厂
 */
@Component
public class PayProviderFactory {

    @Autowired
    private List<PaymentProvider> providers;

    @Autowired
    private PayConfig payConfig;

    /** 按通道获取实现(未传/传both时用默认通道) */
    public PaymentProvider getProvider(String channel) {
        String ch = channel;
        if (ch == null || ch.isEmpty() || "both".equalsIgnoreCase(ch)) {
            ch = payConfig.getChannel();
        }
        if ("both".equalsIgnoreCase(ch)) {
            ch = "wechat";
        }
        for (PaymentProvider p : providers) {
            if (p.getChannel().equalsIgnoreCase(ch)) {
                return p;
            }
        }
        return providers.isEmpty() ? null : providers.get(0);
    }

    /** 通道是否可用 */
    public boolean enabled(String channel) {
        String ch = payConfig.getChannel();
        if ("both".equalsIgnoreCase(ch)) {
            return true;
        }
        return ch.equalsIgnoreCase(channel);
    }

    public List<PaymentProvider> all() {
        return providers;
    }
}
