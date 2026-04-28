package com.paymentsserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDto {
    private Long paymentId;
    private Long refundAmount;
    private String refundReason;
}
