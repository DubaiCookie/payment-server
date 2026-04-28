package com.paymentsserver.service;

import com.paymentsserver.client.TossPaymentClient;
import com.paymentsserver.dto.RefundRequestDto;
import com.paymentsserver.dto.TossRefundRequest;
import com.paymentsserver.dto.TossPaymentResponse;
import com.paymentsserver.entity.Order;
import com.paymentsserver.entity.OrderStatus;
import com.paymentsserver.entity.Payment;
import com.paymentsserver.entity.PaymentStatus;
import com.paymentsserver.entity.Refund;
import com.paymentsserver.entity.RefundStatus;
import com.paymentsserver.kafka.KafkaProducer;
import com.paymentsserver.repository.OrderRepository;
import com.paymentsserver.repository.PaymentRepository;
import com.paymentsserver.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 환불(Refund) 도메인의 비즈니스 로직을 처리하는 서비스
 * - Toss Payments 취소 API를 호출하고 환불/결제/주문 상태를 일괄 업데이트한다
 * - 비관적 락(Pessimistic Lock)으로 중복 환불 요청에 대한 멱등성을 보장한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final KafkaProducer kafkaProducer;

    /**
     * 환불 처리 메인 메서드
     * 1) Toss Payments 취소 API 호출
     * 2) Refund 상태 → COMPLETED, Payment 상태 → CANCELLED, Order 상태 → REFUNDED 로 일괄 갱신
     * 3) Kafka refund-events 토픽으로 환불 완료 이벤트 발행
     */
    @Transactional
    public Refund processRefund(RefundRequestDto request) {
        // 멱등성 체크: 비관적 락으로 결제 조회
        Payment payment = paymentRepository.findByIdWithLock(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + request.getPaymentId()));

        // 이미 취소된 결제 → 기존 완료된 환불 반환 (중복 요청 방어)
        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            return refundRepository.findByPaymentIdAndRefundStatus(request.getPaymentId(), RefundStatus.COMPLETED)
                    .orElseThrow(() -> new RuntimeException("Refund record not found for cancelled payment: " + request.getPaymentId()));
        }

        // 환불 대상 주문 조회 (상태 갱신에 사용)
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + payment.getOrderId()));

        try {
            if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
                throw new RuntimeException("Payment is not completed: " + payment.getPaymentId());
            }

            if (payment.getPaymentKey() == null) {
                throw new RuntimeException("Payment key is null: " + payment.getPaymentId());
            }

            // 환불 기록 생성 (orderId 포함)
            Refund refund = Refund.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(order.getOrderId())
                    .refundAmount(request.getRefundAmount())
                    .refundReason(request.getRefundReason())
                    .refundStatus(RefundStatus.PENDING)
                    .build();

            Refund savedRefund = refundRepository.save(refund);

            // Toss Payment API 환불 요청
            TossRefundRequest tossRequest = TossRefundRequest.builder()
                    .cancelReason(request.getRefundReason())
                    .cancelAmount(request.getRefundAmount())
                    .build();

            tossPaymentClient.cancelPayment(payment.getPaymentKey(), tossRequest);

            // 환불 완료: Refund → COMPLETED
            savedRefund.setRefundStatus(RefundStatus.COMPLETED);
            refundRepository.save(savedRefund);

            // 결제 상태 → CANCELLED
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            // 주문 상태 → REFUNDED (환불 완료 반영)
            order.setOrderStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);

            log.info("Refund completed: refundId={}, paymentId={}, orderId={}, amount={}",
                    savedRefund.getRefundId(), payment.getPaymentId(), order.getOrderId(), request.getRefundAmount());

            // Kafka 이벤트 전송
            kafkaProducer.sendRefundCompletedEvent(
                    payment.getUserId(),
                    payment.getPaymentId(),
                    savedRefund.getRefundId(),
                    savedRefund.getRefundAmount()
            );

            return savedRefund;

        } catch (Exception e) {
            log.error("Refund failed: paymentId={}", request.getPaymentId(), e);

            // 환불 실패 기록 저장
            Refund refund = Refund.builder()
                    .paymentId(request.getPaymentId())
                    .orderId(order.getOrderId())
                    .refundAmount(request.getRefundAmount())
                    .refundReason(request.getRefundReason())
                    .refundStatus(RefundStatus.FAILED)
                    .build();
            refundRepository.save(refund);

            throw new RuntimeException("Refund failed", e);
        }
    }

    /**
     * 환불 내역 조회 (결제 ID로)
     */
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByPaymentId(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }

    /**
     * 환불 조회 (환불 ID로)
     */
    @Transactional(readOnly = true)
    public Refund getRefundById(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund not found: " + refundId));
    }
}
