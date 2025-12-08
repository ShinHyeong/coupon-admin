# 쿠폰시스템 Admin
## 명세서
- 특정 사용자들에게 대량으로 쿠폰을 발급해야한다.
- 사용자 목록은 csv와 excel에 아래와 같은 양식으로 업로드된다.
    
    ```java
    customer_id
    11111
    22222
    33333
    ```
    
- csv와 excel 파일에 대한 검증은 아래 항목에 대해 진행한다.
    - 헤더값이 `customer_id`가 맞는지
    - 회원 목록이 비어있지 않은지
    - 각각의 회원번호가 유효한지는 검사하지 않는다

- 업로드한 파일은 디버깅을 위해 사용자가 다시 다운로드할 수 있다

- API 1 : 파일 업로드 API
- API 2 : 파일 다운로드 API


# 파일 업로드 API
<div align="center">
    <br>
    <img width="80%" alt="API1 Sequence Diagram" src="https://github.com/user-attachments/assets/9c10eae7-e3c7-46b3-b55d-2216e5213014" />
    <br><br>
</div>

# 프로젝트 구조

```
📂 src/main/java/com/coupon/system/couponadmin
 ├─ 📜 CouponAdminApplication.java       # 1. 애플리케이션 메인 실행 클래스
 ├─ 📂 controller                        # 2. 요청 처리
 │  ├─ 📂 auth                               # 관리자 로그인 및 인증 관련 API
 │  └─ 📂 coupon                             # 쿠폰 발급 요청 및 조회 API
 │
 ├─ 📂 domain                            # 3. JPA 엔티티 및 리포지토리
 │  ├─ 📂 auth
 │  │  ├─ 📜 Admin.java                      # 관리자 엔티티
 │  │  ├─ 📜 AdminRepository.java            # 관리자 DB 접근 리포지토리
 │  │  └─ 📜 AdminRole.java                  # 관리자 권한 Enum (ROLE_ADMIN 등)
 │  ├─ 📂 coupon
 │  │  ├─ 📜 Coupon.java                     # 쿠폰 엔티티
 │  │  ├─ 📜 CouponRepository.java           # 쿠폰 DB 접근 리포지토리
 │  │  └─ 📜 CouponStatus.java               # 쿠폰 상태 Enum (USED, UNUSED 등)
 │  └─ 📂 couponissurancejob
 │     ├─ 📜 CouponIssuanceJob.java          # 비동기 발급 작업 이력 엔티티
 │     ├─ 📜 CouponIssuanceJobRepository.java
 │     └─ 📜 CouponIssuanceJobStatus.java     # 작업 상태 Enum (PENDING, COMPLETED 등)
 │
 ├─ 📂 dto                               # 4. 데이터 전송 객체 (Request/Response)
 │  ├─ 📂 auth                               # 로그인 요청/응답 DTO
 │  ├─ 📂 couponissurancejob                 # 발급 작업 요청/응답 DTO
 │  └─ 📂 file                               # 파일 응답 DTO
 │
 ├─ 📂 service                           # 5. 비즈니스 로직
 │  ├─ 📂 auth                               # 로그인 로직 처리
 │  ├─ 📂 coupon                             # 쿠폰 발급 로직 처리
 │  │  ├─ 📜 CouponBatchService.java             # 대량 쿠폰 발급 배치 처리 서비스
 │  │  └─ 📜 CouponIssuanceService.java          # 쿠폰 발급 관련 서비스
 │  └─ 📂 file                               # 파일 스토리지 전략 (Strategy Pattern)
 │     ├─ 📜 AbstractFileService.java            # 파일 저장소 공통 로직 추상 클래스
 │     ├─ 📜 FileService.java                    # 파일 저장소 인터페이스
 │     ├─ 📜 LocalService.java                   # 로컬 디스크 저장 구현체
 │     └─ 📜 S3Service.java                      # AWS S3 저장 구현체
 │
 └─ 📂 util                              # 6. 공통 유틸리티
 │  └─ 📜 GenericBatchProcessor.java         # 제네릭 기반의 범용 배치 처리 프로세서
 │
 ├─ 📂 config                            # 7. 설정 (AWS S3, Spring Security)
 ├─ 📂 exception                         # 8. 예외 처리
 └─ 📂 security                          # 9. 보안 관련 로직 (JWT)
```
