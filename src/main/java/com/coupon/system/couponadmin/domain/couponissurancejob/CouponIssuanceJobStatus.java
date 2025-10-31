package com.coupon.system.couponadmin.domain.couponissurancejob;

public enum CouponIssuanceJobStatus {
    UPLOADED, //파일 업로드 완료 및 Job 생성
    PENDING, //처리 대기 중 및 큐 등록 완료
    COMPLETED,
    FAILED
}
