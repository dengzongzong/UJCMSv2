package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.VideoDTO;
import com.exam.entity.Student;
import com.exam.service.VideoManageService;
import com.exam.vo.AdminVideoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 视频管理
 */
@RestController
@RequestMapping("/admin/video")
public class VideoManageController {

    @Autowired
    private VideoManageService videoManageService;

    /**
     * 分页查询视频
     */
    @GetMapping("/page")
    public Result<PageResult<AdminVideoVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Long professionId) {
        PageResult<AdminVideoVO> result = videoManageService.page(page, size, name, categoryId, professionId);
        return Result.success(result);
    }

    /**
     * 视频详情
     */
    @GetMapping("/{id}")
    public Result<AdminVideoVO> detail(@PathVariable Long id) {
        AdminVideoVO video = videoManageService.detail(id);
        return Result.success(video);
    }

    /**
     * 新增视频
     */
    @PostMapping
    public Result<Void> add(@RequestBody VideoDTO dto) {
        videoManageService.add(dto);
        return Result.success();
    }

    /**
     * 编辑视频
     */
    @PutMapping
    public Result<Void> update(@RequestBody VideoDTO dto) {
        videoManageService.update(dto);
        return Result.success();
    }

    /**
     * 删除视频
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        videoManageService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除视频
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        videoManageService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 按播放量排序返回视频列表
     */
    @GetMapping("/sort")
    public Result<List<AdminVideoVO>> sort() {
        List<AdminVideoVO> result = videoManageService.sortByPlayCount();
        return Result.success(result);
    }

    /**
     * 分页查询视频已开通/未开通学生
     * unopened == null 或 0：查询已开通该视频的学生
     * unopened == 1：查询未开通该视频的学生
     */
    @GetMapping("/{id}/students")
    public Result<PageResult<Student>> students(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam(required = false) String idCard,
                                                @RequestParam(required = false) Integer exactCount,
                                                @RequestParam(required = false) Integer unopened) {
        PageResult<Student> result = videoManageService.studentsPage(id, page, size, phone, idCard, exactCount, unopened);
        return Result.success(result);
    }

    /**
     * 批量开通视频给学生
     */
    @PostMapping("/{id}/students")
    public Result<Void> openStudents(@PathVariable Long id,
                                     @RequestBody List<Long> studentIds) {
        videoManageService.openStudents(id, studentIds);
        return Result.success();
    }

    /**
     * 取消开通（删除某学生的视频开通记录）
     */
    @DeleteMapping("/{id}/students/{studentId}")
    public Result<Void> closeStudent(@PathVariable Long id,
                                     @PathVariable Long studentId) {
        videoManageService.closeStudent(id, studentId);
        return Result.success();
    }
}
