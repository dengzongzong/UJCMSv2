package com.exam.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long page;
    private long size;
    private List<T> records;

    public PageResult() {}

    public PageResult(IPage<T> page) {
        this.total = page.getTotal();
        this.page = page.getCurrent();
        this.size = page.getSize();
        this.records = page.getRecords();
    }

    /**
     * 全字段构造(用于 pageWithTemplateName 等"自定义结果"场景)
     * <p>第 5 个参数 records 显式带上类型, 避免编译器因没有匹配的构造方法而无法推断 T</p>
     */
    public PageResult(long total, long page, long size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }
}
