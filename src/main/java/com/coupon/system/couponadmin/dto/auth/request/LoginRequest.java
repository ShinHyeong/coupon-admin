package com.coupon.system.couponadmin.dto.auth.request;

import lombok.Getter;
import jakarta.validation.constraints.NotBlank;

@Getter
public class LoginRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String adminName;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
