package com.coupon.system.couponadmin.domain.couponissurancejob;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/* 업로드된 파일 1건에 대한 '작업' 정보를 저장함 */
@Entity
@Getter
public class CouponIssuanceJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String originalFileName; //원본 파일명 (예: users.csv)

    @Column(nullable = false, length = 255, unique = true)
    private String savedFilePath; //서버에 임시 저장된 경로

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponIssuanceJobStatus jobStatus = CouponIssuanceJobStatus.UPLOADED;

    @Column(nullable = false)
    private Long adminId; //해당 파일을 올린 관리자id (FK)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt = null;

    private int totalCount = 0; //파일 내 총 customer_id 수
    private int successCount = 0; //발급 성공 건수
    private int failCount = 0; //발급 실패 건수

    protected CouponIssuanceJob() {}

    public CouponIssuanceJob(String originalFileName, String savedFilePath, Long adminId) {
        this.originalFileName = originalFileName;
        this.savedFilePath = savedFilePath;
        this.adminId = adminId;
    }

    public void updateJobStatus(CouponIssuanceJobStatus jobStatus){
        this.jobStatus = jobStatus;
    }

    public void updateTotalCount(int totalCount){
        this.totalCount = totalCount;
    }
    public void updateSuccessCount(int successCount){
        this.successCount = successCount;
    }
    public void updateFailCount(int failCount){
        this.failCount = failCount;
    }

    public void updateCompletedAt(LocalDateTime completedAt){
        this.completedAt = completedAt;
    }
}
