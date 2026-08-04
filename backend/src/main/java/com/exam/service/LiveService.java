package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.LiveMessage;
import com.exam.entity.LiveRoom;

import java.util.List;
import java.util.Map;

public interface LiveService extends IService<LiveRoom> {

    PageResult<LiveRoom> page(Integer page, Integer size, String keyword, Integer status);

    /** 管理端详情(含推流地址) */
    LiveRoom adminDetail(Long id);

    /** 用户端详情: 校验课程开通后返回 playUrl/replayUrl */
    Map<String, Object> publicDetail(Long id, Long userId);

    /** 课程下的直播场次 */
    List<LiveRoom> courseLives(Long courseId);

    /** 直播大厅 */
    List<LiveRoom> liveList();

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

    /** 进入直播间: 校验权限 + 累计观看人次 */
    void enter(Long id, Long userId);

    /** 拉取最近聊天记录 */
    List<LiveMessage> messages(Long id, Integer limit);

    /** 发送聊天消息(HTTP 兜底) */
    LiveMessage sendMessage(Long id, Long userId, String content);
}
