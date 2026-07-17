package com.exam.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * /user/exam/paper 接口返回 VO。
 *
 * <p>与 ExamStartVO 字段几乎一致,但用 recordId 字段保持原样(无 recordId 时从 startExam 来,有 recordId 时透传)。</p>
 */
@Data
public class ExamPaperVO {
    private Long recordId;
    private Long examId;
    private String examName;
    private Integer duration;
    private Integer questionCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<QuestionVO> questions;
}
