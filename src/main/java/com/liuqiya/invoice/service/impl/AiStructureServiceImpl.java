package com.liuqiya.invoice.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.pojo.dto.StructuredInvoice;
import com.liuqiya.invoice.service.AiStructureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AI结构化发票服务实现类
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.type", havingValue = "qwen")
public class AiStructureServiceImpl implements AiStructureService {
    @Value("${app.ai.model:qwen-plus}")
    private String model;

    @Value("${app.ai.temperature:0.1}")
    private float temperature;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 系统提示词
     */
    private static final String SYSTEM_PROMPT = """
            你是一个发票信息提取助手。请从用户提供的发票文本中提取字段，严格按 JSON 格式输出。
            不要输出任何解释，不要加 markdown 代码块标记。
            """;

    /**
     * AI结构化发票
     */
    @Override
    public StructuredInvoice structure(String rawText) {
        //校验参数
        log.info("待结构化文本: {}", rawText);
        if (rawText == null || rawText.isBlank()) {
            throw new BizException(400, "待结构化文本为空");
        }
        //校验环境变量
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new BizException(500, "环境变量 DASHSCOPE_API_KEY 未配置");
        }

        try {
            // 构造 prompt
            String userPrompt = buildUserPrompt(rawText);

            // 调用千问接口
            String jsonResult = callQwen(apiKey, userPrompt);

            //解析 JSON 成 StructuredInvoice
            return parseJson(jsonResult);

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("千问结构化" +
                    "失败", e);
            throw new BizException(2006, "AI 结构化失败：" + e.getMessage());
        }
    }

    /**
     * 拼接用户 prompt 提示词
     */
    private String buildUserPrompt(String rawText) {
        return """
                请从以下发票文本中提取字段，按此 JSON 格式输出：
                {
                  "sellerName": "销售方名称",
                  "sellerTaxNo": "销售方纳税人识别号",
                  "buyerName": "购买方名称",
                  "buyerTaxNo": "购买方税号",
                  "invoiceNo": "发票号码",
                  "invoiceDate": "yyyy-MM-dd",
                  "totalAmount": 数字,
                  "items": [
                    {
                      "itemName": "商品名称",
                      "quantity": 数字,
                      "unitPrice": 数字,
                      "amount": 数字
                    }
                  ]
                }

                要求：
                1. 只输出 JSON，不要加任何解释或 markdown 代码块标记
                2. 找不到的字段填 null
                3. 金额、数量用数字，不加引号
                4. 日期格式 yyyy-MM-dd

                发票文本：
                ---
                %s
                ---
                """.formatted(rawText);
    }

    /**
     * 调千问 API
     */
    private String callQwen(String apiKey, String userPrompt) throws Exception {
        Generation gen = new Generation();

        // 构造系统提示词
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(SYSTEM_PROMPT)
                .build();

        // 构造用户提示词
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userPrompt)
                .build();

        // 构造请求参数
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(List.of(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .temperature(temperature)
                .build();

        // 调用千问接口
        GenerationResult result = gen.call(param);

        // 校验返回结果
        if (result == null || result.getOutput() == null
                || result.getOutput().getChoices() == null
                || result.getOutput().getChoices().isEmpty()) {
            throw new BizException(2006, "千问返回结果为空");
        }

        // 提取返回内容
        String content = result.getOutput().getChoices().get(0).getMessage().getContent();
        log.info("千问原始返回：{}", content);
        return content;
    }

    /**
     * 解析千问返回的 JSON
     */
    private StructuredInvoice parseJson(String jsonStr) throws Exception {
        if (jsonStr == null || jsonStr.isBlank()) {
            throw new BizException(2006, "千问返回内容为空");
        }

        String cleaned = jsonStr.trim();

        // 去掉 markdown 代码块标记 ```json ... ```
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "")
                    .replaceAll("```\\s*$", "");
        }

        // 只截取第一个 { 到最后一个 } 之间的内容（去掉模型加的解释文字）
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BizException(2006, "千问返回内容不含合法 JSON：" + jsonStr);
        }
        cleaned = cleaned.substring(start, end + 1);

        JsonNode root;
        try {
            root = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("JSON 解析失败，原始内容：{}", cleaned);
            throw new BizException(2006, "千问返回 JSON 格式错误：" + e.getMessage());
        }

        StructuredInvoice result = new StructuredInvoice();
        result.setSellerName(textOrNull(root, "sellerName"));
        result.setSellerTaxNo(textOrNull(root, "sellerTaxNo"));
        result.setBuyerName(textOrNull(root, "buyerName"));
        result.setBuyerTaxNo(textOrNull(root, "buyerTaxNo"));
        result.setInvoiceNo(textOrNull(root, "invoiceNo"));
        result.setTotalAmount(decimalOrNull(root, "totalAmount"));

        // 日期多格式兼容
        String dateStr = textOrNull(root, "invoiceDate");
        if (dateStr != null && !dateStr.isBlank()) {
            result.setInvoiceDate(parseDate(dateStr));
        }

        // 商品明细
        JsonNode itemsNode = root.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            List<StructuredInvoice.Item> items = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                StructuredInvoice.Item item = new StructuredInvoice.Item();
                item.setItemName(textOrNull(itemNode, "itemName"));
                item.setQuantity(decimalOrNull(itemNode, "quantity"));
                item.setUnitPrice(decimalOrNull(itemNode, "unitPrice"));
                item.setAmount(decimalOrNull(itemNode, "amount"));

                // 过滤掉完全空的商品条目
                if (item.getItemName() != null || item.getAmount() != null) {
                    items.add(item);
                }
            }
            result.setItems(items);
        }

        // 如果 totalAmount 缺失，用 items 金额求和
        if (result.getTotalAmount() == null && result.getItems() != null) {
            BigDecimal sum = result.getItems().stream()
                    .map(StructuredInvoice.Item::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("totalAmount 缺失，用 items 求和替代：{}", sum);
                result.setTotalAmount(sum);
            }
        }

        return result;
    }

    /**
     * 取字符串字段，兼容 null / "null" / 空串
     */
    private String textOrNull(JsonNode node, String field) {
        if (node == null) {return null;}
        JsonNode f = node.get(field);
        if (f == null || f.isNull()) {return null;}
        String v = f.asText().trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) {return null;}
        return v;
    }

    /**
     * 取数字字段，兼容 number 和 string 两种形式，去掉千分位逗号和货币符号
     */
    private BigDecimal decimalOrNull(JsonNode node, String field) {
        if (node == null) {return null;}
        JsonNode f = node.get(field);
        if (f == null || f.isNull()) {return null;}

        String raw = f.asText().trim();
        if (raw.isEmpty() || "null".equalsIgnoreCase(raw)) {return null;}

        // 去掉常见干扰字符：千分位逗号、货币符号、空格
        String cleaned = raw.replace(",", "")
                .replace("¥", "")
                .replace("￥", "")
                .replace("$", "")
                .replace(" ", "")
                .trim();

        try {
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            log.warn("数字解析失败，字段={}，原值={}", field, raw);
            return null;
        }
    }

    /**
     * 日期多格式兼容解析
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {return null;}

        // 先做字符替换，把中文/斜杠/点 统一成横杠
        String normalized = dateStr.trim()
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
                .replace("/", "-")
                .replace(".", "-")
                .replace(" ", "");

        // 补零：2026-1-8变成2026-01-08
        try {
            String[] parts = normalized.split("-");
            if (parts.length == 3) {
                String y = parts[0];
                String m = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
                String d = parts[2].length() == 1 ? "0" + parts[2] : parts[2];
                normalized = y + "-" + m + "-" + d;
                return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        } catch (Exception e) {
            log.warn("日期归一化失败：{}", dateStr);
        }

        // 或其它几种格式
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd"};
        for (String p : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }

        log.warn("日期无法解析：{}", dateStr);
        return null;
    }
}

