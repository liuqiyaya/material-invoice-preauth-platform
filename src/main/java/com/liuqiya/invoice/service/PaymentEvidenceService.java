package com.liuqiya.invoice.service;

import com.liuqiya.invoice.pojo.vo.EvidenceGroupVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 支付凭证服务
 */
public interface PaymentEvidenceService {

    /**
     * 上传一组凭证
     */
    Long uploadPaymentEvidenceGroup(Long invoiceId, MultipartFile[] screenshots, MultipartFile[] payments, String remark);

    /**
     * 查询某张发票的所有凭证组
     */
    List<EvidenceGroupVO> getPaymentEvidenceListByInvoice(Long invoiceId);

    /**
     * 查询凭证组详情
     */
    EvidenceGroupVO getPaymentEvidenceGroupDetail(Long groupId);

    /**
     * 审核一组凭证
     */
    void reviewPaymentEvidenceGroup(Long groupId, Boolean pass, String remark, Long reviewerId);

}
