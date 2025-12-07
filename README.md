# 쿠폰시스템 Admin ${\textsf{\color{grey}Coupon System Admin}}$

## 명세서 ${\textsf{\color{grey}Specifications}}$
- 특정 사용자들에게 대량으로 쿠폰을 발급해야한다.
<br>${\textsf{\color{grey}Coupons must be issued in bulk to specific users.}}$
- 사용자 목록은 csv와 excel에 아래와 같은 양식으로 업로드된다.
<br>${\textsf{\color{grey}The user list is uploaded via CSV or Excel in the following format:}}$
    
    ```java
    customer_id
    11111
    22222
    33333
    ```
    
- csv와 excel 파일에 대한 검증은 아래 항목에 대해 진행한다.
<br>${\textsf{\color{grey}Validation for the CSV and Excel files is performed on the following items:}}$
    - 헤더값이 `customer_id`가 맞는지
      <br>${\textsf{\color{grey}Checks if the header value is}}$ `customer_id`${\textsf{\color{grey}.}}$
    - 회원 목록이 비어있지 않은지
      <br>${\textsf{\color{grey}Checks if the user list is not empty.}}$
    - 각각의 회원번호가 유효한지는 검사하지 않는다
      <br>${\textsf{\color{grey}The validity of individual customer IDs is not checked.}}$

- 업로드한 파일은 디버깅을 위해 사용자가 다시 다운로드할 수 있다
  <br>${\textsf{\color{grey}The uploaded file can be re-downloaded by the user for debugging purposes.}}$

- API 1 : 파일 업로드 API
  <br>${\textsf{\color{grey}API 1: File Upload API}}$
- API 2 : 파일 다운로드 API
  <br>${\textsf{\color{grey}API 2: File Download API}}$


# 파일 업로드 API
<img width="80%" alt="API1 Sequence Diagram" src="https://github.com/user-attachments/assets/9c10eae7-e3c7-46b3-b55d-2216e5213014" />
