package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.LiveService;
import com.exam.util.provider.LiveProvider;
import com.exam.util.provider.LiveProviderFactory;
import com.exam.ws.LiveWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LiveServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveService {

    @Autowired
    private LiveMessageMapper liveMessageMapper;

    @Autowired
    private StudentCourseMapper studentCourseMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private LiveProviderFactory providerFactory;

    @Autowired
    private LiveWebSocketHandler liveWebSocketHandler;

    @Override
    public PageResult<LiveRoom> page(Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(LiveRoom::getTitle, keyword);
        }
        if (status != null) {
            wrapper.eq(LiveRoom::getStatus, status);
        }
        wrapper.orderByDesc(LiveRoom::getCreateTime);
        IPage<LiveRoom> result = this.page(new Page<>(page, size), wrapper);
        for (LiveRoom room : result.getRecords()) {
            fillExtra(room);
        }
        return new PageResult<>(result);
    }

    @Override
    public LiveRoom adminDetail(Long id) {
        LiveRoom room = this.getById(id);
        if (room == null) {
            throw new BusinessException("直播场次不存在");
        }
        fillExtra(room);
        return room;
    }

    @Override
    public Map<String, Object> publicDetail(Long id, Long userId) {
        LiveRoom room = this.getById(id);
        if (room == null || room.getStatus() == 3) {
            throw new BusinessException("直播场次不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", room.getId());
        result.put("courseId", room.getCourseId());
        result.put("title", room.getTitle());
        result.put("coverUrl", room.getCoverUrl());
        result.put("anchorName", room.getAnchorName());
        result.put("intro", room.getIntro());
        result.put("startTime", room.getStartTime());
        result.put("endTime", room.getEndTime());
        result.put("status", room.getStatus());
        result.put("viewCount", room.getViewCount());
        result.put("replayUrl", room.getReplayUrl());
        Course course = courseMapper.selectById(room.getCourseId());
        result.put("courseName", course != null ? course.getName() : "");

        // 在线人数(WebSocket实时)
        int online = liveWebSocketHandler.getOnlineCount(room.getId());
        result.put("onlineCount", online);
        if (online > (room.getMaxOnline() == null ? 0 : room.getMaxOnline())) {
            room.setMaxOnline(online);
            this.updateById(room);
        }

        // 是否已开通该课程(决定能否播放直播/回放)
        boolean opened = checkOpened(room.getCourseId(), userId);
        result.put("opened", opened);

        // 仅已开通用户返回播放地址(直播中/已结束均可观看)
        if (opened) {
            result.put("playUrl", room.getPlayUrl());
            result.put("streamName", room.getStreamName());
        } else {
            result.put("playUrl", "");
            result.put("streamName", "");
        }
        return result;
    }

    @Override
    public List<LiveRoom> courseLives(Long courseId) {
        LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveRoom::getCourseId, courseId)
                .ne(LiveRoom::getStatus, 3)
                .orderByAsc(LiveRoom::getStartTime);
        List<LiveRoom> list = this.list(wrapper);
        for (LiveRoom room : list) {
            hideSecret(room);
            fillExtra(room);
        }
        return list;
    }

    @Override
    public List<LiveRoom> liveList() {
        LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(LiveRoom::getStatus, 3)
                .orderByDesc(LiveRoom::getStartTime);
        List<LiveRoom> list = this.list(wrapper);
        for (LiveRoom room : list) {
            hideSecret(room);
            fillExtra(room);
        }
        return list;
    }

    @Override
    @Transactional
    public LiveRoom add(LiveRoom room) {
        LiveProvider provider = providerFactory.getProvider();
        if (provider == null) {
            throw new BusinessException("直播服务未配置");
        }
        if (room.getTitle() == null || room.getTitle().isEmpty()) {
            throw new BusinessException("请填写直播标题");
        }
        if (room.getCourseId() == null) {
            throw new BusinessException("请选择所属课程");
        }
        // 自动生成流名与推拉流地址
        String streamName = provider.genStreamName();
        room.setStreamName(streamName);
        room.setPushUrl(provider.buildPushUrl(streamName));
        room.setPlayUrl(provider.buildPlayUrl(streamName));
        room.setStatus(room.getStatus() == null ? 0 : room.getStatus());
        room.setViewCount(0);
        room.setMaxOnline(0);
        room.setSort(room.getSort() == null ? 0 : room.getSort());
        this.save(room);
        return room;
    }

    @Override
    public void update(LiveRoom room) {
        LiveRoom exist = this.getById(room.getId());
        if (exist == null) {
            throw new BusinessException("直播场次不存在");
        }
        // 流名与推流地址不允许在编辑中更改(开始后更换会导致直播中断)
        room.setStreamName(exist.getStreamName());
        room.setPushUrl(exist.getPushUrl());
        room.setPlayUrl(exist.getPlayUrl());
        room.setStatus(exist.getStatus());
        room.setViewCount(exist.getViewCount());
        room.setMaxOnline(exist.getMaxOnline());
        room.setCreateTime(exist.getCreateTime());
        this.updateById(room);
    }

    @Override
    public void delete(Long id) {
        this.removeById(id);
        liveWebSocketHandler.closeRoom(id);
        // 清理聊天记录
        LambdaQueryWrapper<LiveMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveMessage::getLiveId, id);
        liveMessageMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public LiveRoom start(Long id) {
        LiveRoom room = this.getById(id);
        if (room == null) {
            throw new BusinessException("直播场次不存在");
        }
        // 开始直播时重新生成播放地址(延长有效期)
        LiveProvider provider = providerFactory.getProvider();
        if (provider == null) {
            throw new BusinessException("直播服务未配置");
        }
        room.setPlayUrl(provider.buildPlayUrl(room.getStreamName()));
        room.setStatus(1);
        this.updateById(room);
        return room;
    }

    @Override
    @Transactional
    public void stop(Long id) {
        LiveRoom room = this.getById(id);
        if (room == null) {
            throw new BusinessException("直播场次不存在");
        }
        room.setStatus(2);
        room.setEndTime(java.time.LocalDateTime.now());
        this.updateById(room);
        liveWebSocketHandler.closeRoom(id);
    }

    @Override
    @Transactional
    public void setReplay(Long id, String replayUrl) {
        LiveRoom room = this.getById(id);
        if (room == null) {
            throw new BusinessException("直播场次不存在");
        }
        room.setReplayUrl(replayUrl);
        this.updateById(room);
    }

    @Override
    @Transactional
    public boolean autoReplay(String streamName, String replayUrl) {
        if (streamName == null || streamName.isEmpty()
                || replayUrl == null || replayUrl.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveRoom::getStreamName, streamName).last("LIMIT 1");
        LiveRoom room = this.getOne(wrapper);
        if (room == null) {
            log.warn("录制回调未匹配到场次: streamName={}", streamName);
            return false;
        }
        room.setReplayUrl(replayUrl);
        // 录制完成说明直播已结束, 若仍处于直播中则自动置为已结束
        if (room.getStatus() != null && room.getStatus() == 1) {
            room.setStatus(2);
            room.setEndTime(java.time.LocalDateTime.now());
        }
        this.updateById(room);
        log.info("录制回调自动回填回放: liveId={} streamName={} url={}", room.getId(), streamName, replayUrl);
        return true;
    }

    @Override
    @Transactional
    public void enter(Long id, Long userId) {
        LiveRoom room = this.getById(id);
        if (room == null || room.getStatus() == 3) {
            throw new BusinessException("直播场次不存在");
        }
        if (room.getStatus() == 0) {
            throw new BusinessException("直播尚未开始");
        }
        // 权限校验: 必须已开通该课程
        if (!checkOpened(room.getCourseId(), userId)) {
            throw new BusinessException("您尚未开通该课程，请联系管理员开通后再观看直播");
        }
        room.setViewCount((room.getViewCount() == null ? 0 : room.getViewCount()) + 1);
        this.updateById(room);
    }

    @Override
    public List<LiveMessage> messages(Long id, Integer limit) {
        int size = (limit == null || limit <= 0 || limit > 200) ? 50 : limit;
        LambdaQueryWrapper<LiveMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveMessage::getLiveId, id)
                .orderByDesc(LiveMessage::getCreateTime)
                .last("LIMIT " + size);
        List<LiveMessage> list = liveMessageMapper.selectList(wrapper);
        // 倒序变正序(最新在底部)
        java.util.Collections.reverse(list);
        return list;
    }

    @Override
    @Transactional
    public LiveMessage sendMessage(Long id, Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("消息过长(最多500字)");
        }
        LiveRoom room = this.getById(id);
        if (room == null) {
            throw new BusinessException("直播场次不存在");
        }
        if (room.getStatus() == 3) {
            throw new BusinessException("直播已取消");
        }
        Student student = userId != null ? studentMapper.selectById(userId) : null;
        String nickname = student != null && student.getName() != null ? student.getName()
                : (student != null && student.getPhone() != null ? student.getPhone() : "游客");

        LiveMessage msg = new LiveMessage();
        msg.setLiveId(id);
        msg.setStudentId(userId);
        msg.setNickname(nickname);
        msg.setContent(content.trim());
        liveMessageMapper.insert(msg);
        return msg;
    }

    private boolean checkOpened(Long courseId, Long userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<StudentCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourse::getStudentId, userId)
                .eq(StudentCourse::getCourseId, courseId)
                .last("LIMIT 1");
        return studentCourseMapper.selectCount(wrapper) > 0;
    }

    private void fillExtra(LiveRoom room) {
        if (room.getCourseId() != null) {
            Course course = courseMapper.selectById(room.getCourseId());
            room.setCourseName(course != null ? course.getName() : "");
        }
        room.setOnlineCount(liveWebSocketHandler.getOnlineCount(room.getId()));
    }

    /** 公开列表不暴露推拉流地址(避免未开通用户绕过权限直接取流) */
    private void hideSecret(LiveRoom room) {
        room.setStreamName(null);
        room.setPushUrl(null);
        room.setPlayUrl(null);
    }
}
