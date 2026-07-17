package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.PhotoBatchImportResult;
import com.exam.entity.CertificatePhoto;
import com.exam.service.CertificatePhotoService;
import com.exam.util.ImageCompressUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/certificate/photo")
public class CertificatePhotoController {

    @Autowired
    private CertificatePhotoService photoService;

    @Autowired
    private com.exam.mapper.CertificateMapper certificateMapper;

    @Value("${upload.path}")
    private String uploadPath;

    /**
     * 按身份证号查询所有证书记录(用于照片管理中选择关联证书)
     */
    @GetMapping("/certs-by-idcard")
    public Result<List<java.util.Map<String, Object>>> certsByIdCard(@RequestParam String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }
        List<com.exam.entity.Certificate> certs = certificateMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.exam.entity.Certificate>()
                        .eq(com.exam.entity.Certificate::getIdCard, idCard.trim())
                        .orderByDesc(com.exam.entity.Certificate::getCreateTime));
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.exam.entity.Certificate c : certs) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("idCard", c.getIdCard());
            m.put("profession", c.getProfession());
            m.put("skillLevel", c.getSkillLevel());
            m.put("certNo", c.getCertNo());
            result.add(m);
        }
        return Result.success(result);
    }

    @GetMapping("/page")
    public Result<PageResult<CertificatePhoto>> page(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) String idCard,
                                                     @RequestParam(required = false) String name) {
        return Result.success(photoService.page(page, size, idCard, name));
    }

    @PostMapping
    public Result<Void> add(@RequestBody CertificatePhoto photo) {
        // 确保 certificateId 被正确保存(支持同一个人不同证书各自绑定不同照片)
        this.photoService.save(photo);
        return Result.success();
    }

    /**
     * 为指定证书记录上传照片(支持同一个人不同证书设置不同照片)
     * @param file 图片文件
     * @param certificateId 证书记录ID
     * @param idCard 身份证号
     * @param name 姓名
     */
    @PostMapping("/upload-for-certificate")
    public Result<Void> uploadForCertificate(@RequestParam("file") MultipartFile file,
                                              @RequestParam("certificateId") Long certificateId,
                                              @RequestParam("idCard") String idCard,
                                              @RequestParam(value = "name", required = false) String name) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片");
        }
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/")) {
            throw new BusinessException("只允许上传图片文件");
        }
        String url = savePhotoFile(file);
        CertificatePhoto photo = new CertificatePhoto();
        photo.setIdCard(idCard);
        photo.setName(name);
        photo.setUrl(url);
        photo.setCertificateId(certificateId);
        photo.setUploadTime(java.time.LocalDateTime.now());
        // 先删除该证书记录的旧照片，保证每个证书只保留最新一张照片
        photoService.remove(new LambdaQueryWrapper<CertificatePhoto>()
                .eq(CertificatePhoto::getCertificateId, certificateId));
        this.photoService.save(photo);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> delete(@RequestBody List<Long> ids) {
        photoService.delete(ids);
        return Result.success();
    }

    /**
     * 批量导入照片(支持多文件)
     * - 表单字段:files (可重复)
     * - 文件名规则:文件名为 18 位身份证号(可带前缀/后缀),自动剥离出身份证号
     *   例: 110101199001011234.jpg  /  张三_110101199001011234.png  /  110101199001011234_正面.jpeg
     * - 后端按 UUID 重命名后保存,但 idCard 通过文件名解析得到
     * - 一次上传多张,只接受图片(image/*)
     */
    @PostMapping("/batch-import")
    public Result<PhotoBatchImportResult> batchImport(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请至少上传一张图片");
        }
        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;
            String type = f.getContentType();
            if (type == null || !type.startsWith("image/")) {
                throw new BusinessException("只允许上传图片文件,违规文件: " + f.getOriginalFilename());
            }
        }
        PhotoBatchImportResult result = photoService.batchImport(files, this::savePhotoFile);
        return Result.success("导入完成:成功 " + result.getSuccessCount() + " 条,失败 " + result.getFailCount() + " 条", result);
    }

    /**
     * 实际保存图片到磁盘(用 UUID 命名,保留原扩展名)
     */
    private String savePhotoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String newName = UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(uploadPath, newName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            ImageCompressUtil.compressAndSave(file, dest, ext);
        } catch (IOException e) {
            throw new BusinessException("保存图片失败: " + e.getMessage());
        }
        return "/uploads/" + newName;
    }
}
