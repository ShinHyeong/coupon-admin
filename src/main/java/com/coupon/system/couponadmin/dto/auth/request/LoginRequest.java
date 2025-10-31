package com.coupon.system.couponadmin.dto.auth.request;

import lombok.Getter;

@Getter
public class LoginRequest {
    private String adminName;
    private String password;
}
