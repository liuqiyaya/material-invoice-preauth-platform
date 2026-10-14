package com.liuqiya.invoice.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.pojo.dto.AiCategoryResult;
import com.liuqiya.invoice.service.AiCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI审核品类服务实现类
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.type", havingValue = "qwen")
public class AiCategoryServiceImpl implements AiCategoryService {

    @Value("${app.ai.model:qwen-plus}")
    private String model;

    @Value("${app.ai.temperature:0.1}")
    private float temperature;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            你是一个报销品类审核助手。根据工作室/实验室材料费报销规则，判断商品是否可以报销。

            【可以报销的主要品类】
            - 嵌入式/电子硬件：开发板、电路板、电机、传感器、电子元器件、通信模块
            - 电源供电类：电源适配器、开关电源、DC-DC模块
            - 电池类：锂电池、18650、电池组、电池盒
            - 机器人部件：舵机、轮子、履带、减速器、联轴器
            - 机械结构件：铝型材、亚克力板、支架、轴承、齿轮
            - 紧固件：螺丝、螺母、垫片、扎带
            - 电子/加工耗材：PLA、ABS、树脂、焊锡、焊锡膏、热缩管
            - 线材：USB线、网线、HDMI线、电源线、杜邦线
            - 转接器：USB转串口、Type-C转HDMI
            - 网络设备：路由器、交换机、AP
            - 通信扩展设备：USB网卡、串口转换器、CAN转换器
            - 电脑扩展配件：USB Hub、扩展坞、读卡器
            - U盘
            - 工具：剪刀、钳子、电烙铁、螺丝刀、电钻、热风枪
            - 测量仪器：万用表、示波器、测距仪、电子秤
            - 电工/五金：插座、插头、保险丝、接线端子
            - 办公用品：打印纸、笔、文件夹、订书机
            - 打印耗材：墨盒、硒鼓、碳粉、标签纸
            - 胶粘材料：双面胶、绝缘胶带、502、热熔胶
            - 劳保用品：手套、护目镜、口罩、防静电手环
            - 包装用品：纸箱、气泡膜、泡沫、封箱胶
            - 指定生活电器：排插、台灯

            【存疑品类】
            - 其他存储设备：SD卡、TF卡、移动硬盘（U盘除外）
            - 无人机相关设备：飞控、电调、桨叶、遥控器

            【不可以报销的品类】
            - 个人用品/清洁用品、电脑外设（鼠标键盘）、电脑升级配件（内存/SSD/HDD）
            - 摄像设备（USB摄像头、工业相机）、GPU/计算卡（显卡）
            - 工控设备（工控机、PLC）、电脑整机、显示设备、影音设备、摄影设备
            - 个人电子产品（手机、平板、智能手表、充电宝）
            - 家具/人体工学用品、加工服务（PCB打板、3D打印）
            - 软件/云服务、书籍资料、展示宣传用品、其他生活电器

            请严格输出 JSON，不要加任何解释或 markdown 代码块标记：
            {
              "level": 1或2或3,
              "categoryName": "归入的品类名",
              "reason": "判断理由（20字内）"
            }

            其中 level：1=可以报销，2=存疑，3=不可以报销。
            无法可靠判断时，必须输出 2（存疑），不要瞎猜。
            """;

    @Override
    public AiCategoryResult judge(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return doubt("商品名为空");
        }

        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DASHSCOPE_API_KEY 未配置，AI 品类审核降级为存疑");
            return doubt("AI 未配置");
        }

        try {
            String content = callQwen(apiKey, "商品名：" + itemName);
            return parseResult(content);
        } catch (Exception e) {
            log.error("AI 品类审核失败：itemName={}", itemName, e);
            return doubt("AI 调用失败：" + e.getMessage());
        }
    }

    private String callQwen(String apiKey, String userPrompt) throws Exception {
        Generation gen = new Generation();
        Message sys = Message.builder().role(Role.SYSTEM.getValue()).content(SYSTEM_PROMPT).build();
        Message user = Message.builder().role(Role.USER.getValue()).content(userPrompt).build();

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(List.of(sys, user))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .temperature(temperature)
                .build();

        GenerationResult result = gen.call(param);
        if (result == null || result.getOutput() == null
                || result.getOutput().getChoices() == null
                || result.getOutput().getChoices().isEmpty()) {
            throw new BizException(2006, "千问返回为空");
        }
        return result.getOutput().getChoices().get(0).getMessage().getContent();
    }

    private AiCategoryResult parseResult(String jsonStr) {
        try {
            String cleaned = jsonStr.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "");
            }
            int s = cleaned.indexOf('{');
            int e = cleaned.lastIndexOf('}');
            if (s >= 0 && e > s) {cleaned = cleaned.substring(s, e + 1);}

            JsonNode root = objectMapper.readTree(cleaned);

            AiCategoryResult r = new AiCategoryResult();
            JsonNode levelNode = root.get("level");
            r.setLevel(levelNode == null || levelNode.isNull() ? 2 : levelNode.asInt(2));
            r.setCategoryName(root.path("categoryName").asText(null));
            r.setReason(root.path("reason").asText(null));

            // 保证 level 合法
            if (r.getLevel() < 1 || r.getLevel() > 3) {r.setLevel(2);}
            return r;
        } catch (Exception e) {
            log.warn("AI 品类结果解析失败：{}", jsonStr);
            return doubt("AI 返回格式异常");
        }
    }

    private AiCategoryResult doubt(String reason) {
        AiCategoryResult r = new AiCategoryResult();
        r.setLevel(2);
        r.setReason(reason);
        return r;
    }
}
