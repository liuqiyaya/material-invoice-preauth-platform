package com.liuqiya.invoice.service;

import com.liuqiya.invoice.pojo.dto.StructuredInvoice;

/**
 * AI结构化发票服务
 */
public interface AiStructureService {
    /**
     * 结构化发票
     */
    StructuredInvoice structure(String rawText);

}
