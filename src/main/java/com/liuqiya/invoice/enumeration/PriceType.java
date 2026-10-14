package com.liuqiya.invoice.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 单价类别
 */
@Getter
public enum PriceType {
    MATERIAL(1, "材料"),
    LOW_VALUE(2, "低值品"),
    ASSET(3, "资产");

    @EnumValue
    private final Integer code;
    private final String desc;

    PriceType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PriceType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PriceType s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知 PriceType: " + code);
    }

    public static String descOf(Integer code) {
        PriceType s = of(code);
        return s == null ? null : s.getDesc();
    }

}
