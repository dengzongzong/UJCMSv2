package com.exam.service.impl;

import com.exam.common.BusinessException;
import com.exam.service.CaptchaResult;
import com.exam.service.CaptchaService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码 Service(无外部依赖,纯 JDK)
 * <p>特点:</p>
 * <ul>
 *   <li>4 位字符(数字 + 大写字母), 排除易混淆的 0/O/1/I</li>
 *   <li>存到内存 ConcurrentHashMap(captchaKey -> code),5 分钟自动过期</li>
 *   <li>verify(captchaKey, code) 时一次性消费并删除</li>
 * </ul>
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LEN = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final long TTL_MILLIS = 5 * 60 * 1000L; // 5 分钟过期

    /** captchaKey -> { code, createTime } */
    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "captcha-cleaner");
        t.setDaemon(true);
        return t;
    });

    public CaptchaServiceImpl() {
        // 30 秒一次,清理过期
        cleaner.scheduleAtFixedRate(this::cleanup, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public CaptchaResult generate() {
        String code = randomCode();
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        store.put(captchaKey, new CodeEntry(code, System.currentTimeMillis()));
        String base64 = drawImage(code);
        return new CaptchaResult(captchaKey, base64, TTL_MILLIS);
    }

    @Override
    public boolean verify(String captchaKey, String code) {
        if (captchaKey == null || code == null) return false;
        CodeEntry entry = store.remove(captchaKey);  // 一次性消费
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.createTime > TTL_MILLIS) return false;
        return entry.code.equalsIgnoreCase(code.trim());
    }

    @Override
    public void assertValid(String captchaKey, String code) {
        if (!verify(captchaKey, code)) {
            throw new BusinessException("验证码错误或已过期");
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /** 画干扰线 + 字符,返回 PNG 的 base64(不含 data:image/png;base64, 前缀) */
    private String drawImage(String code) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            // 背景
            g.setColor(new Color(245, 248, 255));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(180 + random.nextInt(60), 180 + random.nextInt(60), 180 + random.nextInt(60)));
                int x1 = random.nextInt(WIDTH);
                int y1 = random.nextInt(HEIGHT);
                int x2 = random.nextInt(WIDTH);
                int y2 = random.nextInt(HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }

            // 字符
            Font font = new Font("Arial", Font.BOLD, 26);
            g.setFont(font);
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(
                        20 + random.nextInt(120),
                        20 + random.nextInt(120),
                        20 + random.nextInt(120)));
                int x = 18 + i * 24;
                int y = 28 + random.nextInt(6) - 3;
                double angle = (random.nextInt(40) - 20) * Math.PI / 180;
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }

            // 干扰点
            for (int i = 0; i < 30; i++) {
                img.setRGB(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)).getRGB());
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成验证码失败", e);
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().createTime > TTL_MILLIS);
    }

    private static class CodeEntry {
        final String code;
        final long createTime;
        CodeEntry(String code, long createTime) {
            this.code = code;
            this.createTime = createTime;
        }
    }
}
