package com.coupon.system.couponadmin.dto.auth.response;

public record LoginResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn
) {
    /**
     * 정적 팩토리 메소드 (생성자를 대신함)
     * @param accessToken 액세스 토큰 (JWT)
     * @param refreshToken 리프레시 토큰 (JWT)
     * @param accessTokenExpiresIn 만료 시간(초) (e.g. 3600초 = 1시간)
     * @return LoginResponseDto
     */
    public static LoginResponse of(String accessToken, String refreshToken, Long accessTokenExpiresIn) {
        return new LoginResponse(
                "Bearer",
                accessToken,
                refreshToken,
                accessTokenExpiresIn
        );
    }
}