package com.liuqiya.invoice.service.impl;

import org.springframework.core.io.Resource;
import cn.hutool.core.util.IdUtil;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    // 存储路径
    @Value("${app.file.storage-path}")
    private String storagePath;

    // 上传前缀
    @Value("${app.file.upload-prefix:upload}")
    private String uploadPrefix;

    // 允许的文件类型
    @Value("${app.file.allowed-extensions}")
    private String allowedExtensions;

    /**
     * 存储文件
     */
    @Override
    public String storeFile(MultipartFile file, String subDir) {
        //校验文件非空
        if (file == null || file.isEmpty() ) {
            log.error("文件为空或null");
            throw new BizException(400, "文件为空或null");
        }
        //提取文件扩展名并校验是否允许
        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);
        checkExtension(ext);
        // 2. 生成文件路径
        String filePath = generateFilePath(ext, subDir);
        //3.拼绝对路径
        Path target = Paths.get(storagePath, filePath);
        // 4. 建目录
        File parent = target.toFile().getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ok = parent.mkdirs();
            if (!ok) {
                throw new BizException(2001, "创建目录失败：" + parent.getAbsolutePath());
            }
        }
        // 5. 写文件
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("文件保存失败：{}", target, e);
            throw new BizException(2001, "文件保存失败：" + e.getMessage());
        }

        log.info("文件已保存：{}（{} 字节）", filePath, file.getSize());
        return filePath;
    }

    /**
     * 读取文件
     */
    @Override
    public Resource readFile(String filePath) {
        //拼绝对路径
        Path target = Paths.get(storagePath, filePath);
        File file = target.toFile();
        if (!file.exists()) {
            throw new BizException(404, "文件不存在：" + filePath);
        }
        return new FileSystemResource(file);
    }

    /**
     * 删除文件
     */
    @Override
    public void deleteFile(String filePath) {
        Path target = Paths.get(storagePath, filePath);
        File file = target.toFile();
        if (!file.exists()) {
            throw new BizException(404, "文件不存在：" + filePath);
        }
        try {
            Files.deleteIfExists(Paths.get(storagePath, filePath));
        } catch (IOException e) {
            throw new BizException(2001, "删除文件失败：" + e.getMessage());
        }
        log.info("文件已删除：{}", filePath);
    }

    /**
     * 判断文件是否存在
     */
    @Override
    public boolean fileExists(String filePath) {
        Path target = Paths.get(storagePath, filePath);
        File file = target.toFile();
        return file.exists();
    }



    //提取文件扩展名
    private String extractExtension(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            throw new BizException(2002, "文件没有扩展名");
        }
        return originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
    }

    // 校验文件扩展名是否允许
    private void checkExtension(String ext) {
        Set<String> allowed = Set.of(allowedExtensions.split(","));
        if (!allowed.contains(ext)) {
            throw new BizException(2002, "不支持的文件类型：" + ext);
        }
    }

    // 生成文件路径
    private String generateFilePath(String ext, String subDir) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = IdUtil.fastSimpleUUID();
        String sub = (subDir == null || subDir.isBlank())
                ? ""
                : subDir.replaceAll("^/+|/+$", "") + "/";
        return String.format("%s/%s%s/%s.%s", uploadPrefix, sub, datePath, uuid, ext);
    }



}
