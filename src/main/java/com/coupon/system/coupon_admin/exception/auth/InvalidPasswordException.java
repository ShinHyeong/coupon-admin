package com.coupon.system.coupon_admin.exception.auth;

import com.coupon.system.coupon_admin.exception.CustomException;
import com.coupon.system.coupon_admin.exception.ErrorCode;

public class InvalidPasswordException extends CustomException {
    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD);
    }
}
