package com.maritime.platform.common.core.exception;

public class BusinessException extends RuntimeException {

    private final int code;
    private Object data;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.code = errorCode.code();
    }

    public BusinessException(ErrorCode errorCode, String detail, Object data) {
        super(detail);
        this.code = errorCode.code();
        this.data = data;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.code = errorCode.code();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }
}
