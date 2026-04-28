package com.paymentsserver.repository;

import com.paymentsserver.entity.Order;
import com.paymentsserver.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 사용자별 주문 조회
    List<Order> findByUserId(Long userId);

    // 주문 상태로 조회
    List<Order> findByOrderStatus(OrderStatus orderStatus);

    // 사용자별 주문 상태 조회
    List<Order> findByUserIdAndOrderStatus(Long userId, OrderStatus orderStatus);

    // 만료된 주문 조회 (결제 미완료 주문 정리용)
    List<Order> findByOrderStatusAndExpiredAtBefore(OrderStatus orderStatus, LocalDateTime dateTime);
}
