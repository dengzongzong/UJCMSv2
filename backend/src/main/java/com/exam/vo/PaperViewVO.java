package com.exam.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 试卷查看(只读)返回 VO
 * <p>用于证书查询页/课程中心等场景,展示题目+选项+参考答案+学生历史作答</p>
 */
@Data
public class PaperViewVO {
    private Long examId;
    private String examName;
    /** 题目列表,每个题目包含 options(选项含 isCorrect 标记)、correctAnswer(参考答案) */
    private List<QuestionViewVO> questions;
    /** 该学生历史作答: questionId -> studentAnswer(从未参加则为空 map) */
    private Map<Long, String> answers;
}
