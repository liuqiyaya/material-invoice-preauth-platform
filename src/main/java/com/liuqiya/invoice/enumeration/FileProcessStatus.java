package com.liuqiya.invoice.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 文件处理状态
 */
@Getter
public enum FileProcessStatus {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    SKIPPED(4, "跳过");

    @EnumValue
    private final Integer code;
    private final String desc;

    FileProcessStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FileProcessStatus of(Integer code) {
        if (code == null)
        {
            return null;
        }
        for (FileProcessStatus s : values()) {
            if (s.code.equals(code))
            {
                return s;
            }
        }
        throw new IllegalArgumentException("未知 FileProcessStatus: " + code);
    }

    public static String descOf(Integer code) {
        FileProcessStatus s = of(code);
        return s == null ? null : s.getDesc();
    }
}
