package com.payserver.controller;

import com.payserver.entity.Order;
import com.payserver.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주문(Order) 관련 REST API를 노출하는 컨트롤러
 * - 주문 단건 조회, 사용자별 주문 목록 조회, 주문 취소 엔드포인트를 제공한다
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "주문 API", description = "주문 관련 API")
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 ID로 단건 조회
     * GET /orders/{orderId}
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "주문 조회", description = "주문 ID로 주문 정보를 조회합니다.")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Order not found: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 사용자 ID로 해당 유저의 전체 주문 목록 조회
     * GET /orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자 주문 내역 조회", description = "사용자 ID로 전체 주문 내역을 조회합니다.")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Failed to get orders for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 주문 취소 처리
     * PATCH /orders/{orderId}/cancel
     * - PAID 상태 주문은 취소 불가 (환불 API 사용 필요)
     */
    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "주문 취소", description = "주문을 취소합니다. 결제 완료(PAID) 상태의 주문은 취소할 수 없습니다.")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Order cancellation failed: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
