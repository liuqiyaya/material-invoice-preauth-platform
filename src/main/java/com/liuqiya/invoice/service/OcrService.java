package com.liuqiya.invoice.service;

import java.io.File;

public interface OcrService {
    /**
     * pdf文件OCR处理
     */
    String ocrPdf(File pdfFile);

    /**
     * 单张图片OCR处理
     */
    String ocrImage(byte[] imageBytes);
}
