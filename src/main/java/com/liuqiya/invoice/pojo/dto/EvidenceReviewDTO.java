package com.liuqiya.invoice.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付凭证审核DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvidenceReviewDTO {

    /**
     * 凭证组 id
     */
    @NotNull(message = "groupId 不能为空")
    private Long groupId;

    /**
     * 是否通过：true 通过，false 不通过
     */
    @NotNull(message = "pass 不能为空")
    private Boolean pass;

    /**
     * 审核备注
     */
    private String remark;
}
