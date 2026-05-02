package com.paymentsserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoPaymentRequestDto {
    private Long attractionImageId;  // 구매할 사진 ID
    private String orderName;        // ex) "롤러코스터 탑승 사진"
    private Long amount;             // 결제 금액 (attraction-server에서 미리 확인한 가격)
}
