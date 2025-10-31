package com.coupon.system.couponadmin.exception.auth;

import com.coupon.system.couponadmin.exception.CustomException;
import com.coupon.system.couponadmin.exception.ErrorCode;

public class AdminNotFoundException extends CustomException {
    public AdminNotFoundException(){
        super(ErrorCode.ADMIN_NOT_FOUND);
    }
}
