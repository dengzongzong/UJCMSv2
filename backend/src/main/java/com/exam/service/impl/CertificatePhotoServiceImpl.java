package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.PageResult;
import com.exam.dto.PhotoBatchImportResult;
import com.exam.entity.Certificate;
import com.exam.entity.CertificatePhoto;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.CertificatePhotoMapper;
import com.exam.service.CertificatePhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

@Service
public class CertificatePhotoServiceImpl extends ServiceImpl<CertificatePhotoMapper, CertificatePhoto>
        implements CertificatePhotoService {

    @Autowired
    private CertificateMapper certificateMapper;

    /** 18 位身份证号(末位可为 X) */
    private static final Pattern IDCARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    @Override
    public boolean save(CertificatePhoto entity) {
        // 兜底:即便 MetaObjectHandler 没填充 uploadTime,这里也保证入库时不带 null
        if (entity.getUploadTime() == null) {
            entity.setUploadTime(LocalDateTime.now());
        }
        return super.save(entity);
    }

    @Override
    public CertificatePhoto getLatestByIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) return null;
        return this.getOne(new LambdaQueryWrapper<CertificatePhoto>()
                .eq(CertificatePhoto::getIdCard, idCard)
                .orderByDesc(CertificatePhoto::getUploadTime)
                .last("LIMIT 1"), false);
    }

    @Override
    public CertificatePhoto getByCertificateId(Long certificateId, String idCard) {
        // 严格按 certificateId 查询,不回退到 idCard(避免同一用户多条记录共享照片)
        if (certificateId != null) {
            return this.getOne(new LambdaQueryWrapper<CertificatePhoto>()
                    .eq(CertificatePhoto::getCertificateId, certificateId)
                    .orderByDesc(CertificatePhoto::getUploadTime)
                    .last("LIMIT 1"), false);
        }
        return null;
    }

    @Override
    public PageResult<CertificatePhoto> page(Integer page, Integer size, String idCard, String name) {
        LambdaQueryWrapper<CertificatePhoto> w = new LambdaQueryWrapper<CertificatePhoto>()
                .like(StringUtils.hasText(idCard), CertificatePhoto::getIdCard, idCard)
                .like(StringUtils.hasText(name), CertificatePhoto::getName, name)
                .orderByDesc(CertificatePhoto::getUploadTime);
        return new PageResult<>(this.page(new Page<>(page, size), w));
    }

    @Override
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        this.removeByIds(ids);
    }

    @Override
    public PhotoBatchImportResult batchImport(List<MultipartFile> files,
                                              Function<MultipartFile, String> savedUrlFn) {
        PhotoBatchImportResult result = new PhotoBatchImportResult();
        List<PhotoBatchImportResult.FailedItem> failed = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            result.setTotal(0);
            result.setSuccessCount(0);
            result.setFailCount(0);
            result.setFailedItems(failed);
            return result;
        }
        int total = files.size();
        int success = 0;
        // 先收集所有识别出的 idCard,一次性去 certificate 表查 name
        List<String> idCardsToQuery = new ArrayList<>();
        // 记录 file -> 解析出的 idCard 的对应关系
        List<String> resolvedIdCards = new ArrayList<>(files.size());
        for (MultipartFile f : files) {
            String idCard = extractIdCardFromFileName(f.getOriginalFilename());
            resolvedIdCards.add(idCard);
            if (idCard != null) idCardsToQuery.add(idCard);
        }
        // 一次性查 certificate 表,得到 idCard -> List<Certificate> (同一身份证可能有多条证书记录)
        java.util.Map<String, String> nameMap = new java.util.HashMap<>();
        java.util.Map<String, List<Long>> certIdMap = new java.util.HashMap<>();
        if (!idCardsToQuery.isEmpty()) {
            Set<String> uniqueCards = new HashSet<>(idCardsToQuery);
            List<Certificate> certs = certificateMapper.selectList(
                    new LambdaQueryWrapper<Certificate>().in(Certificate::getIdCard, uniqueCards));
            for (Certificate c : certs) {
                nameMap.put(c.getIdCard(), c.getName());
                certIdMap.computeIfAbsent(c.getIdCard(), k -> new java.util.ArrayList<>()).add(c.getId());
            }
        }
        // 逐个保存
        List<CertificatePhoto> toSave = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            String original = f.getOriginalFilename();
            String idCard = resolvedIdCards.get(i);
            if (idCard == null) {
                PhotoBatchImportResult.FailedItem fi = new PhotoBatchImportResult.FailedItem();
                fi.setFileName(original);
                fi.setReason("文件名不包含有效身份证号(需 18 位)");
                failed.add(fi);
                continue;
            }
            if (!nameMap.containsKey(idCard)) {
                PhotoBatchImportResult.FailedItem fi = new PhotoBatchImportResult.FailedItem();
                fi.setFileName(original);
                fi.setReason("身份证号 " + idCard + " 未找到对应学员");
                failed.add(fi);
                continue;
            }
            String url;
            try {
                url = savedUrlFn.apply(f);
            } catch (Exception e) {
                PhotoBatchImportResult.FailedItem fi = new PhotoBatchImportResult.FailedItem();
                fi.setFileName(original);
                fi.setReason("保存图片失败: " + e.getMessage());
                failed.add(fi);
                continue;
            }
            if (url == null || url.isEmpty()) {
                PhotoBatchImportResult.FailedItem fi = new PhotoBatchImportResult.FailedItem();
                fi.setFileName(original);
                fi.setReason("保存图片失败: 未返回 URL");
                failed.add(fi);
                continue;
            }
            CertificatePhoto photo = new CertificatePhoto();
            photo.setIdCard(idCard);
            photo.setName(nameMap.get(idCard));
            photo.setUrl(url);
            photo.setUploadTime(LocalDateTime.now());
            // 绑定到证书记录的自增ID(同一身份证可能有多条记录,都关联)
            List<Long> certIds = certIdMap.get(idCard);
            if (certIds != null && !certIds.isEmpty()) {
                // 如果只有一条记录,直接绑定;多条记录则都绑定同一张照片
                for (Long cid : certIds) {
                    CertificatePhoto p = new CertificatePhoto();
                    p.setIdCard(idCard);
                    p.setName(nameMap.get(idCard));
                    p.setUrl(url);
                    p.setCertificateId(cid);
                    p.setUploadTime(LocalDateTime.now());
                    toSave.add(p);
                }
            } else {
                toSave.add(photo);
            }
        }
        if (!toSave.isEmpty()) {
            this.saveBatch(toSave);
            success = toSave.size();
        }
        result.setTotal(total);
        result.setSuccessCount(success);
        result.setFailCount(failed.size());
        result.setFailedItems(failed);
        return result;
    }

    /**
     * 从文件名中提取 18 位身份证号
     * 支持命名示例:
     *   110101199001011234.jpg
     *   张三_110101199001011234.png
     *   110101199001011234_正面.jpeg
     * 实现:去掉扩展名后,在字符串中找第一个连续 17~18 位 数字+X 的子串
     */
    private String extractIdCardFromFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) return null;
        // 去掉扩展名
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        // 找第一个 18 位 [0-9Xx] 序列
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{15,18}[\\dXx]?").matcher(base);
        // 用更严格的:先取连续 18 位
        java.util.regex.Matcher strict = java.util.regex.Pattern.compile("[\\dXx]{15,18}").matcher(base);
        String candidate = null;
        while (strict.find()) {
            String s = strict.group();
            // 截取末 18 位
            if (s.length() >= 18) {
                candidate = s.substring(s.length() - 18);
                if (IDCARD_PATTERN.matcher(candidate).matches()) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
