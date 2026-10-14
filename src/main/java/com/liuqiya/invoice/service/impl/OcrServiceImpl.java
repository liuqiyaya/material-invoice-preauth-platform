package com.liuqiya.invoice.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeGeneralRequest;
import com.aliyun.ocr_api20210707.models.RecognizeGeneralResponse;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * OCR服务实现类
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    @Autowired
    private Client ocrClient;

    /**
     * PDF 转图片的 DPI，越高越清晰但越慢
     */
    @Value("${app.ocr.dpi:200}")
    private int dpi;

    /**
     * pdf文件OCR处理
     */
    @Override
    public String ocrPdf(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new BizException(400, "PDF 文件不存在");
        }

        StringBuilder result = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            //PDF渲染器
            PDFRenderer renderer = new PDFRenderer(document);
            //PDF页数
            int pageCount = document.getNumberOfPages();

            log.info("开始 OCR：{}，共 {} 页", pdfFile.getName(), pageCount);

            for (int i = 0; i < pageCount; i++) {
                // PDF 每页渲染成图片
                BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);

                // 图片转 PNG 字节数组（内存处理）
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                byte[] imageBytes = baos.toByteArray();

                // 调阿里云 OCR 识别图片内容
                String pageText = doRecognize(imageBytes);
                // 追加当前页文本以及换行分隔页
                result.append(pageText).append("\n");

                log.info("OCR 第 {}/{} 页完成，识别 {} 字", i + 1, pageCount, pageText.length());
            }
        } catch (IOException e) {
            log.error("PDF OCR 失败：{}", pdfFile.getAbsolutePath(), e);
            throw new BizException(2004, "OCR 失败：" + e.getMessage());
        }

        String text = result.toString().trim();
        log.info("OCR 全部完成：{}，总长 {} 字", pdfFile.getName(), text.length());
        return text;
    }

    /**
     * 单张图片OCR处理
     */
    @Override
    public String ocrImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BizException(400, "图片内容为空");
        }
        return doRecognize(imageBytes);
    }

    /**
     * 调阿里云通用文字识别
     */
    private String doRecognize(byte[] imageBytes) {
        try {
            RecognizeGeneralRequest request = new RecognizeGeneralRequest()
                    .setBody(new ByteArrayInputStream(imageBytes));

            RecognizeGeneralResponse response = ocrClient.recognizeGeneral(request);

            if (response == null || response.getBody() == null) {
                log.warn("阿里云 OCR 返回为空");
                return "";
            }

            // 返回的 data 是一段 JSON 字符串，里面有 content 字段
            String dataJson = response.getBody().getData();
            if (dataJson == null || dataJson.isBlank()) {
                return "";
            }

            JSONObject json = JSON.parseObject(dataJson);
            String content = json.getString("content");
            return content == null ? "" : content;

        } catch (Exception e) {
            log.error("阿里云 OCR 调用失败", e);
            throw new BizException(2004, "阿里云 OCR 调用失败：" + e.getMessage());
        }
    }





}
