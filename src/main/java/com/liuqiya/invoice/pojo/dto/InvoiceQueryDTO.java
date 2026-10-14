package com.liuqiya.invoice.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发票查询DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceQueryDTO {
    /**
     * 页码，从 1 开始
     */
    private Long pageNum = 1L;

    /**
     * 每页条数
     */
    private Long pageSize = 20L;

    /**
     * 销售方名称（模糊）
     */
    private String sellerName;

    /**
     * 销售方税号（精确）
     */
    private String sellerTaxNo;

    /**
     * 发票号码（精确）
     */
    private String invoiceNo;

    /**
     * 资质审核状态
     */
    private Integer qualifyStatus;

    /**
     * 财务提交状态
     */
    private Integer financeStatus;

    /**
     * 最终报销状态
     */
    private Integer reimburseStatus;

    /**
     * 所属自然周（yyyy-MM-dd 格式，会匹配 week_start）
     */
    private String weekStart;

    /**
     * 上传批次号
     */
    private String uploadBatch;

    /**
     * 提交时间起（yyyy-MM-dd）
     */
    private String submitTimeFrom;

    /**
     * 提交时间止（yyyy-MM-dd）
     */
    private String submitTimeTo;
}
