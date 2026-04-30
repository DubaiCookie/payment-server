package com.paymentsserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.paymentsserver.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentRequestDto {
    private Long orderId;
    private OrderType orderType;
    private String orderName;
    private Long amount;
}
