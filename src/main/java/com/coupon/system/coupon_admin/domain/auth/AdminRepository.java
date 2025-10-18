package com.coupon.system.coupon_admin.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByAdminName(String adminName);
}
