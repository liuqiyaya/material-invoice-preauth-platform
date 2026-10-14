package com.liuqiya.invoice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 异步线程池配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 发票预审专用线程池
     */
    @Bean("invoiceExecutor")
    public Executor invoiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);// 核心线程数
        executor.setMaxPoolSize(8);// 最大线程数
        executor.setQueueCapacity(100);// 队列容量
        executor.setKeepAliveSeconds(60);// 空闲线程最大存活时间（秒）
        executor.setThreadNamePrefix("invoice-async-");// 线程名称前缀
        // 队列满了由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
