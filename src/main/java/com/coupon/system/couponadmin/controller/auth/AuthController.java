package com.coupon.system.couponadmin.controller.auth;

import com.coupon.system.couponadmin.domain.auth.Admin;
import com.coupon.system.couponadmin.dto.auth.request.LoginRequest;
import com.coupon.system.couponadmin.service.auth.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpSession session){
        // 1. Admin 객체를 서비스로부터 받음
        Admin admin = authService.login(request);

        // 형식적인 "ROLE_ADMIN" 권한 생성
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        // 2. Spring Security용 Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                admin.getAdminName(), // 1. principal (이름)
                null,                 // 2. credentials (비밀번호는 null) : 이미 서비스계층에서 검증해서 세션에 비번 저장 안함
                authorities           // 3. authorities (권한 목록)
        );

        // 3. SecurityContext에 Authentication 객체 저장
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);

        // 4. (제일 중요) 이 SecurityContext를 HttpSession에 저장
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        // 5. 200 OK 응답 반환
        return ResponseEntity.ok().build();
    }


}
