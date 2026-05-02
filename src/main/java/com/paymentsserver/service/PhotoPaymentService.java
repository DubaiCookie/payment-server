package com.paymentsserver.service;

import com.paymentsserver.dto.PaymentCreateResponseDto;
import com.paymentsserver.dto.PaymentRequestDto;
import com.paymentsserver.dto.PhotoPaymentRequestDto;
import com.paymentsserver.entity.Order;
import com.paymentsserver.entity.OrderStatus;
import com.paymentsserver.entity.OrderType;
import com.paymentsserver.exception.PaymentException;
import com.paymentsserver.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoPaymentService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Transactional
    public PaymentCreateResponseDto createPhotoPayment(PhotoPaymentRequestDto request, Long userId) {
        if (request.getAttractionImageId() == null) {
            throw new PaymentException("MISSING_REQUIRED_FIELD", "attractionImageId는 필수값입니다.");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new PaymentException("INVALID_AMOUNT", "금액은 0보다 커야 합니다.");
        }

        // PHOTO 타입 Order 생성 (ticketQuantity, ticketManagementId는 null)
        Order order = Order.builder()
                .userId(userId)
                .orderName(request.getOrderName())
                .totalAmount(request.getAmount())
                .orderStatus(OrderStatus.PENDING)
                .orderType(OrderType.PHOTO)
                .attractionImageId(request.getAttractionImageId())
                .build();
        order = orderRepository.save(order);

        log.info("Photo order created: orderId={}, userId={}, attractionImageId={}, amount={}",
                order.getOrderId(), userId, request.getAttractionImageId(), request.getAmount());

        // 기존 PaymentService.createPayment() 재사용
        PaymentRequestDto paymentRequest = PaymentRequestDto.builder()
                .orderId(order.getOrderId())
                .orderType(OrderType.PHOTO)
                .orderName(request.getOrderName())
                .amount(request.getAmount())
                .build();

        return paymentService.createPayment(paymentRequest, userId);
    }
}
