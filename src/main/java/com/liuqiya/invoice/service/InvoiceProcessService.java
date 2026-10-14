package com.liuqiya.invoice.service;

/**
 * 发票预审调度服务实现
 */
public interface InvoiceProcessService {

    /**
     * 处理单张发票
     */
    void process(Long invoiceId);

    /**
     * 异步处理（上传后调用）
     */
    void processAsync(Long invoiceId);
}
