package com.coupon.system.coupon_admin.exception;

public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // RuntimeException의 message 필드에도 저장
        this.errorCode = errorCode;
    }
}
