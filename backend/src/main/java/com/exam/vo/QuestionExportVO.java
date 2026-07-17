package com.exam.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 题目导出VO
 */
@Data
public class QuestionExportVO {

    @ExcelProperty(value = "题型", index = 0)
    @ColumnWidth(10)
    private String type;

    @ExcelProperty(value = "分类", index = 1)
    @ColumnWidth(15)
    private String categoryName;

    @ExcelProperty(value = "专业", index = 2)
    @ColumnWidth(15)
    private String professionName;

    @ExcelProperty(value = "题干", index = 3)
    @ColumnWidth(50)
    private String content;

    @ExcelProperty(value = "选项A", index = 4)
    @ColumnWidth(20)
    private String optionA;

    @ExcelProperty(value = "选项B", index = 5)
    @ColumnWidth(20)
    private String optionB;

    @ExcelProperty(value = "选项C", index = 6)
    @ColumnWidth(20)
    private String optionC;

    @ExcelProperty(value = "选项D", index = 7)
    @ColumnWidth(20)
    private String optionD;

    @ExcelProperty(value = "正确答案", index = 8)
    @ColumnWidth(12)
    private String correctAnswer;

    @ExcelProperty(value = "解析", index = 9)
    @ColumnWidth(30)
    private String analysis;

    @ExcelProperty(value = "分值", index = 10)
    @ColumnWidth(8)
    private String score;

    @ExcelProperty(value = "是否可用", index = 11)
    @ColumnWidth(10)
    private String enabled;
}
