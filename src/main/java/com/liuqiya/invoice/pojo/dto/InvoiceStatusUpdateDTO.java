package com.liuqiya.invoice.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceStatusUpdateDTO {

    @NotNull(message = "invoiceId 不能为空")
    private Long invoiceId;

    /**
     * 要变更的状态类型：
     * FINANCE    —— 财务提交
     * REIMBURSE  —— 最终报销
     * CANCEL     —— 取消发票
     */
    @NotNull(message = "type 不能为空")
    private String type;

    /**
     * 目标状态值（整数）
     * FINANCE:    1=已提交
     * REIMBURSE:  1=成功 / 2=失败
     * CANCEL:     忽略此字段
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
