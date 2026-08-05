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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LiveServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveService {

    @Autowired
    private LiveMessageMapper liveMessageMapper;

    @Autowired
    private StudentLiveMapper studentLiveMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ProfessionMapper professionMapper;

    @Autowired
    private StudentProfessionMapper studentProfessionMapper;

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
        result.put("title", room.getTitle());
        result.put("coverUrl", room.getCoverUrl());
        result.put("anchorName", room.getAnchorName());
        result.put("intro", room.getIntro());
        result.put("startTime", room.getStartTime());
        result.put("endTime", room.getEndTime());
        result.put("status", room.getStatus());
        result.put("viewCount", room.getViewCount());
        result.put("replayUrl", room.getReplayUrl());

        // 在线人数(WebSocket实时)
        int online = liveWebSocketHandler.getOnlineCount(room.getId());
        result.put("onlineCount", online);
        if (online > (room.getMaxOnline() == null ? 0 : room.getMaxOnline())) {
            room.setMaxOnline(online);
            this.updateById(room);
        }

        // 是否已开通该直播(决定能否播放直播/回放)
        boolean opened = checkOpened(room.getId(), userId);
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
    public List<LiveRoom> liveList(Long userId) {
        LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(LiveRoom::getStatus, 3)
                .orderByDesc(LiveRoom::getStartTime);
        List<LiveRoom> list = this.list(wrapper);
        for (LiveRoom room : list) {
            hideSecret(room);
            room.setOpened(checkOpened(room.getId(), userId));
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
        LiveRoom room = findByStreamName(streamName);
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
    public boolean onPushStart(String streamName) {
        if (streamName == null || streamName.isEmpty()) {
            return false;
        }
        LiveRoom room = findByStreamName(streamName);
        if (room == null) {
            log.warn("推流开始回调未匹配到场次: streamName={}", streamName);
            return false;
        }
        // 未开始/已结束(重新推流)才自动置为直播中
        if (room.getStatus() == null || room.getStatus() == 0 || room.getStatus() == 2) {
            // 重新生成播放地址(延长鉴权有效期)
            LiveProvider provider = providerFactory.getProvider();
            if (provider != null) {
                room.setPlayUrl(provider.buildPlayUrl(room.getStreamName()));
            }
            room.setStatus(1);
            this.updateById(room);
            log.info("推流回调自动开始直播: liveId={} streamName={}", room.getId(), streamName);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean onPushEnd(String streamName) {
        if (streamName == null || streamName.isEmpty()) {
            return false;
        }
        LiveRoom room = findByStreamName(streamName);
        if (room == null) {
            log.warn("推流结束回调未匹配到场次: streamName={}", streamName);
            return false;
        }
        if (room.getStatus() != null && room.getStatus() == 1) {
            room.setStatus(2);
            room.setEndTime(java.time.LocalDateTime.now());
            this.updateById(room);
            liveWebSocketHandler.closeRoom(room.getId());
            log.info("推流回调自动结束直播: liveId={} streamName={}", room.getId(), streamName);
        }
        return true;
    }

    /** 按流名查询场次 */
    private LiveRoom findByStreamName(String streamName) {
        LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveRoom::getStreamName, streamName).last("LIMIT 1");
        return this.getOne(wrapper);
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
        // 权限校验: 必须已开通该直播
        if (!checkOpened(room.getId(), userId)) {
            throw new BusinessException("您尚未开通该直播，请联系管理员开通后再观看直播");
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

    private boolean checkOpened(Long liveId, Long userId) {
        if (userId == null) {
            log.warn("[live] checkOpened userId=null, liveId={}, 未登录或token未注入", liveId);
            return false;
        }
        LambdaQueryWrapper<StudentLive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentLive::getStudentId, userId)
                .eq(StudentLive::getLiveId, liveId)
                .last("LIMIT 1");
        boolean opened = studentLiveMapper.selectCount(wrapper) > 0;
        log.info("[live] checkOpened liveId={}, userId={}, opened={}", liveId, userId, opened);
        return opened;
    }

    private void fillExtra(LiveRoom room) {
        room.setOnlineCount(liveWebSocketHandler.getOnlineCount(room.getId()));
    }

    /** 公开列表不暴露推拉流地址(避免未开通用户绕过权限直接取流) */
    private void hideSecret(LiveRoom room) {
        room.setStreamName(null);
        room.setPushUrl(null);
        room.setPlayUrl(null);
    }

    @Override
    public PageResult<Student> studentsPage(Long liveId, Integer page, Integer size, String phone, String idCard, Integer exactCount, Integer unopened, String profession) {
        // 显示最新N条：固定第1页，size = exactCount
        if (exactCount != null && exactCount > 0) {
            page = 1;
            size = exactCount;
        }
        // 查询已开通该直播的学生ID集合
        List<StudentLive> studentLives = studentLiveMapper.selectList(
                new LambdaQueryWrapper<StudentLive>().eq(StudentLive::getLiveId, liveId));
        Set<Long> openedIds = studentLives.stream().map(StudentLive::getStudentId).collect(Collectors.toSet());

        // 按专业筛选: 先查 profession 表按名称匹配,再查 student_profession 关联表获取 studentId
        Set<Long> professionFilteredIds = null;
        if (StringUtils.hasText(profession)) {
            List<Profession> matchedProfessions = professionMapper.selectList(
                    new LambdaQueryWrapper<Profession>().like(Profession::getName, profession));
            if (matchedProfessions.isEmpty()) {
                return new PageResult<>(new Page<>(page, size));
            }
            Set<Long> profIds = matchedProfessions.stream().map(Profession::getId).collect(Collectors.toSet());
            List<StudentProfession> sps = studentProfessionMapper.selectList(
                    new LambdaQueryWrapper<StudentProfession>().in(StudentProfession::getProfessionId, profIds));
            professionFilteredIds = sps.stream().map(StudentProfession::getStudentId).collect(Collectors.toSet());
            if (professionFilteredIds.isEmpty()) {
                return new PageResult<>(new Page<>(page, size));
            }
        }

        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(phone), Student::getPhone, phone);
        wrapper.like(StringUtils.hasText(idCard), Student::getIdCard, idCard);
        if (professionFilteredIds != null) {
            wrapper.in(Student::getId, professionFilteredIds);
        }
        if (unopened != null && unopened == 1) {
            // 未开通：id NOT IN openedIds
            if (!openedIds.isEmpty()) {
                wrapper.notIn(Student::getId, openedIds);
            }
        } else {
            // 已开通：id IN openedIds
            if (openedIds.isEmpty()) {
                return new PageResult<>(new Page<>(page, size)); // 空分页
            }
            wrapper.in(Student::getId, openedIds);
        }
        wrapper.orderByDesc(Student::getCreateTime).orderByDesc(Student::getId);
        Page<Student> p = new Page<>(page, size);
        Page<Student> result = studentMapper.selectPage(p, wrapper);
        result.getRecords().forEach(s -> s.setPassword(null));
        // 填充专业名称
        fillProfessionNames(result.getRecords());
        return new PageResult<>(result);
    }

    /** 批量填充学生的专业名称(通过 student_profession 关联表) */
    private void fillProfessionNames(List<Student> students) {
        if (students == null || students.isEmpty()) return;
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        List<StudentProfession> sps = studentProfessionMapper.selectList(
                new LambdaQueryWrapper<StudentProfession>().in(StudentProfession::getStudentId, studentIds));
        if (sps.isEmpty()) return;
        Set<Long> profIds = sps.stream().map(StudentProfession::getProfessionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> profNameMap = new HashMap<>();
        if (!profIds.isEmpty()) {
            List<Profession> professions = professionMapper.selectBatchIds(profIds);
            for (Profession p : professions) {
                profNameMap.put(p.getId(), p.getName());
            }
        }
        Map<Long, List<String>> studentProfNames = new HashMap<>();
        for (StudentProfession sp : sps) {
            String pname = profNameMap.get(sp.getProfessionId());
            if (pname != null) {
                studentProfNames.computeIfAbsent(sp.getStudentId(), k -> new ArrayList<>()).add(pname);
            }
        }
        for (Student s : students) {
            List<String> names = studentProfNames.get(s.getId());
            if (names != null && !names.isEmpty()) {
                s.setProfessionName(String.join(",", names));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openStudents(Long liveId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        // 查询已存在的开通记录
        List<StudentLive> existing = studentLiveMapper.selectList(
                new LambdaQueryWrapper<StudentLive>()
                        .eq(StudentLive::getLiveId, liveId)
                        .in(StudentLive::getStudentId, studentIds));
        Set<Long> existingIds = existing.stream().map(StudentLive::getStudentId).collect(Collectors.toSet());
        for (Long studentId : studentIds) {
            if (existingIds.contains(studentId)) {
                continue;
            }
            StudentLive sl = new StudentLive();
            sl.setStudentId(studentId);
            sl.setLiveId(liveId);
            studentLiveMapper.insert(sl);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeStudent(Long liveId, Long studentId) {
        studentLiveMapper.delete(new LambdaQueryWrapper<StudentLive>()
                .eq(StudentLive::getLiveId, liveId)
                .eq(StudentLive::getStudentId, studentId));
    }
}
