package com.ganzithon.station.realtime.messaging;

/**
 * Service → 전송: 어느 방(/topic/rooms/{roomCode})에 어떤 메시지를 보낼지.
 */
public record RoomEvent(
        String roomCode,
        Object payload
) {
}
