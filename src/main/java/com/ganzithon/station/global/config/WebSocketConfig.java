package com.ganzithon.station.global.config;

import com.ganzithon.station.global.security.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket 설정. 데모 서버(test/demo_test)에서 검증한 구조를 그대로 씁니다. 경로는 docs/API_CONVENTION.md 7장.
 * <ul>
 *     <li>연결: /ws (네이티브 WebSocket, SockJS 없음). 허용 Origin은 REST CORS와 같은 값</li>
 *     <li>클라이언트 → 서버: /app/rooms/{roomCode}/{동작}</li>
 *     <li>방 전체 수신: /topic/rooms/{roomCode}, 나에게만 수신: /user/queue/events</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** 개인 메시지(입장 직후 방 상태, ERROR) 큐. 클라이언트는 /user/queue/events를 구독합니다. */
    public static final String USER_QUEUE = "/queue/events";

    /**
     * STOMP heartbeat [서버 송신 간격, 서버가 기대하는 클라이언트 송신 간격] (ms).
     * 클라이언트가 heartbeat를 협상하면, 서버는 max(클라이언트 송신 간격, 10초) × 3 동안 아무 프레임도 받지 못한 세션을 끊습니다.
     * 조용히 끊긴 네트워크, 멈춘 탭처럼 소켓만 남은 연결을 정리해서 LEAVE가 나가게 하기 위함입니다.
     */
    private static final long[] HEARTBEAT = {10_000, 10_000};

    private final TaskScheduler heartbeatScheduler;
    private final CorsProperties corsProperties;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    /** WebSocket 설정이 만드는 스케줄러를 heartbeat에 재사용합니다. 순환 참조를 피하려고 @Lazy. */
    public WebSocketConfig(@Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler heartbeatScheduler,
                           CorsProperties corsProperties,
                           StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.heartbeatScheduler = heartbeatScheduler;
        this.corsProperties = corsProperties;
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsProperties.allowedOriginPatterns().toArray(String[]::new));
        // 한 세션 안에서 SUBSCRIBE → SEND(join) 순서를 보장합니다.
        registry.setPreserveReceiveOrder(true);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        // /topic: 방 전체, /queue: 세션 개인 메시지 (user destination 변환 대상)
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(HEARTBEAT)
                .setTaskScheduler(heartbeatScheduler);
        // 한 세션으로 나가는 메시지 순서 보장 (입장 직후 방 상태 → 이후 JOIN/LEAVE)
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
