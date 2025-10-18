package com.coupon.system.coupon_admin.domain.coupon;

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
    private String customerId; //쿠폰을 발급받은 고객 id)

    @Column(nullable = false, length = 20)
    private String couponStatus = "ACTIVE"; //쿠폰 상태 (ACTIVE, USED, EXPIRED)

    @Column(nullable = false)
    private Long jobId; //이 쿠폰을 생성한 발급 작업 ID

    @Column(nullable = false)
    private LocalDateTime issuedAt; //발급 일시

    @Column(nullable = false)
    private LocalDateTime expiresAt; //만료일시

    protected Coupon() {}

    public Coupon(String couponCode, String customerId, Long jobId, LocalDateTime expiresAt) {
        this.couponCode = couponCode;
        this.customerId = customerId;
        this.jobId = jobId;
        this.expiresAt = expiresAt;
        this.couponStatus = "ACTIVE";
        this.issuedAt = LocalDateTime.now(); // 생성 시점의 발급 시간
    }
}
