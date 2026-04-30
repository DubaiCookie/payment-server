package com.paymentsserver.dto;

import com.paymentsserver.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmResponseDto {
    private Long paymentId;
    private String paymentKey;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
}
