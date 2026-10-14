package com.liuqiya.invoice.service;


import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * 存储文件
     */
    String storeFile(MultipartFile file, String subDir);

    /**
     * 读取文件
     */
    Resource readFile(String filePath);

    /**
     * 删除文件
     */
    void deleteFile(String filePath);

    /**
     * 判断文件是否存在
     */
    boolean fileExists(String filePath);
}
