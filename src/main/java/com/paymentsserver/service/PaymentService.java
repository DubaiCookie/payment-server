package com.paymentsserver.service;

import com.paymentsserver.client.TossPaymentClient;
import com.paymentsserver.dto.*;
import com.paymentsserver.entity.*;
import com.paymentsserver.exception.PaymentException;
import com.paymentsserver.kafka.KafkaProducer;
import com.paymentsserver.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final KafkaProducer kafkaProducer;

    @Transactional
    public PaymentCreateResponseDto createPayment(PaymentRequestDto request, Long userId) {
        if (request.getOrderId() == null || request.getOrderType() == null
                || request.getOrderName() == null || request.getOrderName().isBlank()) {
            throw new PaymentException("MISSING_REQUIRED_FIELD", "필수 입력값이 누락되었습니다.");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new PaymentException("INVALID_AMOUNT", "금액은 0보다 커야 합니다.");
        }

        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(existing -> {
            if (existing.getPaymentStatus() == PaymentStatus.CANCELLED) {
                throw new PaymentException("ORDER_ALREADY_CANCELLED", "취소된 주문입니다.");
            }
            if (existing.getPaymentStatus() != PaymentStatus.FAILED) {
                throw new PaymentException("PAYMENT_ALREADY_EXISTS", "이미 결제가 진행 중인 주문입니다.");
            }
        });

        String tossOrderId = "ORDER-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Payment payment = Payment.builder()
                .userId(userId)
                .orderId(request.getOrderId())
                .orderType(request.getOrderType())
                .orderName(request.getOrderName())
                .amount(request.getAmount())
                .tossOrderId(tossOrderId)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: paymentId={}, orderId={}, userId={}, amount={}",
                saved.getPaymentId(), saved.getOrderId(), userId, saved.getAmount());

        return PaymentCreateResponseDto.builder()
                .paymentId(saved.getPaymentId())
                .orderId(saved.getOrderId())
                .orderType(saved.getOrderType())
                .amount(saved.getAmount())
                .paymentStatus(saved.getPaymentStatus())
                .build();
    }

    @Transactional
    public PaymentConfirmResponseDto confirmPayment(PaymentConfirmDto confirmDto) {
        if (confirmDto.getPaymentKey() == null || confirmDto.getOrderId() == null || confirmDto.getAmount() == null) {
            throw new PaymentException("MISSING_REQUIRED_FIELD", "필수 입력값이 누락되었습니다.");
        }

        Payment existingPayment = paymentRepository.findByOrderIdWithLock(confirmDto.getOrderId())
                .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "존재하지 않는 결제입니다."));

        if (existingPayment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentException("PAYMENT_ALREADY_COMPLETED", "이미 완료된 결제입니다.");
        }
        if (existingPayment.getPaymentStatus() == PaymentStatus.FAILED) {
            throw new PaymentException("PAYMENT_ALREADY_FAILED", "이미 실패한 결제입니다.");
        }

        if (!existingPayment.getAmount().equals(confirmDto.getAmount())) {
            throw new PaymentException("AMOUNT_MISMATCH", "결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        try {
            TossPaymentConfirmRequest tossRequest = TossPaymentConfirmRequest.builder()
                    .paymentKey(confirmDto.getPaymentKey())
                    .orderId(existingPayment.getTossOrderId())
                    .amount(confirmDto.getAmount())
                    .build();

            TossPaymentResponse tossResponse = tossPaymentClient.confirmPayment(tossRequest);

            LocalDateTime paidAt = LocalDateTime.now();
            existingPayment.setPaymentKey(tossResponse.getPaymentKey());
            existingPayment.setPaymentMethod(tossResponse.getMethod());
            existingPayment.setPaymentStatus(PaymentStatus.COMPLETED);
            existingPayment.setPaidAt(paidAt);
            paymentRepository.save(existingPayment);

            log.info("Payment confirmed: paymentId={}, orderId={}", existingPayment.getPaymentId(), confirmDto.getOrderId());

            kafkaProducer.sendPaymentCompletedEvent(
                    existingPayment.getUserId(),
                    existingPayment.getPaymentId(),
                    existingPayment.getOrderId(),
                    existingPayment.getAmount(),
                    null
            );

            return PaymentConfirmResponseDto.builder()
                    .paymentId(existingPayment.getPaymentId())
                    .paymentKey(tossResponse.getPaymentKey())
                    .paymentMethod(tossResponse.getMethod())
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .paidAt(paidAt)
                    .build();

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Payment confirmation failed: orderId={}", confirmDto.getOrderId(), e);
            existingPayment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(existingPayment);
            throw new PaymentException("PAYMENT_CONFIRM_FAILED",
                    e.getMessage() == null ? "결제 승인에 실패했습니다." : e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "존재하지 않는 결제입니다."));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "존재하지 않는 결제입니다."));
    }

    @Transactional(readOnly = true)
    public List<PaymentMyResponseDto> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(p -> PaymentMyResponseDto.builder()
                        .paymentId(p.getPaymentId())
                        .orderId(p.getOrderId())
                        .orderType(p.getOrderType())
                        .orderName(p.getOrderName())
                        .amount(p.getAmount())
                        .paymentMethod(p.getPaymentMethod())
                        .paymentStatus(p.getPaymentStatus())
                        .paidAt(p.getPaidAt())
                        .build())
                .collect(Collectors.toList());
    }
}
