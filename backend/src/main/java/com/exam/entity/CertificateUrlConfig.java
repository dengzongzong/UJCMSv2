package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 证书二维码 URL 生成规则配置
 * <p>
 * 用于渲染证书时,按规则拼接出二维码 1/2/3 的跳转链接。
 * 规则字符串中可使用常量与证书用户属性占位符,例如:
 * https://example.com/query?idCard={idCard}&certNo={certNo}&name={name}
 * 占位符在渲染时被证书实际属性值(URL 编码)替换,最终生成符合 url 查询参数格式的链接。
 * 二维码绘制到证书上的位置,由"证书模板"界面编辑的字段配置(fieldKey=qr1/qr2/qr3)决定。
 * </p>
 */
@Data
@TableName("certificate_url_config")
public class CertificateUrlConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 证书二维码1 的 URL 生成规则(留空则回退使用证书本身的 qr_url1) */
    private String qr1Template;
    /** 证书二维码2 的 URL 生成规则(留空则回退使用证书本身的 qr_url2) */
    private String qr2Template;
    /** 证书二维码3 的 URL 生成规则(留空则回退使用证书本身的 qr_url3) */
    private String qr3Template;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
