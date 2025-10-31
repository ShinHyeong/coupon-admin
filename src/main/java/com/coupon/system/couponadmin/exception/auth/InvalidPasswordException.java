package com.coupon.system.couponadmin.exception.auth;

import com.coupon.system.couponadmin.exception.CustomException;
import com.coupon.system.couponadmin.exception.ErrorCode;

public class InvalidPasswordException extends CustomException {
    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD);
    }
}
