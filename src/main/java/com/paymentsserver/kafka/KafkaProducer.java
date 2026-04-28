package com.paymentsserver.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 결제 완료 이벤트를 auth-server로 전송
     */
    public void sendPaymentCompletedEvent(Long userId, Long paymentId, Long orderId, Long amount, Long ticketManagementId) {
        try {
            PaymentEventDto event = PaymentEventDto.builder()
                    .userId(userId)
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .amount(amount)
                    .ticketManagementId(ticketManagementId)
                    .eventType("PAYMENT_COMPLETED")
                    .build();

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("payment-events", message);
            log.info("Payment completed event sent: {}", message);
        } catch (Exception e) {
            log.error("Failed to send payment event", e);
        }
    }

    /**
     * 환불 완료 이벤트를 auth-server로 전송
     */
    public void sendRefundCompletedEvent(Long userId, Long paymentId, Long refundId, Long refundAmount) {
        try {
            RefundEventDto event = RefundEventDto.builder()
                    .userId(userId)
                    .paymentId(paymentId)
                    .refundId(refundId)
                    .refundAmount(refundAmount)
                    .eventType("REFUND_COMPLETED")
                    .build();

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("refund-events", message);
            log.info("Refund completed event sent: {}", message);
        } catch (Exception e) {
            log.error("Failed to send refund event", e);
        }
    }
}
