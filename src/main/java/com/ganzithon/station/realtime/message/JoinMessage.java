package com.ganzithon.station.realtime.message;

/**
 * 방 전체 (/topic/rooms/{roomCode}): 새 승객 입장. 입장한 본인도 받습니다.
 *
 * @param passenger 들어온 승객
 * @param count     입장 후 방 인원
 */
public record JoinMessage(
        MessageType type,
        PassengerMessage passenger,
        int count,
        long serverTs
) {
    public static JoinMessage of(PassengerMessage passenger, int count) {
        return new JoinMessage(MessageType.JOIN, passenger, count, System.currentTimeMillis());
    }
}
