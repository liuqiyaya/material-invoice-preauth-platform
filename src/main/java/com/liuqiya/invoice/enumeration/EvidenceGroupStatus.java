package com.liuqiya.invoice.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 凭证组状态
 */
@Getter
public enum EvidenceGroupStatus {
    PENDING(0, "待上传"),
    PENDING_REVIEW(1, "待人工审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝");

    @EnumValue
    private final Integer code;
    private final String desc;

    EvidenceGroupStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static EvidenceGroupStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (EvidenceGroupStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知 EvidenceGroupStatus: " + code);
    }

    public static String descOf(Integer code) {
        EvidenceGroupStatus s = of(code);
        return s == null ? null : s.getDesc();
    }
}

