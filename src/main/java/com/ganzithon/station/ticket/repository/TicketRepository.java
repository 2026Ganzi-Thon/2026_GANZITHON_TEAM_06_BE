package com.ganzithon.station.ticket.repository;

import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** 이 게스트 토큰으로 발급한 승차권이 있는지 (발급 때 토큰 재사용 판단, idx_tickets_guest_token 사용) */
    boolean existsByGuestToken(String guestToken);

    /**
     * 진행 중일 때만 종료합니다. 바뀐 행 수를 돌려줍니다 (이미 끝났으면 0).
     * 확인과 변경을 한 문장으로 해서, 같은 승차권을 동시에 종료해도 한 번만 성공합니다.
     * 두 번째 요청은 첫 번째가 커밋될 때까지 행 잠금을 기다렸다가 status 조건에 걸려 0이 됩니다.
     * <p>영속성 컨텍스트의 Ticket은 고치지 않습니다 (status가 WAITING으로 남아 있음). 바뀐 값은 호출한 쪽이 가진 값을 씁니다.</p>
     */
    @Modifying
    @Query("""
            update Ticket t
            set t.status = com.ganzithon.station.ticket.entity.TicketStatus.ENDED,
                t.endReason = :endReason, t.endedAt = :endedAt, t.updatedAt = :endedAt
            where t.id = :ticketId and t.status = com.ganzithon.station.ticket.entity.TicketStatus.WAITING
            """)
    int endIfWaiting(@Param("ticketId") Long ticketId,
                     @Param("endReason") EndReason endReason,
                     @Param("endedAt") Instant endedAt);

    /** 종료 사유별 종료 건수 (전광판 통계를 서버 시작 때 한 번 채울 때만 사용) */
    @Query("""
            select t.endReason as endReason, count(t) as count
            from Ticket t
            where t.status = com.ganzithon.station.ticket.entity.TicketStatus.ENDED
            group by t.endReason
            """)
    List<EndReasonCount> countEndedByReason();

    interface EndReasonCount {
        EndReason getEndReason();

        long getCount();
    }
}
