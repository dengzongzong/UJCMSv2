package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.QuestionDTO;
import com.exam.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface QuestionManageService extends IService<Question> {

    /**
     * 分页查询题目（含选项和分类名）
     */
    PageResult<Map<String, Object>> page(Integer page, Integer size, Integer type, Long categoryId,
                                         String keyword, String createTimeStart, String createTimeEnd,
                                         Integer enabled, Long professionId);

    /**
     * 题目详情（含选项）
     */
    Map<String, Object> detail(Long id);

    /**
     * 题干重复检测：按题目分类+专业+题干(归一化后精确匹配)查询已存在的题目
     * <p>用于新增/导入试题时，与库中已有题目重复时进行对比展示。
     * 返回匹配到的题目列表(含选项、答案、解析等完整信息)，供前端对比。</p>
     *
     * @param content      题干内容
     * @param categoryId   题目分类ID(可选，为空则不限分类)
     * @param professionId 专业ID(可选，为空则不限专业)
     * @param excludeId    排除的题目ID(编辑场景排除自身)
     * @param type         题目类型(可选，为空则不限题型)
     * @return 重复题目详情列表
     */
    List<Map<String, Object>> checkDuplicate(String content, Long categoryId, Long professionId, Long excludeId, Integer type);

    /**
     * 题干重复检测(兼容旧接口)
     */
    List<Map<String, Object>> checkDuplicate(String content, Long professionId, Long excludeId);

    /**
     * 新增题目（含选项）
     */
    void add(QuestionDTO dto);

    /**
     * 编辑题目（先删除旧选项再重新创建）
     */
    void update(QuestionDTO dto);

    /**
     * 删除题目（含选项、考试关联、错题本）
     */
    void delete(Long id);

    /**
     * 批量删除题目（级联删除选项、错题本等关联数据）
     */
    void batchDelete(List<Long> ids);

    /**
     * 导出题目为Excel
     */
    void export(HttpServletResponse response, Integer type, Long categoryId, String keyword,
                String createTimeStart, String createTimeEnd, Integer enabled, Long professionId);

    /**
     * 下载题目导入模板(含各题型示例与说明)
     */
    void downloadTemplate(HttpServletResponse response);

    /**
     * 批量导入题目(返回成功条数和失败明细)
     */
    Map<String, Object> importQuestions(MultipartFile file);
}
