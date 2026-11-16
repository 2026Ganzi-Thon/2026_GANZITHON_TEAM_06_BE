package com.ganzithon.station.ticket.repository;

import com.ganzithon.station.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** 이 게스트 토큰으로 발급한 승차권이 있는지 (발급 때 토큰 재사용 판단, idx_tickets_guest_token 사용) */
    boolean existsByGuestToken(String guestToken);
}
