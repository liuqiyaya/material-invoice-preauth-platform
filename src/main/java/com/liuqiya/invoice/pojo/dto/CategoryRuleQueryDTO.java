package com.liuqiya.invoice.pojo.dto;

import lombok.Data;

/**
 * 品类规则查询DTO
 */
@Data
public class CategoryRuleQueryDTO extends CategoryRuleDTO {
    private Long pageNum = 1L;
    private Long pageSize = 20L;

    /**
     * 品类名（模糊）
     */
    private String categoryName;

    /**
     * 结论：1/2/3
     */
    private Integer level;

    /**
     * 是否启用：0/1
     */
    private Integer enabled;

    /**
     * 关键词（模糊）
     */
    private String keyword;
}
