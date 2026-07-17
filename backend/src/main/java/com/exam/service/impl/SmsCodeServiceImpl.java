package com.exam.service.impl;

import com.exam.service.SmsCodeService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版短信验证码实现。
 *
 * <p>仅供单实例开发和测试用。多实例部署时内存不共享,会导致验证码无法跨实例校验;
 * 届时请改用 Redis(本仓库已引入 hutool-cache,可平替为 Caffeine 或 RedisTemplate)。</p>
 */
@Service
public class SmsCodeServiceImpl implements SmsCodeService {

    /** 验证码有效时间(毫秒): 5 分钟 */
    private static final long TTL_MILLIS = 5L * 60 * 1000;
    /** 60 秒内不允许重发 */
    private static final long RESEND_INTERVAL_MILLIS = 60L * 1000;
    /** 验证码长度 */
    private static final int CODE_LEN = 6;

    private final SecureRandom random = new SecureRandom();

    /** key: phone|type, value: {code, expireAt, lastSendAt} */
    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();

    @Override
    public synchronized String generate(String phone, String type) {
        String key = phone + "|" + type;
        long now = System.currentTimeMillis();
        CodeEntry existing = store.get(key);
        if (existing != null) {
            long elapsed = now - existing.lastSendAt;
            if (elapsed < RESEND_INTERVAL_MILLIS) {
                long wait = (RESEND_INTERVAL_MILLIS - elapsed + 999) / 1000;
                throw new IllegalStateException("请 " + wait + " 秒后再试");
            }
        }
        String code = String.format("%0" + CODE_LEN + "d", random.nextInt((int) Math.pow(10, CODE_LEN)));
        store.put(key, new CodeEntry(code, now + TTL_MILLIS, now));
        return code;
    }

    @Override
    public synchronized boolean verify(String phone, String type, String code) {
        if (code == null || code.isEmpty()) return false;
        String key = phone + "|" + type;
        CodeEntry e = store.get(key);
        if (e == null) return false;
        if (System.currentTimeMillis() > e.expireAt) {
            store.remove(key);
            return false;
        }
        if (!e.code.equals(code)) return false;
        // 一次校验后失效,避免重放
        store.remove(key);
        return true;
    }

    private static class CodeEntry {
        final String code;
        final long expireAt;
        final long lastSendAt;
        CodeEntry(String code, long expireAt, long lastSendAt) {
            this.code = code;
            this.expireAt = expireAt;
            this.lastSendAt = lastSendAt;
        }
    }
}
