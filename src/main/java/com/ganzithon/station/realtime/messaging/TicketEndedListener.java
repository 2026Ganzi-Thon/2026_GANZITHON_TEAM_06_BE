package com.ganzithon.station.realtime.messaging;

import com.ganzithon.station.realtime.service.RoomPresenceService;
import com.ganzithon.station.ticket.event.TicketEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 기다림 종료(REST) 때 대기실에서 그 승객을 내보내고 방에 LEAVE를 보냅니다.
 * 종료가 커밋된 뒤에만 실행되므로, 종료가 롤백되면 승객은 그대로 방에 남습니다.
 * WebSocket 연결은 끊지 않습니다. 이후 그 세션의 방송은 REALTIME_NOT_JOINED가 됩니다.
 */
@Component
@RequiredArgsConstructor
public class TicketEndedListener {

    private final RoomPresenceService roomPresenceService;
    private final RoomMessenger roomMessenger;

    @TransactionalEventListener
    public void onTicketEnded(TicketEndedEvent event) {
        roomPresenceService.leaveTicket(event.ticketId(), event.roomCode()).ifPresent(roomMessenger::toRoom);
    }
}
