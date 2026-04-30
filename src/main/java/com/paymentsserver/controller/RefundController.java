package com.paymentsserver.controller;

import com.paymentsserver.dto.RefundRequestDto;
import com.paymentsserver.dto.RefundResponseDto;
import com.paymentsserver.entity.Refund;
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
@RequestMapping("/refunds")
@RequiredArgsConstructor
@Tag(name = "환불 API", description = "환불 관련 API")
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @Operation(summary = "환불 처리", description = "결제를 취소하고 환불을 처리합니다.")
    public ResponseEntity<RefundResponseDto> processRefund(@RequestBody RefundRequestDto request,
                                                           HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("authenticatedUserId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RefundResponseDto response = refundService.processRefund(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{refundId}")
    @Operation(summary = "환불 조회", description = "환불 ID로 환불 정보를 조회합니다.")
    public ResponseEntity<Refund> getRefund(@PathVariable Long refundId) {
        try {
            Refund refund = refundService.getRefundById(refundId);
            return ResponseEntity.ok(refund);
        } catch (Exception e) {
            log.error("Refund not found: {}", refundId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "결제별 환불 내역 조회", description = "결제 ID로 환불 내역을 조회합니다.")
    public ResponseEntity<List<Refund>> getRefundsByPayment(@PathVariable Long paymentId) {
        try {
            List<Refund> refunds = refundService.getRefundsByPaymentId(paymentId);
            return ResponseEntity.ok(refunds);
        } catch (Exception e) {
            log.error("Failed to get refunds for payment: {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
