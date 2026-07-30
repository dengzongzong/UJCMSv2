package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.CooperationApply;
import com.exam.service.CooperationApplyService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合作申请公开查询（学员端，按单位名称+授权管理编号查询）
 */
@RestController
@RequestMapping("/public/cooperation-apply")
public class CooperationApplyPublicController {

    @Autowired
    private CooperationApplyService cooperationApplyService;

    /**
     * 按单位名称 + 授权管理编号查询合作申请（两项必填）
     * 只返回部分公开字段，不返回身份证号等敏感信息
     */
    @GetMapping("/query")
    public Result<List<Map<String, Object>>> query(
            @RequestParam String unitName,
            @RequestParam String authCode) {
        if (!StringUtils.hasText(unitName) || !StringUtils.hasText(authCode)) {
            return Result.error(400, "单位名称和授权管理编号均为必填项");
        }
        List<CooperationApply> list = cooperationApplyService.page(1, 20, unitName, authCode, null).getRecords();
        // 只返回公开字段
        List<Map<String, Object>> result = list.stream().map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("unitName", item.getUnitName());
            m.put("authCode", item.getAuthCode());
            m.put("mainBusiness", item.getMainBusiness());
            m.put("contactName", item.getContactName());
            m.put("contactPhone", item.getContactPhone());
            m.put("cooperationIntent", item.getCooperationIntent());
            m.put("status", item.getStatus());
            m.put("createTime", item.getCreateTime());
            m.put("authStartDate", item.getAuthStartDate());
            m.put("authExpireDate", item.getAuthExpireDate());
            // 证书内容(每个合作单位各自维护)
            m.put("certImageUrl", item.getCertImageUrl());
            m.put("certRichText", item.getCertRichText());
            m.put("certBgScale", item.getCertBgScale());
            m.put("certEditorWidth", item.getCertEditorWidth());
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
