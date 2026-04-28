package com.paymentsserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossRefundRequest {
    private String cancelReason;
    private Long cancelAmount;
}
