package com.exam.service;

import com.exam.entity.Certificate;
import com.exam.entity.CertificateTemplate;

import java.io.OutputStream;
import java.util.List;

public interface CertificateGenerateService {

    /**
     * 把单张证书渲染成图片,写入 outputStream。
     * 模板字段会按配置画到背景图对应位置。
     */
    void renderSingle(Certificate cert, CertificateTemplate template, OutputStream outputStream) throws Exception;

    /**
     * 把单张证书渲染成 PDF
     */
    void renderSinglePdf(Certificate cert, CertificateTemplate template, OutputStream outputStream) throws Exception;

    /**
     * 批量渲染为 ZIP 压缩包(内含图片)
     */
    void renderBatchToZip(List<Certificate> certs, CertificateTemplate template, OutputStream outputStream) throws Exception;

    /**
     * 批量渲染为单个 PDF(每张证书一页,合并到一个 PDF 文件)
     */
    void renderBatchPdf(List<Certificate> certs, CertificateTemplate template, OutputStream outputStream) throws Exception;

    /**
     * 批量渲染为单个 PDF,带进度回调。
     * <p>每渲染完一张证书调用一次回调,用于异步任务更新进度条。</p>
     *
     * @param progressCallback 进度回调(processed, total),可为 null
     */
    void renderBatchPdf(List<Certificate> certs, CertificateTemplate template, OutputStream outputStream,
                        java.util.function.BiConsumer<Integer, Integer> progressCallback) throws Exception;

    /**
     * 渲染单张到字节数组(用于返回前端 / HTTP 响应)
     */
    byte[] renderSingleBytes(Certificate cert, CertificateTemplate template) throws Exception;

    /**
     * 渲染单张证书图片(带文件缓存)。
     * <p>缓存文件存于 {uploadPath}/cert_preview/cert_{id}.png;
     * 当证书或模板的 updateTime 晚于缓存文件修改时间时自动失效并重新渲染。</p>
     * <p>首次预览/绑定时渲染一次,后续直接读文件,预览秒开。</p>
     */
    byte[] renderSingleBytesCached(Certificate cert, CertificateTemplate template) throws Exception;

    /**
     * 预渲染证书图片到缓存文件(绑定时调用,失败不抛异常仅记日志)
     */
    void prerender(Certificate cert, CertificateTemplate template);
}
