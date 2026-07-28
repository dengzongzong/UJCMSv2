package com.exam.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.CertificateDTO;
import com.exam.dto.CertificateImportResult;
import com.exam.dto.CertificateImportRow;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateField;
import com.exam.entity.CertificatePhoto;
import com.exam.entity.CertificateTemplate;
import com.exam.entity.CertificateUrlConfig;
import com.exam.entity.CertificateUser;
import com.exam.entity.Profession;
import com.exam.entity.Student;
import com.exam.entity.StudentProfession;
import com.exam.mapper.CertificateExportColumnMapper;
import com.exam.mapper.CertificateFieldMapper;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.CertificatePhotoMapper;
import com.exam.mapper.CertificateTemplateMapper;
import com.exam.mapper.CertificateUrlConfigMapper;
import com.exam.mapper.CertificateUserMapper;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.StudentProfessionMapper;
import com.exam.service.CertificateNumberService;
import com.exam.service.CertificateService;
import com.exam.vo.CertificateVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CertificateServiceImpl extends ServiceImpl<CertificateMapper, Certificate>
        implements CertificateService {

    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日"),
            DateTimeFormatter.ofPattern("yyyy年M月d日"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd号"),
            DateTimeFormatter.ofPattern("yyyy年M月d号"),
            DateTimeFormatter.ofPattern("yyyy年MM月d日"),
            DateTimeFormatter.ofPattern("yyyy年M月dd日"),
            DateTimeFormatter.ofPattern("yyyy年MM月d号"),
            DateTimeFormatter.ofPattern("yyyy年M月dd号")
    };
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private CertificateNumberService numberService;
    @Autowired
    private CertificateTemplateMapper templateMapper;
    @Autowired
    private CertificateFieldMapper fieldMapper;
    @Autowired
    private CertificateExportColumnMapper exportColumnMapper;
    @Autowired
    private CertificateFieldMapper certificateFieldMapper;
    @Autowired
    private CertificatePhotoMapper photoMapper;
    @Autowired
    private ProfessionMapper professionMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private StudentProfessionMapper studentProfessionMapper;
    @Autowired
    private CertificateUrlConfigMapper certificateUrlConfigMapper;
    @Autowired
    private CertificateUserMapper certificateUserMapper;
    @Autowired
    private com.exam.service.AsyncTaskService taskService;
    @Autowired
    private com.exam.service.CertificateUserSyncService certificateUserSyncService;
    @Autowired
    private com.exam.service.CertificateTypeService certificateTypeService;

    @Override
    public PageResult<Certificate> page(Integer page, Integer size, String name,
                                        String idCard, String agency,
                                        String issueDateStart, String issueDateEnd) {
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .like(StringUtils.hasText(name), Certificate::getName, name)
                .like(StringUtils.hasText(idCard), Certificate::getIdCard, idCard)
                .like(StringUtils.hasText(agency), Certificate::getAgency, agency)
                .orderByDesc(Certificate::getCreateTime)
                .orderByDesc(Certificate::getId);
        if (StringUtils.hasText(issueDateStart)) {
            w.ge(Certificate::getIssueDate, parseDate(issueDateStart));
        }
        if (StringUtils.hasText(issueDateEnd)) {
            w.le(Certificate::getIssueDate, parseDate(issueDateEnd));
        }
        Page<Certificate> p = new Page<>(page, size);
        return new PageResult<>(this.page(p, w));
    }

    /**
     * 查询证书记录(带 templateName,JOIN certificate_template 拿模板名)
     * <p>替代 page() 给 List 页用: 多返一个 templateName 字段,用于"模板"列展示</p>
     */
    @Override
    public PageResult<Map<String, Object>> pageWithTemplateName(Integer page, Integer size,
                                                                String name, String idCard,
                                                                String agency,
                                                                String issueDateStart,
                                                                String issueDateEnd) {
        // 兼容旧调用: certType 传 null, 走新的 SQL 级过滤方法
        return pageWithTemplateName(page, size, name, idCard, agency, issueDateStart, issueDateEnd, null);
    }

    /**
     * 查询证书记录(带 templateName,JOIN certificate_template 拿模板名, 支持按 certType SQL 级过滤)
     * <p>certType 非空时, 在 SQL 中追加 WHERE cert_type = ? 条件, 避免查 1 万条再内存过滤。</p>
     */
    @Override
    public PageResult<Map<String, Object>> pageWithTemplateName(Integer page, Integer size,
                                                                String name, String idCard,
                                                                String agency,
                                                                String issueDateStart,
                                                                String issueDateEnd,
                                                                String certType) {
        // 先调基础查询拿 cert 记录(certType 已下推到 SQL)
        PageResult<Certificate> base = this.pageBase(page, size, name, idCard, agency,
                certType, issueDateStart, issueDateEnd);
        List<Certificate> records = base.getRecords();
        if (records == null || records.isEmpty()) {
            return new PageResult<>(base.getTotal(), base.getPage(), base.getSize(),
                    new ArrayList<Map<String, Object>>());
        }
        // 收集所有 templateId, 一次查模板名
        Set<Long> templateIds = new HashSet<>();
        for (Certificate c : records) {
            if (c.getTemplateId() != null) templateIds.add(c.getTemplateId());
        }
        Map<Long, String> templateNameMap = new HashMap<>();
        if (!templateIds.isEmpty()) {
            List<CertificateTemplate> tplList = templateMapper.selectBatchIds(templateIds);
            for (CertificateTemplate t : tplList) {
                templateNameMap.put(t.getId(), t.getName());
            }
        }
        // 查询照片: 先按 certificateId 匹配,未命中则回退到 idCard 匹配
        Set<Long> certIds = records.stream()
                .map(Certificate::getId)
                .collect(Collectors.toSet());
        Map<Long, String> photoUrlByCertId = new HashMap<>();
        if (!certIds.isEmpty()) {
            List<CertificatePhoto> photosByCert = photoMapper.selectList(
                    new LambdaQueryWrapper<CertificatePhoto>()
                            .in(CertificatePhoto::getCertificateId, certIds));
            for (CertificatePhoto photo : photosByCert) {
                if (photo.getCertificateId() != null) {
                    photoUrlByCertId.put(photo.getCertificateId(), photo.getUrl());
                }
            }
        }
        // 回退: 按 idCard 查照片,为 certificateId 未命中的证书补充照片
        Set<String> idCardsNeedingFallback = records.stream()
                .filter(c -> !photoUrlByCertId.containsKey(c.getId()))
                .map(Certificate::getIdCard)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        Map<String, String> photoUrlByIdCard = new HashMap<>();
        if (!idCardsNeedingFallback.isEmpty()) {
            List<CertificatePhoto> photosByIdCard = photoMapper.selectList(
                    new LambdaQueryWrapper<CertificatePhoto>()
                            .in(CertificatePhoto::getIdCard, idCardsNeedingFallback)
                            .orderByDesc(CertificatePhoto::getUploadTime));
            for (CertificatePhoto photo : photosByIdCard) {
                // 取最新的一张(已按 upload_time DESC 排序)
                photoUrlByIdCard.putIfAbsent(photo.getIdCard(), photo.getUrl());
            }
        }
        // 转 Map
        List<Map<String, Object>> out = new ArrayList<>(records.size());
        for (Certificate c : records) {
            Map<String, Object> m = beanToMap(c);
            m.remove("extraJson");
            Long tplId = c.getTemplateId();
            m.put("templateName", tplId != null ? templateNameMap.get(tplId) : null);
            // 照片URL: 优先按 certificateId 取,未命中则回退到 idCard
            String photoUrl = photoUrlByCertId.get(c.getId());
            if (photoUrl == null && c.getIdCard() != null) {
                photoUrl = photoUrlByIdCard.get(c.getIdCard());
            }
            m.put("photoUrl", photoUrl);
            parseExtraJson(c.getExtraJson(), m);
            out.add(m);
        }
        return new PageResult<>(base.getTotal(), base.getPage(), base.getSize(), out);
    }

    /**
     * 新增:支持按专业(profession) 模糊过滤
     * 行为与 pageWithTemplateName 一致,只是多一个 profession 过滤参数
     */
    @Override
    public PageResult<Map<String, Object>> pageWithTemplateNameAndProfession(Integer page, Integer size,
                                                                            String name, String idCard,
                                                                            String agency,
                                                                            String profession,
                                                                            String issueDateStart,
                                                                            String issueDateEnd) {
        // 兼容旧调用: certType 传 null, 走新的 SQL 级过滤方法
        return pageWithTemplateNameAndProfession(page, size, name, idCard, agency, profession,
                issueDateStart, issueDateEnd, null);
    }

    /**
     * 支持按专业(profession) 模糊过滤 + 按 certType SQL 级过滤
     * <p>certType 非空时, 在 SQL 中追加 WHERE cert_type = ? 条件, 避免查 1 万条再内存过滤。</p>
     */
    @Override
    public PageResult<Map<String, Object>> pageWithTemplateNameAndProfession(Integer page, Integer size,
                                                                            String name, String idCard,
                                                                            String agency,
                                                                            String profession,
                                                                            String issueDateStart,
                                                                            String issueDateEnd,
                                                                            String certType) {
        PageResult<Certificate> base = this.pageWithProfessionAndCertType(page, size, name, idCard, agency, profession, certType,
                issueDateStart, issueDateEnd);
        List<Certificate> records = base.getRecords();
        if (records == null || records.isEmpty()) {
            return new PageResult<>(base.getTotal(), base.getPage(), base.getSize(),
                    new ArrayList<Map<String, Object>>());
        }
        Set<Long> templateIds = new HashSet<>();
        for (Certificate c : records) {
            if (c.getTemplateId() != null) templateIds.add(c.getTemplateId());
        }
        Map<Long, String> templateNameMap = new HashMap<>();
        if (!templateIds.isEmpty()) {
            List<CertificateTemplate> tplList = templateMapper.selectBatchIds(templateIds);
            for (CertificateTemplate t : tplList) {
                templateNameMap.put(t.getId(), t.getName());
            }
        }
        // 查询照片: 先按 certificateId 匹配,未命中则回退到 idCard 匹配
        Set<Long> certIds2 = records.stream()
                .map(Certificate::getId)
                .collect(Collectors.toSet());
        Map<Long, String> photoUrlByCertId2 = new HashMap<>();
        if (!certIds2.isEmpty()) {
            List<CertificatePhoto> photosByCert = photoMapper.selectList(
                    new LambdaQueryWrapper<CertificatePhoto>()
                            .in(CertificatePhoto::getCertificateId, certIds2));
            for (CertificatePhoto photo : photosByCert) {
                if (photo.getCertificateId() != null) {
                    photoUrlByCertId2.put(photo.getCertificateId(), photo.getUrl());
                }
            }
        }
        // 回退: 按 idCard 查照片
        Set<String> idCardsNeedingFallback2 = records.stream()
                .filter(c -> !photoUrlByCertId2.containsKey(c.getId()))
                .map(Certificate::getIdCard)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        Map<String, String> photoUrlByIdCard2 = new HashMap<>();
        if (!idCardsNeedingFallback2.isEmpty()) {
            List<CertificatePhoto> photosByIdCard = photoMapper.selectList(
                    new LambdaQueryWrapper<CertificatePhoto>()
                            .in(CertificatePhoto::getIdCard, idCardsNeedingFallback2)
                            .orderByDesc(CertificatePhoto::getUploadTime));
            for (CertificatePhoto photo : photosByIdCard) {
                photoUrlByIdCard2.putIfAbsent(photo.getIdCard(), photo.getUrl());
            }
        }
        Set<Long> professionIds = new HashSet<>();
        for (Certificate c : records) {
            if (c.getProfession() != null) {
                try {
                    professionIds.add(Long.parseLong(c.getProfession()));
                } catch (NumberFormatException e) {
                }
            }
        }
        Map<Long, String> professionNameMap = new HashMap<>();
        if (!professionIds.isEmpty()) {
            List<Profession> professions = professionMapper.selectBatchIds(professionIds);
            for (Profession p : professions) {
                professionNameMap.put(p.getId(), p.getName());
            }
        }
        List<Map<String, Object>> out = new ArrayList<>(records.size());
        for (Certificate c : records) {
            Map<String, Object> m = beanToMap(c);
            m.remove("extraJson");
            m.put("templateName", c.getTemplateId() != null ? templateNameMap.get(c.getTemplateId()) : null);
            // 照片URL: 优先按 certificateId 取,未命中则回退到 idCard
            String photoUrl2 = photoUrlByCertId2.get(c.getId());
            if (photoUrl2 == null && c.getIdCard() != null) {
                photoUrl2 = photoUrlByIdCard2.get(c.getIdCard());
            }
            m.put("photoUrl", photoUrl2);
            if (c.getProfession() != null) {
                try {
                    Long profId = Long.parseLong(c.getProfession());
                    m.put("professionName", professionNameMap.getOrDefault(profId, c.getProfession()));
                } catch (NumberFormatException e) {
                    m.put("professionName", c.getProfession());
                }
            } else {
                m.put("professionName", null);
            }
            // 颁发日期中文格式
            if (c.getIssueDate() != null) {
                m.put("issueDateStr", c.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy年M月d日")));
            }
            parseExtraJson(c.getExtraJson(), m);
            out.add(m);
        }
        return new PageResult<>(base.getTotal(), base.getPage(), base.getSize(), out);
    }

    private void parseExtraJson(String extraJson, Map<String, Object> map) {
        if (StringUtils.hasText(extraJson)) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> extra = mapper.readValue(extraJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (extra != null) {
                    // 成绩字段: 优先用标准键(camelCase),回退到旧键名和 ext_ 键
                    map.put("theoryScore", firstNonNull(
                            extra.get("theoryScore"), extra.get("theory_score"),
                            extra.get("ext_llzscjc"), extra.get("ext_llzscjd"),
                            map.get("theoryScore")));
                    map.put("practicalScore", firstNonNull(
                            extra.get("practicalScore"), extra.get("practical_score"),
                            extra.get("skill_score"),
                            extra.get("ext_czjncjc"), extra.get("ext_czjncjd"),
                            map.get("practicalScore")));
                    map.put("comprehensiveEvaluation", firstNonNull(
                            extra.get("comprehensiveEvaluation"), extra.get("comprehensive_score"),
                            extra.get("ext_zhpjcj"), extra.get("ext_zhpjcjd"),
                            map.get("comprehensiveEvaluation")));
                    map.put("birthday", extra.get("birthday"));
                    // cert_type 优先用数据库列的值(beanToMap 已写入 certType);
                    // 仅当列为空时, 才回退到 extra_json(兼容历史数据未迁移的情况)
                    if (map.get("certType") == null) {
                        map.put("certType", extra.get("cert_type"));
                        if (map.get("certType") == null) {
                            map.put("certType", extra.get("certType"));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /** 返回第一个非 null 的值,全部为 null 则返回 null */
    private Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null && StringUtils.hasText(String.valueOf(v))) return v;
        }
        return null;
    }

    /** 从身份证号提取出生日期(yyyy-MM-dd),无效返回 null */
    private String extractBirthdayFromIdCard(String idCard) {
        if (idCard == null) return null;
        idCard = idCard.trim();
        if (idCard.length() == 18) {
            String year = idCard.substring(6, 10);
            String month = idCard.substring(10, 12);
            String day = idCard.substring(12, 14);
            return year + "-" + month + "-" + day;
        }
        if (idCard.length() == 15) {
            String year = "19" + idCard.substring(6, 8);
            String month = idCard.substring(8, 10);
            String day = idCard.substring(10, 12);
            return year + "-" + month + "-" + day;
        }
        return null;
    }

    private PageResult<Certificate> pageWithProfession(Integer page, Integer size,
                                                        String name, String idCard,
                                                        String agency, String profession,
                                                        String issueDateStart, String issueDateEnd) {
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .like(StringUtils.hasText(name), Certificate::getName, name)
                .like(StringUtils.hasText(idCard), Certificate::getIdCard, idCard)
                .like(StringUtils.hasText(agency), Certificate::getAgency, agency)
                .like(StringUtils.hasText(profession), Certificate::getProfession, profession)
                .orderByDesc(Certificate::getCreateTime)
                .orderByDesc(Certificate::getId);
        if (StringUtils.hasText(issueDateStart)) {
            w.ge(Certificate::getIssueDate, parseDate(issueDateStart));
        }
        if (StringUtils.hasText(issueDateEnd)) {
            w.le(Certificate::getIssueDate, parseDate(issueDateEnd));
        }
        Page<Certificate> p = new Page<>(page, size);
        return new PageResult<>(this.page(p, w));
    }

    /**
     * 基础分页查询(不含 profession),支持按 cert_type 在 SQL 级过滤。
     * certType 为空时不追加条件,与原 page() 行为一致。
     */
    private PageResult<Certificate> pageBase(Integer page, Integer size,
                                              String name, String idCard, String agency,
                                              String certType,
                                              String importTimeStart, String importTimeEnd) {
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .like(StringUtils.hasText(name), Certificate::getName, name)
                .like(StringUtils.hasText(idCard), Certificate::getIdCard, idCard)
                .like(StringUtils.hasText(agency), Certificate::getAgency, agency)
                .eq(StringUtils.hasText(certType), Certificate::getCertType, certType)
                .orderByDesc(Certificate::getCreateTime)
                .orderByDesc(Certificate::getId);
        // 导入时间范围查询(支持小时级别: yyyy-MM-dd HH:mm:ss)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.hasText(importTimeStart)) {
            try {
                w.ge(Certificate::getUploadTime, LocalDateTime.parse(importTimeStart, dtf));
            } catch (Exception e) {
                w.ge(Certificate::getUploadTime, LocalDate.parse(importTimeStart, df).atStartOfDay());
            }
        }
        if (StringUtils.hasText(importTimeEnd)) {
            try {
                w.le(Certificate::getUploadTime, LocalDateTime.parse(importTimeEnd, dtf));
            } catch (Exception e) {
                w.le(Certificate::getUploadTime, LocalDate.parse(importTimeEnd, df).atTime(23, 59, 59));
            }
        }
        Page<Certificate> p = new Page<>(page, size);
        return new PageResult<>(this.page(p, w));
    }

    /**
     * 分页查询(含 profession 模糊过滤),支持按 cert_type 在 SQL 级过滤。
     * certType 为空时不追加条件,与原 pageWithProfession() 行为一致。
     */
    private PageResult<Certificate> pageWithProfessionAndCertType(Integer page, Integer size,
                                                                   String name, String idCard,
                                                                   String agency, String profession,
                                                                   String certType,
                                                                   String importTimeStart, String importTimeEnd) {
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .like(StringUtils.hasText(name), Certificate::getName, name)
                .like(StringUtils.hasText(idCard), Certificate::getIdCard, idCard)
                .like(StringUtils.hasText(agency), Certificate::getAgency, agency)
                .like(StringUtils.hasText(profession), Certificate::getProfession, profession)
                .eq(StringUtils.hasText(certType), Certificate::getCertType, certType)
                .orderByDesc(Certificate::getCreateTime)
                .orderByDesc(Certificate::getId);
        // 导入时间范围查询(支持小时级别)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.hasText(importTimeStart)) {
            try {
                w.ge(Certificate::getUploadTime, LocalDateTime.parse(importTimeStart, dtf));
            } catch (Exception e) {
                w.ge(Certificate::getUploadTime, LocalDate.parse(importTimeStart, df).atStartOfDay());
            }
        }
        if (StringUtils.hasText(importTimeEnd)) {
            try {
                w.le(Certificate::getUploadTime, LocalDateTime.parse(importTimeEnd, dtf));
            } catch (Exception e) {
                w.le(Certificate::getUploadTime, LocalDate.parse(importTimeEnd, df).atTime(23, 59, 59));
            }
        }
        Page<Certificate> p = new Page<>(page, size);
        return new PageResult<>(this.page(p, w));
    }

    private Map<String, Object> beanToMap(Object bean) {
        Map<String, Object> m = new HashMap<>();
        if (bean == null) return m;
        // 反射拿字段(纯 JDK 实现,不依赖 Hutool 的具体方法)
        for (java.lang.reflect.Field f : getAllFields(bean.getClass())) {
            f.setAccessible(true);
            try {
                m.put(f.getName(), f.get(bean));
            } catch (IllegalAccessException ignore) { /* ignore */ }
        }
        return m;
    }

    private List<java.lang.reflect.Field> getAllFields(Class<?> cls) {
        List<java.lang.reflect.Field> out = new ArrayList<>();
        while (cls != null && cls != Object.class) {
            for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                out.add(f);
            }
            cls = cls.getSuperclass();
        }
        return out;
    }

    @Override
    public CertificateVO detail(Long id) {
        Certificate c = this.getById(id);
        if (c == null) throw new BusinessException("证书不存在");
        CertificateVO vo = new CertificateVO();
        org.springframework.beans.BeanUtils.copyProperties(c, vo);
        vo.setIssueDate(c.getIssueDate() == null ? null : c.getIssueDate().toString());
        vo.setGenderName(c.getGender() == null ? null : (c.getGender() == 1 ? "男" : "女"));
        vo.setAgencyFee(c.getAgencyFee() == null ? null : c.getAgencyFee().toPlainString());
        vo.setCreateTime(c.getCreateTime() == null ? null : c.getCreateTime().toString());
        vo.setUpdateTime(c.getUpdateTime() == null ? null : c.getUpdateTime().toString());
        // 解析 extra_json 到 map
        if (StringUtils.hasText(c.getExtraJson())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = MAPPER.readValue(c.getExtraJson(), Map.class);
                vo.setExtra(map);
            } catch (JsonProcessingException e) {
                vo.setExtra(Collections.emptyMap());
            }
        } else {
            vo.setExtra(Collections.emptyMap());
        }
        // 附字段定义
        vo.setFields(fieldMapper.selectList(
                new LambdaQueryWrapper<CertificateField>().orderByAsc(CertificateField::getSort)));
        return vo;
    }

    /**
     * 检查是否存在 姓名+身份证号+专业+级别 完全相同的证书记录
     * 用于导入时数据查重:四项完全相同返回 true(不允许导入)
     */
    @Override
    public boolean existsByNameIdCardProfessionLevel(String name, String idCard, String profession, String skillLevel) {
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getName, name != null ? name : "");
        if (StringUtils.hasText(idCard)) {
            w.eq(Certificate::getIdCard, idCard.trim());
        } else {
            w.and(ww -> ww.isNull(Certificate::getIdCard).or().eq(Certificate::getIdCard, ""));
        }
        if (StringUtils.hasText(profession)) {
            w.eq(Certificate::getProfession, profession.trim());
        } else {
            w.and(ww -> ww.isNull(Certificate::getProfession).or().eq(Certificate::getProfession, ""));
        }
        if (StringUtils.hasText(skillLevel)) {
            w.eq(Certificate::getSkillLevel, skillLevel.trim());
        } else {
            w.and(ww -> ww.isNull(Certificate::getSkillLevel).or().eq(Certificate::getSkillLevel, ""));
        }
        return this.count(w) > 0;
    }

    @Override
    public boolean add(CertificateDTO dto) {
        Certificate c = new Certificate();
        copyDtoToEntity(dto, c);
        // 身份证号必填校验(数据库 id_card NOT NULL,前端/导入路径均已校验,此处兜底)
        if (!StringUtils.hasText(c.getIdCard())) {
            throw new BusinessException("身份证号不能为空");
        }
        // 身份证号校验:校验异常不拦截,正常保存(前端会用浅红色背景标注异常身份证)
        if (StringUtils.hasText(c.getIdCard())) {
            c.setIdCard(c.getIdCard().trim());
        }
        // 空字符串归一化为 null：cert_no / student_no 均有唯一约束(uk_cert_no / uk_student_no)，
        // 空字符串 '' 在 MySQL 唯一索引中被视为相等值，多条 '' 会触发主键/唯一键冲突；
        // 而 NULL 在唯一索引中可重复。证书编号设计上在"绑定模板"时才生成，
        // 学员编号由下方 fillStudentNoIfEmpty 生成，故创建时这两个字段为空一律存 NULL。
        if (!StringUtils.hasText(c.getCertNo())) c.setCertNo(null);
        if (!StringUtils.hasText(c.getStudentNo())) c.setStudentNo(null);
        // 自动从身份证提取性别
        if (c.getGender() == null && StringUtils.hasText(c.getIdCard())) {
            c.setGender(CertificateNumberServiceImpl.extractGenderFromIdCard(c.getIdCard()));
        }
        // 自动从身份证提取出生日期,存入 extra_json
        if (StringUtils.hasText(c.getIdCard())) {
            String birthday = extractBirthdayFromIdCard(c.getIdCard());
            if (birthday != null) {
                Map<String, Object> extra = dto.getExtra() != null ? dto.getExtra() : new HashMap<>();
                if (!extra.containsKey("birthday")) {
                    extra.put("birthday", birthday);
                    dto.setExtra(extra);
                }
            }
        }
        // 默认颁发日期 = 今天
        if (c.getIssueDate() == null) c.setIssueDate(LocalDate.now());
        // 默认技能等级
        if (!StringUtils.hasText(c.getSkillLevel())) c.setSkillLevel("高级");

        // ====== 去重检查: 姓名 + 身份证号 + 专业 + 级别 完全相同则跳过 ======
        LambdaQueryWrapper<Certificate> dupCheck = new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getName, c.getName() != null ? c.getName() : "");
        if (StringUtils.hasText(c.getIdCard())) {
            dupCheck.eq(Certificate::getIdCard, c.getIdCard());
        } else {
            dupCheck.and(w -> w.isNull(Certificate::getIdCard).or().eq(Certificate::getIdCard, ""));
        }
        if (StringUtils.hasText(c.getProfession())) {
            dupCheck.eq(Certificate::getProfession, c.getProfession());
        } else {
            dupCheck.and(w -> w.isNull(Certificate::getProfession).or().eq(Certificate::getProfession, ""));
        }
        if (StringUtils.hasText(c.getSkillLevel())) {
            dupCheck.eq(Certificate::getSkillLevel, c.getSkillLevel());
        } else {
            dupCheck.and(w -> w.isNull(Certificate::getSkillLevel).or().eq(Certificate::getSkillLevel, ""));
        }
        if (this.count(dupCheck) > 0) {
            // 已存在相同 姓名+身份证+专业+级别 的记录,跳过不创建
            return false;
        }

        // 自动生成编号:学员编号在创建时生成;证书编号在"绑定模板"时才生成(为空留待绑定时补)
        numberService.fillStudentNoIfEmpty(c);
        // 应用自定义字段默认值(含系统字段,如成绩等)
        Map<String, Object> extra = dto.getExtra() != null ? dto.getExtra() : new HashMap<>();
        List<CertificateField> allFields = fieldMapper.selectList(
                new LambdaQueryWrapper<CertificateField>().orderByAsc(CertificateField::getSort));
        for (CertificateField cf : allFields) {
            if (cf.getDefaultValue() != null && !cf.getDefaultValue().isEmpty()) {
                String fieldKey = cf.getFieldKey();
                if (!extra.containsKey(fieldKey)) {
                    extra.put(fieldKey, cf.getDefaultValue());
                }
            }
        }
        // 序列化 extra
        if (!extra.isEmpty()) {
            try {
                c.setExtraJson(MAPPER.writeValueAsString(extra));
            } catch (JsonProcessingException e) {
                throw new BusinessException("自定义字段序列化失败");
            }
        }
        // 成绩字段同步写入主表列(导入时 extra 中已有值,确保前端列表直接显示)
        Object tsObj = extra.get("theoryScore");
        if (tsObj != null && StringUtils.hasText(String.valueOf(tsObj))) {
            c.setTheoryScore(String.valueOf(tsObj));
        }
        Object psObj = extra.get("practicalScore");
        if (psObj != null && StringUtils.hasText(String.valueOf(psObj))) {
            c.setPracticalScore(String.valueOf(psObj));
        }
        Object ceObj = extra.get("comprehensiveEvaluation");
        if (ceObj != null && StringUtils.hasText(String.valueOf(ceObj))) {
            c.setComprehensiveEvaluation(String.valueOf(ceObj));
        }
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        // 导入时间(新增/导入时自动填写;以导入时间作为后续筛选/排序依据)
        c.setUploadTime(LocalDateTime.now());
        this.save(c);

        // ====== 自动绑定证书模板: 按 certType 匹配同名模板 ======
        if (StringUtils.hasText(c.getCertType()) && c.getTemplateId() == null) {
            try {
                CertificateTemplate matched = templateMapper.selectOne(
                        new LambdaQueryWrapper<CertificateTemplate>()
                                .eq(CertificateTemplate::getName, c.getCertType().trim())
                                .last("LIMIT 1"));
                if (matched != null) {
                    c.setTemplateId(matched.getId());
                    // 绑定模板时自动生成证书编号
                    numberService.fillCertNoIfEmpty(c, matched.getId());
                    this.updateById(c);
                }
            } catch (Exception e) {
                // 自动绑定失败不阻断主流程,用户可后续手动绑定
            }
        }
        return true;
    }

    @Override
    public void update(CertificateDTO dto) {
        if (dto.getId() == null) throw new BusinessException("id 不能为空");
        // 空字符串归一化为 null：与 add 一致，避免编辑时把 cert_no/student_no 写成空串 ''
        // 触发 uk_cert_no / uk_student_no 唯一键冲突（'' 在唯一索引中视为相等，NULL 可重复）。
        // 证书编号由系统在绑定模板时生成，编辑时传空串视为"不修改该字段"。
        if (dto.getCertNo() != null && !StringUtils.hasText(dto.getCertNo())) dto.setCertNo(null);
        if (dto.getStudentNo() != null && !StringUtils.hasText(dto.getStudentNo())) dto.setStudentNo(null);
        Certificate exist = this.getById(dto.getId());
        if (exist == null) throw new BusinessException("证书不存在");
        Certificate c = new Certificate();
        c.setId(dto.getId());
        if (StringUtils.hasText(dto.getName())) c.setName(dto.getName());
        if (StringUtils.hasText(dto.getIdCard())) c.setIdCard(dto.getIdCard());
        if (dto.getGender() != null) c.setGender(dto.getGender());
        if (StringUtils.hasText(dto.getProfession())) c.setProfession(dto.getProfession());
        if (StringUtils.hasText(dto.getSkillLevel())) c.setSkillLevel(dto.getSkillLevel());
        if (dto.getIssueDate() != null) c.setIssueDate(dto.getIssueDate());
        if (dto.getAgencyFee() != null) c.setAgencyFee(dto.getAgencyFee());
        if (StringUtils.hasText(dto.getAgency())) c.setAgency(dto.getAgency());
        // 支持修改 certNo 和 studentNo（即使之前为空也可以设置）
        if (dto.getCertNo() != null) c.setCertNo(dto.getCertNo());
        if (dto.getStudentNo() != null) c.setStudentNo(dto.getStudentNo());
        if (dto.getQrUrl1() != null) c.setQrUrl1(dto.getQrUrl1());
        if (dto.getQrUrl2() != null) c.setQrUrl2(dto.getQrUrl2());
        if (dto.getQrUrl3() != null) c.setQrUrl3(dto.getQrUrl3());
        if (dto.getExamQrUrl() != null) c.setExamQrUrl(dto.getExamQrUrl());
        if (dto.getExamQrEnabled() != null) c.setExamQrEnabled(dto.getExamQrEnabled());
        if (dto.getRemark() != null) c.setRemark(dto.getRemark());
        if (dto.getExtra() != null) {
            try {
                c.setExtraJson(MAPPER.writeValueAsString(dto.getExtra()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("自定义字段序列化失败");
            }
            // 成绩字段同步写入主表列(编辑时 extra 中已有值,确保前端列表直接显示)
            Map<String, Object> ex = dto.getExtra();
            Object tsObj = ex.get("theoryScore");
            if (tsObj != null && StringUtils.hasText(String.valueOf(tsObj))) {
                c.setTheoryScore(String.valueOf(tsObj));
            }
            Object psObj = ex.get("practicalScore");
            if (psObj != null && StringUtils.hasText(String.valueOf(psObj))) {
                c.setPracticalScore(String.valueOf(psObj));
            }
            Object ceObj = ex.get("comprehensiveEvaluation");
            if (ceObj != null && StringUtils.hasText(String.valueOf(ceObj))) {
                c.setComprehensiveEvaluation(String.valueOf(ceObj));
            }
        }
        c.setUpdateTime(LocalDateTime.now());
        this.updateById(c);
        // ============ 强制写入字段(绕过 updateById 默认 NOT_NULL 策略) ============
        // templateId 允许清空(编辑页 el-select 可 clearable 置 null),
        // 而 updateById 默认 NOT_NULL 策略会忽略 null 字段,故单独用 wrapper 强制写入,
        // 以支持"一人多证"场景下为某张证书指定/清除模板。
        // certNo / studentNo 同理:虽然上面已 setCertNo/setStudentNo 赋值到实体,
        // 但因 updateById 默认 NOT_NULL 策略,在实体该字段为 null(被前端清空/历史空值)时
        // 会被忽略,导致"提交成功但值未变"。这里用带条件的 set 强制写入,确保修改一定落库。
        this.update(new LambdaUpdateWrapper<Certificate>()
                .eq(Certificate::getId, dto.getId())
                .set(Certificate::getTemplateId, dto.getTemplateId())
                .set(dto.getCertNo() != null, Certificate::getCertNo, dto.getCertNo())
                .set(dto.getStudentNo() != null, Certificate::getStudentNo, dto.getStudentNo())
                .set(dto.getCertType() != null, Certificate::getCertType, dto.getCertType()));
        // 绑定模板时(编辑页选择模板),若证书编号仍为空则自动生成(证书编号在绑定模板时才生成)
        if (dto.getTemplateId() != null) {
            Certificate latest = this.getById(dto.getId());
            if (latest != null && !StringUtils.hasText(latest.getCertNo())) {
                // 使用模板的 cert_no_prefix/cert_no_middle 配置生成证书编号(模板未配置时回落到全局编号配置)
                numberService.fillCertNoIfEmpty(latest, dto.getTemplateId());
                this.update(new LambdaUpdateWrapper<Certificate>()
                        .eq(Certificate::getId, dto.getId())
                        .set(Certificate::getCertNo, latest.getCertNo())
                        .set(Certificate::getCertNoPrefix, latest.getCertNoPrefix())
                        .set(Certificate::getCertNoMiddle, latest.getCertNoMiddle()));
            }
        }
        // ====== 自动绑定证书模板: 按 certType 匹配同名模板(编辑时 certType 变更且未手动绑定模板) ======
        if (StringUtils.hasText(dto.getCertType()) && dto.getTemplateId() == null) {
            try {
                CertificateTemplate matched = templateMapper.selectOne(
                        new LambdaQueryWrapper<CertificateTemplate>()
                                .eq(CertificateTemplate::getName, dto.getCertType().trim())
                                .last("LIMIT 1"));
                if (matched != null) {
                    this.update(new LambdaUpdateWrapper<Certificate>()
                            .eq(Certificate::getId, dto.getId())
                            .set(Certificate::getTemplateId, matched.getId()));
                    // 绑定模板时自动生成证书编号
                    Certificate latest2 = this.getById(dto.getId());
                    if (latest2 != null && !StringUtils.hasText(latest2.getCertNo())) {
                        numberService.fillCertNoIfEmpty(latest2, matched.getId());
                        this.update(new LambdaUpdateWrapper<Certificate>()
                                .eq(Certificate::getId, dto.getId())
                                .set(Certificate::getCertNo, latest2.getCertNo())
                                .set(Certificate::getCertNoPrefix, latest2.getCertNoPrefix())
                                .set(Certificate::getCertNoMiddle, latest2.getCertNoMiddle()));
                    }
                }
            } catch (Exception e) {
                // 自动绑定失败不阻断主流程
            }
        }
        // ====== 颁发日期变更时,联动重新生成证书编号和学员编号 ======
        // 编号规则中包含 yyyyMMdd 日期段,日期变化后编号需同步更新
        if (dto.getIssueDate() != null && exist.getIssueDate() != null
                && !dto.getIssueDate().equals(exist.getIssueDate())) {
            try {
                Certificate latest = this.getById(dto.getId());
                if (latest != null) {
                    latest.setIssueDate(dto.getIssueDate());
                    numberService.regenerateNumbers(latest);
                    this.update(new LambdaUpdateWrapper<Certificate>()
                            .eq(Certificate::getId, dto.getId())
                            .set(Certificate::getCertNo, latest.getCertNo())
                            .set(Certificate::getCertNoPrefix, latest.getCertNoPrefix())
                            .set(Certificate::getCertNoMiddle, latest.getCertNoMiddle())
                            .set(Certificate::getStudentNo, latest.getStudentNo())
                            .set(Certificate::getStudentNoPrefix, latest.getStudentNoPrefix())
                            .set(Certificate::getStudentNoMiddle, latest.getStudentNoMiddle()));
                }
            } catch (Exception e) {
                log.warn("颁发日期变更后重新生成编号失败: certId={}, error={}", dto.getId(), e.getMessage());
            }
        }
        // 同步到学生表: 证书管理修改属性后,学生管理界面也同步更新
        syncCertificateToStudentTable(exist, dto);
    }

    /**
     * 证书管理修改属性后,同步到学生表(student)
     * 按证书旧姓名+旧身份证号匹配学生记录,更新姓名/身份证号/专业等属性
     */
    private void syncCertificateToStudentTable(Certificate oldCert, CertificateDTO newDto) {
        if (oldCert == null || newDto == null) return;
        if (oldCert.getName() == null || oldCert.getIdCard() == null) return;
        // 查找匹配的学生记录
        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getName, oldCert.getName())
                        .eq(Student::getIdCard, oldCert.getIdCard()));
        if (students.isEmpty()) return;
        for (Student student : students) {
            boolean changed = false;
            // 同步姓名
            if (StringUtils.hasText(newDto.getName()) && !newDto.getName().equals(student.getName())) {
                student.setName(newDto.getName());
                changed = true;
            }
            // 同步身份证号
            if (StringUtils.hasText(newDto.getIdCard()) && !newDto.getIdCard().equals(student.getIdCard())) {
                student.setIdCard(newDto.getIdCard().trim());
                changed = true;
            }
            // 同步专业: 从证书的专业名称查找对应的 professionId,更新学生的主专业
            if (StringUtils.hasText(newDto.getProfession()) && !newDto.getProfession().equals(oldCert.getProfession())) {
                Profession prof = professionMapper.selectOne(
                        new LambdaQueryWrapper<Profession>()
                                .eq(Profession::getName, newDto.getProfession().trim())
                                .last("LIMIT 1"));
                if (prof != null) {
                    student.setProfessionId(prof.getId());
                    changed = true;
                    // 同时更新 student_profession 关联表: 若新专业不存在则添加
                    List<StudentProfession> sps = studentProfessionMapper.selectByStudentId(student.getId());
                    boolean hasProf = sps.stream().anyMatch(sp -> prof.getId().equals(sp.getProfessionId()));
                    if (!hasProf) {
                        StudentProfession sp = new StudentProfession();
                        sp.setStudentId(student.getId());
                        sp.setProfessionId(prof.getId());
                        studentProfessionMapper.insert(sp);
                    }
                }
            }
            if (changed) {
                studentMapper.updateById(student);
            }
        }
    }

    @Override
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        this.removeByIds(ids);
    }

    /**
     * 解析 Excel(dry-run,只解析不入库)
     * - 返回成功行(待入库) + 失败行(原样回显给用户)
     * - 入库由 commitImport 完成
     */
    @Override
    public CertificateImportResult parseExcel(MultipartFile file) {
        return parseExcelInternal(file, true);
    }

    /**
     * 解析 Excel 并直接入库(非 dry-run 模式)
     * - 兼容老逻辑,留作同步小批量路径(< ASYNC_THRESHOLD 时使用)
     */
    public CertificateImportResult parseAndImport(MultipartFile file) {
        return parseExcelInternal(file, false);
    }

    private CertificateImportResult parseExcelInternal(MultipartFile file, boolean dryRun) {
        if (file == null || file.isEmpty()) throw new BusinessException("文件为空");
        CertificateImportResult result = new CertificateImportResult();
        List<CertificateImportRow> failed = new ArrayList<>();
        List<CertificateDTO> validRows = new ArrayList<>();
        // 数据行从第 2 行开始(第 1 行是表头)
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override public void invoke(Map<Integer, String> row, AnalysisContext ctx) {
                    if (row == null) return;
                    CertificateImportRow r = parseRow(row, ctx.readRowHolder().getRowIndex());
                    if (r.getError() != null) {
                        failed.add(r);
                    } else {
                        validRows.add(toDto(r));
                    }
                }
                @Override public void doAfterAllAnalysed(AnalysisContext ctx) { /* noop */ }
            }).sheet().doRead();
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败: " + e.getMessage());
        }
        result.setFailedRows(failed);
        result.setFailCount(failed.size());
        result.setSuccessCount(validRows.size());
        // dry-run 模式: 把待入库的 DTO 转成 ImportRow 形式回传(供前端展示)
        if (dryRun) {
            List<CertificateImportRow> pending = toImportRows(validRows);
            result.setPendingRows(pending);
            result.setDryRun(true);
            // 把 pendingRows 编码为 token,前端确认时回传 token 即可(无需关心数据)
            try {
                String json = MAPPER.writeValueAsString(pending);
                String token = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
                result.setDryRunToken(token);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                // 序列化失败:日志警告,不影响 dry-run 主流程(用户仍可重试或继续)
                log.warn("dry-run token 生成失败(序列化): {}", e.getMessage());
            } catch (Exception e) {
                // 其它异常:不掩盖,以便排错
                log.error("dry-run token 生成失败(未知): {}", e.getMessage(), e);
            }
            return result;
        }
        // 非 dry-run 模式: 直接入库
        CertificateImportResult importResult = importRows(validRows);
        result.setSuccessCount(importResult.getSuccessCount());
        result.setFailCount(importResult.getFailCount() + failed.size());
        result.setSkipCount(importResult.getSkipCount());
        if (importResult.getFailedRows() != null) {
            result.getFailedRows().addAll(importResult.getFailedRows());
        }
        if (importResult.getSkippedRows() != null) {
            result.setSkippedRows(importResult.getSkippedRows());
        }
        return result;
    }

    /**
     * 把 DTO 列表转回 ImportRow 形式(仅用于 dry-run 展示,实际入库时不依赖此结构)
     */
    @Override
    public CertificateDTO toImportDto(CertificateImportRow r) {
        return toDto(r);
    }

    @Override
    public CertificateImportRow toImportDtoForRow(CertificateDTO d) {
        return toImportRow(d);
    }

    @Override
    public com.exam.common.AsyncTask getTaskById(String taskId) {
        return taskService.get(taskId);
    }

    @Override
    public void syncTheoryScore(String idCard, String profession, String scoreStr) {
        if (!StringUtils.hasText(idCard) || !StringUtils.hasText(profession)) {
            log.warn("syncTheoryScore 参数为空: idCard={}, profession={}", idCard, profession);
            return;
        }

        // 1. 收集该 profession 所有可能的匹配值(ID和名称,双向解析)
        Set<String> matchValues = new HashSet<>();
        matchValues.add(profession.trim());
        Long profId = null;
        String profName = null;
        try {
            profId = Long.parseLong(profession.trim());
        } catch (NumberFormatException ignored) {
        }
        if (profId != null) {
            Profession prof = professionMapper.selectById(profId);
            if (prof != null && StringUtils.hasText(prof.getName())) {
                profName = prof.getName().trim();
                matchValues.add(profName);
            }
        } else {
            // profession 是名称,反查 ID
            Profession prof = professionMapper.selectOne(
                    new LambdaQueryWrapper<Profession>().eq(Profession::getName, profession.trim()));
            if (prof != null) {
                profId = prof.getId();
                matchValues.add(profId.toString());
            }
        }

        // 2. 查出该 idCard 的所有证书(不按 profession 过滤,在 Java 中匹配)
        List<Certificate> allCerts = this.list(
                new LambdaQueryWrapper<Certificate>().eq(Certificate::getIdCard, idCard));
        if (allCerts.isEmpty()) {
            log.warn("syncTheoryScore 该idCard无任何证书: idCard={}", idCard);
            return;
        }

        // 3. 在 Java 中按 profession 匹配(同时支持 ID 和名称)
        List<Certificate> certs = new ArrayList<>();
        for (Certificate c : allCerts) {
            String certProf = c.getProfession();
            if (certProf != null && matchValues.contains(certProf.trim())) {
                certs.add(c);
            }
        }

        if (certs.isEmpty()) {
            log.warn("syncTheoryScore 未匹配到证书: idCard={}, profession={}, matchValues={}, 该学生证书profession分布={}",
                    idCard, profession, matchValues,
                    allCerts.stream().map(Certificate::getProfession).collect(java.util.stream.Collectors.toList()));
            return;
        }
        log.debug("syncTheoryScore 匹配成功: idCard={}, profession={}, matchValues={}, 命中{}条/共{}条",
                idCard, profession, matchValues, certs.size(), allCerts.size());
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (Certificate cert : certs) {
            try {
                Map<String, Object> extra = new HashMap<>();
                if (StringUtils.hasText(cert.getExtraJson())) {
                    extra = MAPPER.readValue(cert.getExtraJson(), new TypeReference<Map<String, Object>>() {});
                }
                // 检查是否已有初始成绩数据(导入时或创建后已填)
                Object practicalObj = extra.get("practicalScore");
                boolean hasInitialPractical = practicalObj != null && StringUtils.hasText(String.valueOf(practicalObj));
                Object evalObj = extra.get("comprehensiveEvaluation");
                boolean hasInitialEval = evalObj != null && StringUtils.hasText(String.valueOf(evalObj));

                // 理论成绩: 始终用考试成绩覆盖(用户要求考试后刷新)
                extra.put("theoryScore", scoreStr);
                // 综合测评: 已有初始数据则不覆盖; 没有则为"合格"
                if (!hasInitialEval) {
                    extra.put("comprehensiveEvaluation", "合格");
                }
                // 实操成绩: 已有初始数据则不覆盖; 没有则基于理论成绩合理推算
                if (!hasInitialPractical) {
                    try {
                        double theory = Double.parseDouble(scoreStr);
                        // 实操成绩在理论成绩 ±10 范围内,但不能低于60,也不能高于85
                        int min = Math.max(60, (int) Math.floor(theory - 10));
                        int max = Math.min(85, (int) Math.ceil(theory + 10));
                        if (max <= min) max = min + 1;
                        int practicalScore = random.nextInt(min, max + 1);
                        extra.put("practicalScore", String.valueOf(practicalScore));
                    } catch (NumberFormatException e) {
                        // 理论成绩非数字时,实操成绩基于身份证号hash生成稳定值(60-85)
                        int hash = Math.abs(idCard.hashCode());
                        int practicalScore = 60 + (hash % 26);
                        extra.put("practicalScore", String.valueOf(practicalScore));
                    }
                }
                // 同时回写到 certificate 主表字段(始终覆盖理论成绩;实操和综合测评同步更新)
                Certificate update = new Certificate();
                update.setId(cert.getId());
                update.setTheoryScore(scoreStr);
                // 同步更新主表的实操成绩和综合测评(之前只更新了extra_json,主表列未更新,导致前端不显示)
                Object psObj = extra.get("practicalScore");
                if (psObj != null && StringUtils.hasText(String.valueOf(psObj))) {
                    update.setPracticalScore(String.valueOf(psObj));
                }
                Object ceObj = extra.get("comprehensiveEvaluation");
                if (ceObj != null && StringUtils.hasText(String.valueOf(ceObj))) {
                    update.setComprehensiveEvaluation(String.valueOf(ceObj));
                }
                update.setExtraJson(MAPPER.writeValueAsString(extra));
                update.setUpdateTime(LocalDateTime.now());
                this.updateById(update);
            } catch (Exception e) {
                log.error("syncTheoryScore 单条证书更新失败: certId={}, idCard={}, error={}",
                        cert.getId(), cert.getIdCard(), e.getMessage(), e);
            }
        }
    }

    @Override
    public List<Long> listFilteredIdsWithTemplate(String name, String idCard, String agency,
                                                  String profession, String issueDateStart, String issueDateEnd) {
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .isNotNull(Certificate::getTemplateId)
                .like(StringUtils.hasText(name), Certificate::getName, name)
                .like(StringUtils.hasText(idCard), Certificate::getIdCard, idCard)
                .like(StringUtils.hasText(agency), Certificate::getAgency, agency)
                .like(StringUtils.hasText(profession), Certificate::getProfession, profession)
                .orderByDesc(Certificate::getCreateTime)
                .orderByDesc(Certificate::getId);
        if (StringUtils.hasText(issueDateStart)) {
            w.ge(Certificate::getIssueDate, parseDate(issueDateStart));
        }
        if (StringUtils.hasText(issueDateEnd)) {
            w.le(Certificate::getIssueDate, parseDate(issueDateEnd));
        }
        w.select(Certificate::getId);
        List<Certificate> list = this.list(w);
        List<Long> ids = new ArrayList<>(list.size());
        for (Certificate c : list) {
            ids.add(c.getId());
        }
        return ids;
    }

    private CertificateImportRow toImportRow(CertificateDTO d) {
        if (d == null) return null;
        CertificateImportRow r = new CertificateImportRow();
        r.setName(d.getName());
        r.setIdCard(d.getIdCard());
        r.setProfession(d.getProfession());
        r.setSkillLevel(d.getSkillLevel());
        r.setIssueDate(d.getIssueDate());
        r.setCertNoPrefix(d.getCertNoPrefix());
        r.setCertNoMiddle(d.getCertNoMiddle());
        r.setStudentNoPrefix(d.getStudentNoPrefix());
        r.setStudentNoMiddle(d.getStudentNoMiddle());
        r.setAgency(d.getAgency());
        r.setAgencyFee(d.getAgencyFee());
        r.setQr1(d.getQrUrl1());
        r.setQr2(d.getQrUrl2());
        r.setQr3(d.getQrUrl3());
        r.setExamQr(d.getExamQrUrl());
        // 成绩与培训信息(从 extra 反向提取)
        if (d.getExtra() != null) {
            r.setTrainingMajor((String) d.getExtra().get("trainingMajor"));
            r.setTrainingHours((String) d.getExtra().get("trainingHours"));
            r.setTrainingDate((String) d.getExtra().get("trainingDate"));
            r.setExamTime((String) d.getExtra().get("examTime"));
            r.setTheoryScore((String) d.getExtra().get("theoryScore"));
            r.setPracticalScore((String) d.getExtra().get("practicalScore"));
            r.setComprehensiveEvaluation((String) d.getExtra().get("comprehensiveEvaluation"));
            r.setPhone((String) d.getExtra().get("phone"));
        }
        return r;
    }

    private List<CertificateImportRow> toImportRows(List<CertificateDTO> dtos) {
        List<CertificateImportRow> list = new ArrayList<>();
        if (dtos == null) return list;
        for (CertificateDTO d : dtos) {
            list.add(toImportRow(d));
        }
        return list;
    }

    /**
     * 提交 dry-run 结果,真正入库
     * - 支持两种入参方式:
     *   1) pendingRows 不为空,直接入库
     *   2) dryRunToken 不为空,反序列化得到 pendingRows 后入库
     * - 同一时刻,token 和 pendingRows 二选一(token 优先)
     */
    @Override
    public CertificateImportResult commitImport(List<CertificateImportRow> pendingRows, String dryRunToken) {
        List<CertificateImportRow> rows = pendingRows;
        if (rows == null || rows.isEmpty()) {
            if (dryRunToken != null && !dryRunToken.isEmpty()) {
                rows = decodeDryRunToken(dryRunToken);
            }
        }
        if (rows == null || rows.isEmpty()) {
            CertificateImportResult r = new CertificateImportResult();
            r.setSuccessCount(0);
            r.setFailCount(0);
            return r;
        }
        List<CertificateDTO> dtos = new ArrayList<>();
        for (CertificateImportRow row : rows) {
            dtos.add(toDto(row));
        }
        return importRows(dtos);
    }

    /**
     * 兼容旧接口(无 token)
     */
    @Override
    public CertificateImportResult commitImport(List<CertificateImportRow> pendingRows) {
        return commitImport(pendingRows, null);
    }

    private List<CertificateImportRow> decodeDryRunToken(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            String json = new String(decoded, StandardCharsets.UTF_8);
            return MAPPER.readValue(json, new TypeReference<List<CertificateImportRow>>() {});
        } catch (Exception e) {
            throw new BusinessException("dry-run token 无效或已过期: " + e.getMessage());
        }
    }

    public CertificateImportResult importRows(List<CertificateDTO> rows) {
        CertificateImportResult r = new CertificateImportResult();
        if (rows == null || rows.isEmpty()) {
            r.setSuccessCount(0); r.setFailCount(0); r.setSkipCount(0);
            return r;
        }
        int ok = 0, fail = 0, skip = 0;
        List<CertificateImportRow> failed = new ArrayList<>();
        List<CertificateImportRow> skipped = new ArrayList<>();
        for (CertificateDTO dto : rows) {
            try {
                boolean created = add(dto);
                if (created) {
                    // 证书导入时同步创建/更新证书用户(不创建学生记录)
                    certificateUserSyncService.syncFromCertificateData(
                            dto.getName(), dto.getIdCard(), dto.getGender(), dto.getProfession());
                    ok++;
                } else {
                    // add 返回 false = 去重跳过,记录为"未成功"
                    skip++;
                    CertificateImportRow row = new CertificateImportRow();
                    row.setRowIndex(0);
                    row.setName(dto.getName());
                    row.setIdCard(dto.getIdCard());
                    row.setError("数据已存在(姓名+身份证+专业+级别重复,已跳过)");
                    skipped.add(row);
                }
            } catch (Exception e) {
                fail++;
                CertificateImportRow row = new CertificateImportRow();
                row.setRowIndex(0);
                row.setName(dto.getName());
                row.setIdCard(dto.getIdCard());
                row.setError(e.getMessage());
                failed.add(row);
            }
        }
        r.setSuccessCount(ok);
        r.setFailCount(fail);
        r.setSkipCount(skip);
        r.setFailedRows(failed);
        r.setSkippedRows(skipped);
        return r;
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        downloadTemplate(response, "证书导入模板", "证书导入模板.xlsx");
    }

    @Override
    public void downloadTemplate(HttpServletResponse response, String sheetName, String fileName) throws Exception {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        // 统一20列模板表头(与用户提供的模板完全一致)
        List<List<String>> head = new ArrayList<>();
        head.add(Arrays.asList("序号"));
        head.add(Arrays.asList("姓名"));
        head.add(Arrays.asList("性别"));
        head.add(Arrays.asList("证件号码"));
        head.add(Arrays.asList("职业名称"));
        head.add(Arrays.asList("技能等级"));
        head.add(Arrays.asList("证书编号"));
        head.add(Arrays.asList("颁发日期"));
        head.add(Arrays.asList("报单机构"));
        head.add(Arrays.asList("报单机构费用统计"));
        head.add(Arrays.asList("培训专业"));
        head.add(Arrays.asList("培训学时"));
        head.add(Arrays.asList("培训日期"));
        head.add(Arrays.asList("理论成绩"));
        head.add(Arrays.asList("实操成绩"));
        head.add(Arrays.asList("综合测评"));
        head.add(Arrays.asList("证书二维码生成1"));
        head.add(Arrays.asList("证书二维码生成2"));
        head.add(Arrays.asList("证书二维码生成3"));
        head.add(Arrays.asList("学员考试二维码"));
        // 证书类型: 动态获取系统设置中的证书类型名称
        String certTypeHint = "证书类型（选填）";
        try {
            List<String> typeNames = certificateTypeService.listAll().stream()
                    .filter(t -> t.getStatus() == null || t.getStatus() == 1)
                    .map(t -> t.getName())
                    .collect(java.util.stream.Collectors.toList());
            if (!typeNames.isEmpty()) {
                certTypeHint = "证书类型（选填，可选值：" + String.join("/", typeNames) + "）";
            }
        } catch (Exception e) { log.warn("证书导入模板获取证书类型失败: {}", e.getMessage()); }
        head.add(Arrays.asList(certTypeHint));
        // 示例行
        List<List<Object>> sample = new ArrayList<>();
        List<Object> s1 = new ArrayList<>();
        s1.add(1);
        s1.add("张三");
        s1.add("男");
        s1.add("130627198905076632");
        s1.add("电工");
        s1.add("三级/高级");
        s1.add("");
        s1.add("2026-05-15");
        s1.add("北京某培训机构");
        s1.add(new BigDecimal("1000.00"));
        s1.add("电工");
        s1.add("48");
        s1.add("2026年5月");
        s1.add("85");
        s1.add("85");
        s1.add("合格");
        s1.add("");
        s1.add("");
        s1.add("");
        s1.add("");
        s1.add("专项职业证书");
        sample.add(s1);
        try (OutputStream os = response.getOutputStream()) {
            EasyExcel.write(os).head(head).sheet(sheetName).doWrite(sample);
        }
    }

    /**
     * 导出证书数据(Excel,按证书绑定的模板分组导出)
     * - 未绑定模板的证书自动过滤,不导出
     * - 多个模板时,每个模板生成一个Excel文件,打包成ZIP下载
     * - 每个Excel的列配置由该模板的导出列配置决定(无配置则用默认20列)
     */
    @Override
    public void exportCertificates(HttpServletResponse response, String name, String idCard,
                                   String agency, String profession,
                                   String issueDateStart, String issueDateEnd,
                                   List<Long> ids) {
        // 1. 查询数据
        List<Certificate> certs;
        if (ids != null && !ids.isEmpty()) {
            certs = this.listByIds(ids);
        } else {
            LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                    .like(StringUtils.hasText(name), Certificate::getName, name)
                    .like(StringUtils.hasText(idCard), Certificate::getIdCard, idCard)
                    .like(StringUtils.hasText(agency), Certificate::getAgency, agency)
                    .like(StringUtils.hasText(profession), Certificate::getProfession, profession)
                    .orderByDesc(Certificate::getCreateTime)
                    .orderByDesc(Certificate::getId);
            if (StringUtils.hasText(issueDateStart)) {
                w.ge(Certificate::getIssueDate, parseDate(issueDateStart));
            }
            if (StringUtils.hasText(issueDateEnd)) {
                w.le(Certificate::getIssueDate, parseDate(issueDateEnd));
            }
            certs = this.list(w);
        }
        exportCertificateList(response, certs);
    }

    @Override
    public void exportCertificateList(HttpServletResponse response, List<Certificate> certs) {
        // 过滤掉未绑定模板的证书
        List<Certificate> boundCerts = certs.stream()
                .filter(c -> c.getTemplateId() != null)
                .collect(java.util.stream.Collectors.toList());

        if (boundCerts.isEmpty()) {
            try {
                response.setContentType("application/vnd.ms-excel");
                response.setCharacterEncoding("utf-8");
                String fileName = URLEncoder.encode("证书数据", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
                response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xls");
                try (OutputStream os = response.getOutputStream()) {
                    EasyExcel.write(os).excelType(ExcelTypeEnum.XLS).head(new ArrayList<List<String>>()).sheet("证书数据").doWrite(new ArrayList<>());
                }
            } catch (Exception e) {
                throw new BusinessException("导出失败: " + e.getMessage());
            }
            return;
        }

        // 按templateId分组
        Map<Long, List<Certificate>> grouped = boundCerts.stream()
                .collect(java.util.stream.Collectors.groupingBy(Certificate::getTemplateId));

        // 查询模板名称(用于文件命名)
        List<Long> templateIds = new ArrayList<>(grouped.keySet());
        List<CertificateTemplate> templates = templateMapper.selectBatchIds(templateIds);
        Map<Long, String> templateNameMap = new java.util.HashMap<>();
        for (CertificateTemplate t : templates) {
            templateNameMap.put(t.getId(), t.getName());
        }

        // 为每个模板组生成Excel
        CertificateUrlConfig urlConfig = getUrlConfigForExport();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy年M月d日");

        List<ExcelFileEntry> excelFiles = new ArrayList<>();

        for (Map.Entry<Long, List<Certificate>> entry : grouped.entrySet()) {
            Long tplId = entry.getKey();
            List<Certificate> groupCerts = entry.getValue();
            String tplName = templateNameMap.getOrDefault(tplId, "模板" + tplId);

            // 解析该模板的导出列配置
            List<ExportColumnDef> columnDefs = resolveExportColumns(tplId);

            // 构建表头
            List<List<String>> head = new ArrayList<>();
            for (ExportColumnDef col : columnDefs) {
                head.add(java.util.Arrays.asList(col.getColumnName()));
            }

            // 逐行构建数据
            List<List<Object>> dataList = new ArrayList<>();
            int idx = 1;
            for (Certificate c : groupCerts) {
                Map<String, Object> extra = parseExtraJsonToMap(c.getExtraJson());
                Map<String, String> qrValueMap = buildQrValueMap(c, extra);
                String qr1 = resolveQrUrlForExport(urlConfig == null ? null : urlConfig.getQr1Template(), qrValueMap, c.getQrUrl1());
                String qr2 = resolveQrUrlForExport(urlConfig == null ? null : urlConfig.getQr2Template(), qrValueMap, c.getQrUrl2());
                String qr3 = resolveQrUrlForExport(urlConfig == null ? null : urlConfig.getQr3Template(), qrValueMap, c.getQrUrl3());

                List<Object> row = new ArrayList<>();
                for (ExportColumnDef col : columnDefs) {
                    row.add(getFieldValue(col.getFieldKey(), c, extra, qr1, qr2, qr3, idx, dateFmt));
                }
                idx++;
                dataList.add(row);
            }

            // 写入内存
            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                EasyExcel.write(baos).excelType(ExcelTypeEnum.XLS).head(head).sheet("证书数据").doWrite(dataList);
                String safeName = tplName.replaceAll("[\\\\/:*?\"<>|]", "_");
                excelFiles.add(new ExcelFileEntry(safeName + ".xls", baos.toByteArray()));
            } catch (Exception e) {
                throw new BusinessException("生成Excel失败: " + e.getMessage());
            }
        }

        // 输出: 单个文件直接返回Excel,多个文件打包ZIP
        try {
            if (excelFiles.size() == 1) {
                ExcelFileEntry entry = excelFiles.get(0);
                String fileName = URLEncoder.encode("证书用户数据下载", StandardCharsets.UTF_8.name())
                        .replaceAll("\\+", "%20");
                response.setContentType("application/vnd.ms-excel");
                response.setCharacterEncoding("utf-8");
                response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xls");
                response.getOutputStream().write(entry.data);
                response.getOutputStream().flush();
            } else {
                String zipFileName = URLEncoder.encode("证书用户数据下载", StandardCharsets.UTF_8.name())
                        .replaceAll("\\+", "%20");
                response.setContentType("application/zip");
                response.setCharacterEncoding("utf-8");
                response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + zipFileName + ".zip");
                try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream())) {
                    for (ExcelFileEntry entry : excelFiles) {
                        java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry(entry.fileName);
                        zos.putNextEntry(ze);
                        zos.write(entry.data);
                        zos.closeEntry();
                    }
                }
                response.getOutputStream().flush();
            }
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    /** Excel文件条目(文件名+字节数据) */
    private static class ExcelFileEntry {
        final String fileName;
        final byte[] data;
        ExcelFileEntry(String fileName, byte[] data) {
            this.fileName = fileName;
            this.data = data;
        }
    }

    /** 导出列定义 */
    private static class ExportColumnDef {
        private final String fieldKey;
        private final String columnName;
        ExportColumnDef(String fieldKey, String columnName) {
            this.fieldKey = fieldKey;
            this.columnName = columnName;
        }
        String getFieldKey() { return fieldKey; }
        String getColumnName() { return columnName; }
    }

    /** 解析导出列配置: 有自定义配置则用配置,否则用默认20列 */
    private List<ExportColumnDef> resolveExportColumns(Long templateId) {
        // 默认20列 (与导入模板一致)
        String[][] defaults = {
            {"seq", "序号"}, {"name", "姓名"}, {"gender", "性别"}, {"idCard", "证件号码"},
            {"profession", "职业名称"}, {"skillLevel", "技能等级"}, {"certNo", "证书编号"},
            {"issueDate", "颁发日期"}, {"agency", "报单机构"}, {"agencyFee", "报单机构费用统计"},
            {"trainingMajor", "培训专业"}, {"trainingHours", "培训学时"}, {"trainingDate", "培训日期"},
            {"theoryScore", "理论成绩"}, {"practicalScore", "实操成绩"}, {"comprehensiveEvaluation", "综合测评"},
            {"qr1", "证书二维码生成1"}, {"qr2", "证书二维码生成2"}, {"qr3", "证书二维码生成3"},
            {"examQr", "学员考试二维码"}
        };

        // 如果指定了模板,尝试加载自定义导出列配置
        if (templateId != null) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.exam.entity.CertificateExportColumn> w =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            w.eq(com.exam.entity.CertificateExportColumn::getTemplateId, templateId)
             .orderByAsc(com.exam.entity.CertificateExportColumn::getSort);
            List<com.exam.entity.CertificateExportColumn> configured = exportColumnMapper.selectList(w);
            if (configured != null && !configured.isEmpty()) {
                List<ExportColumnDef> result = new ArrayList<>();
                for (com.exam.entity.CertificateExportColumn col : configured) {
                    result.add(new ExportColumnDef(col.getFieldKey(), col.getColumnName()));
                }
                return result;
            }
        }

        // 使用默认配置
        List<ExportColumnDef> result = new ArrayList<>();
        for (String[] d : defaults) {
            result.add(new ExportColumnDef(d[0], d[1]));
        }
        return result;
    }

    /** 根据fieldKey获取证书字段的值 */
    private Object getFieldValue(String fieldKey, Certificate c, Map<String, Object> extra,
                                  String qr1, String qr2, String qr3, int seq, DateTimeFormatter dateFmt) {
        switch (fieldKey) {
            case "seq": return seq;
            case "name": return safeStr(c.getName());
            case "gender": return c.getGender() != null ? (c.getGender() == 1 ? "男" : (c.getGender() == 2 ? "女" : "")) : "";
            case "idCard": return safeStr(c.getIdCard());
            case "profession": return safeStr(c.getProfession());
            case "skillLevel": return safeStr(c.getSkillLevel());
            case "certNo": return safeStr(c.getCertNo());
            case "issueDate": return c.getIssueDate() != null ? c.getIssueDate().format(dateFmt) : "";
            case "issueYear": return c.getIssueDate() != null ? String.valueOf(c.getIssueDate().getYear()) : "";
            case "issueMonth": return c.getIssueDate() != null ? String.valueOf(c.getIssueDate().getMonthValue()) : "";
            case "issueDay": return c.getIssueDate() != null ? String.valueOf(c.getIssueDate().getDayOfMonth()) : "";
            case "agency": return safeStr(c.getAgency());
            case "agencyFee": return c.getAgencyFee() != null ? c.getAgencyFee().toPlainString() : "";
            case "trainingMajor": return safeStr(extra.get("trainingMajor"));
            case "trainingHours": return safeStr(extra.get("trainingHours"));
            case "trainingDate": return safeStr(extra.get("trainingDate"));
            case "theoryScore": {
                String v = safeStr(c.getTheoryScore());
                if (v.isEmpty()) v = safeStr(extra.get("theoryScore"));
                if (v.isEmpty()) v = safeStr(extra.get("theory_score"));
                if (v.isEmpty()) v = safeStr(extra.get("ext_llzscjc"));
                if (v.isEmpty()) v = safeStr(extra.get("ext_llzscjd"));
                return v;
            }
            case "practicalScore": {
                String v = safeStr(c.getPracticalScore());
                if (v.isEmpty()) v = safeStr(extra.get("practicalScore"));
                if (v.isEmpty()) v = safeStr(extra.get("practical_score"));
                if (v.isEmpty()) v = safeStr(extra.get("skill_score"));
                if (v.isEmpty()) v = safeStr(extra.get("ext_czjncjc"));
                if (v.isEmpty()) v = safeStr(extra.get("ext_czjncjd"));
                return v;
            }
            case "comprehensiveEvaluation": {
                String v = safeStr(c.getComprehensiveEvaluation());
                if (v.isEmpty()) v = safeStr(extra.get("comprehensiveEvaluation"));
                if (v.isEmpty()) v = safeStr(extra.get("comprehensive_score"));
                if (v.isEmpty()) v = safeStr(extra.get("ext_zhpjcj"));
                if (v.isEmpty()) v = safeStr(extra.get("ext_zhpjcjd"));
                return v;
            }
            case "qr1": return safeStr(qr1);
            case "qr2": return safeStr(qr2);
            case "qr3": return safeStr(qr3);
            case "examQr": return safeStr(c.getExamQrUrl());
            case "certType": return safeStr(c.getCertType());
            case "studentNo": return safeStr(c.getStudentNo());
            case "birthday": {
                String idCard = c.getIdCard();
                if (idCard != null && idCard.length() >= 14) {
                    try {
                        int bYear = Integer.parseInt(idCard.substring(6, 10));
                        int bMonth = Integer.parseInt(idCard.substring(10, 12));
                        int bDay = Integer.parseInt(idCard.substring(12, 14));
                        return String.format("%d年%d月%d日", bYear, bMonth, bDay);
                    } catch (NumberFormatException e) {
                        return "";
                    }
                }
                return "";
            }
            case "phone": return safeStr(extra.get("phone"));
            case "uploadTime": return c.getUploadTime() != null ? c.getUploadTime().toString() : "";
            // ext_ 字段键映射(职业能力/能力等级导出配置使用)
            case "ext_llzscjc":
            case "ext_llzscjd": {
                String v = safeStr(extra.get(fieldKey));
                if (v.isEmpty()) v = safeStr(c.getTheoryScore());
                if (v.isEmpty()) v = safeStr(extra.get("theoryScore"));
                if (v.isEmpty()) v = safeStr(extra.get("theory_score"));
                return v;
            }
            case "ext_czjncjc":
            case "ext_czjncjd": {
                String v = safeStr(extra.get(fieldKey));
                if (v.isEmpty()) v = safeStr(c.getPracticalScore());
                if (v.isEmpty()) v = safeStr(extra.get("practicalScore"));
                if (v.isEmpty()) v = safeStr(extra.get("skill_score"));
                return v;
            }
            case "ext_zhpjcj":
            case "ext_zhpjcjd": {
                String v = safeStr(extra.get(fieldKey));
                if (v.isEmpty()) v = safeStr(c.getComprehensiveEvaluation());
                if (v.isEmpty()) v = safeStr(extra.get("comprehensiveEvaluation"));
                if (v.isEmpty()) v = safeStr(extra.get("comprehensive_score"));
                return v;
            }
            case "ext_jndjc":
            case "ext_jndjd": {
                String v = safeStr(extra.get(fieldKey));
                if (v.isEmpty()) v = safeStr(c.getSkillLevel());
                return v;
            }
            case "ext_zygz":
            case "ext_zymcd": {
                String v = safeStr(extra.get(fieldKey));
                if (v.isEmpty()) v = safeStr(c.getProfession());
                return v;
            }
            default:
                // 自定义字段从extra_json中取
                return safeStr(extra.get(fieldKey));
        }
    }

    /** 安全转字符串,null 转空字符串 */
    private String safeStr(String s) {
        return s != null ? s : "";
    }

    /**
     * 任意对象安全转字符串。
     * <p>extraJson 解析出来的值可能是 Integer/Long/Double/Boolean 等非 String 类型，
     * 直接 (String) 强转会抛 ClassCastException（见 exportCertificates 的兜底取值）。
     * 这里统一用 String.valueOf 转换，null 返回空串。</p>
     */
    private String safeStr(Object o) {
        if (o == null) return "";
        return String.valueOf(o);
    }

    /** 获取证书二维码URL配置(第一条) */
    private CertificateUrlConfig getUrlConfigForExport() {
        List<CertificateUrlConfig> list = certificateUrlConfigMapper.selectList(null);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 构建二维码值映射(用于URL模板替换占位符) */
    private Map<String, String> buildQrValueMap(Certificate cert, Map<String, Object> extra) {
        Map<String, String> map = new HashMap<>();
        map.put("name", safeStr(cert.getName()));
        map.put("idcard", safeStr(cert.getIdCard()));
        map.put("profession", safeStr(cert.getProfession()));
        map.put("skilllevel", safeStr(cert.getSkillLevel()));
        if (cert.getIssueDate() != null) {
            map.put("issuedate", cert.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        map.put("certno", safeStr(cert.getCertNo()));
        map.put("studentno", safeStr(cert.getStudentNo()));
        map.put("agency", safeStr(cert.getAgency()));
        // 补充extraJson中的自定义字段
        if (extra != null) {
            for (Map.Entry<String, Object> e : extra.entrySet()) {
                map.put(e.getKey().toLowerCase(), safeStr(e.getValue()));
            }
        }
        // 补充证书用户属性(手机号/专业名称)
        if (StringUtils.hasText(cert.getIdCard())) {
            try {
                CertificateUser cu = certificateUserMapper.selectOne(
                        new LambdaQueryWrapper<CertificateUser>()
                                .eq(CertificateUser::getIdCard, cert.getIdCard()).last("LIMIT 1"));
                if (cu != null) {
                    map.putIfAbsent("phone", safeStr(cu.getPhone()));
                    map.putIfAbsent("professionname", safeStr(cu.getProfessionName()));
                }
            } catch (Exception ignore) { }
        }
        return map;
    }

    /** 按URL配置规则生成二维码URL(同CertificateGenerateServiceImpl.resolveQrUrl) */
    private String resolveQrUrlForExport(String template, Map<String, String> valueMap, String fallbackUrl) {
        if (!StringUtils.hasText(template)) {
            return safeStr(fallbackUrl);
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{(\\w+)}").matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).toLowerCase();
            String val = valueMap.getOrDefault(key, "");
            String encoded;
            try {
                encoded = java.net.URLEncoder.encode(val, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                encoded = val;
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(encoded));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 解析 extraJson 为 Map */
    private Map<String, Object> parseExtraJsonToMap(String extraJson) {
        if (!StringUtils.hasText(extraJson)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> m = MAPPER.readValue(extraJson, new TypeReference<Map<String, Object>>() {});
            return m != null ? m : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @Override
    public void switchExamQr(List<Long> ids, Boolean allSelected, Integer enabled) {
        if (enabled == null) throw new BusinessException("enabled 不能为空");
        int v = enabled == 1 ? 1 : 0;
        if (Boolean.TRUE.equals(allSelected)) {
            this.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Certificate>()
                    .set(Certificate::getExamQrEnabled, v)
                    .set(Certificate::getUpdateTime, LocalDateTime.now()));
        } else if (ids != null && !ids.isEmpty()) {
            this.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Certificate>()
                    .in(Certificate::getId, ids)
                    .set(Certificate::getExamQrEnabled, v)
                    .set(Certificate::getUpdateTime, LocalDateTime.now()));
        } else {
            throw new BusinessException("请选择要操作的证书");
        }
    }

    // ============ 私有方法 ============

    private void copyDtoToEntity(CertificateDTO dto, Certificate c) {
        c.setCertNo(dto.getCertNo());
        c.setStudentNo(dto.getStudentNo());
        c.setName(dto.getName());
        c.setIdCard(dto.getIdCard());
        c.setGender(dto.getGender());
        c.setProfession(dto.getProfession());
        c.setSkillLevel(dto.getSkillLevel());
        c.setIssueDate(dto.getIssueDate());
        c.setCertNoPrefix(dto.getCertNoPrefix());
        c.setCertNoMiddle(dto.getCertNoMiddle());
        c.setStudentNoPrefix(dto.getStudentNoPrefix());
        c.setStudentNoMiddle(dto.getStudentNoMiddle());
        c.setAgency(dto.getAgency());
        c.setAgencyFee(dto.getAgencyFee());
        c.setQrUrl1(dto.getQrUrl1());
        c.setQrUrl2(dto.getQrUrl2());
        c.setQrUrl3(dto.getQrUrl3());
        c.setExamQrUrl(dto.getExamQrUrl());
        c.setExamQrEnabled(dto.getExamQrEnabled() == null ? 0 : dto.getExamQrEnabled());
        c.setRemark(dto.getRemark());
        c.setTemplateId(dto.getTemplateId());
        c.setCertType(dto.getCertType());
    }

    @Override
    public CertificateImportRow parseImportRow(Map<Integer, String> row, int rowIndex) {
        return parseRow(row, rowIndex);
    }

    private CertificateImportRow parseRow(Map<Integer, String> row, int rowIndex) {
        // 统一20列模板字段顺序(与用户提供的模板一致):
        // 0序号 1姓名 2性别 3证件号码 4职业名称 5技能等级
        // 6证书编号 7颁发日期 8报单机构 9报单机构费用统计
        // 10培训专业 11培训学时 12培训日期
        // 13理论成绩 14实操成绩 15综合测评
        // 16证书二维码1 17证书二维码2 18证书二维码3 19学员考试二维码
        CertificateImportRow r = new CertificateImportRow();
        r.setRowIndex(rowIndex + 1);
        // 序号(0) 选填,自动生成,不存储
        r.setName(trimToNull(row.get(1)));
        r.setGenderStr(trimToNull(row.get(2)));
        r.setIdCard(trimToNull(row.get(3)));
        r.setProfession(trimToNull(row.get(4)));
        r.setSkillLevel(trimToNull(row.get(5)));
        r.setCertNo(trimToNull(row.get(6)));
        // 颁发日期(7)
        if (StringUtils.hasText(row.get(7))) {
            r.setIssueDate(parseDate(row.get(7)));
        }
        r.setAgency(trimToNull(row.get(8)));
        r.setAgencyFee(toDecimal(row.get(9)));
        r.setTrainingMajor(trimToNull(row.get(10)));
        r.setTrainingHours(trimToNull(row.get(11)));
        r.setTrainingDate(trimToNull(row.get(12)));
        r.setTheoryScore(trimToNull(row.get(13)));
        r.setPracticalScore(trimToNull(row.get(14)));
        r.setComprehensiveEvaluation(trimToNull(row.get(15)));
        r.setQr1(trimToNull(row.get(16)));
        r.setQr2(trimToNull(row.get(17)));
        r.setQr3(trimToNull(row.get(18)));
        r.setExamQr(trimToNull(row.get(19)));
        // 证书类型(第21列,选填;为空则不设置)
        r.setCertType(trimToNull(row.get(20)));
        // ============ 字段限制/校验 ============
        List<String> errs = new ArrayList<>();
        if (r.getName() == null) {
            errs.add("姓名为空");
        } else if (r.getName().length() > 50) {
            errs.add("姓名超长(最多50字)");
        }
        if (r.getIdCard() == null) {
            errs.add("证件号码为空");
        }
        // 身份证格式/校验位不正确:不拦截,正常导入(前端会用浅红色背景标注异常身份证)
        if (r.getProfession() == null) {
            errs.add("职业名称为空");
        }
        // 报单机构: 新模板必填
        if (r.getAgency() == null) {
            errs.add("报单机构为空");
        }
        // 报单机构费用: 新模板必填
        if (r.getAgencyFee() == null) {
            errs.add("报单机构费用为空");
        }
        // 性别: 空则自动推断,非空时必须合法
        if (r.getGenderStr() != null && !"男".equals(r.getGenderStr()) && !"女".equals(r.getGenderStr())) {
            errs.add("性别只能为'男'或'女'");
        }
        // 理论成绩: 非空时必须为数字
        if (r.getTheoryScore() != null) {
            try { Double.parseDouble(r.getTheoryScore().trim()); }
            catch (NumberFormatException e) { errs.add("理论成绩不是数字"); }
        }
        if (!errs.isEmpty()) {
            r.setError(String.join("; ", errs));
        }
        return r;
    }

    private CertificateDTO toDto(CertificateImportRow r) {
        CertificateDTO d = new CertificateDTO();
        d.setName(r.getName());
        d.setIdCard(r.getIdCard());
        // 性别:优先用 Excel 中的"男"/"女",其次从身份证号推断
        if ("男".equals(r.getGenderStr()) || "1".equals(r.getGenderStr())) {
            d.setGender(1);
        } else if ("女".equals(r.getGenderStr()) || "2".equals(r.getGenderStr())) {
            d.setGender(2);
        } else {
            d.setGender(CertificateNumberServiceImpl.extractGenderFromIdCard(r.getIdCard()));
        }
        d.setProfession(r.getProfession());
        // ============ 默认值补充(按新模板规则) ============
        // 技能等级: 空时默认"高级"
        d.setSkillLevel(StringUtils.hasText(r.getSkillLevel()) ? r.getSkillLevel() : "高级");
        // 证书编号: 新模板提供了证书编号列,有值则使用,空则在绑定模板时自动生成
        d.setCertNo(StringUtils.hasText(r.getCertNo()) ? r.getCertNo() : null);
        d.setIssueDate(r.getIssueDate());
        // 编号前缀/中段: 仅在 Excel 显式提供时使用,否则留空,由编号配置(fillStudentNoIfEmpty)回落到"编号配置"表
        d.setCertNoPrefix(StringUtils.hasText(r.getCertNoPrefix()) ? r.getCertNoPrefix() : null);
        d.setCertNoMiddle(StringUtils.hasText(r.getCertNoMiddle()) ? r.getCertNoMiddle() : null);
        d.setStudentNoPrefix(StringUtils.hasText(r.getStudentNoPrefix()) ? r.getStudentNoPrefix() : null);
        d.setStudentNoMiddle(StringUtils.hasText(r.getStudentNoMiddle()) ? r.getStudentNoMiddle() : null);
        d.setAgency(r.getAgency());
        // 报单机构费用: 空时默认 0
        d.setAgencyFee(r.getAgencyFee() != null ? r.getAgencyFee() : BigDecimal.ZERO);
        d.setQrUrl1(r.getQr1());
        d.setQrUrl2(r.getQr2());
        d.setQrUrl3(r.getQr3());
        d.setExamQrUrl(r.getExamQr());
        d.setRemark(r.getRemark());
        // ============ 成绩和培训信息默认值 ============
        Map<String, Object> extra = new HashMap<>();
        // 培训专业: 空时默认与职业名称一致
        extra.put("trainingMajor", StringUtils.hasText(r.getTrainingMajor()) ? r.getTrainingMajor() : r.getProfession());
        // 培训学时: 没有填写就不生成(不设置默认值)
        if (StringUtils.hasText(r.getTrainingHours())) {
            extra.put("trainingHours", r.getTrainingHours());
        }
        // 培训日期: 没有填写就不生成
        if (StringUtils.hasText(r.getTrainingDate())) {
            extra.put("trainingDate", r.getTrainingDate());
        }
        // 考试时间: 空时默认当天日期
        if (StringUtils.hasText(r.getExamTime())) {
            extra.put("examTime", r.getExamTime());
        } else {
            extra.put("examTime", LocalDate.now().toString());
        }
        // 理论成绩: 使用导入模板的原始值
        String theoryScoreVal = null;
        if (StringUtils.hasText(r.getTheoryScore())) {
            theoryScoreVal = r.getTheoryScore().trim();
            extra.put("theoryScore", theoryScoreVal);
        }
        // 实操成绩: 使用导入模板的原始值;若为空但理论成绩有值,则按理论成绩±10自动生成(与考试后回写逻辑一致)
        if (StringUtils.hasText(r.getPracticalScore())) {
            extra.put("practicalScore", r.getPracticalScore());
        } else if (theoryScoreVal != null) {
            try {
                double theory = Double.parseDouble(theoryScoreVal);
                int min = Math.max(60, (int) Math.floor(theory - 10));
                int max = Math.min(100, (int) Math.ceil(theory + 10));
                if (max <= min) max = min + 1;
                int ps = java.util.concurrent.ThreadLocalRandom.current().nextInt(min, max + 1);
                extra.put("practicalScore", String.valueOf(ps));
            } catch (NumberFormatException e) {
                // 理论成绩非数字时,基于身份证号hash生成稳定值(60-100)
                int hash = Math.abs(r.getIdCard().hashCode());
                int ps = 60 + (hash % 40);
                extra.put("practicalScore", String.valueOf(ps));
            }
        }
        // 综合测评: 使用导入模板的原始值;若为空但理论成绩有值,则默认"合格"(与考试后回写逻辑一致)
        if (StringUtils.hasText(r.getComprehensiveEvaluation())) {
            extra.put("comprehensiveEvaluation", r.getComprehensiveEvaluation());
        } else if (theoryScoreVal != null) {
            extra.put("comprehensiveEvaluation", "合格");
        }
        // 手机号码
        if (StringUtils.hasText(r.getPhone())) {
            extra.put("phone", r.getPhone());
        }
        // 证书类型
        if (StringUtils.hasText(r.getCertType())) {
            extra.put("cert_type", r.getCertType().trim());
            d.setCertType(r.getCertType().trim());
        }
        d.setExtra(extra);
        return d;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static BigDecimal toDecimal(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析日期,支持多种格式:
     * 2025-10-10 / 2025-1-1 / 2025/01/01 / 2025/1/1 / 2025.10.10 / 2025.1.1
     * 20251010 / 2025年10月10日 / 2025年1月1日 / 2025年1月1 / 2026年10月10号
     * Excel序列号
     */
    public static LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.trim();
        // Excel 序列号(纯数字或带小数,如 45678 或 45678.0)
        if (s.matches("\\d+(\\.\\d+)?")) {
            try {
                double v = Double.parseDouble(s);
                return LocalDate.of(1899, 12, 30).plusDays((long) v);
            } catch (Exception ignored) { }
        }
        // 去掉时间部分(如 2025-10-10 00:00:00 / 2025/10/10 12:30:00)
        String dateOnly = s;
        if (s.matches(".*\\s+\\d{1,2}:\\d{2}(:\\d{2})?.*")) {
            dateOnly = s.split("\\s+")[0];
        }
        // 规范化: 多个连续斜杠 → 单个斜杠
        String normalized = dateOnly.replaceAll("/{2,}", "/");
        // 尝试所有预定义格式
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(normalized, f);
            } catch (Exception ignored) { }
        }
        // 正则提取年月日(兜底,支持各种分隔符和后缀)
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{4})\\D+(\\d{1,2})\\D+(\\d{1,2})").matcher(normalized);
        if (m.find()) {
            try {
                int y = Integer.parseInt(m.group(1));
                int mo = Integer.parseInt(m.group(2));
                int d = Integer.parseInt(m.group(3));
                return LocalDate.of(y, mo, d);
            } catch (Exception ignored) { }
        }
        return null;
    }

    /**
     * 身份证校验位校验
     */
    public static boolean isValidIdCard(String id) {
        if (id == null || id.length() != 18) return false;
        if (!id.matches("^\\d{17}[\\dXx]$")) return false;
        int[] weight = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] check = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id.charAt(i) - '0') * weight[i];
        }
        char c = check[sum % 11];
        return c == Character.toUpperCase(id.charAt(17));
    }

    /**
     * 从学生管理同步数据到证书表(certificate)。
     * 同步逻辑:
     * 1. 按身份证号查找学生,每个专业对应一条证书记录
     * 2. 学生有多少个专业,证书表就应有多少条数据(缺的补建)
     * 3. 学生历史专业变动后,证书表中多余的专业数据需删除
     * 4. 专业为空的数据不创建,已有的专业为空数据也一并清理
     * @return 新创建的记录数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromStudents(String certType) {
        LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
        if (certType != null && !certType.isEmpty()) {
            queryWrapper.eq(Student::getCertType, certType);
        }
        List<Student> students = studentMapper.selectList(queryWrapper);
        if (students.isEmpty()) return 0;
        // 加载专业 id -> name 映射(直接从 profession 表查,不依赖 JOIN 映射)
        Map<Long, String> professionNameMap = professionMapper.selectList(null).stream()
                .collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));
        int created = 0;
        for (Student student : students) {
            String idCard = StringUtils.hasText(student.getIdCard()) ? student.getIdCard().trim() : null;
            if (idCard == null) continue;
            String name = student.getName() != null ? student.getName() : "";

            // ====== 1. 获取学生当前的所有专业名称 ======
            Set<String> expectedProfessions = new LinkedHashSet<>();
            List<StudentProfession> sps = student.getId() == null
                    ? Collections.emptyList()
                    : studentProfessionMapper.selectList(
                            new LambdaQueryWrapper<StudentProfession>()
                                    .eq(StudentProfession::getStudentId, student.getId()));
            for (StudentProfession sp : sps) {
                if (sp.getProfessionId() != null) {
                    String profName = professionNameMap.get(sp.getProfessionId());
                    if (StringUtils.hasText(profName)) {
                        expectedProfessions.add(profName.trim());
                    }
                }
            }
            // 兜底: 没有 student_profession 关联记录时,用 student.professionId
            if (expectedProfessions.isEmpty() && student.getProfessionId() != null) {
                String profName = professionNameMap.get(student.getProfessionId());
                if (StringUtils.hasText(profName)) {
                    expectedProfessions.add(profName.trim());
                }
            }

            // ====== 2. 学生没有专业 → 不创建证书记录,并清理该学生专业为空的脏数据 ======
            if (expectedProfessions.isEmpty()) {
                this.remove(new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getIdCard, idCard)
                        .eq(Certificate::getName, name)
                        .and(w -> w.isNull(Certificate::getProfession).or().eq(Certificate::getProfession, "")));
                continue;
            }

            // ====== 3. 为每个专业创建证书记录(姓名+身份证+专业 相同则跳过) ======
            for (String profName : expectedProfessions) {
                created += createIfNotExists(student, idCard, profName, certType);
            }

            // ====== 4. 删除该学生多余的证书记录(专业不在学生当前专业列表中的) ======
            List<Certificate> existingCerts = this.list(new LambdaQueryWrapper<Certificate>()
                    .eq(Certificate::getIdCard, idCard)
                    .eq(Certificate::getName, name));
            Set<String> finalExpected = expectedProfessions;
            List<Long> toDelete = existingCerts.stream()
                    .filter(cert -> {
                        String certProf = cert.getProfession();
                        // 专业为空 → 删除
                        if (!StringUtils.hasText(certProf)) return true;
                        // 专业不在学生当前专业列表中 → 删除(历史专业变动遗留)
                        return !finalExpected.contains(certProf.trim());
                    })
                    .map(Certificate::getId)
                    .collect(Collectors.toList());
            if (!toDelete.isEmpty()) {
                this.removeByIds(toDelete);
            }
        }
        return created;
    }

    // ==================== 学生管理专用 25 列模板(独立于证书管理 20 列模板) ====================

    @Override
    public void downloadStudentTemplate(HttpServletResponse response, String sheetName, String fileName) throws Exception {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        // 25列学生导入模板表头
        List<List<String>> head = new ArrayList<>();
        head.add(Arrays.asList("序号（选填）"));
        head.add(Arrays.asList("姓名（必填）"));
        head.add(Arrays.asList("证件号码（必填）"));
        head.add(Arrays.asList("职业名称（必填）"));
        head.add(Arrays.asList("技能等级（选填）"));
        head.add(Arrays.asList("颁发日期（选填）"));
        head.add(Arrays.asList("报单机构（必填）"));
        head.add(Arrays.asList("报单机构费用统计（必填）"));
        head.add(Arrays.asList("培训专业（选填）"));
        head.add(Arrays.asList("培训学时（选填）"));
        head.add(Arrays.asList("培训日期（选填）"));
        head.add(Arrays.asList("考试时间（选填）"));
        head.add(Arrays.asList("理论成绩（选填）"));
        head.add(Arrays.asList("实操成绩（选填）"));
        head.add(Arrays.asList("综合测评（选填）"));
        head.add(Arrays.asList("手机号码（选填）"));
        head.add(Arrays.asList("性别（选填）"));
        head.add(Arrays.asList("证书编号前缀字母（选填）"));
        head.add(Arrays.asList("证书编号中段字母（选填）"));
        head.add(Arrays.asList("学员编号前缀字母（选填）"));
        head.add(Arrays.asList("学员编号中段字母（选填）"));
        head.add(Arrays.asList("证书二维码生成1（选填）"));
        head.add(Arrays.asList("证书二维码生成2（选填）"));
        head.add(Arrays.asList("证书二维码生成3（选填）"));
        head.add(Arrays.asList("学员考试二维码（选填）"));
        // 证书类型: 动态获取系统设置中的证书类型名称
        String typeHint = "证书类型（必填）";
        try {
            List<String> typeNames = certificateTypeService.listAll().stream()
                    .filter(t -> t.getStatus() == null || t.getStatus() == 1)
                    .map(t -> t.getName())
                    .collect(java.util.stream.Collectors.toList());
            if (!typeNames.isEmpty()) {
                typeHint = "证书类型（必填，可选值：" + String.join("/", typeNames) + "）";
            }
        } catch (Exception e) { log.warn("学生导入模板获取证书类型失败: {}", e.getMessage()); }
        head.add(Arrays.asList(typeHint));
        // 示例行
        List<List<Object>> sample = new ArrayList<>();
        List<Object> s1 = new ArrayList<>();
        s1.add(1);
        s1.add("张三");
        s1.add("130627198905076632");
        s1.add("电工");
        s1.add("三级/高级");
        s1.add("2026-05-15");
        s1.add("北京某培训机构");
        s1.add(new BigDecimal("1000.00"));
        s1.add("电工");
        s1.add("48");
        s1.add("2026年5月");
        s1.add("2026-05-15");
        s1.add("85");
        s1.add("85");
        s1.add("合格");
        s1.add("13800138000");
        s1.add("男");
        s1.add("ZGZH");
        s1.add("M");
        s1.add("RCCP");
        s1.add("B");
        s1.add("");
        s1.add("");
        s1.add("");
        s1.add("");
        sample.add(s1);
        try (OutputStream os = response.getOutputStream()) {
            EasyExcel.write(os).head(head).sheet(sheetName).doWrite(sample);
        }
    }

    @Override
    public CertificateImportResult parseStudentExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("文件为空");
        CertificateImportResult result = new CertificateImportResult();
        List<CertificateImportRow> failed = new ArrayList<>();
        List<CertificateImportRow> pending = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override public void invoke(Map<Integer, String> row, AnalysisContext ctx) {
                    if (row == null) return;
                    CertificateImportRow r = parseRow25(row, ctx.readRowHolder().getRowIndex());
                    if (r.getError() != null) {
                        failed.add(r);
                    } else {
                        pending.add(r);
                    }
                }
                @Override public void doAfterAllAnalysed(AnalysisContext ctx) { /* noop */ }
            }).sheet().doRead();
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败: " + e.getMessage());
        }
        result.setFailedRows(failed);
        result.setFailCount(failed.size());
        result.setSuccessCount(pending.size());
        result.setPendingRows(pending);
        result.setDryRun(false);
        return result;
    }

    /**
     * 26列模板解析(学生管理专用,独立于证书管理的20列 parseRow)
     * 列顺序: 0序号 1姓名 2证件号码 3职业名称 4技能等级 5颁发日期
     * 6报单机构 7报单机构费用统计 8培训专业 9培训学时 10培训日期
     * 11考试时间 12理论成绩 13实操成绩 14综合测评
     * 15手机号码 16性别 17证书编号前缀 18证书编号中段
     * 19学员编号前缀 20学员编号中段 21证书二维码1 22证书二维码2
     * 23证书二维码3 24学员考试二维码 25证书类型
     */
    private CertificateImportRow parseRow25(Map<Integer, String> row, int rowIndex) {
        CertificateImportRow r = new CertificateImportRow();
        r.setRowIndex(rowIndex + 1);
        r.setName(trimToNull(row.get(1)));
        r.setIdCard(trimToNull(row.get(2)));
        r.setProfession(trimToNull(row.get(3)));
        r.setSkillLevel(trimToNull(row.get(4)));
        if (StringUtils.hasText(row.get(5))) {
            r.setIssueDate(parseDate(row.get(5)));
        }
        r.setAgency(trimToNull(row.get(6)));
        r.setAgencyFee(toDecimal(row.get(7)));
        r.setTrainingMajor(trimToNull(row.get(8)));
        r.setTrainingHours(trimToNull(row.get(9)));
        r.setTrainingDate(trimToNull(row.get(10)));
        r.setExamTime(trimToNull(row.get(11)));
        r.setTheoryScore(trimToNull(row.get(12)));
        r.setPracticalScore(trimToNull(row.get(13)));
        r.setComprehensiveEvaluation(trimToNull(row.get(14)));
        r.setPhone(trimToNull(row.get(15)));
        r.setGenderStr(trimToNull(row.get(16)));
        r.setCertNoPrefix(trimToNull(row.get(17)));
        r.setCertNoMiddle(trimToNull(row.get(18)));
        r.setStudentNoPrefix(trimToNull(row.get(19)));
        r.setStudentNoMiddle(trimToNull(row.get(20)));
        r.setQr1(trimToNull(row.get(21)));
        r.setQr2(trimToNull(row.get(22)));
        r.setQr3(trimToNull(row.get(23)));
        r.setExamQr(trimToNull(row.get(24)));
        r.setCertType(trimToNull(row.get(25)));
        // ============ 字段限制/校验 ============
        List<String> errs = new ArrayList<>();
        if (r.getName() == null) {
            errs.add("姓名为空");
        } else if (r.getName().length() > 50) {
            errs.add("姓名超长(最多50字)");
        }
        if (r.getIdCard() == null) {
            errs.add("证件号码为空");
        }
        if (r.getProfession() == null) {
            errs.add("职业名称为空");
        }
        if (r.getAgency() == null) {
            errs.add("报单机构为空");
        }
        if (r.getAgencyFee() == null) {
            errs.add("报单机构费用为空");
        }
        if (r.getGenderStr() != null && !"男".equals(r.getGenderStr()) && !"女".equals(r.getGenderStr())) {
            errs.add("性别只能为'男'或'女'");
        }
        if (r.getTheoryScore() != null) {
            try { Double.parseDouble(r.getTheoryScore().trim()); }
            catch (NumberFormatException e) { errs.add("理论成绩不是数字"); }
        }
        // 证书类型校验: 必须与系统设置中的证书类型名称一致
        if (r.getCertType() != null) {
            try {
                List<String> validTypes = certificateTypeService.listAll().stream()
                        .filter(t -> t.getStatus() == null || t.getStatus() == 1)
                        .map(t -> t.getName())
                        .collect(java.util.stream.Collectors.toList());
                if (!validTypes.contains(r.getCertType())) {
                    errs.add("证书类型'" + r.getCertType() + "'不在系统设置中(可选: " + String.join("/", validTypes) + ")");
                }
            } catch (Exception e) {
                // 查询失败不阻断导入
            }
        }
        if (!errs.isEmpty()) {
            r.setError(String.join("; ", errs));
        }
        return r;
    }

    /**
     * 检查学生是否已有证书记录(idCard + profession),没有则创建
     * @return 1=新建,0=已存在跳过
     */
    private int createIfNotExists(Student student, String idCard, String profession, String certType) {
        // 查是否已有 姓名+身份证号+专业 的证书记录(精确匹配,避免 like 误匹配)
        // 重复判断标准: 姓名 + 身份证号码 + 专业 三个字段相同即为重复,不再判断技能等级
        String trimmedProfession = StringUtils.hasText(profession) ? profession.trim() : null;
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getName, student.getName() != null ? student.getName() : "");
        w.eq(Certificate::getIdCard, idCard);
        if (StringUtils.hasText(trimmedProfession)) {
            w.eq(Certificate::getProfession, trimmedProfession);
        } else {
            w.and(ww -> ww.isNull(Certificate::getProfession).or().eq(Certificate::getProfession, ""));
        }
        if (this.count(w) > 0) return 0; // 已存在(姓名+身份证+专业重复),跳过

        // 创建新证书记录
        Certificate c = new Certificate();
        c.setName(student.getName());
        c.setIdCard(idCard);
        c.setGender(CertificateNumberServiceImpl.extractGenderFromIdCard(idCard));
        c.setProfession(trimmedProfession);
        c.setSkillLevel("高级");
        c.setIssueDate(LocalDate.now());
        c.setCertNo(null); // 证书编号在绑定模板时生成
        c.setStudentNo(null); // 下方自动生成
        // 设置证书类型(优先用传入的 certType,其次用学生自身的 certType)
        String effectiveCertType = StringUtils.hasText(certType) ? certType : student.getCertType();
        if (StringUtils.hasText(effectiveCertType)) {
            c.setCertType(effectiveCertType.trim());
        }
        // 自动生成学员编号(日期取自颁发日期=今天)
        numberService.fillStudentNoIfEmpty(c);
        // extra_json: 成绩为空(前端显示横杠)
        Map<String, Object> extra = new HashMap<>();
        extra.put("trainingMajor", trimmedProfession != null ? trimmedProfession : "");
        extra.put("examTime", LocalDate.now().toString());
        // 不设置 theoryScore/practicalScore/comprehensiveEvaluation(空值=横杠)
        try {
            c.setExtraJson(MAPPER.writeValueAsString(extra));
        } catch (JsonProcessingException e) {
            // 忽略
        }
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        c.setUploadTime(LocalDateTime.now());
        this.save(c);

        // ====== 自动绑定证书模板: 按 certType 匹配同名模板 ======
        if (StringUtils.hasText(c.getCertType()) && c.getTemplateId() == null) {
            try {
                CertificateTemplate matched = templateMapper.selectOne(
                        new LambdaQueryWrapper<CertificateTemplate>()
                                .eq(CertificateTemplate::getName, c.getCertType().trim())
                                .last("LIMIT 1"));
                if (matched != null) {
                    c.setTemplateId(matched.getId());
                    numberService.fillCertNoIfEmpty(c, matched.getId());
                    this.updateById(c);
                }
            } catch (Exception e) {
                // 自动绑定失败不阻断主流程
            }
        }
        return 1;
    }
}
