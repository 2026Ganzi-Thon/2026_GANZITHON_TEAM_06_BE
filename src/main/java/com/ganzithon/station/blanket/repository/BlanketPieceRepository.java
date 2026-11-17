package com.ganzithon.station.blanket.repository;

import com.ganzithon.station.blanket.entity.BlanketPiece;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlanketPieceRepository extends JpaRepository<BlanketPiece, Long> {

    boolean existsByTicketId(Long ticketId);

    List<BlanketPiece> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
