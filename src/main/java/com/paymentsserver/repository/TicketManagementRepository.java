package com.paymentsserver.repository;

import com.paymentsserver.entity.TicketManagement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TicketManagementRepository extends JpaRepository<TicketManagement, Long> {
    Optional<TicketManagement> findByTicketIdAndAvailableAtBetween(Long ticketId, LocalDateTime start, LocalDateTime end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tm FROM TicketManagement tm WHERE tm.ticketManagementId = :id")
    Optional<TicketManagement> findByIdWithLock(Long id);
}
