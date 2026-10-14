package com.liuqiya.invoice.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 报销状态
 * */
@Getter
public enum ReimburseStatus {
    NOT_FINISHED(0, "未完成"),
    SUCCESS(1, "报销成功"),
    FAILED(2, "报销失败");

    @EnumValue
    private final Integer code;
    private final String desc;

    ReimburseStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReimburseStatus of(Integer code) {
        if (code == null)
        {
            return null;
                }
        for (ReimburseStatus s : values()) {
                if (s.code.equals(code))
                {
                    return s;
                }
                }
        throw new IllegalArgumentException("未知 ReimburseStatus: " + code);
        }

    public static String descOf(Integer code) {
         ReimburseStatus s = of(code);
         return s == null ? null : s.getDesc();
     }

}
