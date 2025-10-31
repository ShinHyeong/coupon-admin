package com.coupon.system.couponadmin.domain.auth;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, name="admin_name")
    private String adminName;

    @Column(nullable = false)
    private String password;

    protected Admin() {}
}
