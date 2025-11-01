package com.coupon.system.couponadmin.controller.auth;

import com.coupon.system.couponadmin.dto.auth.request.LoginRequest;
import com.coupon.system.couponadmin.dto.auth.response.LoginResponse;
import com.coupon.system.couponadmin.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}
