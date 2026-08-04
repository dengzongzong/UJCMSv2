package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.LiveRoom;
import com.exam.entity.Student;
import com.exam.service.LiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端直播管理Controller
 */
@RestController
@RequestMapping("/admin/live")
public class LiveManageController {

    @Autowired
    private LiveService liveService;

    /** 分页查询直播场次 */
    @GetMapping("/page")
    public Result<PageResult<LiveRoom>> page(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(liveService.page(page, size, keyword, status));
    }

    /** 直播详情(含推流地址) */
    @GetMapping("/{id}")
    public Result<LiveRoom> detail(@PathVariable Long id) {
        return Result.success(liveService.adminDetail(id));
    }

    /** 新增直播场次(自动生成推流/播放地址) */
    @PostMapping
    public Result<LiveRoom> add(@RequestBody LiveRoom room) {
        return Result.success(liveService.add(room));
    }

    /** 编辑直播场次 */
    @PutMapping
    public Result<Void> update(@RequestBody LiveRoom room) {
        liveService.update(room);
        return Result.success();
    }

    /** 删除直播场次 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        liveService.delete(id);
        return Result.success();
    }

    /** 开始直播(0->1) */
    @PostMapping("/{id}/start")
    public Result<LiveRoom> start(@PathVariable Long id) {
        return Result.success(liveService.start(id));
    }

    /** 结束直播(1->2) */
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        liveService.stop(id);
        return Result.success();
    }

    /** 回填回放地址(事后观看) */
    @PostMapping("/{id}/replay")
    public Result<Void> setReplay(@PathVariable Long id, @RequestBody LiveRoom room) {
        liveService.setReplay(id, room == null ? null : room.getReplayUrl());
        return Result.success();
    }

    /**
     * 分页查询直播已开通/未开通学生
     */
    @GetMapping("/{id}/students")
    public Result<PageResult<Student>> students(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam(required = false) String idCard,
                                                @RequestParam(required = false) Integer exactCount,
                                                @RequestParam(required = false) Integer unopened,
                                                @RequestParam(required = false) String profession) {
        PageResult<Student> result = liveService.studentsPage(id, page, size, phone, idCard, exactCount, unopened, profession);
        return Result.success(result);
    }

    /**
     * 为直播批量开通学生
     */
    @PostMapping("/{id}/students")
    public Result<Void> openStudents(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Object rawIds = params.get("studentIds");
        @SuppressWarnings("unchecked")
        List<Object> ids = (List<Object>) rawIds;
        List<Long> studentIds = ids.stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
        liveService.openStudents(id, studentIds);
        return Result.success();
    }

    /**
     * 取消开通（删除某学生的直播开通记录）
     */
    @DeleteMapping("/{id}/students/{studentId}")
    public Result<Void> closeStudent(@PathVariable Long id,
                                     @PathVariable Long studentId) {
        liveService.closeStudent(id, studentId);
        return Result.success();
    }
}
