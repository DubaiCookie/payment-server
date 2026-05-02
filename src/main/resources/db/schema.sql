-- ============================================
-- Pay Server Database Schema
-- ============================================

-- 기존 테이블 삭제 (개발 환경에서만 사용, 운영에서는 주석 처리)
-- DROP TABLE IF EXISTS refunds;
-- DROP TABLE IF EXISTS payments;

-- ============================================
-- 0. orders 테이블 (주문 정보)
-- ============================================
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 ID (PK)',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    order_name VARCHAR(255) NOT NULL COMMENT '주문명',
    total_amount BIGINT NOT NULL COMMENT '주문 총액',
    order_status VARCHAR(20) NOT NULL COMMENT '주문 상태 (PENDING, PAID, CANCELLED)',
    order_type VARCHAR(20) NOT NULL DEFAULT 'TICKET' COMMENT '주문 유형 (TICKET, FOOD, PHOTO)',
    ticket_quantity INT NULL COMMENT '티켓 수량 (TICKET 타입일 때만 non-null)',
    ticket_management_id BIGINT NULL COMMENT '티켓 관리 ID (TICKET 타입일 때만 non-null)',
    attraction_image_id BIGINT NULL COMMENT '탑승 사진 ID (PHOTO 타입일 때만 non-null)',
    expired_at DATETIME COMMENT '결제 만료 일시',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME COMMENT '수정 일시',

    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_order_status (order_status),
    INDEX idx_orders_order_type (order_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 정보';

-- 기존 orders 테이블에 컬럼이 없는 경우 추가 (운영 환경 마이그레이션용)
ALTER TABLE orders
    MODIFY COLUMN ticket_quantity INT NULL,
    MODIFY COLUMN ticket_management_id BIGINT NULL;

-- order_type 컬럼이 없으면 추가 (IF NOT EXISTS는 MariaDB 10.x 이상 지원)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_type VARCHAR(20) NOT NULL DEFAULT 'TICKET' COMMENT '주문 유형 (TICKET, FOOD, PHOTO)',
    ADD COLUMN IF NOT EXISTS attraction_image_id BIGINT NULL COMMENT '탑승 사진 ID (PHOTO 타입일 때만 non-null)';

-- ============================================
-- 1. payments 테이블 (결제 정보)
-- ============================================
-- 주의: order_id 는 BIGINT (orders.order_id FK 참조). 과거 VARCHAR(100) UUID 컨벤션과
-- Payment 엔티티(Long)의 타입 불일치로 INSERT 자체가 실패해 사진/티켓 결제가 모두 막혔던
-- 회귀가 있었음. 동시에 order_type, toss_order_id, paid_at 컬럼이 누락되어 있어
-- INSERT 시 NOT NULL 제약 위반이 발생했음. 모두 정합성 맞춤.
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 ID (PK)',
    user_id BIGINT NOT NULL COMMENT '사용자 ID (FK)',
    order_id BIGINT NOT NULL UNIQUE COMMENT '주문 ID (orders.order_id FK)',
    order_type VARCHAR(20) NOT NULL DEFAULT 'TICKET' COMMENT '주문 유형 (TICKET, FOOD, PHOTO)',
    order_name VARCHAR(255) NOT NULL COMMENT '주문명',
    toss_order_id VARCHAR(100) UNIQUE COMMENT 'Toss Payments 주문 ID',
    amount BIGINT NOT NULL COMMENT '결제 금액',
    payment_key VARCHAR(200) UNIQUE COMMENT 'Toss 결제 키',
    payment_method VARCHAR(50) COMMENT '결제 수단 (카드, 계좌이체 등)',
    payment_status VARCHAR(20) NOT NULL COMMENT '결제 상태 (PENDING, COMPLETED, FAILED, CANCELLED)',
    paid_at DATETIME COMMENT '결제 완료 일시',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME COMMENT '수정 일시',

    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id),
    INDEX idx_payment_key (payment_key),
    INDEX idx_payment_status (payment_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='결제 정보';

-- 기존 환경(VARCHAR(100) order_id, 누락 컬럼)에서 운영 중인 DB 마이그레이션
-- (MariaDB 10.0+의 ADD COLUMN IF NOT EXISTS / MODIFY COLUMN 활용)
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS order_type    VARCHAR(20) NOT NULL DEFAULT 'TICKET' COMMENT '주문 유형',
    ADD COLUMN IF NOT EXISTS toss_order_id VARCHAR(100) UNIQUE COMMENT 'Toss 주문 ID',
    ADD COLUMN IF NOT EXISTS paid_at       DATETIME COMMENT '결제 완료 일시';

-- order_id 컬럼이 VARCHAR(100)로 남아 있는 환경 강제 정합화 (파괴적 변환이 아닌 타입 통일)
-- 빈 값/UUID 문자열이 들어 있으면 변환에 실패하므로, 마이그레이션 전에 데이터 정리가 필요.
-- 신규 환경에서는 이미 BIGINT 로 생성되므로 no-op.
ALTER TABLE payments
    MODIFY COLUMN order_id BIGINT NOT NULL;

-- ============================================
-- 2. refunds 테이블 (환불 정보)
-- ============================================
CREATE TABLE IF NOT EXISTS refunds (
    refund_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '환불 ID (PK)',
    payment_id BIGINT NOT NULL COMMENT '결제 ID (FK)',
    refund_amount BIGINT NOT NULL COMMENT '환불 금액',
    refund_reason VARCHAR(500) COMMENT '환불 사유',
    refund_status VARCHAR(20) NOT NULL COMMENT '환불 상태 (PENDING, COMPLETED, FAILED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME COMMENT '수정 일시',

    INDEX idx_payment_id (payment_id),
    INDEX idx_refund_status (refund_status),
    INDEX idx_created_at (created_at),

    CONSTRAINT fk_refunds_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='환불 정보';

-- ============================================
-- 초기 데이터 (테스트용, 선택사항)
-- ============================================

-- 테스트 결제 데이터
-- INSERT INTO payments (user_id, order_id, order_name, amount, payment_status, created_at) VALUES
-- (1, 'order-uuid-001', '테마파크 입장권', 50000, 'COMPLETED', NOW()),
-- (1, 'order-uuid-002', '놀이기구 이용권', 30000, 'COMPLETED', NOW()),
-- (2, 'order-uuid-003', '프리미엄 티켓', 100000, 'PENDING', NOW());
