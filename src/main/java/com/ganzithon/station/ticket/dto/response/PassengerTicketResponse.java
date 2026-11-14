package com.ganzithon.station.ticket.dto.response;

import com.ganzithon.station.room.entity.MoodChip;

import java.time.Instant;

/**
 * realtime이 대기실 입장(승객 등록) 때 받는 승차권 정보. HTTP 응답이 아니라 TicketService → realtime 전달용입니다.
 * 계정 닉네임은 절대 넣지 않습니다 (대기실에는 호칭만).
 *
 * @param ticketId     승차권 ID
 * @param roomCode     대기실 코드
 * @param ticketNumber 승차권 번호
 * @param displayName  대기실 호칭. 예: 104번 두근두근한 양
 * @param moodChip     지금 상태 칩. 없으면 null
 * @param startedAt    기다림 시작 시각 (머문 시간 계산용)
 */
public record PassengerTicketResponse(
        Long ticketId,
        String roomCode,
        int ticketNumber,
        String displayName,
        MoodChip moodChip,
        Instant startedAt
) {
}
