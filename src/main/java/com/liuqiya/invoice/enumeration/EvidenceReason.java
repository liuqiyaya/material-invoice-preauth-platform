package com.liuqiya.invoice.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 补凭证原因
 */
@Getter
public enum EvidenceReason {
    LOW_VALUE(1, "低值品"),
    ACCUMULATE_OVER_1000(2, "累计超1000");

    @EnumValue
    private final Integer code;
    private final String desc;

    EvidenceReason(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static EvidenceReason of(Integer code) {
        if (code == null) {
            return null;
        }
        for (EvidenceReason s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知 EvidenceReason: " + code);
    }

    public static String descOf(Integer code) {
        EvidenceReason s = of(code);
        return s == null ? null : s.getDesc();
    }


}