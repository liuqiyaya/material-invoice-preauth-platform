package com.liuqiya.invoice.service.impl;

import com.liuqiya.invoice.enumeration.QualifyStatus;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.mapper.InvoiceFileMapper;
import com.liuqiya.invoice.mapper.InvoiceItemMapper;
import com.liuqiya.invoice.mapper.InvoiceMapper;
import com.liuqiya.invoice.mapper.OperationLogMapper;
import com.liuqiya.invoice.pojo.dto.RuleResultDTO;
import com.liuqiya.invoice.pojo.dto.StructuredInvoice;
import com.liuqiya.invoice.pojo.entity.Invoice;
import com.liuqiya.invoice.pojo.entity.InvoiceFile;
import com.liuqiya.invoice.pojo.entity.InvoiceItem;
import com.liuqiya.invoice.pojo.entity.OperationLog;
import com.liuqiya.invoice.pojo.vo.InvoiceListVO;
import com.liuqiya.invoice.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发票预审调度服务实现
 */
@Slf4j
@Service
public class InvoiceProcessServiceImpl implements InvoiceProcessService {

    @Autowired
    private InvoiceMapper invoiceMapper;
    @Autowired
    private InvoiceFileMapper invoiceFileMapper;
    @Autowired
    private InvoiceItemMapper invoiceItemMapper;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private PdfExtractService pdfExtractService;
    @Autowired
    private OcrService ocrService;
    @Autowired
    private AiStructureService aiStructureService;

    @Value("${app.file.storage-path}")
    private String storagePath;

    /** 文本层内容少于这个长度，就走 OCR */
    @Value("${app.ocr.min-text-length:20}")
    private int minTextLength;

    /**
     * 处理单张发票（同步调用）
     */
    @Override
    public void process(Long invoiceId) {
        if (invoiceId == null) {
            log.warn("【预审】invoiceId 为空，跳过");
            return;
        }

        log.info("【预审】开始 invoiceId={}", invoiceId);

        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            log.error("【预审】invoiceId={} 不存在，跳过", invoiceId);
            return;
        }

        try {
            // 定位文件
            File pdfFile = locatePdf(invoice);
            if (pdfFile == null) {
                markManual(invoice, "原始 PDF 文件不存在或无法定位");
                return;
            }

            // 文本层提取
            String rawText = extractTextLayer(pdfFile);

            // OCR 兜底
            if (isTooShort(rawText)) {
                log.info("【预审】invoiceId={} 文本层内容不足（长度={}），走 OCR", invoiceId,
                        rawText == null ? 0 : rawText.length());
                rawText = tryOcr(pdfFile);
            }

            // 仍为空转人工
            if (isTooShort(rawText)) {
                log.warn("【预审】invoiceId={} 文本层和 OCR 都拿不到有效内容", invoiceId);
                markManual(invoice, "文本层和 OCR 均无法获取有效内容");
                return;
            }

            log.info("【预审】invoiceId={} 最终文本长度={}", invoiceId, rawText.length());

            // AI 结构化
            StructuredInvoice structured;
            try {
                structured = aiStructureService.structure(rawText);
            } catch (Exception e) {
                log.error("【预审】invoiceId={} AI 结构化失败", invoiceId, e);
                markManual(invoice, "AI 结构化失败：" + e.getMessage());
                return;
            }

            // 结构化结果落库
            try {
                invoiceService.persistStructured(invoiceId, structured, null);
            } catch (Exception e) {
                log.error("【预审】invoiceId={} 落库失败", invoiceId, e);
                markManual(invoice, "结构化结果落库失败：" + e.getMessage());
                return;
            }

            // 重新查一次 invoice（落库可能更新了字段，比如 totalAmount、sellerTaxNo）
            invoice = invoiceMapper.selectById(invoiceId);

            // 规则引擎
            RuleResultDTO ruleResult;
            try {
                ruleResult = invoiceService.evaluate(invoice, structured);
            } catch (Exception e) {
                log.error("【预审】invoiceId={} 规则引擎失败", invoiceId, e);
                markManual(invoice, "规则引擎失败：" + e.getMessage());
                return;
            }

            // 应用结果
            applyRuleResult(invoice, ruleResult);

            log.info("========== 【预审】完成 invoiceId={}，状态={} ==========",
                    invoiceId, ruleResult.getQualifyStatus());

        } catch (BizException e) {
            log.error("【预审】invoiceId={} 业务异常：{}", invoiceId, e.getMessage(), e);
            markManual(invoice, "预审失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("【预审】invoiceId={} 未知异常", invoiceId, e);
            markManual(invoice, "预审未知异常：" + e.getMessage());
        }
        // 去重检测
        try {
            List<InvoiceListVO> duplicates = invoiceService.detectAndMark(invoiceId);
            if (!duplicates.isEmpty()) {
                log.warn("【预审】invoiceId={} 发现疑似重复：{}", invoiceId, duplicates.size());
                // 可以在 invoice 上加个标记，或者不改状态
            }
        } catch (Exception e) {
            log.error("【预审】去重检测失败：invoiceId={}", invoiceId, e);
            // 去重失败不影响主流程
        }

    }

    @Override
    @Async("invoiceExecutor")
    public void processAsync(Long invoiceId) {
        try {
            process(invoiceId);
        } catch (Exception e) {
            // @Async 里抛异常没人接，必须自己 catch
            log.error("【预审】异步处理异常：invoiceId={}", invoiceId, e);
            // 兜底：把状态改成待人工
            try {
                Invoice invoice = invoiceMapper.selectById(invoiceId);
                if (invoice != null) {
                    markManual(invoice, "异步预审异常：" + e.getMessage());
                }
            } catch (Exception ex) {
                log.error("【预审】异步兜底失败：invoiceId={}", invoiceId, ex);
            }
        }
    }


    /**
     * 根据 invoice.sourceFileId → invoice_file.fileKey → 磁盘 File
     */
    private File locatePdf(Invoice invoice) {
        Long fileId = invoice.getSourceFileId();
        if (fileId == null) {
            log.warn("【预审】invoiceId={} sourceFileId 为空", invoice.getId());
            return null;
        }
        InvoiceFile invoiceFile = invoiceFileMapper.selectById(fileId);
        if (invoiceFile == null || invoiceFile.getFileKey() == null) {
            log.warn("【预审】invoiceId={} invoice_file 不存在：fileId={}", invoice.getId(), fileId);
            return null;
        }
        File file = Paths.get(storagePath, invoiceFile.getFileKey()).toFile();
        if (!file.exists()) {
            log.warn("【预审】invoiceId={} 磁盘文件不存在：{}", invoice.getId(), file.getAbsolutePath());
            return null;
        }
        return file;
    }

    /**
     * 文本层提取（异常不抛出，返回 null 让上层决定走 OCR）
     */
    private String extractTextLayer(File pdfFile) {
        try {
            String text = pdfExtractService.extractText(pdfFile);
            log.info("【预审】文本层提取长度={}", text == null ? 0 : text.length());
            return text;
        } catch (Exception e) {
            log.warn("【预审】文本层提取失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * OCR 兜底（异常不抛出，返回 null 让上层决定转人工）
     */
    private String tryOcr(File pdfFile) {
        try {
            String text = ocrService.ocrPdf(pdfFile);
            log.info("【预审】OCR 提取长度={}", text == null ? 0 : text.length());
            return text;
        } catch (Exception e) {
            log.error("【预审】OCR 失败：{}", e.getMessage(), e);
            return null;
        }
    }

    private boolean isTooShort(String text) {
        return text == null || text.trim().length() < minTextLength;
    }

    /**
     * 应用规则引擎结果
     */
    @Transactional(rollbackFor = Exception.class)
    protected void applyRuleResult(Invoice invoice, RuleResultDTO result) {
        // 0. 先记录修改前的状态
        QualifyStatus oldQualifyStatus = invoice.getQualifyStatus();

        // 1. 更新 invoice 状态字段
        invoice.setQualifyStatus(result.getQualifyStatus());
        invoice.setCategoryResult(result.getCategoryResult());
        invoice.setRejectReason(result.getRejectReason());
        invoice.setEvidenceRequired(result.getEvidenceRequired());
        invoice.setEvidenceReason(result.getEvidenceReason());
        invoice.setNeedManual(result.getNeedManual());
        invoiceMapper.updateById(invoice);

        // 2. 逐项回写 invoice_item 的品类 / 单价类别 / 理由
        if (result.getItemResults() != null && !result.getItemResults().isEmpty()) {
            // 拿到刚插入的 invoice_item 列表（按 id 顺序）
            java.util.List<InvoiceItem> items = invoiceItemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InvoiceItem>()
                            .eq(InvoiceItem::getInvoiceId, invoice.getId())
                            .orderByAsc(InvoiceItem::getId)
            );

            // 按顺序逐条对应
            int size = Math.min(items.size(), result.getItemResults().size());
            for (int i = 0; i < size; i++) {
                InvoiceItem entity = items.get(i);
                RuleResultDTO.ItemRuleResult ir = result.getItemResults().get(i);
                entity.setCategoryLevel(ir.getCategoryLevel());
                entity.setCategoryName(ir.getCategoryName());
                entity.setPriceType(ir.getPriceType());
                entity.setAiReason(ir.getAiReason());
                invoiceItemMapper.updateById(entity);
            }
        }

        // 3. 写操作日志
        String beforeStatus = "qualify_status=" + (oldQualifyStatus != null ? oldQualifyStatus.name() : "PENDING");
        String afterStatus = "qualify_status=" + result.getQualifyStatus().name() + ", " + buildLogDetail(result);
        writeLog(invoice.getId(), "PRE_AUDIT", beforeStatus, afterStatus);

        log.info("【预审】invoiceId={} 已应用规则结果：status={}, rejectReason={}",
                invoice.getId(), result.getQualifyStatus(), result.getRejectReason());
    }

    private String buildLogDetail(RuleResultDTO r) {
        StringBuilder sb = new StringBuilder();
        sb.append("categoryResult=").append(r.getCategoryResult());
        if (r.getRejectReason() != null) {
            sb.append(", rejectReason=").append(r.getRejectReason());
        }
        if (r.getWeekAccumulatedAfter() != null) {
            sb.append(", weekAccumulatedBefore=").append(r.getWeekAccumulatedBefore());
            sb.append(", weekAccumulatedAfter=").append(r.getWeekAccumulatedAfter());
        }
        return sb.toString();
    }

    /**
     * 标记为待人工处理
     */
    private void markManual(Invoice invoice, String reason) {
        try {
            invoice.setQualifyStatus(QualifyStatus.PENDING_MANUAL);
            invoice.setNeedManual(1);
            invoice.setManualRemark(reason);
            invoiceMapper.updateById(invoice);
            writeLog(invoice.getId(), "PRE_AUDIT_FAIL", null, reason);
            log.warn("【预审】invoiceId={} 转人工：{}", invoice.getId(), reason);
        } catch (Exception e) {
            log.error("【预审】标记转人工失败：invoiceId={}", invoice.getId(), e);
        }
    }

    /**
     * 写操作日志
     */
    private void writeLog(Long invoiceId, String action, String before, String after) {
        try {
            OperationLog log = new OperationLog();
            log.setInvoiceId(invoiceId);
            log.setAction(action);
            log.setBeforeValue(before);
            log.setAfterValue(after);
            log.setCreateTime(LocalDateTime.now());
            operationLogMapper.insert(log);
        } catch (Exception e) {
            log.error("写操作日志失败：invoiceId={}, action={}", invoiceId, action, e);
        }
    }
}