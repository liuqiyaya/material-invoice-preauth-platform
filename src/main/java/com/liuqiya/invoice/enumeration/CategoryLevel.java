package com.liuqiya.invoice.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 整体品类结论
 */
@Getter
public enum CategoryLevel {
    PASS(1, "可以"),
    DOUBT(2, "存疑"),
    REJECT(3, "不可以");

    @EnumValue
    private final Integer code;
    private final String desc;

    CategoryLevel(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CategoryLevel of(Integer code) {
        if (code == null)
        {
            return null;
        }
        for (CategoryLevel s : values()) {
            if (s.code.equals(code))
            {
                return s;
            }
        }
        throw new IllegalArgumentException("未知 CategoryLevel: " + code);
    }

    public static String descOf(Integer code) {
        CategoryLevel s = of(code);
        return s == null ? null : s.getDesc();
    }

}
