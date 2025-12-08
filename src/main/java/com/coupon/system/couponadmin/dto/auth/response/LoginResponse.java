package com.coupon.system.couponadmin.dto.auth.response;

import com.coupon.system.couponadmin.security.GeneratedToken;

public record LoginResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn
) {
    public static LoginResponse from(GeneratedToken token) {
        return new LoginResponse(
                "Bearer",
                token.accessToken(),
                token.refreshToken(),
                token.expiresIn()
        );
    }
}