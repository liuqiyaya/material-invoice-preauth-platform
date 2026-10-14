package com.liuqiya.invoice.exception;

import lombok.Data;
import lombok.Getter;

/**
 * 业务异常
 * Service 层抛此异常，由 GlobalExceptionHandler 统一捕获并包装成 Result
 */
@Getter
public class BizException extends RuntimeException {
    private Integer code;

    public BizException(String msg) {
        super(msg);
    }


    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

}
