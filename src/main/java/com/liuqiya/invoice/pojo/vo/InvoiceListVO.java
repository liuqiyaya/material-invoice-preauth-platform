package com.liuqiya.invoice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票列表VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceListVO {
    private Long id;

    private String sellerName;
    private String sellerTaxNo;

    private String invoiceNo;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;

    private LocalDateTime submitTime;
    private LocalDate weekStart;

    private Integer qualifyStatus;
    private String qualifyStatusDesc;

    private Integer financeStatus;
    private String financeStatusDesc;

    private Integer reimburseStatus;
    private String reimburseStatusDesc;

    private Integer categoryResult;
    private String categoryResultDesc;

    private Integer evidenceRequired;

    private String rejectReason;

    private Integer needManual;

    private LocalDateTime createTime;
}
