package com.paymentsserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmDto {
    @Schema(description = "Toss에서 반환한 결제 키", example = "tviva20240129160248vnGbK")
    private String paymentKey;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "결제 금액", example = "10000")
    private Long amount;
}
