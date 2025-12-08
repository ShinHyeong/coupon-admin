package com.coupon.system.couponadmin.security;

public record GeneratedToken(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}