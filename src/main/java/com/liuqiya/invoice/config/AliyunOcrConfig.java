package com.liuqiya.invoice.config;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OCR客户端配置类
 */
@Slf4j
@Configuration
public class AliyunOcrConfig {

    /**
     * 阿里云 OCR Client 端点
     */
    @Value("${app.ocr.endpoint}")
    private String endpoint;

    /**
     * 阿里云 OCR Client
     */
    @Bean
    public Client aliyunOcrClient() throws Exception {
        // 从环境变量读取AccessKey ID和AccessKey Secret
        String accessKeyId = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");

        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new IllegalStateException("环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID 未配置");
        }
        if (accessKeySecret == null || accessKeySecret.isBlank()) {
            throw new IllegalStateException("环境变量 ALIBABA_CLOUD_ACCESS_KEY_SECRET 未配置");
        }

        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = endpoint;

        log.info("阿里云 OCR 客户端初始化完成，endpoint={}", endpoint);
        return new Client(config);
    }

}
