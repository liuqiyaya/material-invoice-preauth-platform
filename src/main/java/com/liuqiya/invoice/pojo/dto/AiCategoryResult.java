package com.liuqiya.invoice.pojo.dto;

import lombok.Data;

@Data
public class AiCategoryResult {

    /**
     * 结论：1可以 2存疑 3不可以
     */
    private Integer level;

    /**
     * 归入的品类名
     */
    private String categoryName;

    /**
     * 判断理由
     */
    private String reason;
}
