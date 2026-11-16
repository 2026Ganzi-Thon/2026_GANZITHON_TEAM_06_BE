package com.ganzithon.station.realtime.messaging;

import com.ganzithon.station.realtime.service.RoomPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 연결 종료(정상 DISCONNECT, 탭 닫기, 네트워크 끊김, heartbeat 시간 초과) 때 퇴장 처리.
 */
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RoomPresenceService roomPresenceService;
    private final RoomMessenger roomMessenger;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        roomPresenceService.leave(event.getSessionId()).ifPresent(roomMessenger::toRoom);
    }
}
