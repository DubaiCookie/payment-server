package com.paymentsserver.service;

import com.paymentsserver.client.TossPaymentClient;
import com.paymentsserver.dto.RefundRequestDto;
import com.paymentsserver.dto.RefundResponseDto;
import com.paymentsserver.dto.TossRefundRequest;
import com.paymentsserver.entity.*;
import com.paymentsserver.exception.PaymentException;
import com.paymentsserver.kafka.KafkaProducer;
import com.paymentsserver.repository.PaymentRepository;
import com.paymentsserver.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final KafkaProducer kafkaProducer;

    @Transactional
    public RefundResponseDto processRefund(RefundRequestDto request, Long requesterId) {
        if (request.getRefundAmount() == null) {
            throw new PaymentException("MISSING_REQUIRED_FIELD", "필수 입력값이 누락되었습니다.");
        }

        Payment payment = paymentRepository.findByIdWithLock(request.getPaymentId())
                .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "존재하지 않는 결제입니다."));

        if (!payment.getUserId().equals(requesterId)) {
            throw new PaymentException("FORBIDDEN", "본인의 결제만 환불할 수 있습니다.");
        }

        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new PaymentException("ALREADY_REFUNDED", "이미 환불된 결제입니다.");
        }

        if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException("PAYMENT_NOT_COMPLETED", "완료된 결제만 환불할 수 있습니다.");
        }

        if (payment.getPaymentKey() == null) {
            throw new PaymentException("PAYMENT_NOT_COMPLETED", "완료된 결제만 환불할 수 있습니다.");
        }

        if (request.getRefundAmount() <= 0 || request.getRefundAmount() > payment.getAmount()) {
            throw new PaymentException("INVALID_REFUND_AMOUNT", "환불 금액이 결제 금액을 초과할 수 없습니다.");
        }

        Refund refund = Refund.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .refundAmount(request.getRefundAmount())
                .refundReason(request.getReason())
                .refundStatus(RefundStatus.PENDING)
                .build();

        Refund savedRefund = refundRepository.save(refund);

        try {
            TossRefundRequest tossRequest = TossRefundRequest.builder()
                    .cancelReason(request.getReason())
                    .cancelAmount(request.getRefundAmount())
                    .build();

            tossPaymentClient.cancelPayment(payment.getPaymentKey(), tossRequest);

            savedRefund.setRefundStatus(RefundStatus.COMPLETED);
            refundRepository.save(savedRefund);

            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            log.info("Refund completed: refundId={}, paymentId={}, amount={}",
                    savedRefund.getRefundId(), payment.getPaymentId(), request.getRefundAmount());

            kafkaProducer.sendRefundCompletedEvent(
                    payment.getUserId(),
                    payment.getPaymentId(),
                    savedRefund.getRefundId(),
                    savedRefund.getRefundAmount()
            );

            return RefundResponseDto.builder()
                    .refundId(savedRefund.getRefundId())
                    .paymentId(savedRefund.getPaymentId())
                    .refundAmount(savedRefund.getRefundAmount())
                    .refundStatus(savedRefund.getRefundStatus())
                    .build();

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Refund failed: paymentId={}", request.getPaymentId(), e);
            savedRefund.setRefundStatus(RefundStatus.FAILED);
            refundRepository.save(savedRefund);
            throw new PaymentException("REFUND_FAILED", "환불 처리에 실패했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<Refund> getRefundsByPaymentId(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }

    @Transactional(readOnly = true)
    public Refund getRefundById(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new PaymentException("REFUND_NOT_FOUND", "존재하지 않는 환불입니다."));
    }
}
