package com.ganzithon.station.ticket.service;

import com.ganzithon.station.ticket.dto.response.TicketStatsResponse;
import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.ticket.event.TicketEndedEvent;
import com.ganzithon.station.ticket.repository.TicketRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 전광판 통계 (BR-04) 메모리 카운터. 서버 시작 때 DB에서 한 번 세고, 이후에는 종료가 커밋될 때마다 1씩 올립니다.
 * 전광판은 모든 화면이 자주 조회하므로 요청마다 DB를 세지 않습니다 (DOMAIN.md 7장).
 */
@Component
public class TicketStatsCounter {

    private final Map<EndReason, AtomicLong> counts = new EnumMap<>(EndReason.class);

    public TicketStatsCounter(TicketRepository ticketRepository) {
        for (EndReason endReason : EndReason.values()) {
            counts.put(endReason, new AtomicLong());
        }
        ticketRepository.countEndedByReason().stream()
                .filter(row -> row.getEndReason() != null)
                .forEach(row -> counts.get(row.getEndReason()).set(row.getCount()));
    }

    /** 커밋된 종료만 셉니다. 종료가 롤백되면(조각 저장 실패 등) 오르지 않습니다. */
    @TransactionalEventListener
    public void onTicketEnded(TicketEndedEvent event) {
        counts.get(event.endReason()).incrementAndGet();
    }

    public TicketStatsResponse snapshot() {
        return new TicketStatsResponse(
                counts.get(EndReason.ANSWERED).get(),
                counts.get(EndReason.UNEXPECTED).get(),
                counts.get(EndReason.LET_GO).get());
    }
}
