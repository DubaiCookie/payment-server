package com.paymentsserver.entity;

public enum OrderStatus {
    PENDING,      // 주문 대기
    PAID,         // 결제 완료
    CANCELLED,    // 주문 취소
    EXPIRED,      // 결제 시간 만료
    REFUNDED      // 환불 완료
}
