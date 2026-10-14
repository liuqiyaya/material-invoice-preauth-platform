package com.liuqiya.invoice.pojo.dto;

import com.liuqiya.invoice.enumeration.CategoryLevel;
import com.liuqiya.invoice.enumeration.EvidenceReason;
import com.liuqiya.invoice.enumeration.PriceType;
import com.liuqiya.invoice.enumeration.QualifyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleResultDTO {
    /**
     * 发票 id
     */
    private Long invoiceId;

    /**
     * 最终资质审核状态
     */
    private QualifyStatus qualifyStatus;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 整体品类结论
     */
    private CategoryLevel categoryResult;

    /**
     * 是否要求补凭证
     */
    private Integer evidenceRequired = 0;

    /**
     * 补凭证原因
     */
    private EvidenceReason evidenceReason;

    /**
     * 是否需要人工处理
     */
    private Integer needManual = 0;

    /**
     * 周累计信息
     */
    private BigDecimal weekAccumulatedBefore;   // 已有累计
    private BigDecimal weekAccumulatedAfter;    // 加上当前后
    private String sellerTaxNo;
    private java.time.LocalDate weekStart;

    /**
     * 逐项判定结果
     */
    private List<ItemRuleResult> itemResults = new ArrayList<>();

    @Data
    public static class ItemRuleResult {
        /**
         * 商品明细 id（写库后才知道，这里先留 null）
         */
        private Long itemId;
        /**
         * 商品名
         */
        private String itemName;
        /**
         * 单价
         */
        private BigDecimal unitPrice;
        /**
         * 品类三态
         */
        private CategoryLevel categoryLevel;
        /**
         * 品类名
         */
        private String categoryName;
        /**
         * 单价类别
         */
        private PriceType priceType;
        /**
         * AI 理由
         */
        private String aiReason;
    }
}
