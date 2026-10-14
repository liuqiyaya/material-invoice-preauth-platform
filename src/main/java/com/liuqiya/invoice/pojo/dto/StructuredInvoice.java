package com.liuqiya.invoice.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructuredInvoice {
    /**
     * 销售方名称
     */
    private String sellerName;

    /**
     * 销售方纳税人识别号
     */
    private String sellerTaxNo;

    /**
     * 购买方名称
     */
    private String buyerName;

    /**
     * 购买方税号
     */
    private String buyerTaxNo;

    /**
     * 发票号码
     */
    private String invoiceNo;

    /**
     * 开票日期
     */
    private LocalDate invoiceDate;

    /**
     * 价税合计
     */
    private BigDecimal totalAmount;

    /**
     * 商品明细
     */
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        /**
         * 商品名称
         */
        private String itemName;
        /**
         * 数量
         */
        private BigDecimal quantity;
        /**
         * 单价
         */
        private BigDecimal unitPrice;
        /**
         * 金额
         */
        private BigDecimal amount;
    }
}
