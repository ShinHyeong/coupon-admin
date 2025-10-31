package com.coupon.system.couponadmin.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Auth (인증 관련)
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "해당 관리자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A002", "비밀번호가 일치하지 않습니다.");

    // (추후) 쿠폰 관련, 파일 관련 에러 코드 추가
    // ...

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
