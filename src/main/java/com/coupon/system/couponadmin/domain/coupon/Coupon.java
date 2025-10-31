package com.coupon.system.couponadmin.domain.coupon;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String couponCode; //실제 쿠폰 코드 (UUID 등)

    @Column(nullable = false, length = 100)
    private String customerId; //쿠폰을 발급받은 고객 id

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus couponStatus = CouponStatus.ACTIVE;

    @Column(nullable = false)
    private Long jobId; //이 쿠폰을 생성한 발급 작업 ID

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected Coupon() {}

    public Coupon(String couponCode, String customerId, Long jobId, LocalDateTime expiresAt) {
        this.couponCode = couponCode;
        this.customerId = customerId;
        this.jobId = jobId;
        this.expiresAt = expiresAt;
    }
}
