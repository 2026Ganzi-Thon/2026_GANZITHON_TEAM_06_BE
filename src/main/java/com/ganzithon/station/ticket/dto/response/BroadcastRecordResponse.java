package com.ganzithon.station.ticket.dto.response;

/**
 * 방송 기록 결과. HTTP 응답이 아니라 TicketService → realtime 전달용입니다.
 *
 * @param displayName        바뀐 대기실 호칭. 예: 104번 두근두근한 양
 * @param situationChipLabel 방송한 상황 칩 문구. 예: 답장 기다리는 중
 */
public record BroadcastRecordResponse(
        String displayName,
        String situationChipLabel
) {
}
