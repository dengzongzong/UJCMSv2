package com.exam.dto;

import java.util.List;

/**
 * 批量导入照片结果
 */
public class PhotoBatchImportResult {
    private Integer total;          // 总数
    private Integer successCount;   // 成功数
    private Integer failCount;      // 失败数
    private List<FailedItem> failedItems;  // 失败明细

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public List<FailedItem> getFailedItems() { return failedItems; }
    public void setFailedItems(List<FailedItem> failedItems) { this.failedItems = failedItems; }

    public static class FailedItem {
        private String fileName;     // 原始文件名
        private String reason;       // 失败原因

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
