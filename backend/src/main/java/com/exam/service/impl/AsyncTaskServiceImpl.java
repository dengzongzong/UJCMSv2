package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.AsyncTask;
import com.exam.common.AsyncTaskEvent;
import com.exam.common.SpringContextHolder;
import com.exam.entity.AsyncTaskEntity;
import com.exam.mapper.AsyncTaskMapper;
import com.exam.service.AsyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 异步任务管理(持久化版)
 * - 内存 ConcurrentHashMap 是热数据缓存(查询快)
 * - DB async_task 是冷数据(任务历史、可重新下载结果文件)
 * - 应用启动时把"未完成"任务从 DB 加载到内存,继续轮询
 */
@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskServiceImpl.class);

    private final ConcurrentMap<String, AsyncTask> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();
    /** 内存 -> DB 同步节流:同一任务两次持久化最小间隔(ms) */
    private static final long DB_FLUSH_INTERVAL_MS = 1500L;
    private final ConcurrentMap<String, Long> lastFlushMs = new ConcurrentHashMap<>();

    @Autowired
    private AsyncTaskMapper taskMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 已完成任务的 TTL(单位:天)。超过此天数的 success/failed/cancelled 任务将被定时清理
     * 默认 7 天,可在 application.yml 配置: async.task.retention-days
     */
    @Value("${async.task.retention-days:7}")
    private int retentionDays;

    /**
     * 僵尸任务判定时间(单位:小时)。running 状态超过此时间未结束,视为僵尸,自动标记 failed
     * 默认 2 小时,可在 application.yml 配置: async.task.zombie-hours
     */
    @Value("${async.task.zombie-hours:2}")
    private int zombieHours;

    /**
     * TTL 定时清理(每天凌晨 3 点执行)
     * cron: 秒 分 时 日 月 周
     * 0 0 3 * * ?  -> 每天 03:00:00
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledCleanup() {
        try {
            int deleted = cleanupExpiredTasks();
            int zombies = markZombieTasks();
            if (deleted > 0 || zombies > 0) {
                log.info("异步任务 TTL 清理: 删除 {} 条过期任务, 标记 {} 条僵尸任务", deleted, zombies);
            }
        } catch (Exception e) {
            log.error("异步任务 TTL 清理失败", e);
        }
    }

    /**
     * 删除已完成且超过 retentionDays 的任务
     * 顺带清理源文件(用于"重试"的 Excel 临时文件,以 cert_import_ 开头)
     */
    public int cleanupExpiredTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int n = taskMapper.delete(new LambdaQueryWrapper<AsyncTaskEntity>()
                .in(AsyncTaskEntity::getStatus,
                        AsyncTask.STATUS_SUCCESS,
                        AsyncTask.STATUS_FAILED,
                        AsyncTask.STATUS_CANCELLED)
                .lt(AsyncTaskEntity::getEndTime, cutoff));
        // 同步清理内存
        if (n > 0) {
            tasks.entrySet().removeIf(e -> {
                AsyncTask t = e.getValue();
                String s = t.getStatus();
                if ((AsyncTask.STATUS_SUCCESS.equals(s)
                        || AsyncTask.STATUS_FAILED.equals(s)
                        || AsyncTask.STATUS_CANCELLED.equals(s))
                        && t.getEndTime() != null
                        && t.getEndTime().isBefore(cutoff)) {
                    return true;
                }
                return false;
            });
        }
        // 清理源文件(cert_import_ 开头的 xlsx,与对应 end_time < cutoff 一同删除)
        try {
            cleanupSourceFiles(cutoff);
        } catch (Exception e) {
            log.debug("源文件清理失败", e);
        }
        return n;
    }

    /**
     * 清理已过期的源文件(从 DB 查"已结束且过期"的任务,删除 sourceFilePath 指向的文件)
     */
    private int cleanupSourceFiles(LocalDateTime cutoff) {
        List<AsyncTaskEntity> expired = taskMapper.selectList(new LambdaQueryWrapper<AsyncTaskEntity>()
                .lt(AsyncTaskEntity::getEndTime, cutoff)
                .isNotNull(AsyncTaskEntity::getExtraJson)
                .last("LIMIT 500"));
        if (expired.isEmpty()) return 0;
        int n = 0;
        for (AsyncTaskEntity e : expired) {
            if (e.getExtraJson() == null) continue;
            try {
                Map<?, ?> extra = new com.fasterxml.jackson.databind.ObjectMapper().readValue(e.getExtraJson(), Map.class);
                Object p = extra == null ? null : extra.get("sourceFilePath");
                if (p == null) continue;
                File f = new File(p.toString());
                if (f.exists() && f.getName().startsWith("cert_import_")) {
                    if (f.delete()) n++;
                }
            } catch (Exception ignore) { /* skip */ }
        }
        return n;
    }

    /**
     * 标记僵尸任务(running 状态超过 zombieHours)
     * - 场景:服务异常退出,但 running 状态未及时更新为 failed
     * - 这种任务 DB 中 status=running,endTime=null
     */
    public int markZombieTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(zombieHours);
        List<AsyncTaskEntity> zombies = taskMapper.selectList(new LambdaQueryWrapper<AsyncTaskEntity>()
                .eq(AsyncTaskEntity::getStatus, AsyncTask.STATUS_RUNNING)
                .lt(AsyncTaskEntity::getStartTime, cutoff));
        if (zombies.isEmpty()) return 0;
        int n = 0;
        for (AsyncTaskEntity e : zombies) {
            e.setStatus(AsyncTask.STATUS_FAILED);
            e.setErrorMessage("任务执行超过 " + zombieHours + " 小时未结束,被判定为僵尸任务自动清理");
            e.setEndTime(LocalDateTime.now());
            if (taskMapper.updateById(e) > 0) {
                n++;
                // 同步内存
                AsyncTask t = tasks.get(e.getTaskId());
                if (t != null) {
                    t.setStatus(AsyncTask.STATUS_FAILED);
                    t.setErrorMessage(e.getErrorMessage());
                    t.setEndTime(e.getEndTime());
                }
            }
        }
        return n;
    }

    /**
     * 启动时从 DB 加载"未完成"任务到内存
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onAppReady() {
        try {
            List<AsyncTaskEntity> ongoing = taskMapper.selectList(new LambdaQueryWrapper<AsyncTaskEntity>()
                    .in(AsyncTaskEntity::getStatus, AsyncTask.STATUS_PENDING, AsyncTask.STATUS_RUNNING));
            int recovered = 0;
            for (AsyncTaskEntity e : ongoing) {
                // 服务重启后 running 任务实际已中断,标记为 failed
                if (AsyncTask.STATUS_RUNNING.equals(e.getStatus())) {
                    e.setStatus(AsyncTask.STATUS_FAILED);
                    e.setErrorMessage("服务重启导致任务中断,请重新提交");
                    e.setEndTime(LocalDateTime.now());
                    taskMapper.updateById(e);
                    continue;
                }
                AsyncTask t = AsyncTask.fromEntity(e);
                tasks.put(t.getTaskId(), t);
                cancelFlags.put(t.getTaskId(), new AtomicBoolean(false));
                recovered++;
            }
            log.info("异步任务启动恢复: {} 个未完成任务从 DB 加载", recovered);
        } catch (Exception e) {
            log.error("异步任务启动恢复失败", e);
        }
    }

    @Override
    public String submit(String bizType, String bizName, Integer total, Consumer<AsyncTask> handler) {
        return submit(bizType, bizName, total, handler, null);
    }

    @Override
    public String submit(String bizType, String bizName, Integer total, Consumer<AsyncTask> handler, String createdBy) {
        String id = UUID.randomUUID().toString().replace("-", "");
        AsyncTask t = new AsyncTask();
        t.setTaskId(id);
        t.setBizType(bizType);
        t.setBizName(bizName);
        t.setStatus(AsyncTask.STATUS_PENDING);
        t.setProgress(0);
        t.setProcessed(0);
        t.setTotal(total == null ? 0 : total);
        t.setSuccessCount(0);
        t.setFailCount(0);
        t.setStartTime(LocalDateTime.now());
        t.setCreateTime(LocalDateTime.now());
        t.setCreatedBy(createdBy);
        tasks.put(id, t);
        cancelFlags.put(id, new AtomicBoolean(false));
        // 持久化 pending
        try {
            taskMapper.insert(t.toEntity());
            lastFlushMs.put(id, System.currentTimeMillis());
        } catch (Exception ex) {
            log.warn("任务入库失败(非致命): " + id, ex);
        }
        // 通过 Spring 代理调用 @Async 方法
        AsyncTaskServiceImpl self = SpringContextHolder.getBean(AsyncTaskServiceImpl.class);
        if (self != null) {
            self.executeAsync(id, handler);
        } else {
            executeAsync(id, handler);
        }
        return id;
    }

    @Async
    public void executeAsync(String taskId, Consumer<AsyncTask> handler) {
        AsyncTask t = tasks.get(taskId);
        if (t == null) return;
        t.setStatus(AsyncTask.STATUS_RUNNING);
        t.setStartTime(LocalDateTime.now());
        flushToDb(t, true, false);
        try {
            handler.accept(t);
            if (AsyncTask.STATUS_RUNNING.equals(t.getStatus())) {
                t.setStatus(AsyncTask.STATUS_SUCCESS);
                if (t.getProgress() == null || t.getProgress() < 100) {
                    t.setProgress(100);
                }
            }
        } catch (Exception e) {
            log.error("异步任务执行失败: " + taskId, e);
            t.setStatus(AsyncTask.STATUS_FAILED);
            t.setErrorMessage(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            t.setEndTime(LocalDateTime.now());
            cancelFlags.remove(taskId);
            flushToDb(t, true, true);
        }
    }

    @Override
    public AsyncTask get(String taskId) {
        AsyncTask t = tasks.get(taskId);
        if (t != null) return t;
        // 内存未命中(可能服务重启过): 从 DB 查
        AsyncTaskEntity e = taskMapper.selectOne(new LambdaQueryWrapper<AsyncTaskEntity>()
                .eq(AsyncTaskEntity::getTaskId, taskId));
        if (e == null) return null;
        t = AsyncTask.fromEntity(e);
        // 已完成的历史任务,只在内存放一份"只读"缓存
        tasks.put(taskId, t);
        return t;
    }

    @Override
    public List<AsyncTask> listAll() {
        return listWithPaging(null, null, 1, 200).getRecords();
    }

    @Override
    public List<AsyncTask> listActive() {
        // 进行中只查内存
        return tasks.values().stream()
                .filter(t -> AsyncTask.STATUS_PENDING.equals(t.getStatus())
                        || AsyncTask.STATUS_RUNNING.equals(t.getStatus()))
                .sorted(Comparator.comparing(AsyncTask::getStartTime))
                .collect(Collectors.toList());
    }

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<AsyncTask> listWithPaging(
            String bizType, String status, Integer page, Integer size) {
        Page<AsyncTaskEntity> p = new Page<>(page, size);
        LambdaQueryWrapper<AsyncTaskEntity> w = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) w.eq(AsyncTaskEntity::getBizType, bizType);
        if (status != null && !status.isEmpty()) w.eq(AsyncTaskEntity::getStatus, status);
        w.orderByDesc(AsyncTaskEntity::getCreateTime);
        Page<AsyncTaskEntity> res = taskMapper.selectPage(p, w);
        // 转换
        Page<AsyncTask> out = new Page<>(res.getCurrent(), res.getSize(), res.getTotal());
        out.setRecords(res.getRecords().stream().map(AsyncTask::fromEntity).collect(Collectors.toList()));
        return out;
    }

    @Override
    public boolean cancel(String taskId) {
        AsyncTask t = tasks.get(taskId);
        if (t == null) {
            // 内存没有,直接尝试 DB
            AsyncTaskEntity e = taskMapper.selectOne(new LambdaQueryWrapper<AsyncTaskEntity>()
                    .eq(AsyncTaskEntity::getTaskId, taskId));
            if (e == null) return false;
            if (!AsyncTask.STATUS_PENDING.equals(e.getStatus())) return false;
            e.setStatus(AsyncTask.STATUS_CANCELLED);
            e.setEndTime(LocalDateTime.now());
            return taskMapper.updateById(e) > 0;
        }
        if (!AsyncTask.STATUS_PENDING.equals(t.getStatus())) return false;
        AtomicBoolean flag = cancelFlags.get(taskId);
        if (flag != null) flag.set(true);
        t.setStatus(AsyncTask.STATUS_CANCELLED);
        t.setEndTime(LocalDateTime.now());
        flushToDb(t, true, true);
        return true;
    }

    @Override
    public void clearFinished() {
        // 1. 内存清理
        tasks.entrySet().removeIf(e -> {
            String s = e.getValue().getStatus();
            return AsyncTask.STATUS_SUCCESS.equals(s)
                    || AsyncTask.STATUS_FAILED.equals(s)
                    || AsyncTask.STATUS_CANCELLED.equals(s);
        });
        // 2. DB 清理(用 retentionDays,默认 7 天)
        try {
            cleanupExpiredTasks();
        } catch (Exception e) {
            log.warn("清理已完成任务失败", e);
        }
    }

    @Override
    public boolean isCancelled(String taskId) {
        AtomicBoolean flag = cancelFlags.get(taskId);
        return flag != null && flag.get();
    }

    @Override
    public int getRetentionDays() {
        return retentionDays;
    }

    @Override
    public int getZombieHours() {
        return zombieHours;
    }

    @Override
    public void flushTaskExtra(AsyncTask task) {
        if (task == null || task.getTaskId() == null) return;
        try {
            taskMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AsyncTaskEntity>()
                    .eq(AsyncTaskEntity::getTaskId, task.getTaskId())
                    .set(AsyncTaskEntity::getExtraJson, task.getExtraJson()));
            lastFlushMs.put(task.getTaskId(), System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("刷新任务 extraJson 失败: " + task.getTaskId(), e);
        }
    }

    @Override
    public String getSourceFilePath(AsyncTask task) {
        if (task == null || task.getExtraJson() == null) return null;
        try {
            Map<?, ?> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(task.getExtraJson(), Map.class);
            Object p = m.get("sourceFilePath");
            return p == null ? null : p.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 任务进度回调辅助
     */
    public static void setProgress(AsyncTask t, int progress, int processed) {
        t.setProgress(progress);
        t.setProcessed(processed);
    }

    /**
     * 把内存态同步到 DB(节流: 同一任务最少间隔 DB_FLUSH_INTERVAL_MS)
     * @param force       true 强制写(任务开始/结束/取消时)
     * @param finalState  是否是终态(用于事件分发和推送)
     */
    private void flushToDb(AsyncTask t, boolean force, boolean finalState) {
        if (t == null || t.getTaskId() == null) return;
        Long last = lastFlushMs.get(t.getTaskId());
        long now = System.currentTimeMillis();
        if (!force && last != null && now - last < DB_FLUSH_INTERVAL_MS) {
            // 即使节流,如果是终态也走一次完整推送
            if (finalState) publishEvent(t, true);
            return;
        }
        try {
            taskMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AsyncTaskEntity>()
                    .eq(AsyncTaskEntity::getTaskId, t.getTaskId())
                    .set(AsyncTaskEntity::getStatus, t.getStatus())
                    .set(AsyncTaskEntity::getProgress, t.getProgress() == null ? 0 : t.getProgress())
                    .set(AsyncTaskEntity::getProcessed, t.getProcessed() == null ? 0 : t.getProcessed())
                    .set(AsyncTaskEntity::getTotal, t.getTotal() == null ? 0 : t.getTotal())
                    .set(AsyncTaskEntity::getSuccessCount, t.getSuccessCount() == null ? 0 : t.getSuccessCount())
                    .set(AsyncTaskEntity::getFailCount, t.getFailCount() == null ? 0 : t.getFailCount())
                    .set(AsyncTaskEntity::getErrorMessage, t.getErrorMessage())
                    .set(AsyncTaskEntity::getStartTime, t.getStartTime())
                    .set(AsyncTaskEntity::getEndTime, t.getEndTime())
                    .set(AsyncTaskEntity::getResultFilePath, t.getResultFile() != null ? t.getResultFile().getAbsolutePath() : t.getResultFilePath())
                    .set(AsyncTaskEntity::getResultFileName, t.getResultFileName())
                    .set(AsyncTaskEntity::getExtraJson, t.getExtraJson()));
            lastFlushMs.put(t.getTaskId(), now);
        } catch (Exception e) {
            log.debug("任务状态持久化失败: " + t.getTaskId(), e);
        }
        // 触发事件(终态/或每 ~1.5s 推送一次)
        publishEvent(t, finalState);
    }

    private void publishEvent(AsyncTask t, boolean finalState) {
        if (eventPublisher == null) return;
        try {
            eventPublisher.publishEvent(new AsyncTaskEvent(this, t, finalState));
        } catch (Exception e) {
            log.debug("事件发布失败", e);
        }
    }
}
