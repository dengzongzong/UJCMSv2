package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.VideoProgressDTO;
import com.exam.entity.Course;
import com.exam.vo.CourseDetailVO;
import com.exam.vo.CourseListItemVO;
import com.exam.vo.MyCourseVO;
import com.exam.vo.VideoInfoVO;

import java.util.List;

/**
 * 用户端课程Service
 */
public interface CourseService extends IService<Course> {

    /**
     * 课程列表（仅返回已上架，含学习进度）
     *
     * @param professionId 专业ID(可选,同时包含全站通用课程)
     * @param subjectId    科目ID(可选,同时包含全站通用课程)
     * @param categoryId   视频分类ID(可选,精确过滤)
     * @param studentId    学生ID(可选,登录后用于标注 purchased/学习进度)
     * @param keyword      关键词(可选,模糊搜索课程名称)
     */
    List<CourseListItemVO> getCourseList(Long professionId, Long subjectId, Long categoryId, Long studentId, String keyword);

    /**
     * 课程列表(分页版本)
     * <p>用于课程中心等列表页无限滚动加载, 每页最多 50 条。</p>
     *
     * @param professionId 专业ID(可选,同时包含全站通用课程)
     * @param subjectId    科目ID(可选)
     * @param categoryId   视频分类ID(可选,精确过滤)
     * @param studentId    学生ID(可选,登录后用于标注 purchased/学习进度)
     * @param keyword      关键词(可选,模糊搜索课程名称)
     * @param page         页码(从 1 开始)
     * @param pageSize     每页条数
     */
    PageResult<CourseListItemVO> getCourseListPage(Long professionId, Long subjectId, Long categoryId,
                                                    Long studentId, String keyword, Integer page, Integer pageSize);

    /**
     * 课程详情（未开通课程不返回视频URL）
     */
    CourseDetailVO getCourseDetail(Long courseId, Long studentId);

    /**
     * 我的课程列表（含学习进度）
     */
    List<MyCourseVO> getMyCourses(Long studentId);

    /**
     * 我的课程列表(分页版本, 含学习进度)
     * <p>用于"我的课程"页面无限滚动加载, 每页最多 50 条。</p>
     *
     * @param studentId 学生ID
     * @param page      页码(从 1 开始)
     * @param pageSize  每页条数
     */
    PageResult<MyCourseVO> getMyCoursesPage(Long studentId, Integer page, Integer pageSize);

    /**
     * 上报视频播放进度
     */
    void reportVideoProgress(Long studentId, VideoProgressDTO dto);

    /**
     * 获取视频播放信息（需验证是否已开通课程）
     */
    VideoInfoVO getVideoInfo(Long studentId, Long videoId, Long courseId);

    /**
     * 课程访问闸门校验:
     * <ul>
     *   <li>userId == null: 抛业务异常(由前端拦截跳登录)</li>
     *   <li>已登录但未开通: 抛业务异常(由前端拦截提示)</li>
     *   <li>已开通: 返回 true</li>
     * </ul>
     */
    boolean checkCourseAccess(Long courseId, Long studentId);
}
