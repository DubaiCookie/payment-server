package com.paymentsserver.controller;

import com.paymentsserver.dto.PaymentConfirmDto;
import com.paymentsserver.dto.PaymentRequestDto;
import com.paymentsserver.entity.Payment;
import com.paymentsserver.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "결제 API", description = "결제 관련 API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "결제 준비", description = "새로운 결제를 생성하고 orderId를 반환합니다.")
    public ResponseEntity<Payment> createPayment(@RequestBody PaymentRequestDto request) {
        try {
            Payment payment = paymentService.createPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (Exception e) {
            log.error("Payment creation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/confirm")
    @Operation(summary = "결제 승인", description = "Toss Payment API를 통해 결제를 승인합니다.")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmDto confirmDto) {
        try {
            Payment payment = paymentService.confirmPayment(confirmDto);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Payment confirmation failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                java.util.Map.of(
                    "code", "PAYMENT_CONFIRM_FAILED",
                    "message", e.getMessage() == null ? "unknown" : e.getMessage()
                )
            );
        }
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 조회", description = "결제 ID로 결제 정보를 조회합니다.")
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Payment not found: {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "주문번호로 결제 조회", description = "주문번호로 결제 정보를 조회합니다.")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        try {
            Payment payment = paymentService.getPaymentByOrderId(orderId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Payment not found: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자 결제 내역 조회", description = "사용자 ID로 결제 내역을 조회합니다.")
    public ResponseEntity<List<Payment>> getUserPayments(@PathVariable Long userId) {
        try {
            List<Payment> payments = paymentService.getPaymentsByUserId(userId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Failed to get user payments: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
