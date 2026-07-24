package com.exam.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateUser;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.CertificateUserMapper;
import com.exam.service.CertificateUserSyncService;
import com.exam.vo.CertificateUserExportVO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 证书用户管理(由"用户管理"中的学员数据定时同步而来)
 */
@RestController
@RequestMapping("/admin/certificate/user")
public class CertificateUserController {

    @Autowired
    private CertificateUserMapper certificateUserMapper;
    @Autowired
    private CertificateUserSyncService certificateUserSyncService;
    @Autowired
    private CertificateMapper certificateMapper;
    @Autowired
    private com.exam.service.CertificateService certificateService;

    /**
     * 分页查询证书用户
     */
    @GetMapping("/page")
    public Result<PageResult<CertificateUser>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) String certType) {
        LambdaQueryWrapper<CertificateUser> wrapper = new LambdaQueryWrapper<CertificateUser>()
                .orderByDesc(CertificateUser::getSyncTime);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword;
            wrapper.and(w -> w.like(CertificateUser::getName, kw)
                    .or().like(CertificateUser::getPhone, kw)
                    .or().like(CertificateUser::getIdCard, kw)
                    .or().like(CertificateUser::getProfessionName, kw));
        }
        if (StringUtils.hasText(idCard)) {
            wrapper.like(CertificateUser::getIdCard, idCard);
        }
        if (StringUtils.hasText(certType)) {
            wrapper.eq(CertificateUser::getCertType, certType);
        }
        Page<CertificateUser> p = new Page<>(page, size);
        Page<CertificateUser> result = certificateUserMapper.selectPage(p, wrapper);
        return Result.success(new PageResult<>(result));
    }

    /**
     * 手动触发一次全量同步:
     * 1. 同步学生数据到 certificate_user 表
     * 2. 同步学生数据到 certificate 表(界面实际展示的数据源)
     */
    @PostMapping("/sync")
    public Result<Map<String, Object>> sync(@RequestParam(required = false) String certType) {
        // 1. 同步到 certificate_user 表
        int userCount = certificateUserSyncService.syncAll(certType);
        // 2. 同步到 certificate 表(界面展示的数据源,创建缺失的证书记录)
        int certCount = certificateService.syncFromStudents(certType);
        Map<String, Object> data = new HashMap<>();
        data.put("synced", userCount);
        data.put("created", certCount);
        return Result.success(data);
    }

    /**
     * 手动新增证书用户(支持录入证书类型等属性,可为空)
     */
    @PostMapping
    public Result<CertificateUser> add(@RequestBody CertificateUser user) {
        if (!StringUtils.hasText(user.getName())) {
            throw new BusinessException("姓名不能为空");
        }
        user.setId(null);
        user.setSyncTime(java.time.LocalDateTime.now());
        certificateUserMapper.insert(user);
        return Result.success(user);
    }

    /**
     * 修改证书用户(支持修改证书类型等属性)
     */
    @PutMapping
    public Result<CertificateUser> update(@RequestBody CertificateUser user) {
        if (user.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        CertificateUser exist = certificateUserMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException("证书用户不存在");
        }
        if (user.getName() != null) exist.setName(user.getName());
        if (user.getIdCard() != null) exist.setIdCard(user.getIdCard());
        if (user.getPhone() != null) exist.setPhone(user.getPhone());
        if (user.getProfessionName() != null) exist.setProfessionName(user.getProfessionName());
        if (user.getCertType() != null) exist.setCertType(user.getCertType());
        if (user.getGender() != null) exist.setGender(user.getGender());
        certificateUserMapper.updateById(exist);
        return Result.success(exist);
    }

    /**
     * 删除证书用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        certificateUserMapper.deleteById(id);
        return Result.success(null);
    }

    /**
     * 导出证书用户数据(使用指定模板文件填充数据)
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String idCard,
                       @RequestParam(required = false) String certType,
                       HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<CertificateUser> wrapper = new LambdaQueryWrapper<CertificateUser>()
                .orderByDesc(CertificateUser::getSyncTime);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword;
            wrapper.and(w -> w.like(CertificateUser::getName, kw)
                    .or().like(CertificateUser::getPhone, kw)
                    .or().like(CertificateUser::getIdCard, kw)
                    .or().like(CertificateUser::getProfessionName, kw));
        }
        if (StringUtils.hasText(idCard)) {
            wrapper.like(CertificateUser::getIdCard, idCard);
        }
        if (StringUtils.hasText(certType)) {
            wrapper.eq(CertificateUser::getCertType, certType);
        }
        List<CertificateUser> users = certificateUserMapper.selectList(wrapper);

        Map<String, Certificate> certMap = new HashMap<>();
        if (!users.isEmpty()) {
            List<String> idCards = users.stream()
                    .map(CertificateUser::getIdCard)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            if (!idCards.isEmpty()) {
                List<Certificate> certs = certificateMapper.selectList(
                        new LambdaQueryWrapper<Certificate>()
                                .in(Certificate::getIdCard, idCards)
                                .orderByDesc(Certificate::getCreateTime));
                for (Certificate cert : certs) {
                    if (!certMap.containsKey(cert.getIdCard())) {
                        certMap.put(cert.getIdCard(), cert);
                    }
                }
            }
        }

        List<CertificateUserExportVO> exportList = new ArrayList<>();
        int serialNumber = 1;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

        for (CertificateUser user : users) {
            CertificateUserExportVO vo = new CertificateUserExportVO();
            vo.setSerialNumber(serialNumber++);
            vo.setName(user.getName());
            vo.setGender(user.getGender() != null ? (user.getGender() == 1 ? "男" : "女") : "");
            vo.setIdCard(user.getIdCard());
            vo.setProfessionName(user.getProfessionName());

            Certificate cert = certMap.get(user.getIdCard());
            if (cert != null) {
                vo.setSkillLevel(cert.getSkillLevel());
                vo.setCertNo(cert.getCertNo());
                vo.setIssueDate(cert.getIssueDate() != null ? cert.getIssueDate().format(dateFormatter) : "");
                vo.setAgency(cert.getAgency());
                vo.setAgencyFee(cert.getAgencyFee() != null ? cert.getAgencyFee().toString() : "");
                vo.setQrUrl1(cert.getQrUrl1());
                vo.setQrUrl2(cert.getQrUrl2());
                vo.setQrUrl3(cert.getQrUrl3());
                vo.setExamQrUrl(cert.getExamQrUrl());
            }

            exportList.add(vo);
        }

        String fileName = URLEncoder.encode("证书用户数据下载", "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        InputStream templateStream = null;
        OutputStream out = response.getOutputStream();
        try {
            templateStream = new ClassPathResource("templates/证书用户数据下载.xlsx").getInputStream();
            Workbook workbook = new XSSFWorkbook(templateStream);
            Sheet sheet = workbook.getSheetAt(0);

            int dataRowStart = 1;
            for (int i = 0; i < exportList.size(); i++) {
                CertificateUserExportVO vo = exportList.get(i);
                Row row = sheet.getRow(dataRowStart + i);
                if (row == null) {
                    row = sheet.createRow(dataRowStart + i);
                }

                setCellValue(row, 0, vo.getSerialNumber());
                setCellValue(row, 1, vo.getName());
                setCellValue(row, 2, vo.getGender());
                setCellValue(row, 3, vo.getIdCard());
                setCellValue(row, 4, vo.getProfessionName());
                setCellValue(row, 5, vo.getSkillLevel());
                setCellValue(row, 6, vo.getCertNo());
                setCellValue(row, 7, vo.getIssueDate());
                setCellValue(row, 8, vo.getAgency());
                setCellValue(row, 9, vo.getAgencyFee());
                setCellValue(row, 10, vo.getTrainingProfession());
                setCellValue(row, 11, vo.getTrainingHours());
                setCellValue(row, 12, vo.getTrainingDate());
                setCellValue(row, 13, vo.getTheoryScore());
                setCellValue(row, 14, vo.getPracticalScore());
                setCellValue(row, 15, vo.getComprehensiveAssessment());
                setCellValue(row, 16, vo.getQrUrl1());
                setCellValue(row, 17, vo.getQrUrl2());
                setCellValue(row, 18, vo.getQrUrl3());
                setCellValue(row, 19, vo.getExamQrUrl());
            }

            workbook.write(out);
            workbook.close();
        } catch (Exception e) {
            EasyExcel.write(out, CertificateUserExportVO.class)
                    .sheet("证书用户数据")
                    .doWrite(exportList);
        } finally {
            if (templateStream != null) {
                try {
                    templateStream.close();
                } catch (IOException ignored) {
                }
            }
            out.flush();
        }
    }

    private void setCellValue(Row row, int column, Object value) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
        }
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
