package com.paymentsserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossPaymentRequest {
    private String orderId;
    private String orderName;
    private Long amount;
    private String successUrl;
    private String failUrl;
}
