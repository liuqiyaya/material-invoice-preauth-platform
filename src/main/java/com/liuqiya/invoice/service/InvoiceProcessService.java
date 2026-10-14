package com.liuqiya.invoice.service;

/**
 * 发票预审调度服务实现
 */
public interface InvoiceProcessService {

    /**
     * 处理单张发票（同步调用）
     * 内部可能较慢（AI 调用），建议外层用 @Async
     *
     * @param invoiceId 发票 id
     */
    void process(Long invoiceId);

    /**
     * 异步处理（上传后调用）
     * ⚠️ 必须走 Spring 代理，不能在同类内部调用
     */
    void processAsync(Long invoiceId);
}
