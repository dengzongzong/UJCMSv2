package com.exam.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 题目导入VO（不含图片）
 * Excel列：题型 分类 专业 题干 选项A 选项B 选项C 选项D 正确答案 解析 分值
 */
@Data
public class QuestionImportVO {

    @ExcelProperty(value = "题型")
    private String type;

    @ExcelProperty(value = "分类")
    private String categoryName;

    @ExcelProperty(value = "专业")
    private String professionName;

    @ExcelProperty(value = "题干")
    private String content;

    @ExcelProperty(value = "选项A")
    private String optionA;

    @ExcelProperty(value = "选项B")
    private String optionB;

    @ExcelProperty(value = "选项C")
    private String optionC;

    @ExcelProperty(value = "选项D")
    private String optionD;

    @ExcelProperty(value = "正确答案")
    private String correctAnswer;

    @ExcelProperty(value = "解析")
    private String analysis;

    @ExcelProperty(value = "分值")
    private String score;
}
