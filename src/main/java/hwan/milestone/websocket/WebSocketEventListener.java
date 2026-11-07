package hwan.milestone.websocket;

import hwan.milestone.service.BroadcastService;
import hwan.milestone.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** 연결 종료(정상 DISCONNECT, 탭 닫기, 네트워크 끊김) 시 퇴장 처리. */
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RoomService roomService;
    private final BroadcastService broadcastService;
    private final RoomMessenger messenger;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        roomService.leave(sessionId).ifPresent(messenger::toRoom);
        broadcastService.forget(sessionId);
    }
}
