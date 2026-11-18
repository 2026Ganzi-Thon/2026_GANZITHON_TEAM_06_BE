package com.ganzithon.station.ticket.entity;

import com.ganzithon.station.global.config.TrainProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 열차 수 계산 (Spring 없이). 내 첫 열차(시작 10초 뒤, TR-01) + 공용 열차(epoch부터 30분마다, 매시 00분·30분).
 */
class TicketTest {

    private static final TrainProperties TRAIN = new TrainProperties(1800, Duration.ofSeconds(10));

    @Test
    void 입장_10초_뒤에_내_첫_열차가_지나간다() {
        Ticket ticket = startedAt("2026-11-17T12:10:00Z");

        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T12:10:09Z"), TRAIN)).isZero();
        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T12:10:10Z"), TRAIN)).isEqualTo(1);
        assertThat(ticket.firstTrainAt(TRAIN)).isEqualTo(Instant.parse("2026-11-17T12:10:10Z"));
    }

    @Test
    void 그_뒤로는_매시_00분과_30분의_공용_열차를_센다() {
        Ticket ticket = startedAt("2026-11-17T12:10:00Z");

        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T12:29:59Z"), TRAIN)).isEqualTo(1);
        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T12:30:00Z"), TRAIN)).isEqualTo(2);
        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T13:45:00Z"), TRAIN)).isEqualTo(4);
    }

    @Test
    void 같은_방_사람은_들어온_시각이_달라도_같은_공용_열차를_본다() {
        Ticket early = startedAt("2026-11-17T12:01:00Z");
        Ticket late = startedAt("2026-11-17T12:20:00Z");
        Instant now = Instant.parse("2026-11-17T12:25:00Z");

        assertThat(early.nextTrainAt(now, TRAIN)).isEqualTo(Instant.parse("2026-11-17T12:30:00Z"));
        assertThat(late.nextTrainAt(now, TRAIN)).isEqualTo(Instant.parse("2026-11-17T12:30:00Z"));
    }

    @Test
    void 다음_열차는_내_첫_열차가_아직이면_그_시각이다() {
        Ticket ticket = startedAt("2026-11-17T12:10:00Z");

        assertThat(ticket.nextTrainAt(Instant.parse("2026-11-17T12:10:01Z"), TRAIN))
                .isEqualTo(Instant.parse("2026-11-17T12:10:10Z"));
        assertThat(ticket.nextTrainAt(Instant.parse("2026-11-17T12:10:10Z"), TRAIN))
                .isEqualTo(Instant.parse("2026-11-17T12:30:00Z"));
    }

    @Test
    void 공용_열차가_첫_열차보다_먼저_오면_다음_열차는_공용_열차다() {
        Ticket ticket = startedAt("2026-11-17T12:29:55Z");

        assertThat(ticket.nextTrainAt(Instant.parse("2026-11-17T12:29:56Z"), TRAIN))
                .isEqualTo(Instant.parse("2026-11-17T12:30:00Z"));
        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T12:30:05Z"), TRAIN)).isEqualTo(2);
    }

    @Test
    void 기준_시각이_시작보다_앞이면_0() {
        Ticket ticket = startedAt("2026-11-17T12:10:00Z");

        assertThat(ticket.countPassedTrains(Instant.parse("2026-11-17T11:00:00Z"), TRAIN)).isZero();
    }

    private static Ticket startedAt(String startedAt) {
        return Ticket.builder().ticketNumber(100).startedAt(Instant.parse(startedAt)).build();
    }
}
