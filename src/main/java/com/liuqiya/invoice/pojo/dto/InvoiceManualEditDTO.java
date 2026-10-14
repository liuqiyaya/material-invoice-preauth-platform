package com.liuqiya.invoice.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 人工修改请求
 * 只传要改的字段，为 null 的字段不动
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceManualEditDTO {
    /**
     *  发票 id（必填）
     */
    @NotNull(message = "发票 id 必填")
    private Long invoiceId;

    /**
     *  修改备注（可选，记录到日志）
     */
    private String remark;

    // ============ 发票主表字段（可改） ============

    private String sellerName;
    private String sellerTaxNo;
    private String buyerName;
    private String buyerTaxNo;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;

    /**
     *  资质审核状态（管理员手动改）
     */
    private Integer qualifyStatus;

    /**
     *  人工备注
     */
    private String manualRemark;

    /**
     *  是否标记为需要人工（0/1）
     */
    private Integer needManual;

    // ============ 商品明细（可改） ============

    /**
     *  商品明细列表，传了就全量替换
     */
    private List<ItemDTO> items;

    @Data
    public static class ItemDTO {
        private Long id;              // 已有 item 传 id，新增不传
        private String itemName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private Integer categoryLevel;   // 品类三态
        private String categoryName;
        private Integer priceType;       // 单价类别
        private String aiReason;
    }
}
