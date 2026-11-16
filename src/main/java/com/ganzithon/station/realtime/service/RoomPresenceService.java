package com.ganzithon.station.realtime.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.realtime.message.JoinMessage;
import com.ganzithon.station.realtime.message.LeaveMessage;
import com.ganzithon.station.realtime.message.PassengerMessage;
import com.ganzithon.station.realtime.message.RoomStateMessage;
import com.ganzithon.station.realtime.messaging.RoomEvent;
import com.ganzithon.station.realtime.store.Passenger;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import com.ganzithon.station.realtime.store.RoomPassengerStore.Departure;
import com.ganzithon.station.realtime.store.RoomPassengerStore.JoinResult;
import com.ganzithon.station.ticket.dto.response.PassengerTicketResponse;
import com.ganzithon.station.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 대기실 입장·퇴장과 자동 착석 (ON-05, 담당: 환희). 메모리만 사용합니다.
 * 호출 방향: realtime → ticket (승차권 확인).
 */
@Service
@RequiredArgsConstructor
public class RoomPresenceService {

    private final TicketService ticketService;
    private final RoomPassengerStore roomPassengerStore;

    /**
     * 입장. 승차권이 요청한 사람의 것인지, 진행 중인지, 이 대기실의 것인지 확인한 뒤 좌석을 배정합니다.
     *
     * @throws BusinessException COMMON_INVALID_INPUT ticketId 없음, TICKET_NOT_FOUND, TICKET_FORBIDDEN,
     *                           TICKET_ALREADY_ENDED, TICKET_ROOM_MISMATCH
     */
    public JoinOutcome join(String roomCode, Long ticketId, Requester requester, String sessionId) {
        if (ticketId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        PassengerTicketResponse ticket = ticketService.getPassengerTicket(ticketId, requester);
        if (!ticket.roomCode().equals(roomCode)) {
            throw new BusinessException(ErrorCode.TICKET_ROOM_MISMATCH);
        }

        JoinResult result = roomPassengerStore.join(ticket, sessionId);
        Passenger passenger = result.passenger();
        RoomStateMessage roomState = RoomStateMessage.of(roomCode, passenger.ticketNumber(),
                roomPassengerStore.getSeatCount(),
                roomPassengerStore.findByRoomCode(roomCode).stream().map(PassengerMessage::from).toList());
        Optional<RoomEvent> join = result.newlyJoined()
                ? Optional.of(new RoomEvent(roomCode, JoinMessage.of(PassengerMessage.from(passenger), result.count())))
                : Optional.empty();
        return new JoinOutcome(result.previous().map(RoomPresenceService::toLeaveEvent), roomState, join);
    }

    /** 연결이 끊긴 세션의 퇴장. 방에 없던 세션이면 empty */
    public Optional<RoomEvent> leave(String sessionId) {
        return roomPassengerStore.leave(sessionId).map(RoomPresenceService::toLeaveEvent);
    }

    private static RoomEvent toLeaveEvent(Departure departure) {
        Passenger passenger = departure.passenger();
        return new RoomEvent(passenger.roomCode(),
                LeaveMessage.of(passenger.ticketNumber(), passenger.seatNumber(), departure.remaining()));
    }

    /**
     * 보낼 순서: previousRoomLeave(이전 방) → roomState(나에게만) → join(방 전체)
     *
     * @param previousRoomLeave 다른 승객으로 있다가 옮겨 온 경우 이전 방에 보낼 LEAVE
     * @param roomState         입장한 세션에게만 보낼 방 상태
     * @param join              새로 들어온 경우 방에 보낼 JOIN (재접속이면 empty)
     */
    public record JoinOutcome(Optional<RoomEvent> previousRoomLeave, RoomStateMessage roomState, Optional<RoomEvent> join) {
    }
}
