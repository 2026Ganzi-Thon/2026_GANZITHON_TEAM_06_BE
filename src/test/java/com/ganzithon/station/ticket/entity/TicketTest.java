package com.ganzithon.station.ticket.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공용 열차 시각표 (Spring 없이). epoch부터 간격마다 1대, 30분이면 매시 00분·30분.
 * 내 열차 수 = 시작 뒤에 지나간 공용 열차 수.
 */
class TicketTest {

    private static final Duration HALF_HOUR = Duration.ofMinutes(30);

    @Test
    void 열차는_매시_00분과_30분에_지나간다() {
        Ticket ticket = startedAt("2026-11-17T12:10:00Z");

        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T12:29:59Z"), HALF_HOUR)).isZero();
        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T12:30:00Z"), HALF_HOUR)).isEqualTo(1);
        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T13:45:00Z"), HALF_HOUR)).isEqualTo(3);
    }

    @Test
    void 같은_방_사람은_들어온_시각이_달라도_같은_열차를_센다() {
        Ticket early = startedAt("2026-11-17T12:01:00Z");
        Ticket late = startedAt("2026-11-17T12:29:00Z");
        Instant justAfterTrain = Instant.parse("2026-11-17T12:30:01Z");

        assertThat(early.countPassedTrains(justAfterTrain, HALF_HOUR)).isEqualTo(1);
        assertThat(late.countPassedTrains(justAfterTrain, HALF_HOUR)).isEqualTo(1);
    }

    @Test
    void 시작하는_순간_지나간_열차는_세지_않는다() {
        Ticket ticket = startedAt("2026-11-17T12:30:00Z");

        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T12:59:59Z"), HALF_HOUR)).isZero();
        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T13:00:00Z"), HALF_HOUR)).isEqualTo(1);
    }

    @Test
    void 기준_시각이_시작보다_앞이면_0() {
        Ticket ticket = startedAt("2026-11-17T12:10:00Z");

        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T11:00:00Z"), HALF_HOUR)).isZero();
    }

    @Test
    void 다음_열차_시각은_모두에게_같다() {
        assertThat(Ticket.nextTrainAt(Instant.parse("2026-11-17T12:10:00Z"), HALF_HOUR))
                .isEqualTo(Instant.parse("2026-11-17T12:30:00Z"));
        assertThat(Ticket.nextTrainAt(Instant.parse("2026-11-17T12:30:00Z"), HALF_HOUR))
                .isEqualTo(Instant.parse("2026-11-17T13:00:00Z"));
        assertThat(Ticket.nextTrainAt(Instant.parse("2026-11-17T12:00:15Z"), Duration.ofSeconds(20)))
                .isEqualTo(Instant.parse("2026-11-17T12:00:20Z"));
    }

    private static Ticket startedAt(String startedAt) {
        return Ticket.builder().ticketNumber(100).startedAt(Instant.parse(startedAt)).build();
    }
}
