package com.ganzithon.station.realtime.message;

import java.util.List;

/**
 * 입장 직후 나에게만 (/user/queue/events). 이 메시지로 화면을 처음 그리고, 이후에는 JOIN/LEAVE로 고칩니다.
 *
 * @param roomCode         대기실 코드
 * @param myTicketNumber   passengers 중 내 승객
 * @param seatCount        이 대기실의 좌석 수
 * @param passengers       방에 있는 승객 전체 (나 포함)
 */
public record RoomStateMessage(
        MessageType type,
        String roomCode,
        int myTicketNumber,
        int seatCount,
        List<PassengerMessage> passengers,
        long serverTs
) {
    public static RoomStateMessage of(String roomCode, int myTicketNumber, int seatCount, List<PassengerMessage> passengers) {
        return new RoomStateMessage(MessageType.ROOM_STATE, roomCode, myTicketNumber, seatCount, passengers,
                System.currentTimeMillis());
    }
}
