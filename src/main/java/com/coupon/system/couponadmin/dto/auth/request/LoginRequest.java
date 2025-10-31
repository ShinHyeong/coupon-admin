package com.coupon.system.couponadmin.dto.auth.request;

import lombok.Getter;

public class LoginRequest {
    private String adminName;
    private String password;

    public String getAdminName() {
        return adminName;
    }

    public String getPassword() {
        return password;
    }
}
