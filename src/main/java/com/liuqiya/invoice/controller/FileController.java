package com.liuqiya.invoice.controller;

import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.result.Result;
import com.liuqiya.invoice.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private FileService fileService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "subDir", required = false) String subDir) {
        //校验文件是否为空
        if (file==null||file.isEmpty()) {
            log.error("文件为空");
            throw new BizException(400, "文件为空");
        }
        // 存储文件
        String filePath = fileService.storeFile(file, subDir);
        return Result.success("上传成功", filePath);
    }

    /**
     * 下载/预览文件
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("filePath") String filePath, @RequestParam(value = "filename", required = false) String filename) {
        // 读取文件
        Resource resource = fileService.readFile(filePath);
        // 用原始文件名下载；没传就取 filePath 的最后一段
        String downloadName = (filename == null || filename.isBlank())
                ? filePath.substring(filePath.lastIndexOf('/') + 1)
                : filename;

        // 中文文件名要 URL 编码
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteFile(@RequestParam("filePath") String filePath) {
        fileService.deleteFile(filePath);
        return Result.success();
    }

    /**
     * 检查文件是否存在
     */
    @GetMapping("/exists")
    public Result<Boolean> fileExists(@RequestParam("filePath") String filePath) {
        return Result.success(fileService.fileExists(filePath));
    }



}
