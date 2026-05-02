package com.paymentsserver.dto;

import com.paymentsserver.entity.OrderType;
import com.paymentsserver.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResponseDto {
    private Long paymentId;
    private Long orderId;
    private String tossOrderId;
    private OrderType orderType;
    private String orderName;
    private Long amount;
    private PaymentStatus paymentStatus;
}
