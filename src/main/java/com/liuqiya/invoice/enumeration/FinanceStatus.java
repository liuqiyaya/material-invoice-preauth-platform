package com.liuqiya.invoice.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 财务提交状态
 */
@Getter
public enum FinanceStatus {
    NOT_SUBMITTED(0, "未提交"),
    SUBMITTED(1, "已提交");

    @EnumValue
    private final Integer code;
    private final String desc;


    FinanceStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FinanceStatus of(Integer code) {
        if (code == null)
        {
            return null;
        }
        for (FinanceStatus s : values()) {
            if (s.code.equals(code))
            {
                return s;
            }
               }
        throw new IllegalArgumentException("未知 FinanceStatus: " + code);
    }

    public static String descOf(Integer code) {
        FinanceStatus s = of(code);
        return s == null ? null : s.getDesc();
    }
}
