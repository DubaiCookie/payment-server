package com.paymentsserver.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundEventDto {
    private Long userId;
    private Long paymentId;
    private Long refundId;
    private Long refundAmount;
    private String eventType;
}
