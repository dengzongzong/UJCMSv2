package com.exam.controller;

import com.exam.common.AsyncTask;
import com.exam.common.BusinessException;
import com.exam.common.Result;
import com.exam.service.AsyncTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 异步任务查询/取消/下载结果
 */
@RestController
@RequestMapping("/admin/task")
public class AsyncTaskController {

    @Autowired
    private AsyncTaskService taskService;

    /**
     * 查询单个任务
     */
    @GetMapping("/{taskId}")
    public Result<AsyncTask> get(@PathVariable String taskId) {
        return Result.success(taskService.get(taskId));
    }

    /**
     * 查询所有进行中的任务(轮询用)
     */
    @GetMapping("/active")
    public Result<List<AsyncTask>> active() {
        return Result.success(taskService.listActive());
    }

    /**
     * 查询所有任务(展示历史用)
     */
    @GetMapping("/list")
    public Result<List<AsyncTask>> list() {
        return Result.success(taskService.listAll());
    }

    /**
     * 分页查询(支持 bizType / status 筛选)
     */
    @GetMapping("/page")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AsyncTask>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String status) {
        return Result.success(taskService.listWithPaging(bizType, status, page, size));
    }

    /**
     * 取消任务(对未开始的任务生效)
     */
    @PostMapping("/{taskId}/cancel")
    public Result<Boolean> cancel(@PathVariable String taskId) {
        return Result.success(taskService.cancel(taskId));
    }

    /**
     * 重试失败的任务(重新提交一个等价的任务)
     */
    @PostMapping("/{taskId}/retry")
    public Result<?> retry(@PathVariable String taskId) {
        // 引入 CertificateTaskService 注入(由调用方注入,这里用静态持有避免循环依赖)
        com.exam.service.CertificateTaskService certTaskService =
                com.exam.common.SpringContextHolder.getBean(com.exam.service.CertificateTaskService.class);
        if (certTaskService == null) {
            throw new BusinessException("CertificateTaskService 不可用");
        }
        String newTaskId = certTaskService.retry(taskId);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("originalTaskId", taskId);
        data.put("newTaskId", newTaskId);
        return Result.success("已提交重试任务", data);
    }

    /**
     * 清理已完成任务(内存立即清,DB 用 retentionDays)
     */
    @DeleteMapping("/finished")
    public Result<Void> clearFinished() {
        taskService.clearFinished();
        return Result.success();
    }

    /**
     * 手动触发 TTL 清理(立即执行,不用等定时)
     */
    @PostMapping("/cleanup")
    public Result<?> cleanup() {
        int deleted = taskService.cleanupExpiredTasks();
        int zombies = taskService.markZombieTasks();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("deletedExpired", deleted);
        data.put("markedZombie", zombies);
        return Result.success("清理完成", data);
    }

    /**
     * 查看 TTL 配置
     */
    @GetMapping("/config")
    public Result<?> config() {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("retentionDays", taskService.getRetentionDays());
        data.put("zombieHours", taskService.getZombieHours());
        return Result.success(data);
    }

    /**
     * 下载任务结果文件
     */
    @GetMapping("/{taskId}/download")
    public void download(@PathVariable String taskId, HttpServletResponse response) throws Exception {
        AsyncTask t = taskService.get(taskId);
        if (t == null) throw new BusinessException("任务不存在");
        if (t.getResultFile() == null || !t.getResultFile().exists()) {
            throw new BusinessException("结果文件不存在或已过期");
        }
        String fileName = t.getResultFileName() == null
                ? t.getResultFile().getName()
                : t.getResultFileName();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded);
        try (InputStream in = new BufferedInputStream(new FileInputStream(t.getResultFile()));
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }
}
