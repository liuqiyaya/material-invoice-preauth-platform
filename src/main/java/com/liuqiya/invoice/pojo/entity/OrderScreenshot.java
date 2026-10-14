package com.liuqiya.invoice.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 订单截图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("order_screenshot")
public class OrderScreenshot {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属凭证组
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 关联 invoice_file.id
     */
    @TableField("file_id")
    private Long fileId;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}