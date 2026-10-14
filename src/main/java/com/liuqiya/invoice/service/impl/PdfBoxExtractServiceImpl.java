package com.liuqiya.invoice.service.impl;

import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.service.PdfExtractService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class PdfBoxExtractServiceImpl implements PdfExtractService {

    /**
     * 从 PDF 提取文本层内容
     */
    @Override
    public String extractText(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new BizException(400, "PDF 文件不存在");
        }
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 按页提取，保留换行
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("PDF 文本提取完成：{}，长度 {} 字符",
                    pdfFile.getName(), text == null ? 0 : text.length());
            return text == null ? "" : text.trim();
        } catch (IOException e) {
            log.error("PDF 文本提取失败：{}", pdfFile.getAbsolutePath(), e);
            throw new BizException(2005, "PDF 文本提取失败：" + e.getMessage());
        }

    }
}
