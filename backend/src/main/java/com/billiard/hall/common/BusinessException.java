package com.billiard.hall.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一捕获并返回
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
