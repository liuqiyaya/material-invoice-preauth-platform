package com.liuqiya.invoice.controller;

import com.liuqiya.invoice.pojo.dto.InvoiceManualEditDTO;
import com.liuqiya.invoice.pojo.dto.InvoiceQueryDTO;
import com.liuqiya.invoice.pojo.dto.InvoiceStatusUpdateDTO;
import com.liuqiya.invoice.pojo.vo.InvoiceDetailVO;
import com.liuqiya.invoice.pojo.vo.InvoiceListVO;
import com.liuqiya.invoice.pojo.vo.PageVO;
import com.liuqiya.invoice.pojo.vo.UploadResultVO;
import com.liuqiya.invoice.result.Result;
import com.liuqiya.invoice.service.InvoiceProcessService;
import com.liuqiya.invoice.service.InvoiceService;
import com.liuqiya.invoice.utils.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/invoice")
public class InvoiceController {
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private InvoiceProcessService invoiceProcessService;

    /**
     * 上传发票
     */
    @PostMapping("/upload")
    public Result<UploadResultVO> uploadInvoice(@RequestParam("files") MultipartFile[] files) {
        Claims claims = ThreadLocalUtil.get();
        Long adminId = Long.valueOf(claims.get("id").toString());
        UploadResultVO uploadResultVO = invoiceService.uploadInvoiceFile(files, adminId);
        // 异步触发预审
        if (uploadResultVO != null && uploadResultVO.getItems() != null) {
            for (UploadResultVO.Item item : uploadResultVO.getItems()) {
                if ("SUCCESS".equals(item.getStatus()) && item.getInvoiceId() != null) {
                    invoiceProcessService.processAsync(item.getInvoiceId());
                    log.info("【上传】已触发异步预审：invoiceId={}", item.getInvoiceId());
                }
            }
        }
        return Result.success("上传成功", uploadResultVO);
    }

    /**
     * 查询发票列表
     */
    @GetMapping("/list")
    public Result<PageVO<InvoiceListVO>> invoiceList(InvoiceQueryDTO query) {
        return Result.success(invoiceService.getInvoiceList(query));
    }

    /**
     * 查看发票详情
     */
    @GetMapping("/detail")
    public Result<InvoiceDetailVO> invoiceDetail(@RequestParam("invoiceId") Long invoiceId) {
        return Result.success(invoiceService.getInvoiceDetail(invoiceId));
    }

    /**
     * 人工修改发票数据(人工兜底)
     */
    @PutMapping("/manual-edit")
    public Result<Void> manualEdit(@Valid @RequestBody InvoiceManualEditDTO dto) {
        Claims claims = ThreadLocalUtil.get();
        Long adminId = Long.valueOf(claims.get("id").toString());

        invoiceService.manualEditInvoice(dto, adminId);
        return Result.success();
    }

    /**
     * 变更发票状态(提交/报销)
     */
    @PostMapping("/status-update")
    public Result<Void> updateInvoiceStatus(@Valid @RequestBody InvoiceStatusUpdateDTO dto) {
        Claims claims = ThreadLocalUtil.get();
        Long adminId = Long.valueOf(claims.get("id").toString());

        invoiceService.updateInvoiceStatus(dto, adminId);
        return Result.success();
    }
}