package hwan.milestone.controller;

import hwan.milestone.dto.BroadcastRequest;
import hwan.milestone.dto.ErrorEvent;
import hwan.milestone.dto.JoinResult;
import hwan.milestone.dto.MoveRequest;
import hwan.milestone.dto.ReactionRequest;
import hwan.milestone.service.BroadcastService;
import hwan.milestone.service.GameException;
import hwan.milestone.service.RoomService;
import hwan.milestone.websocket.RoomMessenger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomMessageController {

    private final RoomService roomService;
    private final BroadcastService broadcastService;
    private final RoomMessenger messenger;

    /** 입장. 순서: (이전 방 LEAVE) -> 본인에게 전체 목록 -> 방에 JOIN */
    @MessageMapping("/rooms/{roomId}/join")
    public void join(@DestinationVariable String roomId, SimpMessageHeaderAccessor headers) {
        String sessionId = headers.getSessionId();
        JoinResult result = roomService.join(roomId, sessionId);
        result.previousRoomLeave().ifPresent(messenger::toRoom);
        messenger.toSession(sessionId, result.fullSnapshot());
        result.join().ifPresent(messenger::toRoom);
    }

    @MessageMapping("/rooms/{roomId}/move")
    public void move(@DestinationVariable String roomId, @Payload MoveRequest req, SimpMessageHeaderAccessor headers) {
        roomService.move(roomId, headers.getSessionId(), req);
    }

    @MessageMapping("/rooms/{roomId}/broadcast")
    public void broadcast(@DestinationVariable String roomId, @Payload BroadcastRequest req, SimpMessageHeaderAccessor headers) {
        broadcastService.submit(roomId, headers.getSessionId(), req);
    }

    @MessageMapping("/rooms/{roomId}/reaction")
    public void reaction(@DestinationVariable String roomId, @Payload ReactionRequest req, SimpMessageHeaderAccessor headers) {
        messenger.toRoom(broadcastService.react(roomId, headers.getSessionId(), req));
    }

    @MessageExceptionHandler(GameException.class)
    public void handleGameException(GameException e, SimpMessageHeaderAccessor headers) {
        messenger.toSession(headers.getSessionId(), ErrorEvent.of(e.getCode(), e.getMessage(), e.getRetryAfterMs()));
    }

    /** JSON 파싱 실패, 잘못된 enum 값 등 */
    @MessageExceptionHandler(Exception.class)
    public void handleException(Exception e, SimpMessageHeaderAccessor headers) {
        log.debug("bad message from {}: {}", headers.getSessionId(), e.toString());
        messenger.toSession(headers.getSessionId(), ErrorEvent.of("BAD_REQUEST", "잘못된 메시지 형식입니다.", null));
    }
}
