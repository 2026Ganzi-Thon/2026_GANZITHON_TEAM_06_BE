package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.realtime.support.StompIntegrationTest;
import com.ganzithon.station.ticket.dto.request.TicketEndRequest;
import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기다림 종료(REST)가 커밋되면 대기실에서 그 승객이 나갑니다.
 */
class TicketEndWebSocketTest extends StompIntegrationTest {

    @Autowired
    TicketService ticketService;

    @Test
    void 기다림을_끝내면_방에_LEAVE가_가고_그_뒤_방송은_REALTIME_NOT_JOINED() throws Exception {
        Client stayer = joinNewGuest("NO_REPLY");
        BlockingQueue<Map<String, Object>> room = stayer.subscribe("/topic/rooms/NO_REPLY");
        String enderToken = newGuestToken();
        long enderTicketId = insertTicket("NO_REPLY", enderToken, null, false);
        Client ender = connectAsGuest(enderToken);
        ender.join("NO_REPLY", enderTicketId);
        assertThat(ender.nextEvent().get("type")).isEqualTo("ROOM_STATE");
        Map<String, Object> join = pollUntil(room, event -> "JOIN".equals(event.get("type")));
        int enderTicketNumber = (int) map(join.get("passenger")).get("ticketNumber");

        ticketService.endTicket(enderTicketId, new Requester(null, enderToken), new TicketEndRequest(EndReason.LET_GO));

        Map<String, Object> leave = pollUntil(room, event -> "LEAVE".equals(event.get("type")));
        assertThat(leave.get("ticketNumber")).isEqualTo(enderTicketNumber);
        assertThat(leave.get("seatNumber")).isEqualTo(2);
        assertThat(leave.get("count")).isEqualTo(1);

        ender.broadcast("NO_REPLY", Map.of("situationChipCode", "NO_REPLY_UNREAD_ONE", "moodChip", "CALM"));
        assertError(ender.nextEvent(), "REALTIME_NOT_JOINED");
    }
}
