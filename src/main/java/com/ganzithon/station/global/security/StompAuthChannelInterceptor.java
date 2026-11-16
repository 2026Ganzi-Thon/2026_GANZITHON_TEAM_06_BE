package com.ganzithon.station.global.security;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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
 * </ul>
 * 메시지 핸들러에서는 {@link #requester(SimpMessageHeaderAccessor)}로 꺼냅니다.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String REQUESTER_ATTRIBUTE = "station.requester";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
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
