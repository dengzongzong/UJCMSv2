package com.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.Result;
import com.exam.dto.ProfileUpdateDTO;
import com.exam.dto.WrongQuestionPracticeDTO;
import com.exam.dto.WrongQuestionUpdateDTO;
import com.exam.service.ProfileService;
import com.exam.vo.ProfileVO;
import com.exam.vo.QuestionVO;
import com.exam.vo.WrongQuestionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个人中心Controller
 */
@RestController
@RequestMapping("/user/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    /**
     * 获取个人信息
     */
    @GetMapping("/info")
    public Result<ProfileVO> info(@RequestAttribute("userId") Long userId) {
        return Result.success(profileService.getProfile(userId));
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestAttribute("userId") Long userId,
                               @RequestBody ProfileUpdateDTO dto) {
        profileService.updateProfile(userId, dto);
        return Result.success();
    }

    /**
     * 我的错题列表(分页)
     *
     * @param type 0=全部(默认), 1=仅未掌握
     * @param examId 考试 ID 过滤
     * @param keyword 关键词过滤
     * @param page 第几页(从 1 开始)
     * @param size 每页多少条
     */
    @GetMapping("/wrong-questions")
    public Result<Page<WrongQuestionVO>> wrongQuestions(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") Integer type,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(profileService.getWrongQuestions(userId, type, examId, keyword, page, size));
    }

    /**
     * 清空学员的所有错题
     */
    @DeleteMapping("/wrong-questions/clear")
    public Result<Integer> clearWrongQuestions(@RequestAttribute("userId") Long userId) {
        profileService.clearWrongQuestions(userId);
        return Result.success();
    }

    /**
     * 更新错题状态(标记已掌握/未掌握)
     *
     * <p>URL:{PUT} /user/profile/wrong-question?wrongQuestionId=123
     * <br>Body:{ "mastered": 1 }</p>
     */
    @PutMapping("/wrong-question")
    public Result<Void> updateWrongQuestion(
            @RequestParam Long wrongQuestionId,
            @RequestAttribute("userId") Long userId,
            @RequestBody WrongQuestionUpdateDTO dto) {
        profileService.updateWrongQuestion(userId, wrongQuestionId, dto.getMastered());
        return Result.success();
    }

    /**
     * 移除错题
     */
    @DeleteMapping("/wrong-question")
    public Result<Void> removeWrongQuestion(@RequestParam Long wrongQuestionId,
                                            @RequestAttribute("userId") Long userId) {
        profileService.removeWrongQuestion(userId, wrongQuestionId);
        return Result.success();
    }

    /**
     * 错题练习（返回题目列表，不含正确答案）
     */
    @PostMapping("/wrong-question/practice")
    public Result<List<QuestionVO>> practice(@RequestAttribute("userId") Long userId,
                                             @RequestBody WrongQuestionPracticeDTO dto) {
        return Result.success(profileService.practiceWrongQuestions(userId, dto.getWrongQuestionIds()));
    }
}
