package com.exam.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 考试记录导出VO
 */
@Data
public class ExamRecordExportVO {

    @ExcelProperty(value = "考试名称", index = 0)
    @ColumnWidth(25)
    private String examName;

    @ExcelProperty(value = "题目数量", index = 1)
    @ColumnWidth(10)
    private Integer questionCount;

    @ExcelProperty(value = "总分数", index = 2)
    @ColumnWidth(10)
    private String totalScore;

    @ExcelProperty(value = "总时长(分钟)", index = 3)
    @ColumnWidth(12)
    private Integer duration;

    @ExcelProperty(value = "学生姓名", index = 4)
    @ColumnWidth(15)
    private String studentName;

    @ExcelProperty(value = "学生手机号", index = 5)
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty(value = "考试分数", index = 6)
    @ColumnWidth(10)
    private String score;

    @ExcelProperty(value = "提交时间", index = 7)
    @ColumnWidth(20)
    private String submitTime;

    @ExcelProperty(value = "证书情况", index = 8)
    @ColumnWidth(10)
    private String certificate;
}
