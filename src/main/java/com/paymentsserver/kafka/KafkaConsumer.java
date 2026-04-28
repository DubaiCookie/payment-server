package com.paymentsserver.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    /**
     * auth-server로부터 결제 요청 메시지 수신
     * (필요시 구현)
     */
    @KafkaListener(topics = "payment-requests", groupId = "pay-server-group")
    public void consumePaymentRequest(String message) {
        log.info("Received payment request: {}", message);
        // 결제 요청 처리 로직
    }

    /**
     * auth-server로부터 환불 요청 메시지 수신
     * (필요시 구현)
     */
    @KafkaListener(topics = "refund-requests", groupId = "pay-server-group")
    public void consumeRefundRequest(String message) {
        log.info("Received refund request: {}", message);
        // 환불 요청 처리 로직
    }
}
