package com.paymentsserver.service;

import com.paymentsserver.client.TossPaymentClient;
import com.paymentsserver.dto.*;
import com.paymentsserver.entity.*;
import com.paymentsserver.kafka.KafkaProducer;
import com.paymentsserver.repository.OrderRepository;
import com.paymentsserver.repository.PaymentRepository;
import com.paymentsserver.repository.TicketManagementRepository;
import com.paymentsserver.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketManagementRepository ticketManagementRepository;
    private final TossPaymentClient tossPaymentClient;
    private final KafkaProducer kafkaProducer;

    /**
     * 결제 준비 (Order 생성 후 Payment 생성)
     */
    @Transactional
    public Payment createPayment(PaymentRequestDto request, Long userId) {
        int quantity = request.getTicketQuantity() != null ? request.getTicketQuantity() : 1;

        // 1. ticketType으로 Ticket 조회 → price 확인
        Ticket ticket = ticketRepository.findByTicketType(request.getTicketType())
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + request.getTicketType()));

        // 2. ticketId + availableDate로 TicketManagement 조회
        LocalDateTime startOfDay = request.getAvailableDate().atStartOfDay();
        LocalDateTime endOfDay = request.getAvailableDate().atTime(23, 59, 59);
        TicketManagement ticketManagement = ticketManagementRepository
                .findByTicketIdAndAvailableAtBetween(ticket.getTicketId(), startOfDay, endOfDay)
                .orElseThrow(() -> new RuntimeException("TicketManagement not found for date: " + request.getAvailableDate()));

        // 3. amount 서버에서 계산
        long amount = (long) ticket.getPrice() * quantity;
        String orderName = request.getTicketType().name() + " 티켓 " + quantity + "매";

        // 4. Order 생성 (userId는 JWT에서 추출한 값 사용)
        Order order = Order.builder()
                .userId(userId)
                .orderName(orderName)
                .totalAmount(amount)
                .ticketQuantity(quantity)
                .ticketManagementId(ticketManagement.getTicketManagementId())
                .orderStatus(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created: orderId={}, userId={}, amount={}, ticketManagementId={}",
                savedOrder.getOrderId(), userId, amount, ticketManagement.getTicketManagementId());

        // 5. Payment 생성
        Payment payment = Payment.builder()
                .userId(userId)
                .orderId(savedOrder.getOrderId())
                .orderName(orderName)
                .amount(amount)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created: paymentId={}, orderId={}, userId={}, amount={}",
                savedPayment.getPaymentId(), savedOrder.getOrderId(), userId, amount);

        return savedPayment;
    }

    /**
     * 결제 승인 (Toss Payment API 호출)
     */
    @Transactional
    public Payment confirmPayment(PaymentConfirmDto confirmDto) {
        // 멱등성 체크: 비관적 락으로 조회 후 이미 처리된 결제는 즉시 반환
        Payment existingPayment = paymentRepository.findByOrderIdWithLock(confirmDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + confirmDto.getOrderId()));

        if (existingPayment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment already completed (idempotent): orderId={}", confirmDto.getOrderId());
            return existingPayment;
        }
        if (existingPayment.getPaymentStatus() == PaymentStatus.FAILED) {
            throw new RuntimeException("Payment already failed: " + confirmDto.getOrderId());
        }

        try {
            log.info("Confirming payment - orderId: {}, tossOrderId: {}, paymentKey: {}, amount: {}",
                    confirmDto.getOrderId(), confirmDto.getTossOrderId(),
                    confirmDto.getPaymentKey(), confirmDto.getAmount());

            // Toss Payment API 호출 (Toss는 원래 보낸 orderId 형식을 받음)
            TossPaymentConfirmRequest tossRequest = TossPaymentConfirmRequest.builder()
                    .paymentKey(confirmDto.getPaymentKey())
                    .orderId(confirmDto.getTossOrderId())  // Toss에 보낸 원본 orderId 사용
                    .amount(confirmDto.getAmount())
                    .build();

            log.info("Sending to Toss API - request: paymentKey={}, orderId={}, amount={}",
                    tossRequest.getPaymentKey(), tossRequest.getOrderId(), tossRequest.getAmount());

            TossPaymentResponse tossResponse = tossPaymentClient.confirmPayment(tossRequest);

            // DB 업데이트 (이미 락으로 조회한 existingPayment 재사용)
            existingPayment.setPaymentKey(tossResponse.getPaymentKey());
            existingPayment.setPaymentMethod(tossResponse.getMethod());
            existingPayment.setPaymentStatus(PaymentStatus.COMPLETED);

            Payment updatedPayment = paymentRepository.save(existingPayment);

            // Order 상태 업데이트
            Order order = orderRepository.findById(existingPayment.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + existingPayment.getOrderId()));
            order.setOrderStatus(OrderStatus.PAID);
            orderRepository.save(order);

            // 재고 차감
            TicketManagement ticketManagement = ticketManagementRepository.findByIdWithLock(order.getTicketManagementId())
                    .orElseThrow(() -> new RuntimeException("TicketManagement not found: " + order.getTicketManagementId()));
            ticketManagement.reduceStock(order.getTicketQuantity());
            ticketManagementRepository.save(ticketManagement);

            log.info("Stock reduced: ticketManagementId={}, quantity={}, remainingStock={}",
                    order.getTicketManagementId(), order.getTicketQuantity(), ticketManagement.getStock());

            log.info("Payment confirmed: paymentKey={}, orderId={}",
                    tossResponse.getPaymentKey(), confirmDto.getOrderId());

            // Kafka 이벤트 전송
            kafkaProducer.sendPaymentCompletedEvent(
                    existingPayment.getUserId(),
                    existingPayment.getPaymentId(),
                    existingPayment.getOrderId(),
                    existingPayment.getAmount(),
                    order.getTicketManagementId()
            );

            return updatedPayment;

        } catch (Exception e) {
            log.error("Payment confirmation failed: orderId={}", confirmDto.getOrderId(), e);

            // 결제 실패 처리 (이미 락으로 조회한 existingPayment 재사용)
            existingPayment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(existingPayment);

            // Order 상태도 업데이트
            Order order = orderRepository.findById(existingPayment.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            throw new RuntimeException(e.getMessage() == null ? "Payment confirmation failed" : e.getMessage(), e);
        }
    }

    /**
     * 결제 조회 (orderId로)
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + orderId));
    }

    /**
     * 결제 조회 (paymentId로)
     */
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    /**
     * 사용자별 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }
}
