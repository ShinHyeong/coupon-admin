package com.coupon.system.coupon_admin.domain.couponissurancejob;

import com.coupon.system.coupon_admin.domain.auth.Admin;
import jakarta.persistence.*;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.LocalDateTime;

//업로드된 파일 1건에 대한 '작업' 정보를 저장함
@Entity
public class CouponIssuanceJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String originalFileName; //원본 파일명 (예: users.csv)

    @Column(nullable = false, length = 255, unique = true)
    private String savedFilePath; //서버에 임시 저장된 경로

    @Column(nullable = false, length = 20)
    private String jobStatus; //작업 상태 (예: PENDING, COMPLETED, FAILED)

    @Column(nullable = false)
    private Long adminId;  //해당 파일을 올린 관리자id (FK)

    @CurrentTimestamp
    private LocalDateTime createdAt; //올린 시각

    private LocalDateTime completedAt = null; // 작업 완료 일시

    private int totalCount = 0; //파일 내 총 customer_id 수
    private int successCount = 0; //발급 성공 건수
    private int failCount = 0; //발급 실패 건수

    protected CouponIssuanceJob() {}

    public CouponIssuanceJob(String originalFileName, String savedFilePath, Long adminId) {
        this.originalFileName = originalFileName;
        this.savedFilePath = savedFilePath;
        this.adminId = adminId;
        this.jobStatus = "UPLOADED"; // 생성 시점의 기본 상태
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getSavedFilePath() {
        return savedFilePath;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public Long getAdminId() {
        return adminId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void updateJobStatus(String jobStatus){
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
