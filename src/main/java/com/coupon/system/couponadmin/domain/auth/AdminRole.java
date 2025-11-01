package com.coupon.system.couponadmin.domain.auth;

public enum AdminRole {
    ADMIN, // 일반관리자 : 쿠폰 발급만 가능
    SUPER_ADMIN // 슈퍼관리자 : 관리자 계정 생성/삭제
}
