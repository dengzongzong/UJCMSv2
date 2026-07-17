package com.exam.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 证书处理请求(为已存在的 cert 记录校验、补盖章)
 *
 * <p>设计: 证书使用者(姓名/身份证/职业)是在『证书管理 → 新增证书』时录入的,
 * 本接口不再接收这些字段,只对已存在的 cert 做模板绑定/补盖章/校验。</p>
 *
 * <p>核心字段:
 * <ul>
 *   <li>certificateIds: 要处理的证书记录 ID 列表(必填,1~500)</li>
 *   <li>templateId: 使用的模板 ID(nullable:不指定则用系统默认模板)</li>
 * </ul>
 * </p>
 */
public class CertificateIssueDTO {
    @NotEmpty(message = "请至少选择一条证书记录")
    @Size(max = 500, message = "单次最多处理 500 条")
    private List<Long> certificateIds;

    private Long templateId;

    public List<Long> getCertificateIds() { return certificateIds; }
    public void setCertificateIds(List<Long> certificateIds) { this.certificateIds = certificateIds; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
}
