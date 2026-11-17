package com.ganzithon.station.ticket.repository;

import com.ganzithon.station.ticket.entity.RoomTicketCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTicketCounterRepository extends JpaRepository<RoomTicketCounter, Long> {

    /**
     * 그 대기실의 다음 승차권 번호를 받습니다 (100부터, 리셋 없음. 1~99는 가상 승객 번호).
     * 한 문장(UPDATE ... RETURNING)으로 올리고 읽어서, 동시에 발급해도 번호가 겹치지 않습니다.
     * 행 잠금은 호출한 트랜잭션이 끝날 때 풀리므로, 발급이 실패해 롤백되면 번호도 되돌아갑니다.
     */
    @Query(value = """
            update room_ticket_counters
            set last_number = last_number + 1, updated_at = now()
            where room_id = :roomId
            returning last_number
            """, nativeQuery = true)
    int incrementAndGetLastNumber(@Param("roomId") Long roomId);
}
