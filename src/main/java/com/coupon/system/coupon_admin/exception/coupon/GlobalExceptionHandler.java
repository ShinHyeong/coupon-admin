package com.coupon.system.coupon_admin.exception.coupon;

import com.coupon.system.coupon_admin.exception.auth.AdminNotFoundException;
import com.coupon.system.coupon_admin.exception.coupon.InvalidFileException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice // 모든 @RestController에서 발생하는 예외를 처리
public class GlobalExceptionHandler {

    // 1. 파일 검증(헤더, empty) 실패 시 (400 Bad Request)
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<?> handleInvalidFileException(InvalidFileException e) {
        // {"message": "파일 헤더가 'customer_id'가 아닙니다."} JSON 생성
        Map<String, String> response = Map.of("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. 관리자 ID 못 찾았을 때 (404 Not Found)
    @ExceptionHandler(AdminNotFoundException.class)
    public ResponseEntity<?> handleAdminNotFound(AdminNotFoundException e) {
        Map<String, String> response = Map.of("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 3. Job ID 못 찾았을 때 (404 Not Found)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFound(EntityNotFoundException e) {
        Map<String, String> response = Map.of("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 4. 파일 저장/읽기(IO) 실패 시 (500 Internal Server Error)
    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIOException(IOException e) {
        Map<String, String> response = Map.of("message", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 5. 그 외 모든 런타임 예외 (500 Internal Server Error)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        Map<String, String> response = Map.of("message", "서버 내부 오류: " + e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}