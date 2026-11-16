package com.ganzithon.station.realtime.messaging;

import com.ganzithon.station.global.config.WebSocketConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 실시간 메시지 공통 전송. SimpMessagingTemplate을 직접 쓰지 말고 이 클래스를 씁니다 (docs/CODE_CONVENTION.md 5장).
 */
@Component
@RequiredArgsConstructor
public class RoomMessenger {

    private static final String ROOM_TOPIC = "/topic/rooms/";

    private final SimpMessagingTemplate template;

    /** 방 전체: /topic/rooms/{roomCode} */
    public void toRoom(RoomEvent event) {
        template.convertAndSend(ROOM_TOPIC + event.roomCode(), event.payload());
    }

    /**
     * 한 세션에게만: /user/queue/events.
     * STOMP에 Principal을 쓰지 않으므로 sessionId로 user destination을 찾게 합니다.
     */
    public void toSession(String sessionId, Object payload) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        template.convertAndSendToUser(sessionId, WebSocketConfig.USER_QUEUE, payload, headers.getMessageHeaders());
    }
}
