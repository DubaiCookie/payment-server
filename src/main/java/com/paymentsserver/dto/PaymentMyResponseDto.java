package com.paymentsserver.dto;

import com.paymentsserver.entity.OrderType;
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
public class PaymentMyResponseDto {
    private Long paymentId;
    private Long orderId;
    private OrderType orderType;
    private String orderName;
    private Long amount;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
}
