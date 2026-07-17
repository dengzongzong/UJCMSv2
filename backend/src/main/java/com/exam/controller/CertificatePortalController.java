package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateTemplate;
import com.exam.entity.Exam;
import com.exam.entity.ExamRecord;
import com.exam.entity.Profession;
import com.exam.entity.Student;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.CertificateTemplateMapper;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.StudentMapper;
import com.exam.security.JwtUtil;
import com.exam.service.CertificateGenerateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学员侧(用户端)证书查询/下载接口。
 *
 * <p>与 admin 端区别:</p>
 * <ul>
 *   <li>公开接口(无需登录): /portal/certificate/** 已加入 WebMvcConfig 与 JwtInterceptor 白名单</li>
 *   <li>查询/下载必须同时校验 (idCard + name) 双因子,防止越权查别人证书</li>
 *   <li>返回字段经过裁剪,不暴露后台备注/二维码/报单费等敏感字段</li>
 *   <li>支持批量下载(一人多证,ZIP 打包)</li>
 * </ul>
 *
 * <p>典型使用流程:</p>
 * <pre>
 *   1. 用户进入"证书查询"页,输入 身份证号 + 姓名(必填),点查询(无需登录)
 *   2. 后端按 (idCard, name) 同时匹配,返回该人的证书列表
 *   3. 点"下载证书" -> GET /api/portal/certificate/download/{id}?idCard=...&name=...&format=image|pdf
 *   4. 批量下载 -> GET /api/portal/certificate/download/batch?idCard=...&name=...&format=image|pdf
 * </pre>
 */
@RestController
@RequestMapping("/portal/certificate")
public class CertificatePortalController {

    @Autowired
    private CertificateMapper certificateMapper;
    @Autowired
    private CertificateTemplateMapper templateMapper;
    @Autowired
    private CertificateGenerateService generateService;
    @Autowired
    private com.exam.mapper.CertificatePhotoMapper photoMapper;
    @Autowired
    private com.exam.service.CertificatePhotoService photoService;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ExamRecordMapper examRecordMapper;
    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ProfessionMapper professionMapper;

    /**
     * 已登录用户直接查询自己的证书(通过token获取studentId→idCard→查证书)
     *
     * <p>该接口位于 /portal/certificate 前缀下(已在白名单),因此 JwtInterceptor 不会强制要求登录。
     * 这里手动从请求头解析 token 获取 userId:已登录则返回本人证书,未登录返回空列表。</p>
     */
    @GetMapping("/my-certificates")
    public Result<List<Map<String, Object>>> myCertificates(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.success(new ArrayList<>());
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.success(new ArrayList<>());
        }
        // 通过 userId 查 student 表获取 idCard
        Student student = studentMapper.selectById(userId);
        if (student == null || !StringUtils.hasText(student.getIdCard())) {
            return Result.success(new ArrayList<>());
        }
        // 用 idCard 查证书(必须有 certNo 和 templateId)
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getIdCard, student.getIdCard())
                .isNotNull(Certificate::getTemplateId)
                .isNotNull(Certificate::getCertNo)
                .orderByDesc(Certificate::getIssueDate)
                .orderByDesc(Certificate::getId);
        List<Certificate> certs = certificateMapper.selectList(w);

        // 查询该用户的最高考试成绩(按专业分组)
        Map<Long, Map<String, Object>> bestExamByProfession = new LinkedHashMap<>();
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, userId)
                .in(ExamRecord::getSubmitStatus, 1, 2)
                .orderByDesc(ExamRecord::getScore);
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);

        Map<Long, Exam> examMap = new HashMap<>();
        Map<Long, String> professionNameMap = new HashMap<>();
        if (!records.isEmpty()) {
            List<Long> examIds = records.stream().map(ExamRecord::getExamId).distinct().collect(Collectors.toList());
            examMap = examMapper.selectBatchIds(examIds).stream().collect(Collectors.toMap(Exam::getId, e -> e));

            Set<Long> professionIds = examMap.values().stream()
                    .map(Exam::getProfessionId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!professionIds.isEmpty()) {
                List<Profession> professions = professionMapper.selectBatchIds(professionIds);
                for (Profession p : professions) {
                    professionNameMap.put(p.getId(), p.getName());
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (ExamRecord r : records) {
            Exam exam = examMap.get(r.getExamId());
            Long profId = exam != null ? exam.getProfessionId() : null;
            Map<String, Object> existing = bestExamByProfession.get(profId);
            BigDecimal existingScore = existing != null ? (BigDecimal) existing.get("score") : null;
            boolean shouldUpdate = false;
            if (existing == null) {
                shouldUpdate = true;
            } else if (r.getScore() != null) {
                if (existingScore == null) {
                    shouldUpdate = true;
                } else if (r.getScore().compareTo(existingScore) > 0) {
                    shouldUpdate = true;
                }
            }
            if (shouldUpdate) {
                Map<String, Object> item = new HashMap<>();
                item.put("recordId", r.getId());
                item.put("examId", r.getExamId());
                item.put("examName", exam != null ? exam.getName() : (r.getExamName() != null ? r.getExamName() : "已删除考试"));
                // 封面图：从考试实体取 coverUrl，供前端"我的考试成绩"卡片封面展示
                item.put("coverUrl", exam != null ? exam.getCoverUrl() : r.getExamCoverUrl());
                item.put("score", r.getScore());
                item.put("professionId", profId);
                item.put("professionName", profId == null ? "通用考试" : professionNameMap.getOrDefault(profId, "通用考试"));
                item.put("submitTime", r.getSubmitTime() != null ? r.getSubmitTime().format(formatter) : null);
                item.put("duration", r.getDuration());
                item.put("correctCount", r.getCorrectCount());
                item.put("wrongCount", r.getWrongCount());
                item.put("totalCount", r.getTotalCount());
                bestExamByProfession.put(profId, item);
            }
        }

        // 获取所有证书的专业名称(同时支持profession存储为ID或名称)
        Set<Long> certProfessionIds = new HashSet<>();
        Map<String, Long> professionNameToIdMap = new HashMap<>();
        for (Certificate c : certs) {
            if (c.getProfession() != null) {
                try {
                    certProfessionIds.add(Long.parseLong(c.getProfession()));
                } catch (NumberFormatException e) {
                    // profession 存储的是名称,通过 Profession 表反查ID
                    professionNameToIdMap.put(c.getProfession(), null);
                }
            }
        }
        if (!professionNameToIdMap.isEmpty()) {
            List<Profession> byName = professionMapper.selectList(
                    new LambdaQueryWrapper<Profession>().in(Profession::getName, professionNameToIdMap.keySet()));
            for (Profession p : byName) {
                certProfessionIds.add(p.getId());
                professionNameToIdMap.put(p.getName(), p.getId());
            }
        }
        Map<Long, String> certProfessionNameMap = new HashMap<>();
        if (!certProfessionIds.isEmpty()) {
            List<Profession> certProfessions = professionMapper.selectBatchIds(certProfessionIds);
            for (Profession p : certProfessions) {
                certProfessionNameMap.put(p.getId(), p.getName());
            }
        }

        // 转 VO(复用 toPortalVO 逻辑),并关联考试信息
        List<Map<String, Object>> result = new ArrayList<>();
        // 如果有考试记录但专业匹配失败，则使用第一个考试记录作为备选
        Map<String, Object> fallbackExamInfo = bestExamByProfession.isEmpty() ? null : bestExamByProfession.values().iterator().next();
        
        for (Certificate c : certs) {
            // 使用按证书记录关联的照片(严格按 certificateId 查,不回退到 idCard)
            com.exam.entity.CertificatePhoto certPhoto = photoService.getByCertificateId(c.getId(), c.getIdCard());
            String certPhotoUrl = (certPhoto != null && StringUtils.hasText(certPhoto.getUrl())) ? certPhoto.getUrl() : null;
            Map<String, Object> vo = toPortalVO(c, null);
            if (certPhotoUrl != null) {
                vo.put("photoUrl", certPhotoUrl);
            }
            // 添加专业名称
            if (c.getProfession() != null) {
                try {
                    Long profId = Long.parseLong(c.getProfession());
                    vo.put("professionName", certProfessionNameMap.getOrDefault(profId, c.getProfession()));
                } catch (NumberFormatException e) {
                    Long resolvedId = professionNameToIdMap.get(c.getProfession());
                    vo.put("professionName", c.getProfession());
                }
            } else {
                vo.put("professionName", null);
            }
            // 尝试通过专业匹配考试记录(支持profession存储为ID或名称)
            Long certProfessionId = null;
            if (c.getProfession() != null) {
                try {
                    certProfessionId = Long.parseLong(c.getProfession());
                } catch (NumberFormatException e) {
                    // profession存储的是名称,用反查到的ID
                    certProfessionId = professionNameToIdMap.get(c.getProfession());
                }
            }
            Map<String, Object> examInfo = bestExamByProfession.get(certProfessionId);
            // 如果精确匹配失败，且有考试记录，使用第一个考试记录作为备选
            if (examInfo == null && fallbackExamInfo != null) {
                examInfo = fallbackExamInfo;
            }
            if (examInfo != null) {
                vo.put("examRecordId", examInfo.get("recordId"));
                vo.put("examId", examInfo.get("examId"));
                vo.put("examName", examInfo.get("examName"));
                vo.put("examScore", examInfo.get("score"));
                vo.put("examProfessionName", examInfo.get("professionName"));
                vo.put("examSubmitTime", examInfo.get("submitTime"));
                vo.put("examDuration", examInfo.get("duration"));
                vo.put("examCorrectCount", examInfo.get("correctCount"));
                vo.put("examWrongCount", examInfo.get("wrongCount"));
                vo.put("examTotalCount", examInfo.get("totalCount"));
            }
            result.add(vo);
        }
        return Result.success(result);
    }

    /**
     * 已登录用户查询自己的考试记录(按专业分组取最高分)
     * <p>该接口位于 /portal/certificate 前缀下(已在白名单),因此 JwtInterceptor 不会强制要求登录。
     * 这里手动从请求头解析 token 获取 userId:已登录则返回本人考试记录,未登录返回空列表。</p>
     */
    @GetMapping("/my-exam-records")
    public Result<List<Map<String, Object>>> myExamRecords(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.success(new ArrayList<>());
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.success(new ArrayList<>());
        }

        // 先查询用户的证书，获取证书的专业ID列表(同时支持profession存储为ID或名称)
        Student student = studentMapper.selectById(userId);
        Set<Long> certProfessionIds = new HashSet<>();
        Set<String> certProfessionNames = new HashSet<>();
        if (student != null && StringUtils.hasText(student.getIdCard())) {
            List<Certificate> certs = certificateMapper.selectList(
                    new LambdaQueryWrapper<Certificate>()
                            .eq(Certificate::getIdCard, student.getIdCard())
                            .isNotNull(Certificate::getTemplateId)
                            .isNotNull(Certificate::getCertNo));
            for (Certificate c : certs) {
                if (c.getProfession() != null) {
                    try {
                        certProfessionIds.add(Long.parseLong(c.getProfession()));
                    } catch (NumberFormatException e) {
                        // profession存储的是名称,稍后批量反查ID
                        certProfessionNames.add(c.getProfession());
                    }
                }
            }
        }
        // 将证书中存储为名称的专业反查为ID
        if (!certProfessionNames.isEmpty()) {
            List<Profession> byName = professionMapper.selectList(
                    new LambdaQueryWrapper<Profession>().in(Profession::getName, certProfessionNames));
            for (Profession p : byName) {
                certProfessionIds.add(p.getId());
            }
        }

        // 如果没有证书专业，则返回空列表
        if (certProfessionIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 查询该用户的考试记录
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, userId)
               .in(ExamRecord::getSubmitStatus, 1, 2)
               .orderByDesc(ExamRecord::getScore);
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        // 获取考试信息和专业名称映射
        List<Long> examIds = records.stream().map(ExamRecord::getExamId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Exam> examMap = examIds.isEmpty() ? new HashMap<>() :
            examMapper.selectBatchIds(examIds).stream().collect(Collectors.toMap(Exam::getId, e -> e));

        Map<Long, String> professionNameMap = new HashMap<>();
        List<Profession> professions = professionMapper.selectBatchIds(certProfessionIds);
        for (Profession p : professions) {
            professionNameMap.put(p.getId(), p.getName());
        }

        // 按证书专业分组，取每个专业的最高考试成绩
        Map<Long, Map<String, Object>> bestByProfession = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (ExamRecord r : records) {
            Exam exam = examMap.get(r.getExamId());
            // 优先从考试实体获取professionId,删除的考试从exam_record冗余字段获取
            Long profId = exam != null ? exam.getProfessionId() : (r.getExamProfessionId() != null ? r.getExamProfessionId() : null);

            // 只处理证书专业对应的考试记录
            if (profId == null || !certProfessionIds.contains(profId)) {
                continue;
            }

            Map<String, Object> existing = bestByProfession.get(profId);
            BigDecimal existingScore = existing != null ? (BigDecimal) existing.get("score") : null;
            boolean shouldUpdate = false;
            if (existing == null) {
                shouldUpdate = true;
            } else if (r.getScore() != null) {
                if (existingScore == null) {
                    shouldUpdate = true;
                } else if (r.getScore().compareTo(existingScore) > 0) {
                    shouldUpdate = true;
                }
            }

            if (shouldUpdate) {
                Map<String, Object> item = new HashMap<>();
                item.put("recordId", r.getId());
                item.put("examId", r.getExamId());
                item.put("examName", exam != null ? exam.getName() : "");
                item.put("score", r.getScore());
                item.put("professionId", profId);
                item.put("professionName", professionNameMap.getOrDefault(profId, "通用考试"));
                item.put("submitTime", r.getSubmitTime() != null ? r.getSubmitTime().format(formatter) : null);
                item.put("duration", r.getDuration());
                item.put("correctCount", r.getCorrectCount());
                item.put("wrongCount", r.getWrongCount());
                item.put("totalCount", r.getTotalCount());
                bestByProfession.put(profId, item);
            }
        }

        return Result.success(new ArrayList<>(bestByProfession.values()));
    }

    /**
     * 学员查询证书。
     *
     * <p>查询逻辑: 按 (idCard + name) 双因子精确匹配, 防越权查别人证书。</p>
     *
     * <p>设计说明: 这里的『学员』和『证书使用者』是两个独立的概念。
     * <ul>
     *   <li>证书使用者(姓名/身份证)在『证书管理 → 新增证书』时录入, 存在 certificate 表</li>
     *   <li>学员(账号)在 user-web 用手机号注册, 存在 student 表</li>
     *   <li>一个学员登录后, 输入自己的 身份证+姓名 来定位『我作为证书使用者的证书』</li>
     * </ul>
     * </p>
     *
     * @param dto { idCard, name, certNo(可选), page(默认 1), size(默认 10) }
     * @return 分页结果,字段已裁剪
     */
    @PostMapping("/search")
    public Result<PageResult<Map<String, Object>>> search(
            @RequestBody SearchDTO dto) {
        // 公开接口(无需登录):
        // - 方式1: 凭 (idCard + name) 双因子匹配,防止越权查别人证书
        // - 方式2: 仅凭证书编号(certNo)精确匹配,方便用户单张查询下载

        if (dto == null) dto = new SearchDTO();

        boolean hasCertNo = StringUtils.hasText(dto.getCertNo());
        boolean hasIdCard = StringUtils.hasText(dto.getIdCard());
        boolean hasName = StringUtils.hasText(dto.getName());

        // 至少提供 (idCard+name) 或 certNo 中的一种
        if (!hasCertNo && !(hasIdCard && hasName)) {
            throw new BusinessException("请输入身份证号和姓名,或输入证书编号进行查询");
        }

        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .orderByDesc(Certificate::getIssueDate)
                .orderByDesc(Certificate::getId);

        w.isNotNull(Certificate::getTemplateId);
        // 证书编号在"绑定模板"时才生成,未绑定模板的证书没有证书编号,查询不到
        w.isNotNull(Certificate::getCertNo);

        if (hasCertNo) {
            // 仅凭证书编号查询(精确匹配)
            w.eq(Certificate::getCertNo, dto.getCertNo().trim());
        } else {
            // 双因子校验: idCard + name 必须同时匹配
            String idCard = dto.getIdCard().trim();
            String name = dto.getName().trim();
            // 身份证号格式校验已去除,允许任意格式
            if (name.length() < 1 || name.length() > 32) {
                throw new BusinessException("姓名长度不正确");
            }
            w.eq(Certificate::getIdCard, idCard)
             .eq(Certificate::getName, name);
            // 如果同时传了 certNo,追加精确匹配
            if (hasCertNo) {
                w.eq(Certificate::getCertNo, dto.getCertNo().trim());
            }
        }

        int pageNum = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int pageSize = dto.getSize() == null || dto.getSize() < 1 || dto.getSize() > 50
                ? 10 : dto.getSize();
        Page<Certificate> p = new Page<>(pageNum, pageSize);
        Page<Certificate> result = certificateMapper.selectPage(p, w);
        
        Map<Long, String> certPhotoUrlMap = new HashMap<>();
        List<Certificate> records = result.getRecords();
        if (!records.isEmpty()) {
            // 查询每个证书记录对应的照片(严格按 certificateId 查,不回退到 idCard)
            for (Certificate c : records) {
                com.exam.entity.CertificatePhoto photo = photoService.getByCertificateId(c.getId(), c.getIdCard());
                if (photo != null && StringUtils.hasText(photo.getUrl())) {
                    certPhotoUrlMap.put(c.getId(), photo.getUrl());
                }
            }
        }

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setTotal(result.getTotal());
        pr.setPage(pageNum);
        pr.setSize(pageSize);
        pr.setRecords(records.stream().map(c -> {
            Map<String, Object> vo = toPortalVO(c, null);
            // 严格按证书记录ID取照片URL
            if (certPhotoUrlMap.containsKey(c.getId())) {
                vo.put("photoUrl", certPhotoUrlMap.get(c.getId()));
            }
            return vo;
        }).collect(Collectors.toList()));
        return Result.success(pr);
    }

    /**
     * 学员下载单张证书。
     *
     * <p>必须带 (idCard + name) 两个查询参数,后端再次校验: 仅当该证书的 idCard/name
     * 与请求参数完全一致时才返回文件流,否则 403 拒绝(防越权)。</p>
     */
    @GetMapping("/download/{id}")
    public void download(
            HttpServletResponse response,
            @PathVariable Long id,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long templateId,
            @RequestParam(defaultValue = "image") String format) throws Exception {

        Certificate cert = certificateMapper.selectById(id);
        if (cert == null) throw new BusinessException("证书不存在");

        if (cert.getTemplateId() == null) {
            throw new BusinessException("该证书未绑定证书模板，无法下载");
        }

        // 越权校验: 如果提供了 idCard+name,则必须与证书一致
        // (用户通过身份证+姓名查询到的证书,下载时再次校验)
        // 如果未提供(仅凭证书编号查询的场景),则跳过校验
        if (StringUtils.hasText(idCard) && StringUtils.hasText(name)) {
            if (!idCard.trim().equalsIgnoreCase(cert.getIdCard())
                    || !name.trim().equals(cert.getName())) {
                throw new BusinessException(403, "该证书不属于当前账号,无法下载");
            }
        }

        // 只使用证书绑定的模板，不再自动应用默认模板
        CertificateTemplate template = templateMapper.selectById(cert.getTemplateId());
        if (template == null) {
            throw new BusinessException("证书绑定的模板不存在");
        }

        boolean pdf = "pdf".equalsIgnoreCase(format);
        String fileName = buildFileName(cert, pdf);
        prepareDownload(response, fileName, pdf ? "application/pdf" : "image/jpeg");
        try (OutputStream os = response.getOutputStream()) {
            if (pdf) {
                generateService.renderSinglePdf(cert, template, os);
            } else {
                os.write(generateService.renderSingleBytes(cert, template));
            }
        }
    }

    /**
     * 学员批量下载证书(ZIP 打包) - 一个人可有多张证书,一次性打包下载。
     *
     * <p>公开接口(无需登录): 仍以 (idCard + name) 双因子匹配该人全部证书,防越权。
     * 内部复用 generateService.renderBatchToZip / renderBatchPdfToZip,每张证书
     * 优先使用自己绑定的 templateId(与单张下载行为一致)。</p>
     *
     * @param idCard 身份证号
     * @param name   姓名
     * @param format image(png) | pdf
     */
    @GetMapping("/download/batch")
    public void downloadBatch(
            HttpServletResponse response,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String certNo,
            @RequestParam(defaultValue = "image") String format) throws Exception {

        // 支持两种查询方式:
        // 1) (idCard + name) 双因子 -> 下载该人全部证书
        // 2) 仅 certNo -> 下载该编号对应的单张证书
        boolean hasCertNo = StringUtils.hasText(certNo);
        boolean hasIdCard = StringUtils.hasText(idCard);
        boolean hasName = StringUtils.hasText(name);

        if (!hasCertNo && !(hasIdCard && hasName)) {
            throw new BusinessException("请提供身份证号和姓名,或提供证书编号");
        }

        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .orderByDesc(Certificate::getIssueDate)
                .orderByDesc(Certificate::getId);
        if (hasCertNo) {
            w.eq(Certificate::getCertNo, certNo.trim());
        } else {
            String idCardTrim = idCard.trim();
            // 身份证号格式校验已去除,允许任意格式
            w.eq(Certificate::getIdCard, idCardTrim)
             .eq(Certificate::getName, name.trim());
        }
        w.isNotNull(Certificate::getTemplateId);

        List<Certificate> certs = certificateMapper.selectList(w);
        if (certs == null || certs.isEmpty()) {
            throw new BusinessException("未找到匹配的证书");
        }

        boolean pdf = "pdf".equalsIgnoreCase(format);
        // 仅一张证书时直接返回单张文件(不打包 ZIP),避免"下载全部"只有一个文件的问题
        if (certs.size() == 1) {
            Certificate c = certs.get(0);
            CertificateTemplate template = templateMapper.selectById(c.getTemplateId());
            String singleName = buildCertFileName(c, pdf);
            if (pdf) {
                prepareDownload(response, singleName, "application/pdf");
            } else {
                prepareDownload(response, singleName, "image/jpeg");
            }
            try (OutputStream os = response.getOutputStream()) {
                if (pdf) {
                    generateService.renderSinglePdf(c, template, os);
                } else {
                    os.write(generateService.renderSingleBytes(c, template));
                }
            }
            return;
        }

        String fileName = "certificates_" + (pdf ? "pdf_" : "img_") + System.currentTimeMillis() + ".zip";
        prepareDownload(response, fileName, "application/zip");
        try (OutputStream os = response.getOutputStream()) {
            if (pdf) {
                generateService.renderBatchPdfToZip(certs, null, os);
            } else {
                generateService.renderBatchToZip(certs, null, os);
            }
        }
    }

    /**
     * 学员选择部分证书下载(ZIP 打包) - 一个人可有多张证书,用户可勾选其中几张打包下载。
     *
     * <p>公开接口(无需登录): 仍以 (idCard + name) 双因子校验,确保只能下载自己的证书。
     * 请求体中 ids 列表里的每条证书都会验证其 (idCard, name) 与请求参数一致。</p>
     *
     * @param body { idCard, name, ids, format }
     */
    @PostMapping("/download/selected")
    public void downloadSelected(
            @RequestBody SelectedDownloadDTO body,
            HttpServletResponse response) throws Exception {

        if (body == null || body.getIds() == null || body.getIds().isEmpty()) {
            throw new BusinessException("请选择要下载的证书");
        }

        // 按选中的 ID 查询证书记录
        List<Certificate> certs = certificateMapper.selectBatchIds(body.getIds());
        if (certs == null || certs.isEmpty()) {
            throw new BusinessException("未找到选中的证书");
        }

        // 越权校验: 如果提供了 idCard+name,则每条证书必须匹配
        // (未提供时跳过,适用于仅凭证书编号查询后勾选下载的场景)
        boolean hasIdCard = StringUtils.hasText(body.getIdCard());
        boolean hasName = StringUtils.hasText(body.getName());
        if (hasIdCard && hasName) {
            String idCardTrim = body.getIdCard().trim();
            String nameTrim = body.getName().trim();
            for (Certificate c : certs) {
                if (!idCardTrim.equalsIgnoreCase(c.getIdCard())
                        || !nameTrim.equals(c.getName())) {
                    throw new BusinessException(403, "证书[" + (c.getCertNo() == null ? c.getId() : c.getCertNo())
                            + "]不属于该用户,无法下载");
                }
            }
        }

        // 过滤掉未绑定模板的证书
        certs = certs.stream().filter(c -> c.getTemplateId() != null).collect(Collectors.toList());
        if (certs.isEmpty()) {
            throw new BusinessException("选中的证书未绑定模板，无法下载");
        }

        boolean pdf = "pdf".equalsIgnoreCase(body.getFormat());
        // 仅一张证书时直接返回单张文件(不打包 ZIP)
        if (certs.size() == 1) {
            Certificate c = certs.get(0);
            CertificateTemplate template = templateMapper.selectById(c.getTemplateId());
            String singleName = buildCertFileName(c, pdf);
            if (pdf) {
                prepareDownload(response, singleName, "application/pdf");
            } else {
                prepareDownload(response, singleName, "image/jpeg");
            }
            try (OutputStream os = response.getOutputStream()) {
                if (pdf) {
                    generateService.renderSinglePdf(c, template, os);
                } else {
                    os.write(generateService.renderSingleBytes(c, template));
                }
            }
            return;
        }

        String fileName = "certificates_selected_" + (pdf ? "pdf_" : "img_") + System.currentTimeMillis() + ".zip";
        prepareDownload(response, fileName, "application/zip");
        try (OutputStream os = response.getOutputStream()) {
            if (pdf) {
                generateService.renderBatchPdfToZip(certs, null, os);
            } else {
                generateService.renderBatchToZip(certs, null, os);
            }
        }
    }

    /** 解析证书使用的模板: 优先 cert.templateId, 否则用 defaultTemplate */
    private CertificateTemplate resolveTemplateForCert(Certificate cert, CertificateTemplate defaultTemplate) {
        if (cert.getTemplateId() != null) {
            CertificateTemplate t = templateMapper.selectById(cert.getTemplateId());
            if (t != null) return t;
        }
        return defaultTemplate;
    }

    /** 构建单张证书下载文件名 */
    private String buildCertFileName(Certificate c, boolean pdf) {
        String base = c.getName() != null ? c.getName() : "certificate";
        String certNo = c.getCertNo() != null ? c.getCertNo() : String.valueOf(c.getId());
        String safe = (base + "_" + certNo).replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "");
        return safe + (pdf ? ".pdf" : ".png");
    }

    /** 从 Authorization header 中提取 Bearer token(也支持 query string 中的 token) */
    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        // 也支持 query string 中的 token(给 <img src> 这类不能带 header 的场景用)
        return request.getParameter("token");
    }

    private Map<String, Object> toPortalVO(Certificate c, Map<String, String> photoUrlMap) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("certNo", c.getCertNo());
        m.put("studentNo", c.getStudentNo());
        m.put("name", c.getName());
        m.put("idCard", c.getIdCard());
        m.put("idCardRaw", c.getIdCard());
        m.put("gender", c.getGender());
        m.put("profession", c.getProfession());
        m.put("skillLevel", c.getSkillLevel());
        m.put("issueDate", c.getIssueDate() == null ? null : c.getIssueDate().toString());
        m.put("issueDateStr", c.getIssueDate() == null ? null : c.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        m.put("agency", c.getAgency());
        // photoUrl 不在此设置,由调用方按 certificateId 查询后单独 put
        m.put("theoryScore", c.getTheoryScore());
        m.put("practicalScore", c.getPracticalScore());
        m.put("comprehensiveEvaluation", c.getComprehensiveEvaluation());
        if (StringUtils.hasText(c.getExtraJson())) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> extra = mapper.readValue(c.getExtraJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (extra != null) {
                    if (m.get("theoryScore") == null) m.put("theoryScore", extra.get("theoryScore"));
                    if (m.get("practicalScore") == null) m.put("practicalScore", extra.get("practicalScore"));
                    if (m.get("comprehensiveEvaluation") == null) m.put("comprehensiveEvaluation", extra.get("comprehensiveEvaluation"));
                }
            } catch (Exception ignored) {
            }
        }
        return m;
    }

    /** 身份证号脱敏: 保留前 4 后 4 */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String buildFileName(Certificate c, boolean pdf) {
        String name = c.getName() == null ? "未命名" : c.getName();
        String id = c.getIdCard() == null ? "" : c.getIdCard();
        if (id.length() >= 6) id = id.substring(id.length() - 6);
        return name + "_" + id + (pdf ? ".pdf" : ".jpg");
    }

    private void prepareDownload(HttpServletResponse response, String fileName, String contentType) {
        response.setContentType(contentType);
        response.setCharacterEncoding("utf-8");
        // 禁止浏览器/CDN缓存,确保模板更新后预览能看到最新效果
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        try {
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded);
        } catch (java.io.UnsupportedEncodingException ignore) {
            response.setHeader("Content-disposition", "attachment;filename=\"" + fileName + "\"");
        }
    }

    /** 选择下载入参 */
    public static class SelectedDownloadDTO {
        private String idCard;
        private String name;
        private List<Long> ids;
        private String format;

        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Long> getIds() { return ids; }
        public void setIds(List<Long> ids) { this.ids = ids; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }

    /** 查询入参 */
    public static class SearchDTO {
        private String idCard;
        private String name;
        private String certNo;
        private Integer page;
        private Integer size;

        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCertNo() { return certNo; }
        public void setCertNo(String certNo) { this.certNo = certNo; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }
}
