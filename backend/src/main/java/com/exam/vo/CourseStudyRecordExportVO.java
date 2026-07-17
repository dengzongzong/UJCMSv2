package com.exam.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 课程学习记录导出VO
 */
@Data
public class CourseStudyRecordExportVO {

    @ExcelProperty(value = "课程名称", index = 0)
    @ColumnWidth(25)
    private String courseName;

    @ExcelProperty(value = "标签", index = 1)
    @ColumnWidth(10)
    private String tag;

    @ExcelProperty(value = "价格", index = 2)
    @ColumnWidth(10)
    private String price;

    @ExcelProperty(value = "小节数量", index = 3)
    @ColumnWidth(10)
    private Integer sectionCount;

    @ExcelProperty(value = "状态", index = 4)
    @ColumnWidth(10)
    private String status;

    @ExcelProperty(value = "学生手机号", index = 5)
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty(value = "学习开始时间", index = 6)
    @ColumnWidth(20)
    private String studyStartTime;

    @ExcelProperty(value = "学习进度", index = 7)
    @ColumnWidth(12)
    private String progress;
}
