package com.liuqiya.invoice.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 品类规则DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRuleDTO {
    /**
     * 修改时必填，新增时不填
     */
    private Long id;

    @NotBlank(message = "品类名不能为空")
    private String categoryName;

    /**
     * 关键词，逗号分隔
     */
    private String keywords;

    /**
     * 结论：1可以 2存疑 3不可以
     */
    @NotNull(message = "level 不能为空")
    private Integer level;

    private String remark;

    /**
     * 是否启用：0禁用 1启用，不传默认启用
     */
    private Integer enabled;
}
