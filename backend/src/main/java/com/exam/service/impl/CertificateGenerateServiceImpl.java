package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.entity.*;
import com.exam.mapper.CertificateFieldMapper;
import com.exam.mapper.CertificatePhotoMapper;
import com.exam.mapper.CertificateTemplateFieldMapper;
import com.exam.mapper.CertificateTemplateMapper;
import com.exam.service.CertificateGenerateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 证书生成核心: 把背景图 + 模板字段 + 证书记录 -> 实际图片
 */
@Service
public class CertificateGenerateServiceImpl implements CertificateGenerateService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 与 application.yml 中的 upload.path 保持一致(key 以前误写为 file.upload-path,
    // 导致 yml 中的真实路径 E:/毕业设计/UJCMS/exam-platform/backend/uploads 永远拿不到,
    // 实际运行中会回落到默认值 ./uploads/)
    @Value("${upload.path}")
    private String uploadPath;

    // 文件访问前缀(浏览器/前端拼 URL 用),与 application.yml 中的 upload.access-prefix 对齐
    @Value("${upload.access-prefix:http://localhost:8080/api/uploads/}")
    private String accessPrefix;

    @Autowired
    private CertificateTemplateFieldMapper templateFieldMapper;
    @Autowired
    private CertificateTemplateMapper templateMapper;
    @Autowired
    private CertificateFieldMapper fieldMapper;
    @Autowired
    private CertificatePhotoMapper photoMapper;
    @Autowired
    private com.exam.mapper.CertificateUrlConfigMapper urlConfigMapper;
    @Autowired
    private com.exam.mapper.CertificateUserMapper certificateUserMapper;

    @Override
    public void renderSingle(Certificate cert, CertificateTemplate template, OutputStream outputStream) throws Exception {
        try {
            BufferedImage image = renderImage(cert, template);
            writeCompressedImage(image, outputStream);
        } catch (Exception e) {
            System.err.println("[证书渲染] 失败 certId=" + (cert != null ? cert.getId() : "null") + ", templateId=" + (template != null ? template.getId() : "null") + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void writeCompressedImage(BufferedImage image, OutputStream outputStream) throws Exception {
        // JPEG 不支持 alpha 通道(TYPE_INT_ARGB),需要先转成 RGB
        BufferedImage rgbImage = image;
        if (image.getTransparency() != java.awt.Transparency.OPAQUE) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        // 确保 ImageIO 可用
        try {
            javax.imageio.ImageIO.scanForPlugins();
        } catch (Throwable ignored) { }
        java.util.Iterator<javax.imageio.ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            // 无 JPG writer,降级为 PNG
            ImageIO.write(rgbImage, "png", outputStream);
            return;
        }
        javax.imageio.ImageWriter writer = writers.next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f);
        writer.setOutput(ImageIO.createImageOutputStream(outputStream));
        writer.write(null, new javax.imageio.IIOImage(rgbImage, null, null), param);
        writer.dispose();
    }

    @Override
    public byte[] renderSingleBytes(Certificate cert, CertificateTemplate template) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        renderSingle(cert, template, baos);
        return baos.toByteArray();
    }

    @Override
    public byte[] renderSingleBytesCached(Certificate cert, CertificateTemplate template) throws Exception {
        File cacheFile = certPreviewFile(cert == null ? null : cert.getId());
        // 缓存有效性: 文件存在 且 其修改时间 >= 证书更新时间 且 >= 模板更新时间
        if (cacheFile != null && cacheFile.exists()) {
            long cacheMtime = cacheFile.lastModified();
            if (cacheMtime >= toEpochMillis(cert == null ? null : cert.getUpdateTime())
                    && cacheMtime >= toEpochMillis(template == null ? null : template.getUpdateTime())) {
                try {
                    return Files.readAllBytes(cacheFile.toPath());
                } catch (Exception e) {
                    // 读取失败则重新渲染
                }
            }
        }
        // 渲染
        byte[] bytes = renderSingleBytes(cert, template);
        // 写入缓存(失败不影响返回)
        if (cacheFile != null) {
            try {
                cacheFile.getParentFile().mkdirs();
                Files.write(cacheFile.toPath(), bytes);
            } catch (Exception ignored) { }
        }
        return bytes;
    }

    @Override
    public void prerender(Certificate cert, CertificateTemplate template) {
        if (cert == null || cert.getId() == null || template == null) return;
        try {
            renderSingleBytesCached(cert, template);
        } catch (Exception e) {
            // 预渲染失败不影响业务流程,仅记日志(首次预览时会再次尝试)
            System.err.println("[证书预渲染] 失败 certId=" + cert.getId() + ": " + e.getMessage());
        }
    }

    /** 证书预览缓存文件 */
    private File certPreviewFile(Long certId) {
        if (certId == null) return null;
        return new File(new File(uploadPath, "cert_preview_v2"), "cert_" + certId + ".png");
    }

    /** LocalDateTime -> epoch 毫秒(null 返回 0) */
    private long toEpochMillis(java.time.LocalDateTime ldt) {
        return ldt == null ? 0L : ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    public void renderBatchToZip(List<Certificate> certs, CertificateTemplate template, OutputStream outputStream) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            for (Certificate c : certs) {
                CertificateTemplate t = resolveTemplate(c, template);
                byte[] bytes = renderSingleBytes(c, t);
                String name = fileNameOf(c);
                zip.putNextEntry(new ZipEntry(name + ".jpg"));
                zip.write(bytes);
                zip.closeEntry();
            }
        }
    }

    @Override
    public void renderSinglePdf(Certificate cert, CertificateTemplate template, OutputStream outputStream) throws Exception {
        BufferedImage image = renderImage(cert, template);
        writeImageAsPdf(image, outputStream);
    }

    @Override
    public void renderBatchPdfToZip(List<Certificate> certs, CertificateTemplate template, OutputStream outputStream) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            for (Certificate c : certs) {
                CertificateTemplate t = resolveTemplate(c, template);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BufferedImage image = renderImage(c, t);
                writeImageAsPdf(image, baos);
                String name = fileNameOf(c);
                zip.putNextEntry(new ZipEntry(name + ".pdf"));
                zip.write(baos.toByteArray());
                zip.closeEntry();
            }
        }
    }

    /**
     * 根据证书绑定的 templateId 解析模板，优先使用证书自己的模板，否则使用传入的默认模板
     * 如果 defaultTemplate 为 null，则必须证书绑定了模板才能渲染
     */
    private CertificateTemplate resolveTemplate(Certificate cert, CertificateTemplate defaultTemplate) {
        if (cert.getTemplateId() != null) {
            CertificateTemplate t = templateMapper.selectById(cert.getTemplateId());
            if (t != null) {
                return t;
            }
        }
        if (defaultTemplate == null) {
            throw new BusinessException("证书[" + (cert.getCertNo() == null ? cert.getId() : cert.getCertNo()) + "]未绑定证书模板");
        }
        return defaultTemplate;
    }

    /**
     * 将 BufferedImage 写入 PDF 单页
     */
    private void writeImageAsPdf(BufferedImage image, OutputStream outputStream) throws Exception {
        // 1. PNG 字节数组(itextpdf 直接支持 PNG)
        ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", pngBaos);
        com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(pngBaos.toByteArray());
        // 2. 页面尺寸 = 背景图尺寸(单位:pt, 1pt = 1/72 inch, 1px @ 72dpi = 1pt)
        // 给一点白边
        com.itextpdf.text.Rectangle pageSize = new com.itextpdf.text.Rectangle(
                pdfImage.getScaledWidth() + 10,
                pdfImage.getScaledHeight() + 10
        );
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(pageSize, 5, 5, 5, 5);
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, outputStream);
        document.open();
        pdfImage.setAbsolutePosition(5, 5);
        document.add(pdfImage);
        document.close();
    }

    // ============= 核心渲染 =============

    private BufferedImage renderImage(Certificate cert, CertificateTemplate template) throws Exception {
        // 1. 加载背景图
        BufferedImage bg = loadBackgroundImage(template);
        Graphics2D g = bg.createGraphics();
        // 抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 2. 准备字段值
        Map<String, String> valueMap = buildValueMap(cert);

        // 3. 读取模板字段配置
        List<CertificateTemplateField> fields = templateFieldMapper.selectList(
                new LambdaQueryWrapper<CertificateTemplateField>()
                        .eq(CertificateTemplateField::getTemplateId, template.getId())
                        .orderByAsc(CertificateTemplateField::getSort));
        if (fields == null) fields = Collections.emptyList();

        // 4. 加载 photo (先按 certificateId 查,未找到则回退到 idCard 查最新照片)
        BufferedImage photoImg = null;
        CertificatePhoto photoEntity = null;
        if (cert.getId() != null) {
            photoEntity = photoMapper.selectOne(new LambdaQueryWrapper<CertificatePhoto>()
                    .eq(CertificatePhoto::getCertificateId, cert.getId())
                    .orderByDesc(CertificatePhoto::getUploadTime)
                    .last("LIMIT 1"));
        }
        // 回退: 按 idCard 查最新照片(兼容批量导入时未绑定 certificateId 的情况)
        if (photoEntity == null && StringUtils.hasText(cert.getIdCard())) {
            photoEntity = photoMapper.selectOne(new LambdaQueryWrapper<CertificatePhoto>()
                    .eq(CertificatePhoto::getIdCard, cert.getIdCard())
                    .orderByDesc(CertificatePhoto::getUploadTime)
                    .last("LIMIT 1"));
        }
        if (photoEntity != null && StringUtils.hasText(photoEntity.getUrl())) {
            try {
                photoImg = loadImage(photoEntity.getUrl());
            } catch (Exception e) {
                System.err.println("[证书渲染] 照片加载失败 certId=" + cert.getId()
                        + ", photoUrl=" + photoEntity.getUrl() + ": " + e.getMessage());
            }
        }

        // 5. 逐字段绘制
        for (CertificateTemplateField tf : fields) {
            String key = tf.getFieldKey();
            String keyLower = key.toLowerCase(); // 统一转小写用于匹配
            if ("photo".equalsIgnoreCase(key)) {
                if (photoImg != null) {
                    // 照片使用字段自身配置的位置和尺寸(与钢印完全独立)
                    int x = tf.getX() == null ? 0 : tf.getX();
                    int y = tf.getY() == null ? 0 : tf.getY();
                    int w = tf.getWidth() == null || tf.getWidth() == 0 ? 120 : tf.getWidth();
                    // 高度: 优先使用模板配置的 height,未配置时按图片原始宽高比等比缩放
                    int h = (tf.getHeight() != null && tf.getHeight() > 0)
                            ? tf.getHeight()
                            : w * photoImg.getHeight() / photoImg.getWidth();
                    g.drawImage(photoImg, x, y, w, h, null);
                }
            } else if (keyLower.startsWith("qr") || "examqr".equals(keyLower)) {
                String url = valueMap.get(keyLower);
                if (StringUtils.hasText(url)) {
                    int qrSize = tf.getWidth() == null ? 100 : tf.getWidth();
                    BufferedImage qr = generateQrCode(url, qrSize);
                    int qrH = (tf.getHeight() != null && tf.getHeight() > 0) ? tf.getHeight() : qr.getHeight();
                    int qrW = (tf.getWidth() != null && tf.getWidth() > 0) ? tf.getWidth() : qr.getWidth();
                    int qrX = tf.getX() == null ? 0 : tf.getX();
                    int qrY = tf.getY() == null ? 0 : tf.getY();
                    g.drawImage(qr, qrX, qrY, qrW, qrH, null);
                }
            } else {
                String text = valueMap.get(keyLower);
                if (text != null) {
                    drawText(g, tf, text);
                }
            }
        }
        // 6. 叠加钢印(透明图片,在所有字段绘制完成后绘制)
        drawStamp(g, template);
        g.dispose();
        return bg;
    }

    /**
     * 绘制钢印(透明背景PNG图片叠加在证书上)。
     * 支持透明度、旋转、缩放和位置控制。
     * 钢印图片必须是透明背景的PNG才能正确叠加(白色背景会遮挡证书内容)。
     */
    private void drawStamp(Graphics2D g, CertificateTemplate template) {
        if (!StringUtils.hasText(template.getStampUrl())) {
            return;
        }
        try {
            BufferedImage stampImg = loadImage(template.getStampUrl());
            if (stampImg == null) return;

            // 自动去除白色/接近白色的背景,使其透明(适用于白底印章图片)
            stampImg = removeWhiteBackground(stampImg);

            // 保存原始 Graphics2D 状态
            java.awt.geom.AffineTransform oldTransform = g.getTransform();
            java.awt.Composite oldComposite = g.getComposite();

            int x = template.getStampX() == null ? 0 : template.getStampX();
            int y = template.getStampY() == null ? 0 : template.getStampY();
            int w = template.getStampWidth() == null || template.getStampWidth() == 0
                    ? stampImg.getWidth() : template.getStampWidth();
            // 高度按比例缩放
            int h = w * stampImg.getHeight() / stampImg.getWidth();
            double rotation = template.getStampRotation() == null ? 0 : template.getStampRotation();
            float opacity = template.getStampOpacity() == null ? 0.8f : template.getStampOpacity();

            // 设置透明度
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

            // 旋转(围绕钢印中心)
            if (rotation != 0) {
                double centerX = x + w / 2.0;
                double centerY = y + h / 2.0;
                double radians = Math.toRadians(rotation);
                g.rotate(radians, centerX, centerY);
            }

            // 绘制钢印
            g.drawImage(stampImg, x, y, w, h, null);

            // 恢复原始状态
            g.setTransform(oldTransform);
            g.setComposite(oldComposite);
        } catch (Exception e) {
            // 钢印加载失败不影响证书生成
        }
    }

    /**
     * 自动去除白色背景,使其变透明。
     * 判断标准:RGB三通道值都 >= 240 视为接近白色,设为透明。
     * 如果图片本身已有alpha通道且背景透明,则不做任何处理。
     */
    private BufferedImage removeWhiteBackground(BufferedImage img) {
        if (img == null) return null;
        int w = img.getWidth();
        int h = img.getHeight();
        // 创建支持透明通道的副本
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                // 如果已经是完全透明的像素,直接跳过
                if (alpha == 0) {
                    result.setRGB(x, y, 0);
                    continue;
                }
                // 接近白色的像素(RGB都 >= 240)设为透明
                if (red >= 240 && green >= 240 && blue >= 240) {
                    result.setRGB(x, y, 0); // ARGB = 0 表示完全透明
                } else {
                    result.setRGB(x, y, pixel);
                }
            }
        }
        return result;
    }

    private static boolean fontLogged = false;

    private Font loadFontFromFile(int style, int fontSize, String text) {
        // 尝试1: 加载 OTF 字体(Java 9+ 支持,Java 8 可能失败)
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("fonts/NotoSansCJKsc-Regular.otf");
            if (is != null) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(style, fontSize);
                    if (font.canDisplayUpTo(text) == -1) {
                        return font;
                    }
                } finally {
                    is.close();
                }
            }
        } catch (Exception e) {
            // 字体加载失败,继续尝试系统字体
        }
        // 尝试2: 系统常见中文字体(按优先级)
        String[] candidateFonts = { "Noto Sans CJK SC", "Noto Sans CJK", "WenQuanYi Micro Hei",
                "WenQuanYi Zen Hei", "Source Han Sans CN", "SimSun", "Microsoft YaHei",
                "Arial Unicode MS", "SansSerif" };
        for (String fontName : candidateFonts) {
            try {
                Font sysFont = new Font(fontName, style, fontSize);
                if (sysFont.canDisplayUpTo(text) == -1) {
                    return sysFont;
                }
            } catch (Exception ignored) { }
        }
        // 兜底: 逻辑字体(Java 保证存在,但中文可能显示为方框)
        return new Font("SansSerif", style, fontSize);
    }

    private void drawText(Graphics2D g, CertificateTemplateField tf, String text) {
        if (!fontLogged) {
            fontLogged = true;
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFonts = ge.getAvailableFontFamilyNames();
            System.out.println("=== 服务器可用字体 ===");
            for (String fontName : availableFonts) {
                if (fontName.contains("Sim") || fontName.contains("Hei") || fontName.contains("YaHei") || fontName.contains("Song") || fontName.contains("Kai") || fontName.contains("CJK") || fontName.contains("WenQuanYi") || fontName.contains("Noto")) {
                    System.out.println(fontName);
                }
            }
            System.out.println("======================");
        }
        if (text != null && text.length() > 0 && text.charAt(0) > 127) {
            System.out.println("=== 渲染中文文本 ===");
            System.out.println("原始文本: " + text);
            System.out.println("文本长度: " + text.length());
            for (int i = 0; i < Math.min(text.length(), 5); i++) {
                char c = text.charAt(i);
                System.out.println("字符 " + i + ": '" + c + "' (Unicode: " + (int)c + ")");
            }
        }
        int style = (tf.getFontWeight() != null && tf.getFontWeight() == 2) ? Font.BOLD : Font.PLAIN;
        int fontSize = tf.getFontSize() == null ? 24 : tf.getFontSize();
        Font font = loadFontFromFile(style, fontSize, text);
        if (font == null) {
            String[] fontNames = {"SimSun", "Microsoft YaHei", "STSong", "STKaiti", "KaiTi", "FangSong", "SimHei", "Arial Unicode MS", "Noto Sans CJK SC", "WenQuanYi Micro Hei", "Noto Serif CJK SC"};
            for (String fontName : fontNames) {
                try {
                    Font tempFont = new Font(fontName, style, fontSize);
                    if (tempFont.canDisplayUpTo(text) == -1) {
                        font = tempFont;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (font == null) {
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFonts = ge.getAvailableFontFamilyNames();
            for (String fontName : availableFonts) {
                try {
                    Font tempFont = new Font(fontName, style, fontSize);
                    if (tempFont.canDisplayUpTo(text) == -1) {
                        font = tempFont;
                        System.out.println("使用后备字体: " + fontName);
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (font == null) {
            font = new Font("Serif", style, fontSize);
        }
        g.setFont(font);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Color color = parseColor(tf.getColor());
        if (color != null) g.setColor(color);
        int x = tf.getX() == null ? 0 : tf.getX();
        int y = (tf.getY() == null ? 0 : tf.getY()) + fontSize;
        int width = tf.getWidth() == null ? 600 : tf.getWidth();
        if (tf.getAlign() == null || tf.getAlign() == 1) {
            g.drawString(text, x, y);
        } else if (tf.getAlign() == 2) {
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, x + (width - textWidth) / 2, y);
        } else if (tf.getAlign() == 3) {
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, x + (width - textWidth), y);
        }
    }

    private BufferedImage loadBackgroundImage(CertificateTemplate template) throws Exception {
        BufferedImage img = loadImage(template.getBgImageUrl());
        if (img == null) {
            // 兜底: 800x600 白底
            img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 800, 600);
            g.setColor(Color.GRAY);
            g.drawString("请上传证书背景图", 350, 300);
            g.dispose();
        }
        return img;
    }

    private BufferedImage loadImage(String url) throws Exception {
        if (!StringUtils.hasText(url)) return null;
        BufferedImage img;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                try (java.io.InputStream in = new URL(url).openStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) baos.write(buf, 0, n);
                }
                img = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
            }
        } else {
            // 本地路径
            String localPath = url;
            if (url.startsWith(accessPrefix)) {
                localPath = uploadPath + url.substring(accessPrefix.length());
            } else if (url.startsWith("/uploads/")) {
                // 支持相对路径 /uploads/ 前缀
                localPath = uploadPath + url.substring("/uploads".length());
            } else if (url.startsWith("/static/")) {
                // 支持相对路径 /static/ 前缀(文件实际在 uploads/static/ 下)
                localPath = uploadPath + url;
            } else if (!url.startsWith("/") && !url.contains(":")) {
                // 纯文件名(无路径前缀),拼接 uploadPath
                localPath = uploadPath + "/" + url;
            }
            java.io.File imgFile = new java.io.File(localPath);
            if (!imgFile.exists()) {
                System.err.println("[证书渲染] 图片文件不存在: " + localPath + " (原始URL: " + url + ")");
                return null;
            }
            img = ImageIO.read(imgFile);
        }
        if (img == null) {
            System.err.println("[证书渲染] ImageIO.read 返回 null,可能是不支持的图片格式: " + url);
            return null;
        }
        // 确保图片支持alpha通道(透明背景)
        if (img != null && img.getTransparency() == BufferedImage.OPAQUE) {
            // 图片没有alpha通道,转换为支持透明的ARGB格式
            BufferedImage argbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = argbImg.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();
            return argbImg;
        }
        return img;
    }

    private BufferedImage generateQrCode(String content, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        QRCodeWriter qr = new QRCodeWriter();
        BitMatrix m = qr.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(m);
    }

    private Map<String, String> buildValueMap(Certificate cert) {
        Map<String, String> map = new HashMap<>();
        map.put("name", safe(cert.getName()));
        map.put("idcard", safe(cert.getIdCard()));
        // 性别:优先用数据库列的值;为空时从身份证号自动推断
        Integer gender = cert.getGender();
        if (gender == null && StringUtils.hasText(cert.getIdCard())) {
            gender = CertificateNumberServiceImpl.extractGenderFromIdCard(cert.getIdCard());
        }
        String genderStr = gender == null ? "" : (gender == 1 ? "男" : "女");
        map.put("gender", genderStr);
        map.put("sex", genderStr);
        map.put("xb", genderStr);
        map.put("profession", safe(cert.getProfession()));
        map.put("skilllevel", safe(cert.getSkillLevel()));
        if (cert.getIssueDate() != null) {
            // 颁发日期:统一输出中文格式 2026年9月13日 (不补零)
            String fullCnDate = String.format("%d年%d月%d日",
                    cert.getIssueDate().getYear(),
                    cert.getIssueDate().getMonthValue(),
                    cert.getIssueDate().getDayOfMonth());
            map.put("issuedate", fullCnDate);
            map.put("issuedatefull", fullCnDate);
            map.put("issueyear", String.valueOf(cert.getIssueDate().getYear()));
            map.put("issuemonth", String.valueOf(cert.getIssueDate().getMonthValue()));
            map.put("issueday", String.valueOf(cert.getIssueDate().getDayOfMonth()));
        }
        // 出生日期: 从身份证号提取,输出中文格式(不补零,如 1990年1月5日)
        if (StringUtils.hasText(cert.getIdCard()) && cert.getIdCard().length() >= 14) {
            try {
                int bYear = Integer.parseInt(cert.getIdCard().substring(6, 10));
                int bMonth = Integer.parseInt(cert.getIdCard().substring(10, 12));
                int bDay = Integer.parseInt(cert.getIdCard().substring(12, 14));
                map.put("birthday", String.format("%d年%d月%d日", bYear, bMonth, bDay));
            } catch (NumberFormatException ignored) { }
        }
        map.put("certno", safe(cert.getCertNo()));
        map.put("studentno", safe(cert.getStudentNo()));
        map.put("agency", safe(cert.getAgency()));
        map.put("agencyfee", cert.getAgencyFee() == null ? "" : cert.getAgencyFee().toPlainString());
        // 证书二维码1/2/3: 优先使用"URL配置"中的规则拼接(支持常量+证书属性占位符),
        // 规则为空时回退使用证书本身的 qr_url1/2/3。具体拼接在下方(需等待 extra 字段合并后)进行。
        // 考试二维码: 仅当开关开启(examQrEnabled==1)且有URL时才渲染
        if (cert.getExamQrEnabled() != null && cert.getExamQrEnabled() == 1) {
            map.put("examqr", safe(cert.getExamQrUrl()));
        } else {
            map.put("examqr", "");
        }
        // 自定义字段(统一转小写)
        Map<String, Object> extra = new HashMap<>();
        if (StringUtils.hasText(cert.getExtraJson())) {
            try {
                Map<String, Object> parsed = MAPPER.readValue(cert.getExtraJson(), new TypeReference<Map<String, Object>>() {});
                if (parsed != null) {
                    extra.putAll(parsed);
                }
            } catch (Exception ignored) { }
        }
        // 应用自定义字段默认值(如果证书中没有设置该字段的值)
        List<CertificateField> customFields = fieldMapper.selectList(
                new LambdaQueryWrapper<CertificateField>().eq(CertificateField::getIsSystem, 0));
        for (CertificateField cf : customFields) {
            if (cf.getDefaultValue() != null && !cf.getDefaultValue().isEmpty()) {
                String fieldKey = cf.getFieldKey();
                if (!extra.containsKey(fieldKey)) {
                    extra.put(fieldKey, cf.getDefaultValue());
                }
            }
        }
        // 转换为小写键 —— 使用 putIfAbsent 避免自定义字段覆盖系统字段(如 issuedate/name 等)
        extra.forEach((k, v) -> map.putIfAbsent(k.toLowerCase(), v == null ? "" : v.toString()));
        // 补充证书用户属性(手机号/专业名称): 二维码 URL 规则可能引用这些属性
        if (StringUtils.hasText(cert.getIdCard())) {
            try {
                CertificateUser cu = certificateUserMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CertificateUser>()
                                .eq(CertificateUser::getIdCard, cert.getIdCard()).last("LIMIT 1"));
                if (cu != null) {
                    if (StringUtils.hasText(cu.getPhone())) {
                        map.put("phone", cu.getPhone());
                    }
                    if (StringUtils.hasText(cu.getProfessionName())) {
                        map.putIfAbsent("professionname", cu.getProfessionName());
                    }
                }
            } catch (Exception ignore) {
                // 查询失败不影响渲染
            }
        }
        // 证书二维码1/2/3: 按"URL配置"规则拼接(支持 {idCard}/{certNo}/{name} 等占位符),
        // 规则为空时回退使用证书本身的 qr_url1/2/3。使二维码可跳转到配置的链接。
        CertificateUrlConfig urlConfig = getUrlConfig();
        map.put("qr1", resolveQrUrl(urlConfig == null ? null : urlConfig.getQr1Template(), cert, map, cert.getQrUrl1()));
        map.put("qr2", resolveQrUrl(urlConfig == null ? null : urlConfig.getQr2Template(), cert, map, cert.getQrUrl2()));
        map.put("qr3", resolveQrUrl(urlConfig == null ? null : urlConfig.getQr3Template(), cert, map, cert.getQrUrl3()));
        return map;
    }

    private CertificateUrlConfig getUrlConfig() {
        List<CertificateUrlConfig> list = urlConfigMapper.selectList(null);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按 URL 配置规则拼接二维码内容:
     * - template 非空: 将 {占位符}(大小写不敏感)替换为证书对应属性值(URL 编码),生成符合 url 查询参数格式的链接。
     * - template 为空: 回退使用证书自身的 fallbackUrl(qr_url1/2/3)。
     */
    private String resolveQrUrl(String template, Certificate cert, Map<String, String> valueMap, String fallbackUrl) {
        if (!StringUtils.hasText(template)) {
            return safe(fallbackUrl);
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{(\\w+)}").matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).toLowerCase();
            String val = valueMap.getOrDefault(key, "");
            String encoded;
            try {
                encoded = java.net.URLEncoder.encode(val, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                encoded = val;
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(encoded));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static Color parseColor(String hex) {
        if (!StringUtils.hasText(hex)) return null;
        hex = hex.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() == 6) {
            return new Color(Integer.parseInt(hex, 16));
        }
        return null;
    }

    /**
     * 文件命名: 姓名_身份证后6位
     */
    private String fileNameOf(Certificate c) {
        String name = c.getName() == null ? "未命名" : c.getName();
        String id = c.getIdCard() == null ? "" : c.getIdCard();
        if (id.length() >= 6) id = id.substring(id.length() - 6);
        // 加入证书编号,避免同一用户多张证书在 ZIP 中文件名重复导致覆盖
        String certNo = c.getCertNo() != null ? c.getCertNo() : String.valueOf(c.getId());
        return name + "_" + id + "_" + certNo;
    }
}
