package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.CourseThreeImage;
import com.exam.service.CourseThreeImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程关联三图(后台)
 */
@RestController
@RequestMapping("/admin/course-three-image")
public class CourseThreeImageManageController {

    @Autowired
    private CourseThreeImageService service;

    @GetMapping("/page")
    public Result<PageResult<CourseThreeImage>> page(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) Long courseId,
                                                     @RequestParam(required = false) Integer status) {
        return Result.success(service.page(page, size, courseId, status));
    }

    @PostMapping
    public Result<Void> save(@RequestBody CourseThreeImage entity) {
        service.saveImage(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody CourseThreeImage entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        service.saveImage(entity);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> delete(@RequestBody List<Long> ids) {
        service.delete(ids);
        return Result.success();
    }
}
