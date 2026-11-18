package com.ganzithon.station.ticket.dto.response;

import com.ganzithon.station.blanket.dto.response.PublicCardStatus;
import com.ganzithon.station.room.entity.MoodChip;

import java.time.Instant;
import java.util.List;

/**
 * 대기실 승객 확대 카드 (MY-05). HTTP 응답이 아니라 TicketService → realtime 전달용입니다.
 * 계정 닉네임과 종료 표식은 절대 넣지 않습니다.
 *
 * @param visibility   PUBLIC(공개 + 조각 있음) / NO_PIECE(공개 + 조각 없음) / PRIVATE(비공개) / GUEST(비로그인)
 * @param patternCodes PUBLIC일 때만: 담요 조각 도안 (최신 조각부터 최대 9개). 그 외 null
 * @param trainCount   PUBLIC일 때만: 지금까지 지나간 열차 수. 그 외 null (명세: 비공개는 머문 시간과 상태 칩만)
 */
public record PassengerCardResponse(
        int ticketNumber,
        String displayName,
        MoodChip moodChip,
        Instant startedAt,
        PublicCardStatus visibility,
        List<String> patternCodes,
        Integer trainCount
) {
}
