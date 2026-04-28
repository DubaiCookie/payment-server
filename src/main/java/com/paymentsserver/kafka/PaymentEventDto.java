package com.paymentsserver.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDto {
    private Long userId;
    private Long paymentId;
    private Long orderId;
    private Long amount;
    private Long ticketManagementId;
    private String eventType;
}
