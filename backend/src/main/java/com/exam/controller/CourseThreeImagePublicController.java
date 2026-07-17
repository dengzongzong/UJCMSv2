package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.CourseThreeImage;
import com.exam.service.CourseThreeImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程关联三图(公开查询,学员端调用)
 */
@RestController
@RequestMapping("/public/course-three-image")
public class CourseThreeImagePublicController {

    @Autowired
    private CourseThreeImageService service;

    /**
     * 学员端:获取某课程的三图
     * - 不传 courseId:返回全站通用三图
     * - 传 courseId:优先该课程下的图,不足 3 个补全站通用图
     */
    @GetMapping("/list")
    public Result<List<CourseThreeImage>> list(@RequestParam(required = false) Long courseId) {
        return Result.success(service.listForFrontend(courseId));
    }
}
