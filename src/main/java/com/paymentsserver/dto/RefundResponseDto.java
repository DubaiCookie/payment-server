package com.paymentsserver.dto;

import com.paymentsserver.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponseDto {
    private Long refundId;
    private Long paymentId;
    private Long refundAmount;
    private RefundStatus refundStatus;
}
