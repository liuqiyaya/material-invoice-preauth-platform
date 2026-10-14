package com.liuqiya.invoice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuqiya.invoice.enumeration.EvidenceGroupStatus;
import com.liuqiya.invoice.enumeration.QualifyStatus;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.mapper.*;
import com.liuqiya.invoice.pojo.entity.*;
import com.liuqiya.invoice.pojo.vo.EvidenceGroupVO;
import com.liuqiya.invoice.service.FileService;
import com.liuqiya.invoice.service.PaymentEvidenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付凭证服务实现
 */
@Slf4j
@Service
public class PaymentEvidenceServiceImpl implements PaymentEvidenceService {

    @Autowired
    private InvoiceMapper invoiceMapper;
    @Autowired
    private PaymentEvidenceGroupMapper groupMapper;
    @Autowired
    private OrderScreenshotMapper screenshotMapper;
    @Autowired
    private PaymentRecordMapper recordMapper;
    @Autowired
    private InvoiceFileMapper invoiceFileMapper;
    @Autowired
    private FileService fileService;
    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     *上传支付凭证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadPaymentEvidenceGroup(Long invoiceId, MultipartFile[] screenshots,
                            MultipartFile[] payments, String remark) {
        // 参数校验
        if (invoiceId == null) {
            throw new BizException(400, "invoiceId 不能为空");
        }
        if (screenshots == null || screenshots.length == 0) {
            throw new BizException(400, "订单截图至少上传一张");
        }
        if (payments == null || payments.length == 0) {
            throw new BizException(400, "支付记录至少上传一张");
        }

        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BizException(404, "发票不存在：" + invoiceId);
        }

        log.info("【凭证】上传凭证组：invoiceId={}, 截图 {} 张, 支付记录 {} 张",
                invoiceId, screenshots.length, payments.length);

        // 创建凭证组
        PaymentEvidenceGroup group = new PaymentEvidenceGroup();
        group.setInvoiceId(invoiceId);
        group.setStatus(EvidenceGroupStatus.PENDING_REVIEW);   // 上传后直接进待审核
        groupMapper.insert(group);
        Long groupId = group.getId();

        //  上传订单截图
        for (MultipartFile file : screenshots) {
            if (file == null || file.isEmpty()) {continue;}
            String fileKey = fileService.storeFile(file, "screenshot");
            Long fileId = saveInvoiceFile(fileKey, file, "IMG");
            OrderScreenshot os = new OrderScreenshot();
            os.setGroupId(groupId);
            os.setFileId(fileId);
            os.setOriginalName(file.getOriginalFilename());
            screenshotMapper.insert(os);
        }

        // 上传支付记录
        for (MultipartFile file : payments) {
            if (file == null || file.isEmpty()) {continue;}
            String fileKey = fileService.storeFile(file, "payment");
            Long fileId = saveInvoiceFile(fileKey, file, "IMG");
            PaymentRecord pr = new PaymentRecord();
            pr.setGroupId(groupId);
            pr.setFileId(fileId);
            pr.setOriginalName(file.getOriginalFilename());
            recordMapper.insert(pr);
        }

        log.info("【凭证】凭证组创建完成：groupId={}", groupId);
        return groupId;
    }

    /**
     * 查看支付凭证列表
     */
    @Override
    public List<EvidenceGroupVO> getPaymentEvidenceListByInvoice(Long invoiceId) {
        if (invoiceId == null) {
            throw new BizException(400, "invoiceId 不能为空");
        }
        List<PaymentEvidenceGroup> groups = groupMapper.selectList(
                new LambdaQueryWrapper<PaymentEvidenceGroup>()
                        .eq(PaymentEvidenceGroup::getInvoiceId, invoiceId)
                        .orderByDesc(PaymentEvidenceGroup::getId)
        );
        List<EvidenceGroupVO> result = new ArrayList<>();
        for (PaymentEvidenceGroup g : groups) {
            result.add(buildGroupVO(g));
        }
        return result;
    }

    /**
     *查看支付凭证详情
     */
    @Override
    public EvidenceGroupVO getPaymentEvidenceGroupDetail(Long groupId) {
        if (groupId == null) {
            throw new BizException(400, "groupId 不能为空");
        }
        PaymentEvidenceGroup g = groupMapper.selectById(groupId);
        if (g == null) {
            throw new BizException(404, "凭证组不存在：" + groupId);
        }
        return buildGroupVO(g);
    }

    /**
     * 审核支付凭证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPaymentEvidenceGroup(Long groupId, Boolean pass, String remark, Long reviewerId) {
        // 参数校验
        if (groupId == null) {
            throw new BizException(400, "groupId 不能为空");
        }
        if (pass == null) {
            throw new BizException(400, "pass 不能为空");
        }
        if (reviewerId == null) {
            throw new BizException(400, "审核人不能为空");
        }

        // 查组
        PaymentEvidenceGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BizException(404, "凭证组不存在：" + groupId);
        }

        // 状态校验
        if (group.getStatus() == EvidenceGroupStatus.APPROVED) {
            throw new BizException(400, "该凭证组已通过，不能重复审核");
        }
        if (group.getStatus() == EvidenceGroupStatus.REJECTED) {
            throw new BizException(400, "该凭证组已拒绝，不能重复审核");
        }

        log.info("【凭证审核】groupId={}, pass={}, reviewerId={}, remark={}",
                groupId, pass, reviewerId, remark);

        // 更新组
        group.setStatus(pass ? EvidenceGroupStatus.APPROVED : EvidenceGroupStatus.REJECTED);
        group.setReviewerId(reviewerId);
        group.setReviewTime(LocalDateTime.now());
        group.setReviewRemark(remark);
        groupMapper.updateById(group);

        Long invoiceId = group.getInvoiceId();
        //写日志
        writeLog(invoiceId, reviewerId, "EVIDENCE_REVIEW",
                "groupId=" + groupId + ", pass=" + pass + ", remark=" + remark);
        // 联动 invoice 状态
        syncInvoiceStatus(invoiceId,reviewerId);


    }

    /**
     * 根据所有凭证组的状态，同步 invoice 的资质审核状态
     */
    private void syncInvoiceStatus(Long invoiceId, Long operatorId) {
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {return;}

        List<PaymentEvidenceGroup> groups = groupMapper.selectList(
                new LambdaQueryWrapper<PaymentEvidenceGroup>()
                        .eq(PaymentEvidenceGroup::getInvoiceId, invoiceId)
        );
        if (groups.isEmpty()) {return;}

        // 统计
        boolean hasRejected = false;
        boolean hasPending = false;
        for (PaymentEvidenceGroup g : groups) {
            if (g.getStatus() == EvidenceGroupStatus.REJECTED) {hasRejected = true;}
            else if (g.getStatus() == EvidenceGroupStatus.PENDING
                    || g.getStatus() == EvidenceGroupStatus.PENDING_REVIEW) {hasPending = true;}
        }

        // 判定
        QualifyStatus targetStatus;
        String rejectReason = null;
        if (hasRejected) {
            targetStatus = QualifyStatus.REJECTED;
            rejectReason = "凭证审核不通过";
        } else if (!hasPending) {
            targetStatus = QualifyStatus.APPROVED;
        } else {
            targetStatus = QualifyStatus.PENDING_EVIDENCE;
        }

        // 状态没变，直接返回
        QualifyStatus oldStatus = invoice.getQualifyStatus();
        if (oldStatus == targetStatus) {
            log.info("【凭证审核】invoiceId={} 状态未变（{}），跳过", invoiceId, targetStatus);
            return;
        }

        // 更新
        invoice.setQualifyStatus(targetStatus);
        invoice.setRejectReason(rejectReason);
        if (targetStatus == QualifyStatus.APPROVED
                || targetStatus == QualifyStatus.REJECTED
                || targetStatus == QualifyStatus.CANCELLED) {
            invoice.setEvidenceRequired(0);
            invoice.setEvidenceReason(null);
        }
        invoiceMapper.updateById(invoice);

        // 写日志 2：状态同步
        writeChangeLog(invoiceId, operatorId, "QUALIFY_STATUS_SYNC",
                "qualify_status=" + (oldStatus == null ? "PENDING_EVIDENCE" : oldStatus.name()),
                "qualify_status=" + targetStatus.name()
                        + (rejectReason != null ? ", rejectReason=" + rejectReason : ""));

        log.info("【凭证审核】invoiceId={} 状态同步：{} → {}", invoiceId, oldStatus, targetStatus);
    }

    /**
     * 记录操作日志
     */
    private void writeLog(Long invoiceId, Long operatorId, String action, String detail) {
        try {
            OperationLog log = new OperationLog();
            log.setInvoiceId(invoiceId);
            log.setOperatorId(operatorId);
            log.setAction(action);
            log.setAfterValue(detail);
            operationLogMapper.insert(log);
        } catch (Exception e) {
            log.error("写操作日志失败：invoiceId={}, action={}", invoiceId, action, e);
        }
    }

    /**
     * 写日志（带 before / after）
     */
    private void writeChangeLog(Long invoiceId, Long operatorId, String action,
                          String before, String after) {
        OperationLog log = new OperationLog();
        log.setInvoiceId(invoiceId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setBeforeValue(before);
        log.setAfterValue(after);
        operationLogMapper.insert(log);
    }
    /**
     * 保存文件到 invoice_file 表，返回 fileId
     */
    private Long saveInvoiceFile(String fileKey, MultipartFile file, String type) {
        InvoiceFile invoiceFile = new InvoiceFile();
        invoiceFile.setFileKey(fileKey);
        invoiceFile.setOriginalName(file.getOriginalFilename());
        invoiceFile.setFileType(type);
        invoiceFile.setFileSize(file.getSize());
        invoiceFileMapper.insert(invoiceFile);
        return invoiceFile.getId();
    }

    /**
     * 构建凭证组VO
     */
    private EvidenceGroupVO buildGroupVO(PaymentEvidenceGroup g) {
        EvidenceGroupVO vo = new EvidenceGroupVO();
        vo.setId(g.getId());
        vo.setInvoiceId(g.getInvoiceId());
        vo.setStatus(g.getStatus() == null ? null : g.getStatus().getCode());
        vo.setStatusDesc(g.getStatus() == null ? null : g.getStatus().getDesc());
        vo.setReviewerId(g.getReviewerId());
        vo.setReviewTime(g.getReviewTime());
        vo.setReviewRemark(g.getReviewRemark());
        vo.setCreateTime(g.getCreateTime());

        // 订单截图
        List<OrderScreenshot> screenshots = screenshotMapper.selectList(
                new LambdaQueryWrapper<OrderScreenshot>().eq(OrderScreenshot::getGroupId, g.getId())
        );
        for (OrderScreenshot s : screenshots) {
            vo.getScreenshots().add(buildFileVO(s.getId(), s.getFileId(), s.getOriginalName()));
        }

        // 支付记录
        List<PaymentRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getGroupId, g.getId())
        );
        for (PaymentRecord r : records) {
            vo.getPayments().add(buildFileVO(r.getId(), r.getFileId(), r.getOriginalName()));
        }

        return vo;
    }

    /**
     * 构建文件VO
     */
    private EvidenceGroupVO.FileVO buildFileVO(Long id, Long fileId, String originalName) {
        EvidenceGroupVO.FileVO f = new EvidenceGroupVO.FileVO();
        f.setId(id);
        f.setFileId(fileId);
        f.setOriginalName(originalName);
        // 查 fileKey
        InvoiceFile file = invoiceFileMapper.selectById(fileId);
        if (file != null) {
            f.setFilePath(file.getFileKey());
        }
        return f;
    }


}
