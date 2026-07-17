package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.VideoProgressDTO;
import com.exam.service.CourseService;
import com.exam.vo.CourseDetailVO;
import com.exam.vo.CourseListItemVO;
import com.exam.vo.MyCourseVO;
import com.exam.vo.VideoInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端课程Controller
 */
@RestController
@RequestMapping("/user/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    /**
     * 课程中心(列表): 所有上架课程, 未登录也能浏览; 已登录则标记 purchased
     */
    @GetMapping("/list")
    public Result<List<CourseListItemVO>> list(
            @RequestParam(required = false) Long professionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(courseService.getCourseList(professionId, subjectId, categoryId, userId, keyword));
    }

    /**
     * 课程中心(公开): 允许未登录访问
     * <p>等同 /user/course/list,放在 /public/** 下以便 JwtInterceptor 不拦截</p>
     */
    @GetMapping("/public/list")
    public Result<List<CourseListItemVO>> publicList(
            @RequestParam(required = false) Long professionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(courseService.getCourseList(professionId, subjectId, categoryId, userId, keyword));
    }

    /**
     * 校验当前学生是否已开通该课程(进入详情/学习前的权限闸门)
     * <p>未登录 userId=null 直接抛 1001 业务异常由前端拦截跳登录;
     * 已登录但未开通抛 1002 业务异常由前端拦截提示。</p>
     */
    @GetMapping("/check-access")
    public Result<Boolean> checkAccess(@RequestParam Long courseId,
                                       @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(courseService.checkCourseAccess(courseId, userId));
    }

    /**
     * 课程详情(未开通课程不返回视频URL; 未登录抛业务异常)
     */
    @GetMapping("/detail")
    public Result<CourseDetailVO> detail(@RequestParam Long courseId,
                                         @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(courseService.getCourseDetail(courseId, userId));
    }

    /**
     * 我的课程列表(含学习进度)
     */
    @GetMapping("/my")
    public Result<List<MyCourseVO>> my(@RequestAttribute("userId") Long userId) {
        return Result.success(courseService.getMyCourses(userId));
    }

    /**
     * 上报视频播放进度
     */
    @PostMapping("/video-progress")
    public Result<Void> videoProgress(@RequestAttribute("userId") Long userId,
                                      @RequestBody VideoProgressDTO dto) {
        courseService.reportVideoProgress(userId, dto);
        return Result.success();
    }

    /**
     * 获取视频播放信息(需验证是否已开通课程)
     */
    @GetMapping("/video-info")
    public Result<VideoInfoVO> videoInfo(@RequestParam Long videoId,
                                         @RequestParam Long courseId,
                                         @RequestAttribute("userId") Long userId) {
        return Result.success(courseService.getVideoInfo(userId, videoId, courseId));
    }
}
