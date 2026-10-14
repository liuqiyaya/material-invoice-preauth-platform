package com.liuqiya.invoice.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("operation_log")
public class OperationLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联发票
     */
    @TableField("invoice_id")
    private Long invoiceId;

    /**
     * 操作人id
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作人名称
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 动作：AI_STRUCT/MANUAL_EDIT/STATUS_CHANGE/EVIDENCE_REVIEW等
     */
    @TableField("action")
    private String action;

    /**
     * 修改前值
     */
    @TableField("before_value")
    private String beforeValue;

    /**
     * 修改后值
     */
    @TableField("after_value")
    private String afterValue;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}