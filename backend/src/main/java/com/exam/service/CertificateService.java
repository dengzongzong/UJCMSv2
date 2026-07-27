package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.CertificateDTO;
import com.exam.dto.CertificateImportResult;
import com.exam.dto.CertificateImportRow;
import com.exam.entity.Certificate;
import com.exam.vo.CertificateVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface CertificateService extends IService<Certificate> {

    /**
     * 分页查询证书
     */
    PageResult<Certificate> page(Integer page, Integer size, String name,
                                  String idCard, String agency,
                                  String issueDateStart, String issueDateEnd);

    /**
     * 分页查询证书(带 templateName,JOIN certificate_template 拿模板名)
     * <p>替代 page() 给 List 页用: 多返一个 templateName 字段,用于"模板"列展示</p>
     */
    PageResult<Map<String, Object>> pageWithTemplateName(Integer page, Integer size,
                                                        String name, String idCard,
                                                        String agency,
                                                        String issueDateStart,
                                                        String issueDateEnd);

    /**
     * 分页查询证书(带 templateName,JOIN certificate_template 拿模板名, 支持按 certType SQL 级过滤)
     * <p>certType 非空时, 在 SQL 中追加 WHERE cert_type = ? 条件, 避免查 1 万条再内存过滤。</p>
     */
    PageResult<Map<String, Object>> pageWithTemplateName(Integer page, Integer size,
                                                        String name, String idCard,
                                                        String agency,
                                                        String issueDateStart,
                                                        String issueDateEnd,
                                                        String certType);

    /**
     * 分页查询证书(带 templateName + 按 profession 模糊过滤)
     */
    PageResult<Map<String, Object>> pageWithTemplateNameAndProfession(Integer page, Integer size,
                                                                     String name, String idCard,
                                                                     String agency,
                                                                     String profession,
                                                                     String issueDateStart,
                                                                     String issueDateEnd);

    /**
     * 分页查询证书(带 templateName + 按 profession 模糊过滤 + 按 certType SQL 级过滤)
     * <p>certType 非空时, 在 SQL 中追加 WHERE cert_type = ? 条件, 避免查 1 万条再内存过滤。</p>
     */
    PageResult<Map<String, Object>> pageWithTemplateNameAndProfession(Integer page, Integer size,
                                                                     String name, String idCard,
                                                                     String agency,
                                                                     String profession,
                                                                     String issueDateStart,
                                                                     String issueDateEnd,
                                                                     String certType);

    /**
     * 证书详情
     */
    CertificateVO detail(Long id);

    /**
     * 新增证书(自动生成编号、性别)
     */
    boolean add(CertificateDTO dto);

    /**
     * 检查是否存在 姓名+身份证号+专业+级别 完全相同的证书记录
     * 用于导入时数据查重:四项完全相同返回 true(不允许导入)
     */
    boolean existsByNameIdCardProfessionLevel(String name, String idCard, String profession, String skillLevel);

    /**
     * 编辑证书
     */
    void update(CertificateDTO dto);

    /**
     * 删除证书(可批量)
     */
    void delete(List<Long> ids);

    /**
     * 解析 Excel(dry-run,只解析不入库)
     * - 返回 CertificateImportResult { dryRun: true, pendingRows, failedRows, dryRunToken }
     * - 真正入库由 commitImport 完成
     */
    CertificateImportResult parseExcel(MultipartFile file);

    /**
     * 提交 dry-run 结果入库
     * - pendingRows 不为空,直接入库
     * - dryRunToken 不为空,反序列化后入库
     */
    CertificateImportResult commitImport(List<CertificateImportRow> pendingRows, String dryRunToken);

    /**
     * 提交 dry-run 结果入库(老接口,无 token)
     */
    CertificateImportResult commitImport(List<CertificateImportRow> pendingRows);

    /**
     * 下载导入模板(Excel)
     */
    void downloadTemplate(HttpServletResponse response) throws Exception;

    /**
     * 下载导入模板(Excel,可自定义sheet名和文件名)
     */
    void downloadTemplate(HttpServletResponse response, String sheetName, String fileName) throws Exception;

    /**
     * 切换考试二维码启用状态(单条/批量/全部)
     */
    void switchExamQr(List<Long> ids, Boolean allSelected, Integer enabled);

    /**
     * 解析单行(供异步任务调用,无副作用)
     */
    CertificateImportRow parseImportRow(Map<Integer, String> row, int rowIndex);

    /**
     * 把解析行转换为 DTO(供异步任务调用)
     */
    CertificateDTO toImportDto(CertificateImportRow row);

    /**
     * 把 DTO 转回 ImportRow(供异步 dry-run 编码 token 使用)
     */
    CertificateImportRow toImportDtoForRow(CertificateDTO dto);

    /**
     * 从任务 ID 获取任务(供 controller 查询 dry-run token 使用)
     */
    com.exam.common.AsyncTask getTaskById(String taskId);

    /**
     * 考试交卷后,将理论成绩回写到匹配的证书用户记录
     * 匹配规则: 身份证号 + 专业名称
     * 回写: extra_json 中的 theoryScore 和 comprehensiveEvaluation(综合成绩=理论成绩)
     */
    void syncTheoryScore(String idCard, String profession, String scoreStr);

    /**
     * 按列表筛选条件查询所有"已绑定模板"的证书 ID(用于批量下载全部)
     * 仅返回 template_id 非空的证书(可渲染的证书)
     */
    List<Long> listFilteredIdsWithTemplate(String name, String idCard, String agency,
                                           String profession, String issueDateStart, String issueDateEnd);

    /**
     * 导出证书数据(Excel,按证书绑定的模板分组导出)
     * 多个模板时导出ZIP(一个模板一个Excel文件),未绑定模板的证书自动过滤
     * @param ids 选中的证书ID列表(为空则按筛选条件导出全部)
     */
    void exportCertificates(HttpServletResponse response, String name, String idCard,
                            String agency, String profession,
                            String issueDateStart, String issueDateEnd,
                            List<Long> ids);

    /**
     * 按证书列表导出(Excel,按证书绑定的模板分组导出)
     * 与 exportCertificates 类似,但直接传入证书列表,供证书用户导出复用。
     * 未绑定模板的证书自动过滤;多个模板时导出ZIP。
     */
    void exportCertificateList(HttpServletResponse response, List<Certificate> certs);

    /**
     * 从学生管理同步数据到证书表(certificate)。
     * 按身份证号+专业维度检查,已存在的不重复创建。
     * @return 新创建的记录数
     */
    int syncFromStudents(String certType);

    /**
     * 下载学生导入专用模板(25列,独立于证书管理模板)
     */
    void downloadStudentTemplate(HttpServletResponse response, String sheetName, String fileName) throws Exception;

    /**
     * 解析学生导入Excel(25列,独立于证书管理模板)
     * 返回 pendingRows(供学生管理直接使用),不执行 dry-run / 入库
     */
    CertificateImportResult parseStudentExcel(MultipartFile file);
}
