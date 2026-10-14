package com.liuqiya.invoice.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付凭证上传DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvidenceUploadDTO {

    /**
     *  发票 id
     */
    private Long invoiceId;

    /**
     *  审核备注（可选）
     */
    private String remark;
}
