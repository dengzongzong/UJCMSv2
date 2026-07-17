package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.Result;
import com.exam.dto.AboutUsDTO;
import com.exam.entity.*;
import com.exam.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 系统设置（专业、科目、关于我们、视频分类、题目分类）
 */
@RestController
@RequestMapping("/admin/setting")
public class SystemSettingController {

    @Autowired
    private ProfessionService professionService;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private AboutUsService aboutUsService;
    @Autowired
    private VideoCategoryService videoCategoryService;
    @Autowired
    private QuestionCategoryService questionCategoryService;

    // ==================== 专业 ====================

    /**
     * 专业列表（含科目）
     */
    @GetMapping("/professions")
    public Result<List<Map<String, Object>>> professions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> result = professionService.listWithSubjects();
        return Result.success(result);
    }

    /**
     * 新增专业
     */
    @PostMapping("/profession")
    public Result<Void> addProfession(@RequestBody Profession profession) {
        String name = StringUtils.hasText(profession.getName()) ? profession.getName().trim() : "";
        long count = professionService.count(new LambdaQueryWrapper<Profession>()
                .eq(Profession::getName, name));
        if (count > 0) {
            throw new BusinessException("专业名称「" + name + "」已存在，不允许重复创建");
        }
        profession.setName(name);
        professionService.save(profession);
        return Result.success();
    }

    /**
     * 编辑专业
     */
    @PutMapping("/profession")
    public Result<Void> updateProfession(@RequestBody Profession profession) {
        String name = StringUtils.hasText(profession.getName()) ? profession.getName().trim() : "";
        long count = professionService.count(new LambdaQueryWrapper<Profession>()
                .eq(Profession::getName, name)
                .ne(profession.getId() != null, Profession::getId, profession.getId()));
        if (count > 0) {
            throw new BusinessException("专业名称「" + name + "」已存在，不允许重复创建");
        }
        profession.setName(name);
        professionService.updateById(profession);
        return Result.success();
    }

    /**
     * 删除专业
     */
    @DeleteMapping("/profession/{id}")
    public Result<Void> deleteProfession(@PathVariable Long id) {
        professionService.deleteWithCheck(id);
        return Result.success();
    }

    // ==================== 科目 ====================

    /**
     * 新增科目
     */
    @PostMapping("/subject")
    public Result<Void> addSubject(@RequestBody Subject subject) {
        String name = StringUtils.hasText(subject.getName()) ? subject.getName().trim() : "";
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<Subject>()
                .eq(Subject::getName, name);
        if (subject.getProfessionId() != null) {
            wrapper.eq(Subject::getProfessionId, subject.getProfessionId());
        }
        long count = subjectService.count(wrapper);
        if (count > 0) {
            throw new BusinessException("科目名称「" + name + "」在该专业下已存在，不允许重复创建");
        }
        subject.setName(name);
        subjectService.save(subject);
        return Result.success();
    }

    /**
     * 编辑科目
     */
    @PutMapping("/subject")
    public Result<Void> updateSubject(@RequestBody Subject subject) {
        String name = StringUtils.hasText(subject.getName()) ? subject.getName().trim() : "";
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<Subject>()
                .eq(Subject::getName, name)
                .ne(subject.getId() != null, Subject::getId, subject.getId());
        if (subject.getProfessionId() != null) {
            wrapper.eq(Subject::getProfessionId, subject.getProfessionId());
        }
        long count = subjectService.count(wrapper);
        if (count > 0) {
            throw new BusinessException("科目名称「" + name + "」在该专业下已存在，不允许重复创建");
        }
        subject.setName(name);
        subjectService.updateById(subject);
        return Result.success();
    }

    /**
     * 删除科目
     */
    @DeleteMapping("/subject/{id}")
    public Result<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteWithCheck(id);
        return Result.success();
    }

    // ==================== 关于我们 ====================

    /**
     * 获取关于我们信息
     */
    @GetMapping("/about")
    public Result<AboutUs> about(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AboutUs aboutUs = aboutUsService.getAboutUs();
        return Result.success(aboutUs);
    }

    /**
     * 更新关于我们信息
     */
    @PutMapping("/about")
    public Result<Void> updateAbout(@RequestBody AboutUsDTO dto) {
        aboutUsService.updateAboutUs(dto);
        return Result.success();
    }

    // ==================== 视频分类 ====================

    /**
     * 视频分类列表
     */
    @GetMapping("/video-categories")
    public Result<List<VideoCategory>> videoCategories(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<VideoCategory> result = videoCategoryService.listAll();
        return Result.success(result);
    }

    /**
     * 新增视频分类
     */
    @PostMapping("/video-category")
    public Result<Void> addVideoCategory(@RequestBody VideoCategory videoCategory) {
        String name = StringUtils.hasText(videoCategory.getName()) ? videoCategory.getName().trim() : "";
        long count = videoCategoryService.count(new LambdaQueryWrapper<VideoCategory>()
                .eq(VideoCategory::getName, name));
        if (count > 0) {
            throw new BusinessException("课程分类名称「" + name + "」已存在，不允许重复创建");
        }
        videoCategory.setName(name);
        videoCategoryService.save(videoCategory);
        return Result.success();
    }

    /**
     * 编辑视频分类
     */
    @PutMapping("/video-category")
    public Result<Void> updateVideoCategory(@RequestBody VideoCategory videoCategory) {
        String name = StringUtils.hasText(videoCategory.getName()) ? videoCategory.getName().trim() : "";
        long count = videoCategoryService.count(new LambdaQueryWrapper<VideoCategory>()
                .eq(VideoCategory::getName, name)
                .ne(videoCategory.getId() != null, VideoCategory::getId, videoCategory.getId()));
        if (count > 0) {
            throw new BusinessException("课程分类名称「" + name + "」已存在，不允许重复创建");
        }
        videoCategory.setName(name);
        videoCategoryService.updateById(videoCategory);
        return Result.success();
    }

    /**
     * 删除视频分类
     */
    @DeleteMapping("/video-category/{id}")
    public Result<Void> deleteVideoCategory(@PathVariable Long id) {
        videoCategoryService.deleteWithCheck(id);
        return Result.success();
    }

    // ==================== 题目分类 ====================

    /**
     * 题目分类列表
     */
    @GetMapping("/question-categories")
    public Result<List<QuestionCategory>> questionCategories(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<QuestionCategory> result = questionCategoryService.listAll();
        return Result.success(result);
    }

    /**
     * 新增题目分类
     */
    @PostMapping("/question-category")
    public Result<Void> addQuestionCategory(@RequestBody QuestionCategory questionCategory) {
        String name = StringUtils.hasText(questionCategory.getName()) ? questionCategory.getName().trim() : "";
        long count = questionCategoryService.count(new LambdaQueryWrapper<QuestionCategory>()
                .eq(QuestionCategory::getName, name));
        if (count > 0) {
            throw new BusinessException("题目分类名称「" + name + "」已存在，不允许重复创建");
        }
        questionCategory.setName(name);
        questionCategoryService.save(questionCategory);
        return Result.success();
    }

    /**
     * 编辑题目分类
     */
    @PutMapping("/question-category")
    public Result<Void> updateQuestionCategory(@RequestBody QuestionCategory questionCategory) {
        String name = StringUtils.hasText(questionCategory.getName()) ? questionCategory.getName().trim() : "";
        long count = questionCategoryService.count(new LambdaQueryWrapper<QuestionCategory>()
                .eq(QuestionCategory::getName, name)
                .ne(questionCategory.getId() != null, QuestionCategory::getId, questionCategory.getId()));
        if (count > 0) {
            throw new BusinessException("题目分类名称「" + name + "」已存在，不允许重复创建");
        }
        questionCategory.setName(name);
        questionCategoryService.updateById(questionCategory);
        return Result.success();
    }

    /**
     * 删除题目分类
     */
    @DeleteMapping("/question-category/{id}")
    public Result<Void> deleteQuestionCategory(@PathVariable Long id) {
        questionCategoryService.deleteWithCheck(id);
        return Result.success();
    }
}
