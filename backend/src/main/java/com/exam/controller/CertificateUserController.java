package com.exam.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
     * 导出证书用户数据
     * 按证书绑定的模板分组导出,使用各模板配置的导出列(含颁发日期/年/月/日等)。
     * 未绑定模板的证书自动过滤;多个模板时导出ZIP。
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String idCard,
                       @RequestParam(required = false) String certType,
                       HttpServletResponse response) throws IOException {
        // 1. 查询证书用户(按筛选条件)
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

        // 2. 收集身份证号,查询关联的证书记录
        List<String> idCards = users.stream()
                .map(CertificateUser::getIdCard)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        List<Certificate> certs = new ArrayList<>();
        if (!idCards.isEmpty()) {
            certs = certificateMapper.selectList(
                    new LambdaQueryWrapper<Certificate>()
                            .in(Certificate::getIdCard, idCards)
                            .orderByDesc(Certificate::getCreateTime));
        }

        // 3. 委托给证书导出服务:按模板分组、使用模板配置的导出列
        certificateService.exportCertificateList(response, certs);
    }
}
