package com.ganzithon.station.realtime.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.realtime.message.PassengerCardMessage;
import com.ganzithon.station.realtime.message.PassengerCardRequest;
import com.ganzithon.station.realtime.store.Passenger;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import com.ganzithon.station.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 승객 확대 카드 (MY-05, 담당: 환희). 대기실에서는 승객을 ticketNumber로만 구분하고 ticketId·닉네임은 내보내지 않습니다.
 * 호출 방향: realtime → ticket → blanket (realtime이 blanket을 직접 부르지 않음).
 */
@Service
@RequiredArgsConstructor
public class PassengerCardService {

    private final RoomPassengerStore roomPassengerStore;
    private final TicketService ticketService;

    /**
     * 같은 방에 입장한 승객만 볼 수 있습니다 (방 밖에서 엿보기 방지).
     *
     * @throws BusinessException COMMON_INVALID_INPUT ticketNumber 없음, REALTIME_NOT_JOINED 그 방에 입장 안 함,
     *                           REALTIME_PASSENGER_NOT_FOUND 그 번호의 승객이 방에 없음 (나갔거나 가상 승객)
     */
    public PassengerCardMessage getCard(String roomCode, String sessionId, PassengerCardRequest request) {
        if (request == null || request.ticketNumber() == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        roomPassengerStore.findBySessionId(sessionId)
                .filter(me -> me.roomCode().equals(roomCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.REALTIME_NOT_JOINED));
        Passenger target = roomPassengerStore.findByTicketNumber(roomCode, request.ticketNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.REALTIME_PASSENGER_NOT_FOUND));
        return PassengerCardMessage.from(ticketService.getPassengerCard(target.ticketId()));
    }
}
