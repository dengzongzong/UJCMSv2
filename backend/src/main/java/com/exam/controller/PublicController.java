package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.BannerImage;
import com.exam.entity.AboutUs;
import com.exam.entity.Announcement;
import com.exam.entity.Exam;
import com.exam.entity.HomepageSection;
import com.exam.entity.News;
import com.exam.entity.VideoCategory;
import com.exam.service.BannerImageService;
import com.exam.service.AboutUsService;
import com.exam.service.AnnouncementManageService;
import com.exam.service.CourseService;
import com.exam.service.CertificateTypeService;
import com.exam.service.ExamService;
import com.exam.service.HomepageSectionService;
import com.exam.service.NewsManageService;
import com.exam.service.ProfessionSubjectService;
import com.exam.service.VideoCategoryService;
import com.exam.vo.CourseListItemVO;
import com.exam.vo.ExamListItemVO;
import com.exam.vo.ProfessionVO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公共接口控制器（无需鉴权）
 */
@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private ProfessionSubjectService professionSubjectService;

    @Autowired
    private AnnouncementManageService announcementManageService;

    @Autowired
    private NewsManageService newsManageService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AboutUsService aboutUsService;

    @Autowired
    private HomepageSectionService homepageSectionService;

    @Autowired
    private CertificateTypeService certificateTypeService;

    @Autowired
    private BannerImageService bannerImageService;

    @Autowired
    private VideoCategoryService videoCategoryService;

    @Autowired
    private ExamService examService;

    /**
     * 获取所有启用的专业及科目树
     */
    @GetMapping("/professions")
    public Result<List<ProfessionVO>> listProfessions() {
        return Result.success(professionSubjectService.listEnabledProfessions());
    }

    /**
     * 获取已显示的系统公告列表
     */
    @GetMapping("/announcements")
    public Result<List<Announcement>> listAnnouncements() {
        return Result.success(announcementManageService.listEnabled());
    }

    /**
     * 获取已显示的新闻列表
     */
    @GetMapping("/news")
    public Result<List<News>> listNews() {
        return Result.success(newsManageService.listEnabled());
    }

    /**
     * 获取已显示的重大活动列表(type=2)
     */
    @GetMapping("/events")
    public Result<List<News>> listEvents() {
        return Result.success(newsManageService.listEnabledByType(2));
    }

    /**
     * 获取首页内容板块(政策法规/信息公开)
     * type: 1-政策法规 2-信息公开, 不传则返回全部
     */
    @GetMapping("/homepage-sections")
    public Result<List<HomepageSection>> listHomepageSections(
            @RequestParam(required = false) Integer type) {
        return Result.success(homepageSectionService.listEnabled(type));
    }

    /**
     * 获取单条新闻详情(含content)
     */
    @GetMapping("/news/{id}")
    public Result<News> getNewsDetail(@PathVariable Long id) {
        return Result.success(newsManageService.getPublicDetail(id));
    }

    /**
     * 获取单条公告详情(含content)
     */
    @GetMapping("/announcements/{id}")
    public Result<Announcement> getAnnouncementDetail(@PathVariable Long id) {
        return Result.success(announcementManageService.getPublicDetail(id));
    }

    /**
     * 获取单条首页板块详情(含content)
     */
    @GetMapping("/homepage-sections/{id}")
    public Result<HomepageSection> getHomepageSectionDetail(@PathVariable Long id) {
        return Result.success(homepageSectionService.getPublicDetail(id));
    }

    /**
     * 首页聚合接口 - 一次请求返回首页全部数据,减少HTTP并发
     */
    @GetMapping("/homepage")
    public Result<Map<String, Object>> homepage() {
        Map<String, Object> result = new HashMap<>();
        try { result.put("news", newsManageService.listEnabled()); } catch (Exception e) { result.put("news", new ArrayList<>()); }
        try { result.put("events", newsManageService.listEnabledByType(2)); } catch (Exception e) { result.put("events", new ArrayList<>()); }
        try { result.put("announcements", announcementManageService.listEnabled()); } catch (Exception e) { result.put("announcements", new ArrayList<>()); }
        try { result.put("homepageSections", homepageSectionService.listEnabled(null)); } catch (Exception e) { result.put("homepageSections", new ArrayList<>()); }
        return Result.success(result);
    }

    /**
     * 获取首页横幅图片(轮播图下方的长条图片)
     */
    @GetMapping("/banner-images")
    public Result<List<BannerImage>> listBannerImages() {
        return Result.success(bannerImageService.listEnabled());
    }

    /**
     * 获取全部启用的证书类型(公开接口)
     */
    @GetMapping("/certificate-types")
    public Result<List<com.exam.entity.CertificateType>> listCertificateTypes() {
        return Result.success(certificateTypeService.listAll());
    }

    /**
     * 公开课程列表 - 给学员端首页用（未登录也能浏览）
     * 不返回学习进度（progress=0）
     */
    @GetMapping("/course/list")
    public Result<List<CourseListItemVO>> listCourses(
            @RequestParam(required = false) Long professionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.success(courseService.getCourseList(professionId, subjectId, categoryId, null, keyword));
    }

    /**
     * 获取全部视频分类(按sort排序) - 公开接口,课程中心按分类分组展示用
     */
    @GetMapping("/video-categories")
    public Result<List<VideoCategory>> listVideoCategories() {
        return Result.success(videoCategoryService.listAll());
    }

    /**
     * 关于我们 - 公开接口,无需登录
     */
    @GetMapping("/about")
    public Result<AboutUs> about() {
        return Result.success(aboutUsService.getAboutUs());
    }

    /**
     * 关于我们页面右下角二维码 - 公开接口,无需登录。
     *
     * <p>读取后台配置的 qrcodeLink,用 zxing 生成二维码 PNG 图片返回。
     * 若后台未配置链接,返回一张 1x1 透明 PNG(避免 &lt;img&gt; 标签报错)。</p>
     */
    @GetMapping(value = "/about/qrcode", produces = "image/png")
    public void aboutQrcode(HttpServletResponse response) throws Exception {
        response.setContentType("image/png");
        response.setCharacterEncoding("utf-8");

        AboutUs about = aboutUsService.getAboutUs();
        String link = about == null ? null : about.getQrcodeLink();

        // 未配置链接: 返回 1x1 透明占位图
        if (!StringUtils.hasText(link)) {
            BufferedImage empty = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            ImageIO.write(empty, "png", response.getOutputStream());
            return;
        }

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        QRCodeWriter qr = new QRCodeWriter();
        BitMatrix matrix = qr.encode(link.trim(), BarcodeFormat.QR_CODE, 240, 240, hints);
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
        ImageIO.write(img, "png", response.getOutputStream());
    }

    /**
     * 考试结果二维码 - 公开接口,无需登录。
     *
     * <p>根据考试记录ID生成二维码,方便用户查询成绩。</p>
     */
    @GetMapping(value = "/exam/qrcode", produces = "image/png")
    public void examQrcode(@RequestParam Long recordId, HttpServletResponse response) throws Exception {
        response.setContentType("image/png");
        response.setCharacterEncoding("utf-8");

        if (recordId == null) {
            BufferedImage empty = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            ImageIO.write(empty, "png", response.getOutputStream());
            return;
        }

        String qrContent = "exam://result/" + recordId;

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        QRCodeWriter qr = new QRCodeWriter();
        BitMatrix matrix = qr.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200, hints);
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
        ImageIO.write(img, "png", response.getOutputStream());
    }

    /**
     * 通用搜索接口 - 搜索新闻、公告、课程、考试、政策法规
     * 前端首页搜索框调用，keyword 为关键词
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> search(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            result.put("news", new ArrayList<>());
            result.put("announcements", new ArrayList<>());
            result.put("courses", new ArrayList<>());
            result.put("exams", new ArrayList<>());
            result.put("policies", new ArrayList<>());
            return Result.success(result);
        }

        // 1. 搜索新闻（标题包含关键词）
        try {
            List<News> allNews = newsManageService.listEnabled();
            List<News> newsMatches = allNews.stream()
                    .filter(n -> n.getTitle() != null && n.getTitle().contains(kw))
                    .collect(Collectors.toList());
            result.put("news", newsMatches);
        } catch (Exception e) {
            result.put("news", new ArrayList<>());
        }

        // 2. 搜索公告（标题包含关键词）
        try {
            List<Announcement> allAnno = announcementManageService.listEnabled();
            List<Announcement> annoMatches = allAnno.stream()
                    .filter(a -> a.getTitle() != null && a.getTitle().contains(kw))
                    .collect(Collectors.toList());
            result.put("announcements", annoMatches);
        } catch (Exception e) {
            result.put("announcements", new ArrayList<>());
        }

        // 3. 搜索课程（后端支持 keyword 搜索）
        try {
            List<CourseListItemVO> courses = courseService.getCourseList(null, null, null, null, kw);
            result.put("courses", courses);
        } catch (Exception e) {
            result.put("courses", new ArrayList<>());
        }

        // 4. 搜索考试（后端支持 keyword 搜索）
        try {
            List<ExamListItemVO> exams = examService.getExamList(null, null, null, kw);
            result.put("exams", exams);
        } catch (Exception e) {
            result.put("exams", new ArrayList<>());
        }

        // 5. 搜索政策法规（标题包含关键词）
        try {
            List<HomepageSection> allSections = homepageSectionService.listEnabled(1);
            List<HomepageSection> policyMatches = allSections.stream()
                    .filter(s -> s.getTitle() != null && s.getTitle().contains(kw))
                    .collect(Collectors.toList());
            result.put("policies", policyMatches);
        } catch (Exception e) {
            result.put("policies", new ArrayList<>());
        }

        return Result.success(result);
    }
}
