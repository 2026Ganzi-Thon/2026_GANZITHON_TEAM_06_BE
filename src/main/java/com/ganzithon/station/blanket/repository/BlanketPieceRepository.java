package com.ganzithon.station.blanket.repository;

import com.ganzithon.station.blanket.entity.BlanketPiece;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlanketPieceRepository extends JpaRepository<BlanketPiece, Long> {

    boolean existsByTicketId(Long ticketId);
}
