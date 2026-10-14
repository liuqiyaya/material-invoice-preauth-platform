package com.liuqiya.invoice.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuqiya.invoice.enumeration.*;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.mapper.*;
import com.liuqiya.invoice.pojo.dto.*;
import com.liuqiya.invoice.pojo.entity.*;
import com.liuqiya.invoice.pojo.vo.*;
import com.liuqiya.invoice.service.*;
import com.liuqiya.invoice.utils.ZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InvoiceServiceImpl implements InvoiceService {
    @Autowired
    private FileService fileService;
    @Autowired
    private AiCategoryService aiCategoryService;
    @Autowired
    private InvoiceFileMapper invoiceFileMapper;
    @Autowired
    private InvoiceMapper invoiceMapper;
    @Autowired
    private InvoiceItemMapper invoiceItemMapper;
    @Autowired
    private CategoryRuleMapper categoryRuleMapper;
    @Autowired
    private PaymentEvidenceGroupMapper groupMapper;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private OrderScreenshotMapper screenshotMapper;
    @Autowired
    private PaymentRecordMapper recordMapper;

    private final ObjectMapper objectMapper=new ObjectMapper();

    //存储路径
    @Value("${app.file.storage-path}")
    private String storagePath;
    //单价阈值
    @Value("${app.rule.low-value-threshold:500}")
    private BigDecimal lowValueThreshold;
    //资产阈值
    @Value("${app.rule.asset-threshold:1000}")
    private BigDecimal assetThreshold;
    //周累计阈值
    @Value("${app.rule.week-limit:1000}")
    private BigDecimal weekLimit;
    //周累计拒绝阈值
    @Value("${app.rule.week-reject-limit:3000}")
    private BigDecimal weekRejectLimit;

    /**
     * 上传发票文件
     */
    @Override
    public UploadResultVO uploadInvoiceFile(MultipartFile[] files, Long adminId) {
        if (files == null || files.length == 0) {
            throw new BizException(400, "未选择任何文件");
        }
        // 生成批次号
        String batchId = IdUtil.fastSimpleUUID();
        UploadResultVO uploadResultVO = new UploadResultVO();
        uploadResultVO.setBatchId(batchId);
        // 临时目录：{storage-path}/temp/{batchId}
        Path tempDir = Paths.get(storagePath, "temp", batchId);
        // 创建临时目录
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            throw new BizException(500, "创建临时目录失败：" + e.getMessage());
        }

        int total = 0;
        int success = 0;
        int fail = 0;

        try{
            for (MultipartFile file : files){
                if (file == null || file.isEmpty()) {continue;}
                String originalName = file.getOriginalFilename();
                if (originalName == null) {originalName = "unknown";}
                if (originalName.toLowerCase().endsWith(".pdf")) {
                    // 单 PDF 直接建档
                    total++;
                    UploadResultVO.Item item = handleSinglePdf(file, adminId, batchId, originalName);
                    uploadResultVO.getItems().add(item);
                    if ("SUCCESS".equals(item.getStatus())) {
                    success++;
                } else {
                    fail++;
                }
                }else if(originalName.toLowerCase().endsWith(".zip")){
                    // ZIP 包，解压后处理每个 PDF
                    total += handleZip(file, tempDir.toFile(), adminId, batchId, originalName, uploadResultVO);
                }else{
                    total++;
                    fail++;
                    uploadResultVO.getItems().add(buildFailItem(originalName, "不支持的文件类型"));

                }
            }
        }finally {
            // 清理临时目录
            deleteQuietly(tempDir.toFile());
        }
        // 重新统计成功失败
        success = 0; fail = 0;
        for (UploadResultVO.Item it : uploadResultVO.getItems()) {
            if ("SUCCESS".equals(it.getStatus())) {
                success++;
            } else {
                fail++;
            }
        }
        uploadResultVO.setTotal(total);
        uploadResultVO.setSuccessCount(success);
        uploadResultVO.setFailCount(fail);

        log.info("上传完成：batch={}, total={}, success={}, fail={}", batchId, total, success, fail);
        return uploadResultVO;

    }

    /**
     * 保存AI结构化发票结果和原始 JSON
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persistStructured(Long invoiceId, StructuredInvoice structured, String rawAiJson) {
        log.info("开始保存结构化发票: invoiceId={}, sellerName={}",
                invoiceId, structured.getSellerName());
        if (invoiceId == null || structured == null) {
            throw new BizException(400, "参数错误：invoiceId 或 structured 不能为空");
        }

        // 更新 invoice 主表
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BizException(404, "发票不存在：" + invoiceId);
        }

        invoice.setSellerName(structured.getSellerName());
        invoice.setSellerTaxNo(structured.getSellerTaxNo());
        invoice.setBuyerName(structured.getBuyerName());
        invoice.setBuyerTaxNo(structured.getBuyerTaxNo());
        invoice.setInvoiceNo(structured.getInvoiceNo());
        invoice.setInvoiceDate(structured.getInvoiceDate());
        invoice.setTotalAmount(structured.getTotalAmount());
        invoice.setAiRawResult(rawAiJson);
        invoiceMapper.updateById(invoice);

        // 幂等：先删旧的 invoice_item（重新处理时避免重复）
        invoiceItemMapper.delete(
                new LambdaQueryWrapper<InvoiceItem>().eq(InvoiceItem::getInvoiceId, invoiceId)
        );

        // 3. 插入新的 invoice_item
        List<StructuredInvoice.Item> items = structured.getItems();
        if (items != null && !items.isEmpty()) {
            List<InvoiceItem> entities = new ArrayList<>();
            for (StructuredInvoice.Item item : items) {
                if (item == null || item.getItemName() == null) {continue;}
                if (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("商品 {} 数量异常: {}", item.getItemName(), item.getQuantity());
                    continue;
                }

                if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    log.warn("商品 {} 单价异常: {}", item.getItemName(), item.getUnitPrice());
                    continue;
                }

                if (item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    log.warn("商品 {} 金额异常: {}", item.getItemName(), item.getAmount());
                    continue;
                }
                InvoiceItem entity = new InvoiceItem();
                entity.setInvoiceId(invoiceId);
                entity.setItemName(item.getItemName());
                entity.setQuantity(item.getQuantity());
                entity.setUnitPrice(item.getUnitPrice());
                entity.setAmount(item.getAmount());
                entities.add(entity);
            }
            if (!entities.isEmpty()) {
                for (InvoiceItem e : entities) {
                    invoiceItemMapper.insert(e);
                }
                log.info("发票 {} 落库完成，商品明细 {} 条", invoiceId, entities.size());
            } else {
                log.warn("发票 {} 无有效商品明细", invoiceId);
            }
        } else {
            log.warn("发票 {} 结构化结果中 items 为空", invoiceId);
        }
    }

    /**
     * 根据规则对发票做程序性判定
     */
    @Override
    public RuleResultDTO evaluate(Invoice invoice, StructuredInvoice structured) {
        log.info("开始对发票 {} 做程序性判定", invoice.getId());
        if (invoice == null || structured == null) {
            throw new BizException(400, "参数错误：invoice 或 structured 不能为空");
        }

        if (invoice.getId() == null) {
            throw new BizException(400, "参数错误：invoice ID 不能为空");
        }
        RuleResultDTO result = new RuleResultDTO();
        result.setInvoiceId(invoice.getId());

        // ============ 第一步：逐项品类 + 单价判定 ============
        List<StructuredInvoice.Item> items = structured.getItems();
        if (items == null || items.isEmpty()) {
            result.setQualifyStatus(QualifyStatus.PENDING_MANUAL);
            result.setNeedManual(1);
            result.setRejectReason("无商品明细，需人工处理");
            return result;
        }

        CategoryLevel worstCategory = CategoryLevel.PASS;
        boolean hasLowValue = false;
        boolean hasAsset = false;

        List<RuleResultDTO.ItemRuleResult> itemResults = new ArrayList<>();
        for (StructuredInvoice.Item item : items) {
            if (item == null || item.getItemName() == null) {continue;}
            if (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("evaluate - 商品 {} 数量异常，跳过", item.getItemName());
                continue;
            }
            if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                log.warn("evaluate - 商品 {} 单价异常，跳过", item.getItemName());
                continue;
            }
            if (item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                log.warn("evaluate - 商品 {} 金额异常，跳过", item.getItemName());
                continue;
            }

            RuleResultDTO.ItemRuleResult ir = judgeItem(item);
            itemResults.add(ir);

            // 取"最坏"的品类结论：REJECT > DOUBT > PASS
            if (ir.getCategoryLevel() != null) {
                if (ir.getCategoryLevel() == CategoryLevel.REJECT) {
                    worstCategory = CategoryLevel.REJECT;
                } else if (ir.getCategoryLevel() == CategoryLevel.DOUBT
                        && worstCategory != CategoryLevel.REJECT) {
                    worstCategory = CategoryLevel.DOUBT;
                }
            }

            if (ir.getPriceType() == PriceType.LOW_VALUE) {hasLowValue = true;}
            if (ir.getPriceType() == PriceType.ASSET) {hasAsset = true;}
        }

        result.setItemResults(itemResults);
        result.setCategoryResult(worstCategory);

        // ============ 第二步：品类判定 ============
        if (worstCategory == CategoryLevel.REJECT) {
            result.setQualifyStatus(QualifyStatus.REJECTED);
            result.setRejectReason("存在不可报销商品");
            return result;
        }
        if (worstCategory == CategoryLevel.DOUBT) {
            result.setQualifyStatus(QualifyStatus.PENDING_MANUAL);
            result.setNeedManual(1);
            result.setRejectReason("存在存疑商品，需人工确认");
            return result;
        }

        // ============ 第三步：单价判定 ============
        if (hasAsset) {
            result.setQualifyStatus(QualifyStatus.REJECTED);
            result.setRejectReason("存在单价 ≥ " + assetThreshold + " 元资产商品，不能走材料费报销");
            return result;
        }

        // ============ 第四步：周累计判定 ============
        String sellerTaxNo = invoice.getSellerTaxNo();
        LocalDate weekStart = invoice.getWeekStart();

        if (sellerTaxNo == null || weekStart == null) {
            result.setQualifyStatus(QualifyStatus.PENDING_MANUAL);
            result.setNeedManual(1);
            result.setRejectReason("销售方税号或所属周缺失，无法累计");
            return result;
        }

        BigDecimal accumulated = sumEffectiveAmount(sellerTaxNo, weekStart, invoice.getId());
        BigDecimal currentAmount = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
        BigDecimal total = accumulated.add(currentAmount);

        result.setSellerTaxNo(sellerTaxNo);
        result.setWeekStart(weekStart);
        result.setWeekAccumulatedBefore(accumulated);
        result.setWeekAccumulatedAfter(total);

        // 边界判断
        if (total.compareTo(weekRejectLimit) > 0) {
            // > 3000 直接拒绝
            result.setQualifyStatus(QualifyStatus.REJECTED);
            result.setRejectReason("同周同商家累计 " + total + " 元，超过 " + weekRejectLimit + " 元，不能走材料费报销");
            return result;
        }

        if (total.compareTo(weekLimit) > 0) {
            // 1000 < 累计 ≤ 3000，需补凭证
            result.setQualifyStatus(QualifyStatus.PENDING_EVIDENCE);
            result.setEvidenceRequired(1);
            result.setEvidenceReason(EvidenceReason.ACCUMULATE_OVER_1000);
            return result;
        }

        // ============ 第五步：低值品需要补凭证 ============
        if (hasLowValue) {
            result.setQualifyStatus(QualifyStatus.PENDING_EVIDENCE);
            result.setEvidenceRequired(1);
            result.setEvidenceReason(EvidenceReason.LOW_VALUE);
            return result;
        }

        // ============ 通过 ============
        result.setQualifyStatus(QualifyStatus.APPROVED);
        return result;
    }

    /**
     * 查询发票列表
     */
    @Override
    public PageVO<InvoiceListVO> getInvoiceList(InvoiceQueryDTO query) {
        // 分页参数兜底
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        if (pageSize > 200) {pageSize = 200;}   // 防止拉爆

        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();

        // 筛选条件
        if (StringUtils.hasText(query.getSellerName())) {
            wrapper.like(Invoice::getSellerName, query.getSellerName());
        }
        if (StringUtils.hasText(query.getSellerTaxNo())) {
            wrapper.eq(Invoice::getSellerTaxNo, query.getSellerTaxNo());
        }
        if (StringUtils.hasText(query.getInvoiceNo())) {
            wrapper.eq(Invoice::getInvoiceNo, query.getInvoiceNo());
        }
        if (query.getQualifyStatus() != null) {
            wrapper.eq(Invoice::getQualifyStatus, QualifyStatus.of(query.getQualifyStatus()));
        }
        if (query.getFinanceStatus() != null) {
            wrapper.eq(Invoice::getFinanceStatus, FinanceStatus.of(query.getFinanceStatus()));
        }
        if (query.getReimburseStatus() != null) {
            wrapper.eq(Invoice::getReimburseStatus, ReimburseStatus.of(query.getReimburseStatus()));
        }
        if (StringUtils.hasText(query.getWeekStart())) {
            wrapper.eq(Invoice::getWeekStart, LocalDate.parse(query.getWeekStart()));
        }
        if (StringUtils.hasText(query.getSubmitTimeFrom())) {
            wrapper.ge(Invoice::getSubmitTime, LocalDateTime.parse(query.getSubmitTimeFrom() + "T00:00:00"));
        }
        if (StringUtils.hasText(query.getSubmitTimeTo())) {
            wrapper.le(Invoice::getSubmitTime, LocalDateTime.parse(query.getSubmitTimeTo() + "T23:59:59"));
        }

        wrapper.orderByDesc(Invoice::getId);

        Page<Invoice> page = invoiceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<InvoiceListVO> list = page.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());

        return PageVO.of(page.getTotal(), pageNum, pageSize, list);
    }


    /**
     * 查询发票详情
     */
    @Override
    public InvoiceDetailVO getInvoiceDetail(Long invoiceId) {
        if (invoiceId == null) {throw new BizException(400, "invoiceId 不能为空");}

        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {throw new BizException(404, "发票不存在：" + invoiceId);}

        InvoiceDetailVO vo = new InvoiceDetailVO();
        // 主表
        vo.setId(invoice.getId());
        vo.setSubmitterId(invoice.getSubmitterId());
        vo.setSellerName(invoice.getSellerName());
        vo.setSellerTaxNo(invoice.getSellerTaxNo());
        vo.setBuyerName(invoice.getBuyerName());
        vo.setBuyerTaxNo(invoice.getBuyerTaxNo());
        vo.setInvoiceNo(invoice.getInvoiceNo());
        vo.setInvoiceDate(invoice.getInvoiceDate());
        vo.setTotalAmount(invoice.getTotalAmount());
        vo.setSubmitTime(invoice.getSubmitTime());
        vo.setWeekStart(invoice.getWeekStart());

        vo.setQualifyStatus(invoice.getQualifyStatus() == null ? null : invoice.getQualifyStatus().getCode());
        vo.setQualifyStatusDesc(invoice.getQualifyStatus() == null ? null : invoice.getQualifyStatus().getDesc());
        vo.setFinanceStatus(invoice.getFinanceStatus() == null ? null : invoice.getFinanceStatus().getCode());
        vo.setFinanceStatusDesc(invoice.getFinanceStatus() == null ? null : invoice.getFinanceStatus().getDesc());
        vo.setReimburseStatus(invoice.getReimburseStatus() == null ? null : invoice.getReimburseStatus().getCode());
        vo.setReimburseStatusDesc(invoice.getReimburseStatus() == null ? null : invoice.getReimburseStatus().getDesc());

        vo.setCategoryResult(invoice.getCategoryResult() == null ? null : invoice.getCategoryResult().getCode());
        vo.setCategoryResultDesc(invoice.getCategoryResult() == null ? null : invoice.getCategoryResult().getDesc());
        vo.setRejectReason(invoice.getRejectReason());
        vo.setEvidenceRequired(invoice.getEvidenceRequired());
        vo.setEvidenceReason(invoice.getEvidenceReason() == null ? null : invoice.getEvidenceReason().getCode());
        vo.setNeedManual(invoice.getNeedManual());
        vo.setManualRemark(invoice.getManualRemark());
        vo.setSourceFileId(invoice.getSourceFileId());

        vo.setCreateTime(invoice.getCreateTime());
        vo.setUpdateTime(invoice.getUpdateTime());


        // 原始文件
        if (invoice.getSourceFileId() != null) {
            InvoiceFile file = invoiceFileMapper.selectById(invoice.getSourceFileId());
            if (file != null) {
                vo.setSourceFileKey(file.getFileKey());
                vo.setSourceFileName(file.getOriginalName());
            }
        }

        // 商品明细
        List<InvoiceItem> items = invoiceItemMapper.selectList(
                new LambdaQueryWrapper<InvoiceItem>()
                        .eq(InvoiceItem::getInvoiceId, invoiceId)
                        .orderByAsc(InvoiceItem::getId)
        );
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));

        // 凭证组
        List<PaymentEvidenceGroup> groups = groupMapper.selectList(
                new LambdaQueryWrapper<PaymentEvidenceGroup>()
                        .eq(PaymentEvidenceGroup::getInvoiceId, invoiceId)
                        .orderByDesc(PaymentEvidenceGroup::getId)
        );
        vo.setEvidenceGroups(groups.stream().map(this::toGroupVO).collect(Collectors.toList()));

        // 操作日志
        List<OperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .eq(OperationLog::getInvoiceId, invoiceId)
                        .orderByDesc(OperationLog::getId)
        );
        vo.setLogs(logs.stream().map(this::toLogVO).collect(Collectors.toList()));
        vo.setDuplicates(findDuplicates(invoiceId));
        return vo;
    }

    /**
     * 人工修改发票（结构化字段 + 商品明细）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualEditInvoice(InvoiceManualEditDTO dto, Long operatorId) {
        // 校验参数
        if (dto == null || dto.getInvoiceId() == null) {
            throw new BizException(400, "invoiceId 不能为空");
        }
        if (operatorId == null) {
            throw new BizException(400, "操作人不能为空");
        }

        Invoice invoice = invoiceMapper.selectById(dto.getInvoiceId());
        if (invoice == null) {
            throw new BizException(404, "发票不存在：" + dto.getInvoiceId());
        }

        log.info("【人工修改】invoiceId={}, operatorId={}, remark={}",
                dto.getInvoiceId(), operatorId, dto.getRemark());

        // ============ 1. 修改发票主表 ============
        // ⚠️ 记录 before 快照（只记要改的字段）
        InvoiceSnapshot before = snapshot(invoice);

        boolean changed = false;
        if (dto.getSellerName() != null && !Objects.equals(dto.getSellerName(), invoice.getSellerName())) {
            invoice.setSellerName(dto.getSellerName());
            changed = true;
        }
        if (dto.getSellerTaxNo() != null && !Objects.equals(dto.getSellerTaxNo(), invoice.getSellerTaxNo())) {
            invoice.setSellerTaxNo(dto.getSellerTaxNo());
            changed = true;
        }
        if (dto.getBuyerName() != null && !Objects.equals(dto.getBuyerName(), invoice.getBuyerName())) {
            invoice.setBuyerName(dto.getBuyerName());
            changed = true;
        }
        if (dto.getBuyerTaxNo() != null && !Objects.equals(dto.getBuyerTaxNo(), invoice.getBuyerTaxNo())) {
            invoice.setBuyerTaxNo(dto.getBuyerTaxNo());
            changed = true;
        }
        if (dto.getInvoiceNo() != null && !Objects.equals(dto.getInvoiceNo(), invoice.getInvoiceNo())) {
            invoice.setInvoiceNo(dto.getInvoiceNo());
            changed = true;
        }
        if (dto.getInvoiceDate() != null && !Objects.equals(dto.getInvoiceDate(), invoice.getInvoiceDate())) {
            invoice.setInvoiceDate(dto.getInvoiceDate());
            changed = true;
        }
        if (dto.getTotalAmount() != null && !Objects.equals(dto.getTotalAmount(), invoice.getTotalAmount())) {
            invoice.setTotalAmount(dto.getTotalAmount());
            changed = true;
        }
        if (dto.getQualifyStatus() != null) {
            QualifyStatus newStatus = QualifyStatus.of(dto.getQualifyStatus());
            if (newStatus != invoice.getQualifyStatus()) {
                invoice.setQualifyStatus(newStatus);
                changed = true;

                // 状态变成终态时，清掉补凭证标记
                if (newStatus == QualifyStatus.APPROVED
                        || newStatus == QualifyStatus.REJECTED
                        || newStatus == QualifyStatus.CANCELLED) {
                    invoice.setEvidenceRequired(0);
                    invoice.setEvidenceReason(null);
                }
            }
        }
        if (dto.getManualRemark() != null) {
            invoice.setManualRemark(dto.getManualRemark());
            changed = true;
        }
        if (dto.getNeedManual() != null) {
            invoice.setNeedManual(dto.getNeedManual());
            changed = true;
        }

        // 注意：aiRawResult 不在这里修改，保留 AI 原始结果

        if (changed) {
            invoiceMapper.updateById(invoice);
            InvoiceSnapshot after = snapshot(invoice);
            writeLog(invoice.getId(), operatorId, "MANUAL_EDIT",
                    toJson(before), toJson(after), dto.getRemark());
            log.info("【人工修改】invoiceId={} 主表已更新", dto.getInvoiceId());
        } else {
            log.info("【人工修改】invoiceId={} 主表无变化", dto.getInvoiceId());
        }

        // ============ 2. 修改商品明细 ============
        if (dto.getItems() != null) {
            List<InvoiceItem> oldItems = invoiceItemMapper.selectList(
                    new LambdaQueryWrapper<InvoiceItem>()
                            .eq(InvoiceItem::getInvoiceId, invoice.getId())
                            .orderByAsc(InvoiceItem::getId)
            );
            String oldItemsJson = toJson(oldItems);

            // 全量替换：先删旧的，再插新的
            invoiceItemMapper.delete(
                    new LambdaQueryWrapper<InvoiceItem>()
                            .eq(InvoiceItem::getInvoiceId, invoice.getId())
            );

            List<InvoiceItem> newItems = new ArrayList<>();
            for (InvoiceManualEditDTO.ItemDTO itemDTO : dto.getItems()) {
                if (itemDTO == null || itemDTO.getItemName() == null) {continue;}

                InvoiceItem entity = new InvoiceItem();
                entity.setInvoiceId(invoice.getId());
                entity.setItemName(itemDTO.getItemName());
                entity.setQuantity(itemDTO.getQuantity());
                entity.setUnitPrice(itemDTO.getUnitPrice());
                entity.setAmount(itemDTO.getAmount());
                if (itemDTO.getCategoryLevel() != null) {
                    entity.setCategoryLevel(CategoryLevel.of(itemDTO.getCategoryLevel()));
                }
                entity.setCategoryName(itemDTO.getCategoryName());
                if (itemDTO.getPriceType() != null) {
                    entity.setPriceType(PriceType.of(itemDTO.getPriceType()));
                }
                entity.setAiReason(itemDTO.getAiReason());
                invoiceItemMapper.insert(entity);
                newItems.add(entity);
            }

            writeLog(invoice.getId(), operatorId, "MANUAL_EDIT_ITEMS",
                    oldItemsJson, toJson(newItems), dto.getRemark());
            log.info("【人工修改】invoiceId={} 商品明细已替换，{} → {} 条",
                    dto.getInvoiceId(), oldItems.size(), newItems.size());
        }
    }

    /**
     * 更新发票状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoiceStatus(InvoiceStatusUpdateDTO dto, Long operatorId) {
        // ============ 校验 ============
        if (dto == null || dto.getInvoiceId() == null) {
            throw new BizException(400, "invoiceId 不能为空");
        }
        if (operatorId == null) {
            throw new BizException(400, "操作人不能为空");
        }
        if (dto.getType() == null) {
            throw new BizException(400, "type 不能为空");
        }

        Invoice invoice = invoiceMapper.selectById(dto.getInvoiceId());
        if (invoice == null) {
            throw new BizException(404, "发票不存在：" + dto.getInvoiceId());
        }

        String type = dto.getType().toUpperCase();
        log.info("【状态维护】invoiceId={}, type={}, status={}, operatorId={}",
                dto.getInvoiceId(), type, dto.getStatus(), operatorId);

        switch (type) {
            case "FINANCE":
                updateFinance(invoice, dto, operatorId);
                break;
            case "REIMBURSE":
                updateReimburse(invoice, dto, operatorId);
                break;
            case "CANCEL":
                cancel(invoice, dto, operatorId);
                break;
            default:
                throw new BizException(400, "不支持的 type：" + type);
        }
    }

    /**
     * 检测发票是否疑似重复
     */
    @Override
    public List<InvoiceListVO> detectAndMark(Long invoiceId) {
        if (invoiceId == null) {throw new BizException(400, "invoiceId 不能为空");}

        List<InvoiceListVO> duplicates = findDuplicates(invoiceId);

        if (!duplicates.isEmpty()) {
            log.warn("【去重】invoiceId={} 发现 {} 张疑似重复", invoiceId, duplicates.size());
            String ids = duplicates.stream()
                    .map(d -> String.valueOf(d.getId()))
                    .collect(Collectors.joining(","));
            writeLog(invoiceId, "DUPLICATE_HIT",
                    "疑似重复发票 id：" + ids);
        } else {
            log.info("【去重】invoiceId={} 未发现重复", invoiceId);
        }
        return duplicates;
    }

    /**
     * 查询某张发票的疑似重复列表
     */
    @Override
    public List<InvoiceListVO> findDuplicates(Long invoiceId) {
        if (invoiceId == null) {throw new BizException(400, "invoiceId 不能为空");}

        Invoice current = invoiceMapper.selectById(invoiceId);
        if (current == null) {return new ArrayList<>();}

        List<Invoice> candidates = new ArrayList<>();

        // ============ 强匹配：invoice_no + seller_tax_no ============
        if (StringUtils.hasText(current.getInvoiceNo())
                && StringUtils.hasText(current.getSellerTaxNo())) {
            List<Invoice> strong = invoiceMapper.selectList(
                    new LambdaQueryWrapper<Invoice>()
                            .eq(Invoice::getInvoiceNo, current.getInvoiceNo())
                            .eq(Invoice::getSellerTaxNo, current.getSellerTaxNo())
                            .ne(Invoice::getId, invoiceId)   // 排除自己
            );
            candidates.addAll(strong);
        }

        // ============ 弱匹配：seller_tax_no + invoice_date + total_amount ============
        if (StringUtils.hasText(current.getSellerTaxNo())
                && current.getInvoiceDate() != null
                && current.getTotalAmount() != null) {
            List<Invoice> weak = invoiceMapper.selectList(
                    new LambdaQueryWrapper<Invoice>()
                            .eq(Invoice::getSellerTaxNo, current.getSellerTaxNo())
                            .eq(Invoice::getInvoiceDate, current.getInvoiceDate())
                            .eq(Invoice::getTotalAmount, current.getTotalAmount())
                            .ne(Invoice::getId, invoiceId)
            );
            candidates.addAll(weak);
        }

        // 去重（同一张发票可能强弱都命中）
        return candidates.stream()
                .collect(Collectors.toMap(Invoice::getId, e -> e, (a, b) -> a))
                .values()
                .stream()
                .map(this::toListVO)
                .collect(Collectors.toList());
    }


    /**
     * 处理单个 PDF 文件
     */
    private UploadResultVO.Item handleSinglePdf(MultipartFile file, Long adminId,
                                                String batchId, String originalName) {
        UploadResultVO.Item item = new UploadResultVO.Item();
        item.setFileName(originalName);
        try{
            // 保存文件
            String filePath = fileService.storeFile(file, null);
            // 调用发票服务建档
            //插入发票文件
            InvoiceFile invoiceFile = new InvoiceFile();
            invoiceFile.setFileKey(filePath);
            invoiceFile.setOriginalName(originalName);
            invoiceFile.setFileType("PDF");
            invoiceFile.setFileSize(file.getSize());
            invoiceFile.setUploadBatch(batchId);
            invoiceFile.setProcessStatus(FileProcessStatus.PENDING);
            invoiceFileMapper.insert(invoiceFile);
            // 插入发票
            Invoice invoice = new Invoice();
            invoice.setSubmitterId(adminId);
            invoice.setSubmitTime(LocalDateTime.now());
            invoice.setWeekStart(getMonday(LocalDateTime.now().toLocalDate()));
            invoice.setQualifyStatus(QualifyStatus.PENDING);
            invoice.setFinanceStatus(FinanceStatus.NOT_SUBMITTED);
            invoice.setReimburseStatus(ReimburseStatus.NOT_FINISHED);
            invoice.setEvidenceRequired(0);
            invoice.setNeedManual(0);
            invoice.setSourceFileId(invoiceFile.getId());
            invoiceMapper.insert(invoice);
            //回填发票ID
            invoiceFile.setInvoiceId(invoice.getId());
            invoiceFileMapper.updateById(invoiceFile);

            item.setStatus("SUCCESS");
            item.setInvoiceId(invoice.getId());
            item.setFileId(invoiceFile.getId());

        }catch (Exception e){
            log.error("PDF建档失败：{}", e.getMessage());
            item.setStatus("FAILED");
            item.setErrorMsg(e.getMessage());
        }
        return item;
    }


    /**
     * zip包解压并处理每个PDF
     */
    private int handleZip(MultipartFile zipFile, File tempDir, Long adminId,
                          String batchId, String originalName, UploadResultVO result) {
        // 1. 先把 ZIP 存到磁盘（放到 tempDir 里）
        File zipOnDisk = new File(tempDir, originalName);
        try {
            zipFile.transferTo(zipOnDisk);
        } catch (IOException e) {
            result.getItems().add(buildFailItem(originalName, "ZIP 保存失败：" + e.getMessage()));
            return 0;
        }

        // 2. ZIP 本身也存一条 invoice_file（file_type=ZIP，invoice_id=null）
        String zipFilePath = null;
        Long zipFileId = null;
        try {
            zipFilePath = fileService.storeFile(zipFile, null);
            InvoiceFile zipRecord = new InvoiceFile();
            zipRecord.setFileKey(zipFilePath);
            zipRecord.setOriginalName(originalName);
            zipRecord.setFileType("ZIP");
            zipRecord.setFileSize(zipFile.getSize());
            zipRecord.setUploadBatch(batchId);
            zipRecord.setProcessStatus(FileProcessStatus.SUCCESS);
            invoiceFileMapper.insert(zipRecord);
            zipFileId = zipRecord.getId();
        } catch (Exception e) {
            log.warn("ZIP 本身建档失败：{}", e.getMessage());
        }
        // 3. 解压 ZIP 包
        File extractDir = new File(tempDir, "extract_" + IdUtil.fastSimpleUUID());
        java.util.List<File> pdfs;
        try {
            pdfs = ZipUtil.extractPdfs(zipOnDisk, extractDir);
        } catch (Exception e) {
            log.error("ZIP 解压失败：{}", originalName, e);
            result.getItems().add(buildFailItem(originalName, "ZIP 解压失败：" + e.getMessage()));
            return 0;
        }
        // 4. 处理每个 PDF 文件
        if (pdfs.isEmpty()) {
            result.getItems().add(buildFailItem(originalName, "ZIP 内未找到 PDF 文件"));
            return 0;
        }
        for(File pdf : pdfs){
            UploadResultVO.Item item = new UploadResultVO.Item();
            item.setFileName(pdf.getName());
            try{
                // 存文件
                String filePath = storeLocalFile(pdf);
                // 调用发票服务建档
                // invoice_file
                InvoiceFile invoiceFile = new InvoiceFile();
                invoiceFile.setFileKey(filePath);
                invoiceFile.setOriginalName(pdf.getName());
                invoiceFile.setFileType("PDF");
                invoiceFile.setFileSize(pdf.length());
                invoiceFile.setUploadBatch(batchId);
                invoiceFile.setProcessStatus(FileProcessStatus.PENDING);
                invoiceFile.setParentFileId(zipFileId);      // 指向 ZIP
                invoiceFileMapper.insert(invoiceFile);

                // invoice
                Invoice invoice = new Invoice();
                invoice.setSubmitterId(adminId);
                invoice.setSubmitTime(LocalDateTime.now());
                invoice.setWeekStart(getMonday(LocalDateTime.now().toLocalDate()));
                invoice.setQualifyStatus(QualifyStatus.PENDING);
                invoice.setFinanceStatus(FinanceStatus.NOT_SUBMITTED);
                invoice.setReimburseStatus(ReimburseStatus.NOT_FINISHED);
                invoice.setEvidenceRequired(0);
                invoice.setNeedManual(0);
                invoice.setSourceFileId(invoiceFile.getId());
                invoiceMapper.insert(invoice);

                invoiceFile.setInvoiceId(invoice.getId());
                invoiceFileMapper.updateById(invoiceFile);

                item.setInvoiceId(invoice.getId());
                item.setFileId(invoiceFile.getId());
                item.setStatus("SUCCESS");

            }catch (Exception e){
                log.error("PDF建档失败：{}", e.getMessage());
                item.setStatus("FAILED");
                item.setErrorMsg(e.getMessage());
            }
            result.getItems().add(item);
        }
        //清理临时目录
        deleteQuietly(extractDir);
        return pdfs.size();
    }

    /**
     * 把本地文件复制到 storage 并返回 filePath
     */
    private String storeLocalFile(File localFile) throws IOException {
        String ext = localFile.getName().substring(localFile.getName().lastIndexOf('.') + 1).toLowerCase();
        String datePath = LocalDate.now().toString().replace("-", "/");
        String uuid = IdUtil.fastSimpleUUID();
        String filePath = "upload/" + datePath + "/" + uuid + "." + ext;
        Path target = Paths.get(storagePath, filePath);
        Files.createDirectories(target.getParent());
        Files.copy(localFile.toPath(), target);
        return filePath;
    }

    private UploadResultVO.Item buildFailItem(String fileName, String errorMsg) {
        UploadResultVO.Item item = new UploadResultVO.Item();
        item.setFileName(fileName);
        item.setStatus("FAILED");
        item.setErrorMsg(errorMsg);
        return item;
    }

    /**
     * 取某个日期所在周的周一
     */
    private LocalDate getMonday(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1);
    }

    /**
     * 静默删除目录（不抛异常）
     */
    private void deleteQuietly(File dir) {
        if (dir == null || !dir.exists())
        {return;}
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory())
                {deleteQuietly(f);}
                else {f.delete();}
            }
        }
        dir.delete();
    }

    /**
     * 单件商品判定
     */
    private RuleResultDTO.ItemRuleResult judgeItem(StructuredInvoice.Item item) {
        RuleResultDTO.ItemRuleResult ir = new RuleResultDTO.ItemRuleResult();
        ir.setItemName(item.getItemName());
        ir.setUnitPrice(item.getUnitPrice());

        // 1. 品类判定：关键词匹配 → 未命中 → AI 兜底
        CategoryRule matched = matchCategoryRule(item.getItemName());
        if (matched != null) {
            // 命中规则表，直接用
            ir.setCategoryLevel(CategoryLevel.of(matched.getLevel()));
            ir.setCategoryName(matched.getCategoryName());
            ir.setAiReason("匹配规则：" + matched.getCategoryName());
        } else if (aiCategoryService != null) {
            // 未命中，走 AI
            log.info("【规则引擎】关键词未命中，走 AI 品类审核：{}", item.getItemName());
            try {
                AiCategoryResult aiResult = aiCategoryService.judge(item.getItemName());
                ir.setCategoryLevel(CategoryLevel.of(aiResult.getLevel()));
                ir.setCategoryName(aiResult.getCategoryName());
                ir.setAiReason("AI判断：" + (aiResult.getReason() == null ? "" : aiResult.getReason()));
                log.info("【规则引擎】AI 判定 item={} → level={}, reason={}",
                        item.getItemName(), aiResult.getLevel(), aiResult.getReason());
            } catch (Exception e) {
                log.error("【规则引擎】AI 品类审核异常，降级为存疑", e);
                ir.setCategoryLevel(CategoryLevel.DOUBT);
                ir.setAiReason("AI 调用异常，保守存疑");
            }
        } else {
            // AI 服务未启用（mock 模式），保守存疑
            ir.setCategoryLevel(CategoryLevel.DOUBT);
            ir.setAiReason("未匹配规则且未启用 AI，保守存疑");
        }

        // 2. 单价分类（不变）
        BigDecimal price = item.getUnitPrice();
        if (price == null) {
            ir.setPriceType(null);
        } else if (price.compareTo(assetThreshold) >= 0) {
            ir.setPriceType(PriceType.ASSET);
        } else if (price.compareTo(lowValueThreshold) >= 0) {
            ir.setPriceType(PriceType.LOW_VALUE);
        } else {
            ir.setPriceType(PriceType.MATERIAL);
        }

        return ir;
    }

    /**
     * 关键词匹配品类规则
     * 遍历规则表，看商品名是否包含规则关键词
     */
    private CategoryRule matchCategoryRule(String itemName) {
        if (itemName == null || itemName.isBlank()) {return null;}

        List<CategoryRule> rules = categoryRuleMapper.selectList(
                new LambdaQueryWrapper<CategoryRule>().eq(CategoryRule::getEnabled, 1)
        );
        for (CategoryRule rule : rules) {
            String keywords = rule.getKeywords();
            if (keywords == null || keywords.isBlank()) {continue;}
            for (String kw : keywords.split(",")) {
                if (!kw.isBlank() && itemName.contains(kw.trim())) {
                    return rule;
                }
            }
        }
        return null;
    }

    /**
     * 查询同周同商家的有效累计（不含当前发票）
     */
    private BigDecimal sumEffectiveAmount(String sellerTaxNo, LocalDate weekStart, Long excludeInvoiceId) {
        // 有效状态：待补凭证(1) / 待人工(2) / 通过(3)
        // 排除当前发票（重新判定时避免把自己算进去）
        List<Invoice> list = invoiceMapper.selectList(
                new LambdaQueryWrapper<Invoice>()
                        .eq(Invoice::getSellerTaxNo, sellerTaxNo)
                        .eq(Invoice::getWeekStart, weekStart)
                        .in(Invoice::getQualifyStatus,
                                QualifyStatus.PENDING_EVIDENCE,
                                QualifyStatus.PENDING_MANUAL,
                                QualifyStatus.APPROVED)
                        .ne(excludeInvoiceId != null, Invoice::getId, excludeInvoiceId)
        );

        BigDecimal sum = BigDecimal.ZERO;
        for (Invoice inv : list) {
            if (inv.getTotalAmount() != null) {
                sum = sum.add(inv.getTotalAmount());
            }
        }
        return sum;
    }

    private InvoiceListVO toListVO(Invoice e) {
        InvoiceListVO vo = new InvoiceListVO();
        vo.setId(e.getId());
        vo.setSellerName(e.getSellerName());
        vo.setSellerTaxNo(e.getSellerTaxNo());
        vo.setInvoiceNo(e.getInvoiceNo());
        vo.setInvoiceDate(e.getInvoiceDate());
        vo.setTotalAmount(e.getTotalAmount());
        vo.setSubmitTime(e.getSubmitTime());
        vo.setWeekStart(e.getWeekStart());
        vo.setQualifyStatus(e.getQualifyStatus() == null ? null : e.getQualifyStatus().getCode());
        vo.setQualifyStatusDesc(e.getQualifyStatus() == null ? null : e.getQualifyStatus().getDesc());
        vo.setFinanceStatus(e.getFinanceStatus() == null ? null : e.getFinanceStatus().getCode());
        vo.setFinanceStatusDesc(e.getFinanceStatus() == null ? null : e.getFinanceStatus().getDesc());
        vo.setReimburseStatus(e.getReimburseStatus() == null ? null : e.getReimburseStatus().getCode());
        vo.setReimburseStatusDesc(e.getReimburseStatus() == null ? null : e.getReimburseStatus().getDesc());
        vo.setCategoryResult(e.getCategoryResult() == null ? null : e.getCategoryResult().getCode());
        vo.setCategoryResultDesc(e.getCategoryResult() == null ? null : e.getCategoryResult().getDesc());
        vo.setEvidenceRequired(e.getEvidenceRequired());
        vo.setRejectReason(e.getRejectReason());
        vo.setNeedManual(e.getNeedManual());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    private InvoiceDetailVO.ItemVO toItemVO(InvoiceItem e) {
        InvoiceDetailVO.ItemVO vo = new InvoiceDetailVO.ItemVO();
        vo.setId(e.getId());
        vo.setItemName(e.getItemName());
        vo.setQuantity(e.getQuantity());
        vo.setUnitPrice(e.getUnitPrice());
        vo.setAmount(e.getAmount());
        vo.setCategoryLevel(e.getCategoryLevel() == null ? null : e.getCategoryLevel().getCode());
        vo.setCategoryLevelDesc(e.getCategoryLevel() == null ? null : e.getCategoryLevel().getDesc());
        vo.setCategoryName(e.getCategoryName());
        vo.setPriceType(e.getPriceType() == null ? null : e.getPriceType().getCode());
        vo.setPriceTypeDesc(e.getPriceType() == null ? null : e.getPriceType().getDesc());
        vo.setAiReason(e.getAiReason());
        return vo;
    }

    private EvidenceGroupVO toGroupVO(PaymentEvidenceGroup g) {
        EvidenceGroupVO vo = new EvidenceGroupVO();
        vo.setId(g.getId());
        vo.setInvoiceId(g.getInvoiceId());
        vo.setStatus(g.getStatus() == null ? null : g.getStatus().getCode());
        vo.setStatusDesc(g.getStatus() == null ? null : g.getStatus().getDesc());
        vo.setReviewerId(g.getReviewerId());
        vo.setReviewTime(g.getReviewTime());
        vo.setReviewRemark(g.getReviewRemark());
        vo.setCreateTime(g.getCreateTime());

        List<OrderScreenshot> ss = screenshotMapper.selectList(
                new LambdaQueryWrapper<OrderScreenshot>().eq(OrderScreenshot::getGroupId, g.getId()));
        for (OrderScreenshot s : ss) {
            vo.getScreenshots().add(buildFileVO(s.getId(), s.getFileId(), s.getOriginalName()));
        }

        List<PaymentRecord> pr = recordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getGroupId, g.getId()));
        for (PaymentRecord p : pr) {
            vo.getPayments().add(buildFileVO(p.getId(), p.getFileId(), p.getOriginalName()));
        }
        return vo;
    }

    private EvidenceGroupVO.FileVO buildFileVO(Long id, Long fileId, String originalName) {
        EvidenceGroupVO.FileVO f = new EvidenceGroupVO.FileVO();
        f.setId(id);
        f.setFileId(fileId);
        f.setOriginalName(originalName);
        InvoiceFile file = invoiceFileMapper.selectById(fileId);
        if (file != null) {f.setFilePath(file.getFileKey());}
        return f;
    }

    private InvoiceDetailVO.LogVO toLogVO(OperationLog e) {
        InvoiceDetailVO.LogVO vo = new InvoiceDetailVO.LogVO();
        vo.setId(e.getId());
        vo.setOperatorId(e.getOperatorId());
        vo.setOperatorName(e.getOperatorName());
        vo.setAction(e.getAction());
        vo.setBeforeValue(e.getBeforeValue());
        vo.setAfterValue(e.getAfterValue());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    /**
     * 快照：只保留关键字段，方便对比
     */
    private InvoiceSnapshot snapshot(Invoice e) {
        InvoiceSnapshot s = new InvoiceSnapshot();
        s.sellerName = e.getSellerName();
        s.sellerTaxNo = e.getSellerTaxNo();
        s.buyerName = e.getBuyerName();
        s.buyerTaxNo = e.getBuyerTaxNo();
        s.invoiceNo = e.getInvoiceNo();
        s.invoiceDate = e.getInvoiceDate() == null ? null : e.getInvoiceDate().toString();
        s.totalAmount = e.getTotalAmount() == null ? null : e.getTotalAmount().toPlainString();
        s.qualifyStatus = e.getQualifyStatus() == null ? null : e.getQualifyStatus().getCode();
        s.manualRemark = e.getManualRemark();
        s.needManual = e.getNeedManual();
        return s;
    }

    private void writeLog(Long invoiceId, Long operatorId, String action,
                          String before, String after, String remark) {
        try {
            OperationLog log = new OperationLog();
            log.setInvoiceId(invoiceId);
            log.setOperatorId(operatorId);
            log.setAction(action);
            log.setBeforeValue(before);
            log.setAfterValue(after);
            log.setRemark(remark);
            operationLogMapper.insert(log);
        } catch (Exception e) {
            log.error("写操作日志失败：invoiceId={}, action={}", invoiceId, action, e);
        }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            log.warn("JSON 序列化失败", e);
            return String.valueOf(o);
        }
    }

    /**
     * 快照类（用于日志）
     */
    @lombok.Data
    private static class InvoiceSnapshot {
        private String sellerName;
        private String sellerTaxNo;
        private String buyerName;
        private String buyerTaxNo;
        private String invoiceNo;
        private String invoiceDate;
        private String totalAmount;
        private Integer qualifyStatus;
        private String manualRemark;
        private Integer needManual;
    }

    /**
     * 更新财务状态
     */
    private void updateFinance(Invoice invoice, InvoiceStatusUpdateDTO dto, Long operatorId) {
        // 前置条件：资质审核必须通过
        if (invoice.getQualifyStatus() != QualifyStatus.APPROVED) {
            throw new BizException(400, "资质审核未通过，不能提交财务");
        }

        if (dto.getStatus() == null) {
            throw new BizException(400, "status 不能为空");
        }

        FinanceStatus target = FinanceStatus.of(dto.getStatus());
        if (target == null) {
            throw new BizException(400, "无效的财务状态：" + dto.getStatus());
        }

        FinanceStatus old = invoice.getFinanceStatus();
        if (Objects.equals(old, target)) {
            log.info("【状态维护】invoiceId={} 财务状态已是 {}，跳过", invoice.getId(), target);
            return;
        }

        invoice.setFinanceStatus(target);
        invoiceMapper.updateById(invoice);

        writeLog(invoice.getId(), operatorId, "FINANCE_STATUS_CHANGE",
                old == null ? null : old.getDesc(),
                target.getDesc(),
                dto.getRemark());

        log.info("【状态维护】invoiceId={} 财务状态：{} → {}", invoice.getId(), old, target);
    }

    /**
     * 更新报销状态
     */
    private void updateReimburse(Invoice invoice, InvoiceStatusUpdateDTO dto, Long operatorId) {
        // 前置条件：财务必须已提交
        if (invoice.getFinanceStatus() != FinanceStatus.SUBMITTED) {
            throw new BizException(400, "未提交财务，不能更新报销状态");
        }

        if (dto.getStatus() == null) {
            throw new BizException(400, "status 不能为空");
        }

        ReimburseStatus target = ReimburseStatus.of(dto.getStatus());
        if (target == null) {
            throw new BizException(400, "无效的报销状态：" + dto.getStatus());
        }

        ReimburseStatus old = invoice.getReimburseStatus();
        if (Objects.equals(old, target)) {
            log.info("【状态维护】invoiceId={} 报销状态已是 {}，跳过", invoice.getId(), target);
            return;
        }

        invoice.setReimburseStatus(target);
        invoiceMapper.updateById(invoice);

        writeLog(invoice.getId(), operatorId, "REIMBURSE_STATUS_CHANGE",
                old == null ? null : old.getDesc(),
                target.getDesc(),
                dto.getRemark());

        log.info("【状态维护】invoiceId={} 报销状态：{} → {}", invoice.getId(), old, target);
    }

    /**
     * 更新取消状态
     */
    private void cancel(Invoice invoice, InvoiceStatusUpdateDTO dto, Long operatorId) {
        QualifyStatus old = invoice.getQualifyStatus();

        if (old == QualifyStatus.CANCELLED) {
            throw new BizException(400, "发票已取消");
        }
        if (old == QualifyStatus.REJECTED) {
            throw new BizException(400, "发票已拒绝，不能取消");
        }
        // ⚠️ 已提交财务的不能取消
        if (invoice.getFinanceStatus() == FinanceStatus.SUBMITTED) {
            throw new BizException(400, "已提交财务，不能取消");
        }

        invoice.setQualifyStatus(QualifyStatus.CANCELLED);
        invoice.setEvidenceRequired(0);
        invoice.setEvidenceReason(null);
        invoiceMapper.updateById(invoice);

        writeLog(invoice.getId(), operatorId, "INVOICE_CANCEL",
                old == null ? null : old.getDesc(),
                QualifyStatus.CANCELLED.getDesc(),
                dto.getRemark());

        log.info("【状态维护】invoiceId={} 已取消，原状态={}", invoice.getId(), old);
    }

    private void writeLog(Long invoiceId, String action, String detail) {
        try {
            OperationLog log = new OperationLog();
            log.setInvoiceId(invoiceId);
            log.setAction(action);
            log.setAfterValue(detail);
            operationLogMapper.insert(log);
        } catch (Exception e) {
            log.error("写去重日志失败：invoiceId={}", invoiceId, e);
        }
    }


}