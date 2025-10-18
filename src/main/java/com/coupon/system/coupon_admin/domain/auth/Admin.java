package com.coupon.system.coupon_admin.domain.auth;

import jakarta.persistence.*;

@Entity
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, name="admin_name")
    private String adminName;

    @Column(nullable = false)
    private String password;

    protected Admin() {}

    public Long getId() {
        return id;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getPassword() {
        return password;
    }
}
