package com.liuqiya.invoice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResultVO {
    /**
     * 批次号
     */
    private String batchId;

    /**
     * 总文件数（含 ZIP 内的 PDF）
     */
    private Integer total;

    /**
     * 成功数
     * */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 每个文件的处理结果
     */
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        /**
         * 原始文件名
         * */
        private String fileName;
        /**
         * 发票 id（成功才有）
         * */
        private Long invoiceId;
        /**
         * 文件 id（成功才有）
         * */
        private Long fileId;
        /**
         * 状态：SUCCESS / FAILED
         * */
        private String status;
        /**
         * 失败原因（失败才有）
         * */
        private String errorMsg;
    }

}
