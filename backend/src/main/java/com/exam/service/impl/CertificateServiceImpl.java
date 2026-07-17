package com.exam.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
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

    @Override
    public PageResult<Certificate> page(Integer page, Integer size, String name,
                                        String idCard, String agency,
                                        String issueDateStart, String issueDateEnd) {
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .like(StringUtils.hasText(name), Certificate::getName, name)
                .like(StringUtils.hasText(idCard), Certificate::getIdCard, idCard)
                .like(StringUtils.hasText(agency), Certificate::getAgency, agency)
                .orderByDesc(Certificate::getCreateTime);
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
    public PageResult<Map<String, Object>> pageWithTemplateName(Integer page, Integer size,
                                                                String name, String idCard,
                                                                String agency,
                                                                String issueDateStart,
                                                                String issueDateEnd) {
        // 先调基础 page() 拿 cert 记录
        PageResult<Certificate> base = this.page(page, size, name, idCard, agency,
                issueDateStart, issueDateEnd);
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
        // 查询照片(严格按 certificateId 匹配,不回退到 idCard)
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
        // 转 Map
        List<Map<String, Object>> out = new ArrayList<>(records.size());
        for (Certificate c : records) {
            Map<String, Object> m = beanToMap(c);
            m.remove("extraJson");
            Long tplId = c.getTemplateId();
            m.put("templateName", tplId != null ? templateNameMap.get(tplId) : null);
            // 照片URL: 严格按 certificateId 取,不回退到 idCard
            m.put("photoUrl", photoUrlByCertId.get(c.getId()));
            parseExtraJson(c.getExtraJson(), m);
            out.add(m);
        }
        return new PageResult<>(base.getTotal(), base.getPage(), base.getSize(), out);
    }

    /**
     * 新增:支持按专业(profession) 模糊过滤
     * 行为与 pageWithTemplateName 一致,只是多一个 profession 过滤参数
     */
    public PageResult<Map<String, Object>> pageWithTemplateNameAndProfession(Integer page, Integer size,
                                                                            String name, String idCard,
                                                                            String agency,
                                                                            String profession,
                                                                            String issueDateStart,
                                                                            String issueDateEnd) {
        PageResult<Certificate> base = this.pageWithProfession(page, size, name, idCard, agency, profession,
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
        // 查询照片(严格按 certificateId 匹配,不回退到 idCard)
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
            m.put("photoUrl", photoUrlByCertId2.get(c.getId()));
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
                m.put("issueDateStr", c.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
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
                    map.put("theoryScore", extra.get("theoryScore"));
                    map.put("practicalScore", extra.get("practicalScore"));
                    map.put("comprehensiveEvaluation", extra.get("comprehensiveEvaluation"));
                }
            } catch (Exception ignored) {
            }
        }
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
                .orderByDesc(Certificate::getCreateTime);
        if (StringUtils.hasText(issueDateStart)) {
            w.ge(Certificate::getIssueDate, parseDate(issueDateStart));
        }
        if (StringUtils.hasText(issueDateEnd)) {
            w.le(Certificate::getIssueDate, parseDate(issueDateEnd));
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

    @Override
    public boolean add(CertificateDTO dto) {
        Certificate c = new Certificate();
        copyDtoToEntity(dto, c);
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
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        // 导入时间(新增/导入时自动填写;以导入时间作为后续筛选/排序依据)
        c.setUploadTime(LocalDateTime.now());
        this.save(c);
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
                .set(dto.getStudentNo() != null, Certificate::getStudentNo, dto.getStudentNo()));
        // 绑定模板时(编辑页选择模板),若证书编号仍为空则自动生成(证书编号在绑定模板时才生成)
        if (dto.getTemplateId() != null) {
            Certificate latest = this.getById(dto.getId());
            if (latest != null && !StringUtils.hasText(latest.getCertNo())) {
                numberService.fillCertNoIfEmpty(latest);
                this.update(new LambdaUpdateWrapper<Certificate>()
                        .eq(Certificate::getId, dto.getId())
                        .set(Certificate::getCertNo, latest.getCertNo())
                        .set(Certificate::getCertNoPrefix, latest.getCertNoPrefix())
                        .set(Certificate::getCertNoMiddle, latest.getCertNoMiddle()));
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
        if (importResult.getFailedRows() != null) {
            result.getFailedRows().addAll(importResult.getFailedRows());
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
                        // 实操成绩在理论成绩 ±10 范围内,但不能低于60,也不能高于100
                        int min = Math.max(60, (int) Math.floor(theory - 10));
                        int max = Math.min(100, (int) Math.ceil(theory + 10));
                        if (max <= min) max = min + 1;
                        int practicalScore = random.nextInt(min, max + 1);
                        extra.put("practicalScore", String.valueOf(practicalScore));
                    } catch (NumberFormatException e) {
                        // 理论成绩非数字时,实操成绩基于身份证号hash生成稳定值(60-100)
                        int hash = Math.abs(idCard.hashCode());
                        int practicalScore = 60 + (hash % 40);
                        extra.put("practicalScore", String.valueOf(practicalScore));
                    }
                }
                // 同时回写到 certificate 主表的 theoryScore 字段(始终覆盖,用户要求考试后刷新)
                Certificate update = new Certificate();
                update.setId(cert.getId());
                update.setTheoryScore(scoreStr);
                update.setExtraJson(MAPPER.writeValueAsString(extra));
                update.setUpdateTime(LocalDateTime.now());
                this.updateById(update);
            } catch (Exception e) {
                // 单条更新失败不影响其余
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
                .orderByDesc(Certificate::getCreateTime);
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
            r.setSuccessCount(0); r.setFailCount(0);
            return r;
        }
        int ok = 0, fail = 0;
        List<CertificateImportRow> failed = new ArrayList<>();
        for (CertificateDTO dto : rows) {
            try {
                boolean created = add(dto);
                if (created) {
                    // 证书导入时同步创建/更新证书用户(不创建学生记录)
                    certificateUserSyncService.syncFromCertificateData(
                            dto.getName(), dto.getIdCard(), dto.getGender(), dto.getProfession());
                    ok++;
                }
                // add 返回 false = 去重跳过,不计入成功也不计入失败
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
        r.setFailedRows(failed);
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
        sample.add(s1);
        try (OutputStream os = response.getOutputStream()) {
            EasyExcel.write(os).head(head).sheet(sheetName).doWrite(sample);
        }
    }

    /**
     * 导出证书数据(Excel,使用与导入模板完全相同的20列结构)
     * 按选中ID导出,或按筛选条件导出全部
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
                    .orderByDesc(Certificate::getCreateTime);
            if (StringUtils.hasText(issueDateStart)) {
                w.ge(Certificate::getIssueDate, parseDate(issueDateStart));
            }
            if (StringUtils.hasText(issueDateEnd)) {
                w.le(Certificate::getIssueDate, parseDate(issueDateEnd));
            }
            certs = this.list(w);
        }

        // 2. 构建与导入模板完全一致的20列表头
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

        // 3. 逐行构建导出数据(严格按模板20列顺序，与导入模板完全一致)
        // 0序号 1姓名 2性别 3证件号码 4职业名称 5技能等级
        // 6证书编号 7颁发日期 8报单机构 9报单机构费用统计
        // 10培训专业 11培训学时 12培训日期
        // 13理论成绩 14实操成绩 15综合测评
        // 16证书二维码1 17证书二维码2 18证书二维码3 19学员考试二维码
        // 获取证书二维码URL配置(统一配置,非每个证书单独设置)
        CertificateUrlConfig urlConfig = getUrlConfigForExport();
        List<List<Object>> dataList = new ArrayList<>();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        int idx = 1;
        for (Certificate c : certs) {
            Map<String, Object> extra = parseExtraJsonToMap(c.getExtraJson());

            // 构建二维码值映射
            Map<String, String> qrValueMap = buildQrValueMap(c, extra);
            // 按URL配置规则生成二维码URL
            String qr1 = resolveQrUrlForExport(urlConfig == null ? null : urlConfig.getQr1Template(), qrValueMap, c.getQrUrl1());
            String qr2 = resolveQrUrlForExport(urlConfig == null ? null : urlConfig.getQr2Template(), qrValueMap, c.getQrUrl2());
            String qr3 = resolveQrUrlForExport(urlConfig == null ? null : urlConfig.getQr3Template(), qrValueMap, c.getQrUrl3());

            List<Object> row = new ArrayList<>();
            row.add(idx++);                                              // 0.序号
            row.add(safeStr(c.getName()));                               // 1.姓名
            row.add(c.getGender() != null ? (c.getGender() == 1 ? "男" : (c.getGender() == 2 ? "女" : "")) : ""); // 2.性别
            row.add(safeStr(c.getIdCard()));                             // 3.证件号码
            row.add(safeStr(c.getProfession()));                         // 4.职业名称
            row.add(safeStr(c.getSkillLevel()));                         // 5.技能等级
            row.add(safeStr(c.getCertNo()));                             // 6.证书编号
            row.add(c.getIssueDate() != null ? c.getIssueDate().format(dateFmt) : ""); // 7.颁发日期
            row.add(safeStr(c.getAgency()));                             // 8.报单机构
            row.add(c.getAgencyFee() != null ? c.getAgencyFee().toPlainString() : ""); // 9.报单机构费用统计
            row.add(safeStr(extra.get("trainingMajor")));                // 10.培训专业
            row.add(safeStr(extra.get("trainingHours")));                // 11.培训学时
            row.add(safeStr(extra.get("trainingDate")));                 // 12.培训日期
            row.add(safeStr(c.getTheoryScore()));                        // 13.理论成绩
            if (row.get(13) == null || row.get(13).toString().isEmpty()) {
                row.set(13, safeStr(extra.get("theoryScore")));
            }
            row.add(safeStr(c.getPracticalScore()));                     // 14.实操成绩
            if (row.get(14) == null || row.get(14).toString().isEmpty()) {
                row.set(14, safeStr(extra.get("practicalScore")));
            }
            row.add(safeStr(c.getComprehensiveEvaluation()));            // 15.综合测评
            if (row.get(15) == null || row.get(15).toString().isEmpty()) {
                row.set(15, safeStr(extra.get("comprehensiveEvaluation")));
            }
            row.add(safeStr(qr1));                                         // 16.证书二维码生成1(从URL配置生成)
            row.add(safeStr(qr2));                                         // 17.证书二维码生成2(从URL配置生成)
            row.add(safeStr(qr3));                                         // 18.证书二维码生成3(从URL配置生成)
            row.add(safeStr(c.getExamQrUrl()));                            // 19.学员考试二维码
            dataList.add(row);
        }

        // 4. 写出Excel(直接用EasyExcel按20列表头输出,不使用模板,避免列错位)
        try {
            String fileName = URLEncoder.encode("证书用户数据下载", StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            try (OutputStream os = response.getOutputStream()) {
                EasyExcel.write(os).head(head).sheet("证书数据").doWrite(dataList);
            }
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
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
        // 理论成绩: 使用导入模板的原始值,空时不自动生成(前端显示横杠)
        if (StringUtils.hasText(r.getTheoryScore())) {
            extra.put("theoryScore", r.getTheoryScore().trim());
        }
        // 实操成绩: 使用导入模板的原始值,空时不自动生成(前端显示横杠)
        if (StringUtils.hasText(r.getPracticalScore())) {
            extra.put("practicalScore", r.getPracticalScore());
        }
        // 综合测评: 使用导入模板的原始值,空时不自动生成(前端显示横杠)
        if (StringUtils.hasText(r.getComprehensiveEvaluation())) {
            extra.put("comprehensiveEvaluation", r.getComprehensiveEvaluation());
        }
        // 手机号码
        if (StringUtils.hasText(r.getPhone())) {
            extra.put("phone", r.getPhone());
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
     * 按身份证号+专业维度检查,已存在的不重复创建。
     * @return 新创建的记录数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromStudents() {
        List<Student> students = studentMapper.selectList(null);
        if (students.isEmpty()) return 0;
        // 加载专业名称映射
        Map<Long, String> professionNameMap = professionMapper.selectList(null).stream()
                .collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));
        int created = 0;
        for (Student student : students) {
            String idCard = StringUtils.hasText(student.getIdCard()) ? student.getIdCard().trim() : null;
            if (idCard == null) continue;
            // 查学生的专业
            List<StudentProfession> sps = student.getId() == null
                    ? Collections.emptyList()
                    : studentProfessionMapper.selectByStudentId(student.getId());
            if (sps.isEmpty()) {
                // 没有专业关联:按 idCard 查是否已有证书(profession 为空的)
                created += createIfNotExists(student, idCard, null, professionNameMap);
            } else {
                for (StudentProfession sp : sps) {
                    String profName = sp.getProfessionName();
                    if (profName == null && sp.getProfessionId() != null) {
                        profName = professionNameMap.get(sp.getProfessionId());
                    }
                    created += createIfNotExists(student, idCard, profName, professionNameMap);
                }
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
     * 25列模板解析(学生管理专用,独立于证书管理的20列 parseRow)
     * 列顺序: 0序号 1姓名 2证件号码 3职业名称 4技能等级 5颁发日期
     * 6报单机构 7报单机构费用统计 8培训专业 9培训学时 10培训日期
     * 11考试时间 12理论成绩 13实操成绩 14综合测评
     * 15手机号码 16性别 17证书编号前缀 18证书编号中段
     * 19学员编号前缀 20学员编号中段 21证书二维码1 22证书二维码2
     * 23证书二维码3 24学员考试二维码
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
        if (!errs.isEmpty()) {
            r.setError(String.join("; ", errs));
        }
        return r;
    }

    /**
     * 检查学生是否已有证书记录(idCard + profession),没有则创建
     * @return 1=新建,0=已存在跳过
     */
    private int createIfNotExists(Student student, String idCard, String profession,
                                  Map<Long, String> professionNameMap) {
        // 查是否已有 姓名+身份证号+专业+级别 的证书记录
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getName, student.getName() != null ? student.getName() : "");
        w.eq(Certificate::getIdCard, idCard);
        if (StringUtils.hasText(profession)) {
            w.and(ww -> ww.eq(Certificate::getProfession, profession)
                          .or().like(Certificate::getProfession, profession));
        } else {
            w.and(ww -> ww.isNull(Certificate::getProfession).or().eq(Certificate::getProfession, ""));
        }
        w.eq(Certificate::getSkillLevel, "高级");
        if (this.count(w) > 0) return 0; // 已存在,跳过

        // 创建新证书记录
        Certificate c = new Certificate();
        c.setName(student.getName());
        c.setIdCard(idCard);
        c.setGender(CertificateNumberServiceImpl.extractGenderFromIdCard(idCard));
        c.setProfession(profession);
        c.setSkillLevel("高级");
        c.setIssueDate(LocalDate.now());
        c.setCertNo(null); // 证书编号在绑定模板时生成
        c.setStudentNo(null); // 下方自动生成
        // 自动生成学员编号(日期取自颁发日期=今天)
        numberService.fillStudentNoIfEmpty(c);
        // extra_json: 成绩为空(前端显示横杠)
        Map<String, Object> extra = new HashMap<>();
        extra.put("trainingMajor", profession != null ? profession : "");
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
        return 1;
    }
}
