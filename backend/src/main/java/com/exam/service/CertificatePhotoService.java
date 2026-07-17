package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.PhotoBatchImportResult;
import com.exam.entity.CertificatePhoto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CertificatePhotoService extends IService<CertificatePhoto> {

    /**
     * 按身份证号查照片(取最新)
     */
    CertificatePhoto getLatestByIdCard(String idCard);

    /**
     * 按证书记录ID查照片(支持同一个人不同证书设置不同照片)
     * 优先返回 certificateId 匹配的照片,若没有则返回该身份证号最新的照片
     */
    CertificatePhoto getByCertificateId(Long certificateId, String idCard);

    /**
     * 分页查询(管理后台)
     */
    PageResult<CertificatePhoto> page(Integer page, Integer size, String idCard, String name);

    /**
     * 删除照片
     */
    void delete(List<Long> ids);

    /**
     * 批量导入照片
     * - 文件名去掉扩展名后,取最后 18 位(兼容"张三_110101199001011234"这种带前缀的命名)
     * - 用该 18 位作为身份证号,写入 certificate_photo(idCard, name, url, upload_time)
     * - name 从 certificate 表按 idCard 查(冗余方便后续展示)
     * - 若文件名为空 / 长度不足 / 不是 18 位身份证格式 → 记失败
     * @param files 上传的多张图片
     * @param savedUrlFn 给 file 返回保存后的 url(由 controller 实现 FileController 一样的逻辑)
     */
    PhotoBatchImportResult batchImport(List<MultipartFile> files,
                                       java.util.function.Function<MultipartFile, String> savedUrlFn);
}
