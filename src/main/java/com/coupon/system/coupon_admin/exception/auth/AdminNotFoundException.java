package com.coupon.system.coupon_admin.exception.auth;

import com.coupon.system.coupon_admin.exception.CustomException;
import com.coupon.system.coupon_admin.exception.ErrorCode;

public class AdminNotFoundException extends CustomException {
    public AdminNotFoundException(){
        super(ErrorCode.ADMIN_NOT_FOUND);
    }
}
