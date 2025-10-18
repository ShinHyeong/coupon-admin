package com.coupon.system.coupon_admin.service.auth;

import com.coupon.system.coupon_admin.domain.auth.Admin;
import com.coupon.system.coupon_admin.domain.auth.AdminRepository;
import com.coupon.system.coupon_admin.dto.auth.request.LoginRequest;
import com.coupon.system.coupon_admin.exception.auth.AdminNotFoundException;
import com.coupon.system.coupon_admin.exception.auth.InvalidPasswordException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AdminRepository adminRepository;

    public AuthService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Transactional(readOnly = true)
    public Admin login(LoginRequest request){
        Admin admin = adminRepository.findByAdminName(request.getAdminName())
                .orElseThrow(AdminNotFoundException::new);

        if (!request.getPassword().equals(admin.getPassword())){
            throw new InvalidPasswordException();
        }

        return admin;
    }

}
