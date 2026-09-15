package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.Student;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface StudentMapper extends BaseMapper<Student> {

    /** 统计去重后的学生数(按身份证号去重) */
    @Select("SELECT COUNT(DISTINCT id_card) FROM student WHERE id_card IS NOT NULL AND id_card != ''")
    int countDistinctByIdCard();

    /**
     * 统计未考试学生(从未交卷 submit_status in (1,2))按专业分组数量
     * 以 student_profession 关联表为准, 一个学生多专业会在各专业下各计一次
     */
    @Select("SELECT sp.profession_id AS professionId, p.name AS professionName, " +
            "COUNT(DISTINCT sp.student_id) AS unexamCount " +
            "FROM student_profession sp " +
            "JOIN student s ON s.id = sp.student_id " +
            "JOIN profession p ON p.id = sp.profession_id " +
            "WHERE s.id NOT IN (SELECT er.student_id FROM exam_record er WHERE er.submit_status IN (1,2)) " +
            "GROUP BY sp.profession_id, p.name " +
            "ORDER BY unexamCount DESC")
    List<Map<String, Object>> countUnexamByProfession();

    /** 统计未考试学生总数(按学生去重) */
    @Select("SELECT COUNT(*) FROM student s " +
            "WHERE s.id NOT IN (SELECT er.student_id FROM exam_record er WHERE er.submit_status IN (1,2))")
    long countUnexamTotal();
}
