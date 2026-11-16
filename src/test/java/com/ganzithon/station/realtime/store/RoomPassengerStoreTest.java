package com.ganzithon.station.realtime.store;

import com.ganzithon.station.global.config.RealtimeProperties;
import com.ganzithon.station.realtime.store.RoomPassengerStore.Departure;
import com.ganzithon.station.realtime.store.RoomPassengerStore.JoinResult;
import com.ganzithon.station.ticket.dto.response.PassengerTicketResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좌석 배정과 입장·퇴장 규칙 (Spring 없이).
 */
class RoomPassengerStoreTest {

    private final RoomPassengerStore store = new RoomPassengerStore(
            new RealtimeProperties(2, Duration.ofSeconds(30), Duration.ofSeconds(15), 20));

    @Test
    void 비어_있는_가장_앞_좌석에_앉고_다_차면_서서_기다린다() {
        assertThat(store.join(ticket(1L, "RESULT"), "s1").passenger().seatNumber()).isEqualTo(1);
        assertThat(store.join(ticket(2L, "RESULT"), "s2").passenger().seatNumber()).isEqualTo(2);

        JoinResult third = store.join(ticket(3L, "RESULT"), "s3");

        assertThat(third.passenger().seatNumber()).isNull();
        assertThat(third.count()).isEqualTo(3);
    }

    @Test
    void 나간_자리는_다음_승객이_앉는다() {
        store.join(ticket(1L, "RESULT"), "s1");
        store.join(ticket(2L, "RESULT"), "s2");

        Departure departure = store.leave("s1").orElseThrow();
        JoinResult next = store.join(ticket(3L, "RESULT"), "s3");

        assertThat(departure.passenger().seatNumber()).isEqualTo(1);
        assertThat(departure.remaining()).isEqualTo(1);
        assertThat(next.passenger().seatNumber()).isEqualTo(1);
    }

    @Test
    void 같은_승차권이_새_세션으로_오면_좌석은_그대로이고_이전_세션_종료는_무시한다() {
        store.join(ticket(1L, "RESULT"), "old");

        JoinResult rejoin = store.join(ticket(1L, "RESULT"), "new");

        assertThat(rejoin.newlyJoined()).isFalse();
        assertThat(rejoin.passenger().seatNumber()).isEqualTo(1);
        assertThat(rejoin.passenger().sessionId()).isEqualTo("new");
        assertThat(store.leave("old")).isEmpty();
        assertThat(store.findByRoomCode("RESULT")).hasSize(1);
    }

    @Test
    void 같은_세션이_다른_승차권으로_입장하면_이전_승객은_나간다() {
        store.join(ticket(1L, "RESULT"), "s1");

        JoinResult moved = store.join(ticket(2L, "NO_REPLY"), "s1");

        assertThat(moved.previous()).isPresent();
        assertThat(moved.previous().get().passenger().ticketId()).isEqualTo(1L);
        assertThat(moved.previous().get().remaining()).isZero();
        assertThat(store.findByRoomCode("RESULT")).isEmpty();
        assertThat(store.findByRoomCode("NO_REPLY")).hasSize(1);
    }

    @Test
    void 방에_없던_세션의_종료는_아무것도_하지_않는다() {
        assertThat(store.leave("unknown")).isEmpty();
    }

    private static PassengerTicketResponse ticket(Long ticketId, String roomCode) {
        return new PassengerTicketResponse(ticketId, roomCode, ticketId.intValue(), ticketId + "번 양", null, Instant.now());
    }
}
