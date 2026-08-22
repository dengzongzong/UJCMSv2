package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.Certificate;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CertificateMapper extends BaseMapper<Certificate> {

    /**
     * 查询所有存在"身份证+专业+级别"相同但姓名不同的记录组合
     * 返回组合键: id_card|profession|skill_level
     */
    @Select("SELECT CONCAT(id_card, '|', COALESCE(profession,''), '|', COALESCE(skill_level,'')) " +
            "FROM certificate " +
            "GROUP BY id_card, profession, skill_level " +
            "HAVING COUNT(DISTINCT name) > 1")
    List<String> findDuplicatedComboKeys();
}
