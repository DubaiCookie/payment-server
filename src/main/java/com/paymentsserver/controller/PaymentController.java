package com.paymentsserver.controller;

import com.paymentsserver.dto.PaymentConfirmDto;
import com.paymentsserver.dto.PaymentRequestDto;
import com.paymentsserver.dto.RefundRequestDto;
import com.paymentsserver.entity.Payment;
import com.paymentsserver.entity.Refund;
import com.paymentsserver.service.PaymentService;
import com.paymentsserver.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RefundService refundService;

    @PostMapping
    @Operation(summary = "결제 준비", description = "새로운 결제를 생성하고 orderId를 반환합니다.")
    public ResponseEntity<Payment> createPayment(@RequestBody PaymentRequestDto request,
                                                 HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("authenticatedUserId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Payment payment = paymentService.createPayment(request, userId);
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

    @GetMapping("/my")
    @Operation(summary = "내 결제 내역 조회", description = "인증된 사용자의 결제 내역을 조회합니다.")
    public ResponseEntity<List<Payment>> getMyPayments(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("authenticatedUserId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            List<Payment> payments = paymentService.getPaymentsByUserId(userId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Failed to get payments for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "결제 환불", description = "결제 ID로 환불을 처리합니다.")
    public ResponseEntity<Refund> refundPayment(@PathVariable Long paymentId,
                                                @RequestBody RefundRequestDto request,
                                                HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("authenticatedUserId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        request.setPaymentId(paymentId);
        try {
            Refund refund = refundService.processRefund(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(refund);
        } catch (Exception e) {
            log.error("Refund failed for paymentId: {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
