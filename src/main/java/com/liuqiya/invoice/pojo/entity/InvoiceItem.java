package com.liuqiya.invoice.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.liuqiya.invoice.enumeration.CategoryLevel;
import com.liuqiya.invoice.enumeration.PriceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票商品明细
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("invoice_item")
public class InvoiceItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属发票
     */
    @TableField("invoice_id")
    private Long invoiceId;

    /**
     * 商品名称
     */
    @TableField("item_name")
    private String itemName;

    /**
     * 数量
     */
    @TableField("quantity")
    private BigDecimal quantity;

    /**
     * 单价
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * AI品类三态：1可以 2存疑 3不可以
     */
    @TableField("category_level")
    private CategoryLevel categoryLevel;

    /**
     * 归入的品类名
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 单价类别：1材料 2低值品 3资产
     */
    @TableField("price_type")
    private PriceType priceType;

    /**
     * AI判断理由
     */
    @TableField("ai_reason")
    private String aiReason;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}