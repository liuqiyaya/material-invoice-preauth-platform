package com.liuqiya.invoice.service;

import java.io.File;

/**
 * PDF 文本提取
 */
public interface PdfExtractService {
    /**
     * 从 PDF 提取文本层内容
     */
    String extractText(File pdfFile);
}
