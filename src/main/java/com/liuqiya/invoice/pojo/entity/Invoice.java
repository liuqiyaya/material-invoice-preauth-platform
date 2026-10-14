package com.liuqiya.invoice.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.liuqiya.invoice.enumeration.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票主表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("invoice")
public class Invoice {
    /**
     * 发票ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 提交人id
     */
    @TableField("submitter_id")
    private Long submitterId;

    /**
     * 销售方名称
     */
    @TableField("seller_name")
    private String sellerName;

    /**
     * 销售方纳税人识别号
     */
    @TableField("seller_tax_no")
    private String sellerTaxNo;

    /**
     * 购买方名称
     */
    @TableField("buyer_name")
    private String buyerName;

    /**
     * 购买方纳税人识别号
     */
    @TableField("buyer_tax_no")
    private String buyerTaxNo;

    /**
     * 发票号码
     */
    @TableField("invoice_no")
    private String invoiceNo;

    /**
     * 开票日期
     */
    @TableField("invoice_date")
    private LocalDate invoiceDate;

    /**
     * 价税合计
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 提交时间
     */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /**
     * 所属自然周周一
     */
    @TableField("week_start")
    private LocalDate weekStart;

    /**
     * 资质审核状态
     * 0-待审核, 1-待补凭证, 2-待人工处理, 3-通过, 4-不通过, 5-已取消
     */
    @TableField("qualify_status")
    private QualifyStatus qualifyStatus;

    /**
     * 财务提交状态
     * 0-待提交, 1-已提交
     */
    @TableField("finance_status")
    private FinanceStatus financeStatus;

    /**
     * 最终报销状态
     * 0-未完成, 1-成功, 2-失败
     */
    @TableField("reimburse_status")
    private ReimburseStatus reimburseStatus;

    /**
     * 不通过原因
     */
    @TableField("reject_reason")
    private String rejectReason;

    /**
     * 整体品类结论：1可以 2存疑 3不可以
     */
    @TableField("category_result")
    private CategoryLevel categoryResult;

    /**
     * 是否被要求补凭证：0否 1是
     */
    @TableField("evidence_required")
    private Integer evidenceRequired;

    /**
     * 补凭证原因：1低值品 2累计超1000
     */
    @TableField("evidence_reason")
    private EvidenceReason evidenceReason;

    /**
     * 是否需人工兜底：0否 1是
     */
    @TableField("need_manual")
    private Integer needManual;

    /**
     * 人工兜底备注
     */
    @TableField("manual_remark")
    private String manualRemark;

    /**
     * 原始文件定位
     */
    @TableField("source_file_id")
    private Long sourceFileId;

    /**
     * AI 结构化原始结果 JSON
     */
    @TableField("ai_raw_result")
    private String aiRawResult;

    /**
     * 逻辑删除：0否 1是
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;




}
