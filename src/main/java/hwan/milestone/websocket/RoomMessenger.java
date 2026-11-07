package hwan.milestone.websocket;

import hwan.milestone.config.WebSocketConfig;
import hwan.milestone.dto.RoomEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** STOMP 전송 래퍼: 방 전체(/topic/rooms/{roomId}) 또는 특정 세션(/user/queue/events). */
@Component
@RequiredArgsConstructor
public class RoomMessenger {

    private static final String ROOM_TOPIC = "/topic/rooms/";

    private final SimpMessagingTemplate template;

    public void toRoom(RoomEvent event) {
        template.convertAndSend(ROOM_TOPIC + event.roomId(), event.payload());
    }

    /**
     * 인증(Principal)이 없으므로 sessionId 로 user destination 을 해석시킨다.
     * 클라이언트는 /user/queue/events 를 구독하면 된다.
     */
    public void toSession(String sessionId, Object payload) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        template.convertAndSendToUser(sessionId, WebSocketConfig.USER_QUEUE, payload, headers.getMessageHeaders());
    }
}
