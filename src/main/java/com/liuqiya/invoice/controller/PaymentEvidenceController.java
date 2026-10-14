package com.liuqiya.invoice.controller;

import com.liuqiya.invoice.pojo.dto.EvidenceReviewDTO;
import com.liuqiya.invoice.pojo.vo.EvidenceGroupVO;
import com.liuqiya.invoice.result.Result;
import com.liuqiya.invoice.service.PaymentEvidenceService;
import com.liuqiya.invoice.utils.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 支付凭证控制器
 */
@Slf4j
@RestController
@RequestMapping("/evidence")
public class PaymentEvidenceController {
    @Autowired
    private PaymentEvidenceService paymentEvidenceService;

    /**
     * 上传一组凭证
     */
    @PostMapping("/upload")
    public Result<Long> uploadPaymentEvidence(
            @RequestParam("invoiceId") Long invoiceId,
            @RequestParam("screenshots") MultipartFile[] screenshots,
            @RequestParam("payments") MultipartFile[] payments,
            @RequestParam(value = "remark", required = false) String remark) {

        Long groupId = paymentEvidenceService.uploadPaymentEvidenceGroup(invoiceId, screenshots, payments, remark);
        return Result.success("凭证上传成功", groupId);
    }

    /**
     * 查询某张发票的所有凭证组
     */
    @GetMapping("/list")
    public Result<List<EvidenceGroupVO>> paymentEvidenceList(@RequestParam("invoiceId") Long invoiceId) {
        return Result.success(paymentEvidenceService.getPaymentEvidenceListByInvoice(invoiceId));
    }

    /**
     * 查询支付凭证组详情
     */
    @GetMapping("/detail")
    public Result<EvidenceGroupVO> paymentEvidenceDetail(@RequestParam("groupId") Long groupId) {
        return Result.success(paymentEvidenceService.getPaymentEvidenceGroupDetail(groupId));
    }

    /**
     * 审核一组凭证
     */
    @PostMapping("/review")
    public Result<Void> reviewPaymentEvidence(@Valid @RequestBody EvidenceReviewDTO dto) {
        Claims claims = ThreadLocalUtil.get();
        Long adminId = Long.valueOf(claims.get("id").toString());

        paymentEvidenceService.reviewPaymentEvidenceGroup(dto.getGroupId(), dto.getPass(),
                dto.getRemark(), adminId);
        return Result.success();
    }
}
