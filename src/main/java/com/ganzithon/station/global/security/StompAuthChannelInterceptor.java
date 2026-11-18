package com.ganzithon.station.global.security;

import com.ganzithon.station.global.config.WebSocketConfig;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket(STOMP) 인증. REST와 같은 헤더를 STOMP CONNECT 프레임에 넣어 보냅니다.
 * <pre>{@code
 * client.connect({ Authorization: 'Bearer ' + accessToken, 'X-Guest-Token': guestToken }, ...)
 * }</pre>
 * <ul>
 *     <li>CONNECT 때 한 번 확인하고, 세션이 끝날 때까지 {@link Requester}를 세션에 보관합니다</li>
 *     <li>만료·위조된 계정 토큰이면 연결을 거부합니다 (STOMP ERROR 프레임 후 연결 종료)</li>
 *     <li>토큰이 하나도 없어도 연결은 됩니다. 승차권 확인이 필요한 동작(입장 등)에서 각 Service가 거절합니다</li>
 *     <li><b>목적지 제한</b>: SEND는 /app/** (우리 @MessageMapping)만, SUBSCRIBE는 /topic/rooms/{방}과 /user/queue/events만 받습니다.
 *         내장 브로커는 클라이언트가 /topic·/queue로 직접 보낸 메시지도 그대로 중계하므로, 막지 않으면 입장·칩 검증·쿨다운 없이
 *         가짜 BROADCAST 등을 방 전체에 보낼 수 있습니다. 정상 프론트는 쓰지 않는 경로라 에러 없이 버립니다 (연결은 유지)</li>
 * </ul>
 * 메시지 핸들러에서는 {@link #requester(SimpMessageHeaderAccessor)}로 꺼냅니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String REQUESTER_ATTRIBUTE = "station.requester";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /** 클라이언트가 SEND할 수 있는 목적지 (WebSocketConfig의 application destination prefix) */
    private static final String APP_PREFIX = "/app/";
    /** 클라이언트가 SUBSCRIBE할 수 있는 목적지: 방 전체 메시지와 내 개인 큐 */
    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";
    private static final String MY_QUEUE = "/user" + WebSocketConfig.USER_QUEUE;

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (!isAllowedDestination(accessor.getCommand(), accessor.getDestination())) {
            log.warn("허용되지 않은 STOMP 목적지 차단 session={} command={} destination={}",
                    accessor.getSessionId(), accessor.getCommand(), accessor.getDestination());
            return null;
        }
        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Long userId = resolveUserId(accessor.getFirstNativeHeader(AUTHORIZATION));
            String guestToken = GuestTokenHeader.normalize(accessor.getFirstNativeHeader(GuestTokenHeader.NAME));
            sessionAttributes.put(REQUESTER_ATTRIBUTE, new Requester(userId, guestToken));
        }
        return message;
    }

    /** SEND·SUBSCRIBE만 목적지를 봅니다. 나머지 명령(CONNECT, UNSUBSCRIBE, DISCONNECT 등)은 목적지가 없어서 통과 */
    static boolean isAllowedDestination(StompCommand command, String destination) {
        return switch (command) {
            case SEND -> destination != null && destination.startsWith(APP_PREFIX);
            case SUBSCRIBE -> destination != null
                    && (destination.startsWith(ROOM_TOPIC_PREFIX) || destination.equals(MY_QUEUE));
            default -> true;
        };
    }

    /** 이 세션을 연 사람. CONNECT 때 토큰이 없었으면 userId, guestToken 모두 null */
    public static Requester requester(SimpMessageHeaderAccessor headers) {
        Map<String, Object> sessionAttributes = headers.getSessionAttributes();
        Object requester = sessionAttributes == null ? null : sessionAttributes.get(REQUESTER_ATTRIBUTE);
        return requester instanceof Requester found ? found : new Requester(null, null);
    }

    private Long resolveUserId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        try {
            return Long.valueOf(jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length())).getSubject());
        } catch (JwtException | NumberFormatException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }
}
