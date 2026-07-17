package com.exam.dto;

import lombok.Data;

/**
 * 一键抽题组卷请求DTO
 * <p>支持两种组卷模式:</p>
 * <ul>
 *   <li>byCount: 按题型指定数量,从题库随机抽取</li>
 *   <li>byScore: 指定目标总分,后端自动计算题目组合使总分刚好等于目标值</li>
 * </ul>
 */
@Data
public class AutoGeneratePaperDTO {
    /** 试卷名称 */
    private String name;
    /** 试卷描述 */
    private String description;
    /** 0-未发布 1-已发布 */
    private Integer status;
    /** 专业ID(可选,限定抽题范围) */
    private Long professionId;
    /** 题目分类ID(可选,限定抽题范围) */
    private Long categoryId;
    /** 组卷模式: "byCount"(默认,按数量) 或 "byScore"(按总分) */
    private String generateMode;
    /** 目标总分(byScore 模式必填) */
    private Integer targetScore;
    /** 单选题数量(type=1) */
    private Integer singleCount;
    /** 多选题数量(type=2) */
    private Integer multiCount;
    /** 填空题数量(type=3) */
    private Integer fillCount;
    /** 判断题数量(type=4) */
    private Integer judgeCount;
    /** 简答题数量(type=5) */
    private Integer shortCount;
}
