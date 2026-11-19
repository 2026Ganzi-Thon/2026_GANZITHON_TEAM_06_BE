package com.ganzithon.station.realtime.message;

import java.util.List;

/**
 * 입장 직후 나에게만 (/user/queue/events). 이 메시지로 화면을 처음 그리고, 이후에는 JOIN/LEAVE로 고칩니다.
 *
 * @param roomCode         대기실 코드
 * @param myTicketNumber   passengers 중 내 승객
 * @param mySeatNumber     내 좌석 번호 (1부터). 자리가 다 차서 서서 기다리면 null (입석)
 * @param seatCount        이 대기실의 좌석 수
 * @param passengers       방에 있는 승객 전체 (나 포함)
 * @param onAirBroadcast   지금 전광판에 나와 있는 방송. 방송을 한 번도 안 했거나 노출 시간이 끝났으면 null (기본 안내 문구)
 */
public record RoomStateMessage(
        MessageType type,
        String roomCode,
        int myTicketNumber,
        Integer mySeatNumber,
        int seatCount,
        List<PassengerMessage> passengers,
        OnAirBroadcastMessage onAirBroadcast,
        long serverTs
) {
    public static RoomStateMessage of(String roomCode, int myTicketNumber, Integer mySeatNumber, int seatCount,
                                      List<PassengerMessage> passengers, OnAirBroadcastMessage onAirBroadcast,
                                      long serverTs) {
        return new RoomStateMessage(MessageType.ROOM_STATE, roomCode, myTicketNumber, mySeatNumber, seatCount,
                passengers, onAirBroadcast, serverTs);
    }
}
