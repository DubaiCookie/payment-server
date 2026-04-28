package com.paymentsserver.client;

import com.paymentsserver.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class TossPaymentClient {

    private final WebClient webClient;
    private final String secretKey;

    public TossPaymentClient(
            @Value("${toss.payment.api.url}") String apiUrl,
            @Value("${toss.payment.api.secret-key}") String secretKey
    ) {
        this.secretKey = secretKey;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 결제 승인 요청
     */
    public TossPaymentResponse confirmPayment(TossPaymentConfirmRequest request) {
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return webClient.post()
                .uri("/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Toss API error - Status: {}, Body: {}", response.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("Toss API Error: " + errorBody));
                        })
                )
                .bodyToMono(TossPaymentResponse.class)
                .block();
    }

    /**
     * 결제 취소/환불 요청
     */
    public TossPaymentResponse cancelPayment(String paymentKey, TossRefundRequest request) {
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return webClient.post()
                .uri("/payments/{paymentKey}/cancel", paymentKey)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TossPaymentResponse.class)
                .block();
    }

    /**
     * 결제 조회
     */
    public TossPaymentResponse getPayment(String paymentKey) {
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return webClient.get()
                .uri("/payments/{paymentKey}", paymentKey)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedKey)
                .retrieve()
                .bodyToMono(TossPaymentResponse.class)
                .block();
    }
}
