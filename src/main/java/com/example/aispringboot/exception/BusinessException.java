package com.example.aispringboot.exception;

import lombok.Getter;

/**
 * 业务异常：携带业务码与可选附加数据，由全局异常处理器统一转换响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final transient Object data;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
        this.data = null;
    }
}
