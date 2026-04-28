package com.paymentsserver.repository;

import com.paymentsserver.entity.Ticket;
import com.paymentsserver.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketType(TicketType ticketType);
}
