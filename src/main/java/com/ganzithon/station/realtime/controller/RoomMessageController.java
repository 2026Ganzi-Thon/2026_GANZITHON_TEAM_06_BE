package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.security.StompAuthChannelInterceptor;
import com.ganzithon.station.realtime.message.ErrorMessage;
import com.ganzithon.station.realtime.message.RoomJoinRequest;
import com.ganzithon.station.realtime.messaging.RoomMessenger;
import com.ganzithon.station.realtime.service.RoomPresenceService;
import com.ganzithon.station.realtime.service.RoomPresenceService.JoinOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * 실시간 대기실 STOMP 핸들러. 메시지 명세는 README의 "WebSocket 메시지 명세" 표.
 * 실패는 요청한 세션에게만 ERROR로 보냅니다 (/user/queue/events).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomMessageController {

    private final RoomPresenceService roomPresenceService;
    private final RoomMessenger roomMessenger;

    /** 입장 (ON-05). 순서: (이전 방 LEAVE) → 나에게 ROOM_STATE → 방에 JOIN */
    @MessageMapping("/rooms/{roomCode}/join")
    public void join(@DestinationVariable String roomCode, @Payload RoomJoinRequest request,
                     SimpMessageHeaderAccessor headers) {
        String sessionId = headers.getSessionId();
        JoinOutcome outcome = roomPresenceService.join(roomCode, request.ticketId(),
                StompAuthChannelInterceptor.requester(headers), sessionId);
        outcome.previousRoomLeave().ifPresent(roomMessenger::toRoom);
        roomMessenger.toSession(sessionId, outcome.roomState());
        outcome.join().ifPresent(roomMessenger::toRoom);
    }

    @MessageExceptionHandler(BusinessException.class)
    public void handleBusinessException(BusinessException e, SimpMessageHeaderAccessor headers) {
        roomMessenger.toSession(headers.getSessionId(), ErrorMessage.of(e.getErrorCode()));
    }

    /** JSON 형식 오류, 숫자가 아닌 ticketId 등 */
    @MessageExceptionHandler(Exception.class)
    public void handleException(Exception e, SimpMessageHeaderAccessor headers) {
        log.debug("잘못된 STOMP 메시지 session={}: {}", headers.getSessionId(), e.toString());
        roomMessenger.toSession(headers.getSessionId(), ErrorMessage.of(ErrorCode.COMMON_INVALID_INPUT));
    }
}
