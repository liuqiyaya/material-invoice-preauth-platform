package com.liuqiya.invoice.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.liuqiya.invoice.enumeration.EvidenceGroupStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 支付凭证组
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("payment_evidence_group")
public class PaymentEvidenceGroup {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属发票（一组只能属于一张）
     */
    @TableField("invoice_id")
    private Long invoiceId;

    /**
     * 凭证组状态：0待上传 1待人工审核 2通过 3不通过
     */
    @TableField("status")
    private EvidenceGroupStatus status;

    /**
     * 审核人
     */
    @TableField("reviewer_id")
    private Long reviewerId;

    /**
     * 审核时间
     */
    @TableField("review_time")
    private LocalDateTime reviewTime;

    /**
     * 审核备注
     */
    @TableField("review_remark")
    private String reviewRemark;

    /**
     * 逻辑删除：0否 1是
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}