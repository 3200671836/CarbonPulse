package com.carbonpulse.common;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.util.List;


// 新增分页结果封装类
@Data
@Getter
@Setter
public class PaginatedResult<T> {
    private List<T> list;       // 当前页数据列表
    private long total;         // 总记录数
    private int currentPage;    // 当前页码
    private int pageSize;       // 每页数量
    private int totalPages;     // 总页数

    // 全参数构造器
    public PaginatedResult(List<T> list, long total, int currentPage, int pageSize) {
        this.list = list;
        this.total = total;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }
}