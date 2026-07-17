package com.exam.common;

import org.springframework.context.ApplicationEvent;

/**
 * 异步任务状态变化事件
 * - 由 AsyncTaskService 在状态/progress 更新时发布
 * - WebSocket / 其他监听器可订阅并处理
 */
public class AsyncTaskEvent extends ApplicationEvent {

    private final AsyncTask task;
    /** true 表示是终态(success/failed/cancelled),监听器可减少订阅清理 */
    private final boolean finalState;

    public AsyncTaskEvent(Object source, AsyncTask task, boolean finalState) {
        super(source);
        this.task = task;
        this.finalState = finalState;
    }

    public AsyncTask getTask() {
        return task;
    }

    public boolean isFinalState() {
        return finalState;
    }
}
