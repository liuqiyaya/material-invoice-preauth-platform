package com.liuqiya.invoice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付凭证详情
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvidenceGroupVO {
    private Long id;
    private Long invoiceId;

    /**
     *  组状态：0待上传 1待人工审核 2通过 3不通过
     */
    private Integer status;
    private String statusDesc;

    /**
     *  审核人 id
     */
    private Long reviewerId;
    /**
     *  审核时间
     */
    private LocalDateTime reviewTime;
    /**
     *  审核备注（可选）
     */
    private String reviewRemark;
    /**
     *  创建时间
     */
    private LocalDateTime createTime;
    /**
     *  订单截图列表
     */
    private List<FileVO> screenshots = new ArrayList<>();

    /**
     *  支付记录列表
     */
    private List<FileVO> payments = new ArrayList<>();

    @Data
    public static class FileVO {
        private Long id;
        private Long fileId;
        private String filePath;
        private String originalName;
    }
}
