package com.ganzithon.station.realtime.message;

/**
 * 승객 확대 카드 (MY-05). SEND /app/rooms/{roomCode}/passenger-card
 *
 * @param ticketNumber 탭한 승객의 승차권 번호 (ROOM_STATE·JOIN의 passenger.ticketNumber). 필수
 */
public record PassengerCardRequest(
        Integer ticketNumber
) {
}
