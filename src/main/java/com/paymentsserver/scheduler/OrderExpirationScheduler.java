package com.paymentsserver.scheduler;

import com.paymentsserver.entity.Order;
import com.paymentsserver.entity.OrderStatus;
import com.paymentsserver.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 시간이 만료된 주문을 자동으로 EXPIRED 상태로 전환하는 스케줄러
 * - Order 생성 후 15분 내에 결제 승인이 완료되지 않은 PENDING 주문을 대상으로 한다
 * - 1분마다 실행되어 만료된 주문을 탐지하고 상태를 갱신한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;

    /**
     * 만료된 PENDING 주문을 EXPIRED 상태로 일괄 전환한다
     * - fixedDelay: 이전 실행 완료 후 1분(60,000ms) 간격으로 실행
     * - 대상: orderStatus = PENDING 이고 expiredAt < 현재 시각인 주문
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOrders() {
        List<Order> expiredOrders = orderRepository
                .findByOrderStatusAndExpiredAtBefore(OrderStatus.PENDING, LocalDateTime.now());

        if (expiredOrders.isEmpty()) {
            return;
        }

        // 만료 대상 주문 상태를 EXPIRED로 일괄 변경
        expiredOrders.forEach(order -> order.setOrderStatus(OrderStatus.EXPIRED));
        orderRepository.saveAll(expiredOrders);

        log.info("Expired orders processed: count={}", expiredOrders.size());
    }
}
