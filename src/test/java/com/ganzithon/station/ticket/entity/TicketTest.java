package com.ganzithon.station.ticket.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 지나간 열차 수 계산 (Spring 없이). 열차 간격마다 1대, 남는 시간은 버립니다.
 */
class TicketTest {

    private static final Duration INTERVAL = Duration.ofSeconds(15);
    private static final Instant STARTED_AT = Instant.parse("2026-11-17T12:00:00Z");

    private final Ticket ticket = Ticket.builder().ticketNumber(1).startedAt(STARTED_AT).build();

    @Test
    void 간격을_다_채워야_열차가_한_대_지나간다() {
        assertThat(ticket.countPassedTrains(STARTED_AT, INTERVAL)).isZero();
        assertThat(ticket.countPassedTrains(STARTED_AT.plusMillis(14_999), INTERVAL)).isZero();
        assertThat(ticket.countPassedTrains(STARTED_AT.plusSeconds(15), INTERVAL)).isEqualTo(1);
        assertThat(ticket.countPassedTrains(STARTED_AT.plusSeconds(46), INTERVAL)).isEqualTo(3);
    }

    @Test
    void 기준_시각이_시작보다_앞이면_0() {
        assertThat(ticket.countPassedTrains(STARTED_AT.minusSeconds(30), INTERVAL)).isZero();
    }
}
