package com.liuqiya.invoice.utils;

import com.liuqiya.invoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * zip解压提取pdf工具类
 */
@Slf4j
public class ZipUtil {
    /**
     * 解压 ZIP，递归提取所有 PDF 文件
     */
    public static List<File> extractPdfs(File zipFile, File destDir) {
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw new BizException(2003, "创建解压目录失败：" + destDir.getAbsolutePath());
        }

        List<File> pdfFiles = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)),
                Charset.forName("GBK")    // 兼容 Windows 中文 ZIP
        )) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 目录跳过
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                // 文件名安全处理（防路径穿越）
                String entryName = entry.getName();
                if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                    log.warn("跳过不安全的 ZIP 条目：{}", entryName);
                    zis.closeEntry();
                    continue;
                }

                // 只处理 PDF
                if (!entryName.toLowerCase().endsWith(".pdf")) {
                    log.info("跳过非 PDF 文件：{}", entryName);
                    zis.closeEntry();
                    continue;
                }

                // 写到目标目录
                File outFile = new File(destDir, sanitize(entryName));
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    log.warn("创建目录失败：{}", parent);
                    zis.closeEntry();
                    continue;
                }

                try (OutputStream os = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = zis.read(buf)) > 0) {
                        os.write(buf, 0, len);
                    }
                }

                pdfFiles.add(outFile);
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("ZIP 解压失败：{}", zipFile.getAbsolutePath(), e);
            throw new BizException(2003, "ZIP解压失败：" + e.getMessage());
        }

        return pdfFiles;
    }

    /**
     * 去掉路径里的奇怪字符，防路径穿越
     */
    private static String sanitize(String name) {
        return name.replace("\\", "/")
                .replaceAll("^/+", "")
                .replaceAll("\\.\\./", "");
    }

}
