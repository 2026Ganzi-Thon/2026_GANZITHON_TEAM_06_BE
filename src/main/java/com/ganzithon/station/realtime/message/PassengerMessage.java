package com.ganzithon.station.realtime.message;

import com.ganzithon.station.realtime.store.Passenger;
import com.ganzithon.station.room.entity.MoodChip;

/**
 * 대기실에 보이는 승객 한 명. 다른 승객에게도 보내므로 호칭만 넣습니다.
 * 계정 닉네임과 ticketId(소유 확인용)는 넣지 않고, 방 안에서는 승차권 번호로 승객을 구분합니다.
 *
 * @param ticketNumber 승차권 번호 (방 안에서 겹치지 않음)
 * @param displayName  대기실 호칭. 예: 104번 두근두근한 양
 * @param moodChip     상태 칩. 없으면 null
 * @param seatNumber   좌석 번호 (1부터). 자리가 다 차서 서서 기다리면 null
 * @param startedTs    기다림 시작 시각 (epoch ms). 머문 시간 계산용
 */
public record PassengerMessage(
        int ticketNumber,
        String displayName,
        MoodChip moodChip,
        Integer seatNumber,
        long startedTs
) {
    public static PassengerMessage from(Passenger passenger) {
        return new PassengerMessage(
                passenger.ticketNumber(),
                passenger.displayName(),
                passenger.moodChip(),
                passenger.seatNumber(),
                passenger.startedAt().toEpochMilli());
    }
}
