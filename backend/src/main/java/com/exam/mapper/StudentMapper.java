package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.Student;
import org.apache.ibatis.annotations.Select;

public interface StudentMapper extends BaseMapper<Student> {

    /** 统计去重后的学生数(按身份证号去重) */
    @Select("SELECT COUNT(DISTINCT id_card) FROM student WHERE id_card IS NOT NULL AND id_card != ''")
    int countDistinctByIdCard();
}
