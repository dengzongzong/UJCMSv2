package com.exam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.AsyncTask;

import java.util.List;
import java.util.function.Consumer;

/**
 * 异步任务管理服务
 * - 内存 + DB 持久化
 * - 任务状态保存在 ConcurrentHashMap(热数据)+ async_task 表(冷数据)
 */
public interface AsyncTaskService {

    /**
     * 注册并启动一个异步任务
     */
    String submit(String bizType, String bizName, Integer total, Consumer<AsyncTask> handler);

    /**
     * 注册并启动一个异步任务(带发起人)
     */
    String submit(String bizType, String bizName, Integer total, Consumer<AsyncTask> handler, String createdBy);

    /**
     * 查询任务(优先内存,fallback DB)
     */
    AsyncTask get(String taskId);

    /**
     * 列出所有任务(最近 200 条,DB 查询)
     */
    List<AsyncTask> listAll();

    /**
     * 列出所有进行中的任务(内存查询,O(1))
     */
    List<AsyncTask> listActive();

    /**
     * 分页查询任务(支持业务类型/状态筛选)
     */
    Page<AsyncTask> listWithPaging(String bizType, String status, Integer page, Integer size);

    /**
     * 取消任务(pending 状态可取消;running 状态不能取消)
     */
    boolean cancel(String taskId);

    /**
     * 清理已完成任务(内存立即清,DB 用 retentionDays)
     */
    void clearFinished();

    /**
     * 删除已过期的已完成任务(基于 retentionDays 配置)
     */
    int cleanupExpiredTasks();

    /**
     * 标记僵尸任务(running 状态超过 zombieHours,自动转 failed)
     */
    int markZombieTasks();

    /**
     * 当前 retentionDays(天)
     */
    int getRetentionDays();

    /**
     * 当前 zombieHours(小时)
     */
    int getZombieHours();

    /**
     * 强制把任务的 extraJson 字段刷到 DB(常用于任务创建后立即打补丁)
     */
    void flushTaskExtra(AsyncTask task);

    /**
     * 读取任务源文件路径(从 extraJson 的 sourceFilePath)
     */
    String getSourceFilePath(AsyncTask task);

    /**
     * 检测任务是否已被取消(handler 内部使用)
     */
    boolean isCancelled(String taskId);
}
