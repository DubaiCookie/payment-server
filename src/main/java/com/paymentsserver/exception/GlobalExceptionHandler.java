package com.paymentsserver.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, String>> handlePaymentException(PaymentException e) {
        HttpStatus status = resolveStatus(e.getCode());
        log.warn("PaymentException [{}]: {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(status).body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "INTERNAL_ERROR", "message", "서버 오류가 발생했습니다."));
    }

    private HttpStatus resolveStatus(String code) {
        return switch (code) {
            case "MISSING_REQUIRED_FIELD", "INVALID_ORDER_TYPE", "INVALID_AMOUNT",
                 "AMOUNT_MISMATCH", "INVALID_REFUND_AMOUNT" -> HttpStatus.BAD_REQUEST;
            case "MISSING_TOKEN", "EXPIRED_TOKEN" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "ORDER_NOT_FOUND", "PAYMENT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "PAYMENT_ALREADY_EXISTS", "ORDER_ALREADY_CANCELLED",
                 "PAYMENT_ALREADY_COMPLETED", "PAYMENT_ALREADY_FAILED",
                 "PAYMENT_NOT_COMPLETED", "ALREADY_REFUNDED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
