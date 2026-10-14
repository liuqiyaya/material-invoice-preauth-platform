package com.liuqiya.invoice.service;

import com.liuqiya.invoice.pojo.dto.*;
import com.liuqiya.invoice.pojo.entity.Invoice;
import com.liuqiya.invoice.pojo.vo.InvoiceDetailVO;
import com.liuqiya.invoice.pojo.vo.InvoiceListVO;
import com.liuqiya.invoice.pojo.vo.PageVO;
import com.liuqiya.invoice.pojo.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 发票业务
 */
public interface InvoiceService {
    /**
     * 上传发票文件
     */
    UploadResultVO uploadInvoiceFile(MultipartFile[] files,Long adminId);

    /**
     * 保存AI结构化发票结果和原始 JSON
     */
    void persistStructured(Long invoiceId, StructuredInvoice structured, String rawAiJson);

    /**
     * 对一张发票做完整判定
     */
    RuleResultDTO evaluate(Invoice invoice, StructuredInvoice structured);

    /**
     * 分页查询发票列表
     */
    PageVO<InvoiceListVO> getInvoiceList(InvoiceQueryDTO query);

    /**
     * 查询发票详情
     */
    InvoiceDetailVO getInvoiceDetail(Long invoiceId);

    /**
     * 人工修改发票（结构化字段 + 商品明细）
     */
    void manualEditInvoice(InvoiceManualEditDTO dto, Long operatorId);

    /**
     * 变更状态
     */
    void updateInvoiceStatus(InvoiceStatusUpdateDTO dto, Long operatorId);

    /**
     * 检测发票是否疑似重复
     */
    List<InvoiceListVO> detectAndMark(Long invoiceId);

    /**
     * 查询某张发票的疑似重复列表
     */
    List<InvoiceListVO> findDuplicates(Long invoiceId);

}

