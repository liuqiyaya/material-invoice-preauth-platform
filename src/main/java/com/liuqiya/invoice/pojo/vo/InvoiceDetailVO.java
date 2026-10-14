package com.liuqiya.invoice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 发票详情VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceDetailVO {
    // 主表字段
    private Long id;
    private Long submitterId;

    private String sellerName;
    private String sellerTaxNo;
    private String buyerName;
    private String buyerTaxNo;
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
    private String rejectReason;
    private Integer evidenceRequired;
    private Integer evidenceReason;
    private Integer needManual;
    private String manualRemark;

    private Long sourceFileId;
    private String sourceFileKey;      // 原始 PDF fileKey
    private String sourceFileName;     // 原始 PDF 文件名

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 商品明细
    private List<ItemVO> items = new ArrayList<>();

    // 凭证组
    private List<EvidenceGroupVO> evidenceGroups = new ArrayList<>();

    // 操作日志
    private List<LogVO> logs = new ArrayList<>();

    @Data
    public static class ItemVO {
        private Long id;
        private String itemName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private Integer categoryLevel;
        private String categoryLevelDesc;
        private String categoryName;
        private Integer priceType;
        private String priceTypeDesc;
        private String aiReason;
    }

    @Data
    public static class LogVO {
        private Long id;
        private Long operatorId;
        private String operatorName;
        private String action;
        private String beforeValue;
        private String afterValue;
        private String remark;
        private LocalDateTime createTime;
    }

    /**
     * 疑似重复的发票列表
     */
    private List<InvoiceListVO> duplicates = new ArrayList<>();
}
