package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.LiveMessage;
import com.exam.entity.LiveRoom;
import com.exam.entity.Student;

import java.util.List;
import java.util.Map;

public interface LiveService extends IService<LiveRoom> {

    PageResult<LiveRoom> page(Integer page, Integer size, String keyword, Integer status);

    /** 管理端详情(含推流地址) */
    LiveRoom adminDetail(Long id);

    /** 用户端详情: 校验直播开通后返回 playUrl/replayUrl */
    Map<String, Object> publicDetail(Long id, Long userId);

    /** 直播大厅(可传 userId 标记是否已开通) */
    List<LiveRoom> liveList(Long userId);

    /** 新增场次: 自动生成 streamName/pushUrl/playUrl */
    LiveRoom add(LiveRoom room);

    void update(LiveRoom room);

    void delete(Long id);

    /** 开始直播: 0->1, 刷新 playUrl */
    LiveRoom start(Long id);

    /** 结束直播: 1->2 */
    void stop(Long id);

    /** 回填回放地址(事后观看) */
    void setReplay(Long id, String replayUrl);

    /** 进入直播间: 校验直播开通 + 累计观看人次 */
    void enter(Long id, Long userId);

    /** 拉取最近聊天记录 */
    List<LiveMessage> messages(Long id, Integer limit);

    /** 发送聊天消息(HTTP 兜底) */
    LiveMessage sendMessage(Long id, Long userId, String content);

    /** 录制回调: 按流名自动回填回放地址(云直播录制完成后调用) */
    boolean autoReplay(String streamName, String replayUrl);

    /** 推流回调: 按流名自动开始直播(状态置为直播中) */
    boolean onPushStart(String streamName);

    /** 推流回调: 按流名自动结束直播(状态置为已结束并关闭房间) */
    boolean onPushEnd(String streamName);

    /** 分页查询直播已开通/未开通学生 */
    PageResult<Student> studentsPage(Long liveId, Integer page, Integer size, String phone, String idCard, Integer exactCount, Integer unopened, String profession);

    /** 批量开通直播给学生 */
    void openStudents(Long liveId, List<Long> studentIds);

    /** 取消开通（删除某学生的直播开通记录） */
    void closeStudent(Long liveId, Long studentId);
}
