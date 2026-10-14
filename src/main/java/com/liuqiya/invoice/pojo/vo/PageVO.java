package com.liuqiya.invoice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVO<T> {
    /**
     * 总条数
     */
    private Long total;

    /**
     * 当前页
     */
    private Long pageNum;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 数据
     */
    private List<T> records;

    public static <T> PageVO<T> of(Long total, Long pageNum, Long pageSize, List<T> records) {
        PageVO<T> vo = new PageVO<>();
        vo.setTotal(total);
        vo.setPageNum(pageNum);
        vo.setPageSize(pageSize);
        vo.setPages(pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize);
        vo.setRecords(records);
        return vo;
    }
}
