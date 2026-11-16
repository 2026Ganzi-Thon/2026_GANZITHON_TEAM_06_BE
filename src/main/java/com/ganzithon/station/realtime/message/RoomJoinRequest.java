package com.ganzithon.station.realtime.message;

/**
 * 대기실 입장 (ON-05). SEND /app/rooms/{roomCode}/join
 *
 * @param ticketId 승차권 발급 응답의 ticketId. 그 대기실의 진행 중인 내 승차권이어야 합니다
 */
public record RoomJoinRequest(
        Long ticketId
) {
}
