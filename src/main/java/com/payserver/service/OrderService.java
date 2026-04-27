package com.payserver.service;

import com.payserver.entity.Order;
import com.payserver.entity.OrderStatus;
import com.payserver.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주문(Order) 도메인의 비즈니스 로직을 처리하는 서비스
 * - 주문 단건/목록 조회, 주문 취소 기능을 제공한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * 주문 ID로 단건 조회
     * 존재하지 않으면 RuntimeException 을 던진다
     */
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    /**
     * 사용자 ID로 해당 유저의 전체 주문 목록을 조회한다
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * 주문을 취소 상태(CANCELLED)로 변경한다
     * - 이미 결제 완료(PAID)된 주문은 취소 불가 → 환불 API를 사용해야 한다
     * - 이미 취소된 주문에 재취소 요청하면 예외를 던진다
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getOrderStatus() == OrderStatus.PAID) {
            throw new RuntimeException("결제 완료된 주문은 취소할 수 없습니다. 환불을 이용해주세요.");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("이미 취소된 주문입니다.");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order cancelled: orderId={}, userId={}", orderId, order.getUserId());
        return saved;
    }
}
