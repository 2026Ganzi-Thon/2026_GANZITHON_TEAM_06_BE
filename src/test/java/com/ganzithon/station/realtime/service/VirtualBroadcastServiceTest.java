package com.ganzithon.station.realtime.service;

import com.ganzithon.station.global.config.RealtimeProperties;
import com.ganzithon.station.global.config.VirtualPassengerProperties;
import com.ganzithon.station.realtime.store.BroadcastStore;
import com.ganzithon.station.realtime.store.QueuedBroadcast;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.room.entity.SituationChip;
import com.ganzithon.station.room.entity.VirtualPassenger;
import com.ganzithon.station.room.service.RoomService;
import com.ganzithon.station.ticket.dto.response.PassengerTicketResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 가상 승객 자동 방송 규칙 (Spring 없이, 시각을 직접 넘김). 첫 방송 10초, 이후 4~6분, 전광판 노출 15초.
 */
class VirtualBroadcastServiceTest {

    private static final long FIRST = 10_000;
    private static final long MIN = 240_000;
    private static final long MAX = 360_000;
    private static final long DISPLAY = 15_000;

    private final RoomService roomService = mock(RoomService.class);
    private final RoomPassengerStore passengerStore = new RoomPassengerStore(
            new RealtimeProperties(20, Duration.ofSeconds(30), Duration.ofMillis(DISPLAY), 20));
    private final BroadcastStore broadcastStore = new BroadcastStore(
            new RealtimeProperties(20, Duration.ofSeconds(30), Duration.ofMillis(DISPLAY), 20));

    private VirtualBroadcastService service = create(true);

    @BeforeEach
    void stubRoom() {
        when(roomService.getVirtualPassengerList("RESULT")).thenReturn(List.of(
                new VirtualPassenger("RESULT", 7, MoodChip.NERVOUS),
                new VirtualPassenger("RESULT", 38, MoodChip.TIRED)));
        SituationChip chip = mock(SituationChip.class);
        when(chip.getCode()).thenReturn("RESULT_EXAM");
        when(chip.getLabel()).thenReturn("시험 결과 기다리는 중");
        when(roomService.getRandomSituationChip("RESULT")).thenReturn(chip);
    }

    @Test
    void 실제_승객이_없는_방에서는_방송하지_않는다() {
        service.tick(0);
        service.tick(FIRST * 10);

        assertThat(broadcastStore.pollDue(FIRST * 10)).isEmpty();
    }

    @Test
    void 처음_들어오고_10초_뒤에_첫_가상_방송이_대기열에_들어간다() {
        enter("RESULT", "s1");

        service.tick(0);
        service.tick(FIRST - 1);
        assertThat(broadcastStore.isQueueEmpty("RESULT")).isTrue();

        service.tick(FIRST);
        List<QueuedBroadcast> due = broadcastStore.pollDue(FIRST);
        assertThat(due).hasSize(1);
        QueuedBroadcast broadcast = due.getFirst();
        assertThat(broadcast.roomCode()).isEqualTo("RESULT");
        assertThat(broadcast.ticketNumber()).isIn(7, 38);
        assertThat(broadcast.displayName()).isIn("7번 조마조마한 양", "38번 지친 양");
        assertThat(broadcast.situationChipCode()).isEqualTo("RESULT_EXAM");
        assertThat(broadcast.situationChipLabel()).isEqualTo("시험 결과 기다리는 중");
    }

    @Test
    void 다음_가상_방송은_4분에서_6분_사이다() {
        enter("RESULT", "s1");
        service.tick(0);
        service.tick(FIRST);

        assertThat(service.getNextBroadcastAt("RESULT")).isBetween(FIRST + MIN, FIRST + MAX);
    }

    @Test
    void 대기열에_방송이_있으면_건너뛰고_비면_넣는다() {
        enter("RESULT", "s1");
        service.tick(0);
        broadcastStore.enqueue(new QueuedBroadcast("real", "RESULT", 104, "104번 양", null, "C", "문구", 0));

        service.tick(FIRST);
        assertThat(broadcastStore.pollDue(FIRST)).extracting(QueuedBroadcast::broadcastId).containsExactly("real");

        service.tick(FIRST + 1_000);
        assertThat(broadcastStore.pollDue(FIRST + DISPLAY)).extracting(QueuedBroadcast::ticketNumber)
                .singleElement().isIn(7, 38);
    }

    @Test
    void 방이_비면_잊고_다시_들어오면_10초부터_다시_센다() {
        enter("RESULT", "s1");
        service.tick(0);
        passengerStore.leave("s1");
        service.tick(1_000);
        assertThat(service.getNextBroadcastAt("RESULT")).isNull();

        enter("RESULT", "s2");
        service.tick(5_000);

        assertThat(service.getNextBroadcastAt("RESULT")).isEqualTo(5_000 + FIRST);
    }

    @Test
    void 꺼져_있으면_아무것도_하지_않는다() {
        service = create(false);
        enter("RESULT", "s1");

        service.tick(0);
        service.tick(FIRST);

        assertThat(service.getNextBroadcastAt("RESULT")).isNull();
        assertThat(broadcastStore.isQueueEmpty("RESULT")).isTrue();
    }

    private VirtualBroadcastService create(boolean enabled) {
        return new VirtualBroadcastService(roomService, passengerStore, broadcastStore, new VirtualPassengerProperties(
                enabled, Duration.ofMillis(FIRST), Duration.ofMillis(MIN), Duration.ofMillis(MAX)));
    }

    private void enter(String roomCode, String sessionId) {
        long ticketId = sessionId.hashCode();
        passengerStore.join(new PassengerTicketResponse(ticketId, roomCode, 100, "100번 양", null, Instant.now()), sessionId);
    }
}
