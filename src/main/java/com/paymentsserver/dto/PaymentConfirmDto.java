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
    private Long orderId;           // DB에 저장된 실제 orderId (숫자)

    @Schema(description = "Toss 주문 ID", example = "ORDER-123-456")
    private String tossOrderId;     // Toss에 전달한 orderId (ORDER-xxx-xxx 형식)

    @Schema(description = "결제 금액", example = "10000")
    private Long amount;
}
