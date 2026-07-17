package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.StudentProfession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface StudentProfessionMapper extends BaseMapper<StudentProfession> {

    @Select("SELECT sp.*, p.name as profession_name FROM student_profession sp " +
            "LEFT JOIN profession p ON sp.profession_id = p.id " +
            "WHERE sp.student_id = #{studentId}")
    List<StudentProfession> selectByStudentId(@Param("studentId") Long studentId);
}
