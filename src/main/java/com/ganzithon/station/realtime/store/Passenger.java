package com.ganzithon.station.realtime.store;

import com.ganzithon.station.room.entity.MoodChip;

import java.time.Instant;

/**
 * 대기실에 접속해 있는 승객 (메모리). 승차권 하나에 승객 하나이고, 지금 연결된 STOMP 세션을 가집니다.
 *
 * @param seatNumber 좌석 번호 (1부터). 자리가 없어서 서서 기다리면 null
 */
public record Passenger(
        Long ticketId,
        String roomCode,
        int ticketNumber,
        String displayName,
        MoodChip moodChip,
        Instant startedAt,
        Integer seatNumber,
        String sessionId
) {
    public boolean isSeated() {
        return seatNumber != null;
    }
}
