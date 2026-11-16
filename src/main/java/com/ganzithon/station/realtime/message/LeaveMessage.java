package com.ganzithon.station.realtime.message;

/**
 * 방 전체 (/topic/rooms/{roomCode}): 승객 퇴장 (연결 끊김, 다른 방으로 이동).
 *
 * @param ticketNumber 나간 승객
 * @param seatNumber   비워진 좌석. 서서 기다리던 승객이면 null
 * @param count        퇴장 후 방 인원
 */
public record LeaveMessage(
        MessageType type,
        int ticketNumber,
        Integer seatNumber,
        int count,
        long serverTs
) {
    public static LeaveMessage of(int ticketNumber, Integer seatNumber, int count) {
        return new LeaveMessage(MessageType.LEAVE, ticketNumber, seatNumber, count, System.currentTimeMillis());
    }
}
