package com.exam.mapper;

import org.apache.ibatis.annotations.Select;
import java.util.Map;

/**
 * 仪表盘统计专用 Mapper
 * 将 9 个独立 COUNT 查询合并为 1 个 SQL，减少数据库往返次数
 */
public interface DashboardMapper {

    /** 一次性统计所有仪表盘关键指标 */
    @Select("SELECT " +
            "  (SELECT COUNT(DISTINCT id_card) FROM student WHERE id_card IS NOT NULL AND id_card != '') AS student, " +
            "  (SELECT COUNT(DISTINCT id_card) FROM certificate_user WHERE id_card IS NOT NULL AND id_card != '') AS certificateUser, " +
            "  (SELECT COUNT(*) FROM certificate) AS certificate, " +
            "  (SELECT COUNT(*) FROM course) AS course, " +
            "  (SELECT COUNT(*) FROM exam) AS exam, " +
            "  (SELECT COUNT(*) FROM question) AS question, " +
            "  (SELECT COUNT(*) FROM paper) AS paper, " +
            "  (SELECT COUNT(*) FROM video) AS video, " +
            "  (SELECT COUNT(*) FROM exam_record) AS examRecord")
    Map<String, Object> stats();
}