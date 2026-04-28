package com.paymentsserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossPaymentResponse {
    private String paymentKey;
    private String orderId;
    private String orderName;
    private Long amount;
    private String status;
    private String method;
    private String approvedAt;
}
