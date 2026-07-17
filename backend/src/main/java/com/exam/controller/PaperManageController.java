package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.AutoGeneratePaperDTO;
import com.exam.dto.PaperDTO;
import com.exam.entity.Paper;
import com.exam.service.PaperManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 试卷管理
 */
@RestController
@RequestMapping("/admin/paper")
public class PaperManageController {

    @Autowired
    private PaperManageService paperManageService;

    /**
     * 分页查询试卷
     */
    @GetMapping("/page")
    public Result<PageResult<Paper>> page(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) Long professionId,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Paper> result = paperManageService.page(page, size, name, status, professionId);
        return Result.success(result);
    }

    /**
     * 试卷详情（含题目列表）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> result = paperManageService.detail(id);
        return Result.success(result);
    }

    /**
     * 获取所有已发布试卷（供考试创建时选择）
     */
    @GetMapping("/list")
    public Result<List<Paper>> list(@RequestParam(required = false) Long professionId) {
        List<Paper> result = paperManageService.list(professionId);
        return Result.success(result);
    }

    /**
     * 新增试卷
     */
    @PostMapping
    public Result<Void> add(@RequestBody PaperDTO dto) {
        paperManageService.add(dto);
        return Result.success();
    }

    /**
     * 编辑试卷
     */
    @PutMapping
    public Result<Void> update(@RequestBody PaperDTO dto) {
        paperManageService.update(dto);
        return Result.success();
    }

    /**
     * 删除试卷
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paperManageService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除试卷
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        paperManageService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 一键抽题组卷: 按题型数量从题库随机抽题生成试卷
     */
    @PostMapping("/auto-generate")
    public Result<Map<String, Object>> autoGenerate(@RequestBody AutoGeneratePaperDTO dto) {
        Long paperId = paperManageService.autoGenerate(dto);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", paperId);
        return Result.success(data);
    }
}
