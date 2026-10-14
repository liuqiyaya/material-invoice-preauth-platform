package com.liuqiya.invoice.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.liuqiya.invoice.enumeration.FileProcessStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 原始文件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("invoice_file")
public class InvoiceFile {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联发票（ZIP 记录为 NULL）
     */
    @TableField("invoice_id")
    private Long invoiceId;

    /**
     * 父文件：ZIP 解压出的 PDF 指向其 ZIP
     */
    @TableField("parent_file_id")
    private Long parentFileId;

    /**
     * 存储 Key
     */
    @TableField("file_key")
    private String fileKey;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * PDF / ZIP / JPG / PNG
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 字节
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 同一批次上传的批次号
     */
    @TableField("upload_batch")
    private String uploadBatch;

    /**
     * 处理状态：0待处理 1处理中 2成功 3失败 4跳过
     */
    @TableField("process_status")
    private FileProcessStatus processStatus;

    /**
     * 处理失败原因
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}