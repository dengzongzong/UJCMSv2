package com.exam.util.provider;

import com.exam.config.LiveConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 直播 Provider 工厂: 根据 live.provider 配置返回对应的实现
 */
@Component
public class LiveProviderFactory {

    private final List<LiveProvider> providers;
    private final LiveConfig config;

    public LiveProviderFactory(List<LiveProvider> providers, LiveConfig config) {
        this.providers = providers;
        this.config = config;
    }

    public LiveProvider getProvider() {
        String name = config.getProvider();
        return providers.stream()
                .filter(p -> name.equalsIgnoreCase(p.getProviderName()))
                .findFirst()
                .orElseGet(() -> providers.isEmpty() ? null : providers.get(0));
    }
}
