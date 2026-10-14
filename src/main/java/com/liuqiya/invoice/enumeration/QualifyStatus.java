package com.liuqiya.invoice.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 资质审核状态
 */
@Getter
public enum QualifyStatus {
    PENDING(0, "待审核"),
    PENDING_EVIDENCE(1, "待补凭证"),
    PENDING_MANUAL(2, "待人工处理"),
    APPROVED(3, "审核通过"),
    REJECTED(4, "审核不通过"),
    CANCELLED(5, "已取消");

    @EnumValue
    private final Integer code;
    private final String desc;


    QualifyStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static QualifyStatus of(Integer code) {
        if (code == null)
        {
            return null;
        }
        for (QualifyStatus s : values()) {
            if (s.code.equals(code))
            {
                return s;
            }
        }
        throw new IllegalArgumentException("未知 QualifyStatus: " + code);
    }

    public static String descOf(Integer code) {
        QualifyStatus s = of(code);
        return s == null ? null : s.getDesc();
    }
}
