package com.coupon.system.couponadmin.service.auth;

import com.coupon.system.couponadmin.domain.auth.Admin;
import com.coupon.system.couponadmin.domain.auth.AdminRepository;
import com.coupon.system.couponadmin.dto.auth.request.LoginRequest;
import com.coupon.system.couponadmin.dto.auth.response.LoginResponse;
import com.coupon.system.couponadmin.exception.auth.AdminNotFoundException;
import com.coupon.system.couponadmin.exception.auth.InvalidPasswordException;
import com.coupon.system.couponadmin.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AdminRepository adminRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request){
        Admin admin = adminRepository.findByAdminName(request.getAdminName())
                .orElseThrow(AdminNotFoundException::new);

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        //Spring Security는 기본적으로 "ROLE_" 접두사를 사용하여 토큰을 생성하기 때문에 ex. ROLE_ADMIN
        String role = "ROLE_" + admin.getRole().name();

        return jwtTokenProvider.generateTokens(admin.getAdminName(), role);
    }
}
