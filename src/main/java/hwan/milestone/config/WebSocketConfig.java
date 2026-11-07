package hwan.milestone.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** 개인 메시지(입장 직후 전체 목록, ERROR) 큐. 클라이언트는 /user/queue/events 를 구독한다. */
    public static final String USER_QUEUE = "/queue/events";

    /**
     * STOMP heartbeat [서버 송신 간격, 서버가 기대하는 클라이언트 송신 간격] (ms).
     * 클라이언트가 heartbeat 를 협상하면, 서버는 max(클라이언트 송신 간격, 10초) x 3 동안 아무 프레임도 받지 못한 세션을 끊는다.
     * (stompjs 기본 10초 -> 약 30초) 끊기면 SessionDisconnectEvent -> LEAVE 가 나간다.
     * bfcache 에 들어가 멈춘 페이지, 조용히 끊긴 네트워크처럼 소켓만 남은 연결을 정리하기 위함.
     */
    private static final long[] HEARTBEAT = {10_000, 10_000};

    private final TaskScheduler heartbeatScheduler;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    /** WebSocket 설정이 만드는 스케줄러를 재사용한다. 순환 참조를 피하려고 @Lazy. */
    public WebSocketConfig(@Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler heartbeatScheduler) {
        this.heartbeatScheduler = heartbeatScheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 네이티브 WebSocket 만 사용 (SockJS 미사용).
        // 같은 출처(index.html)와 Origin 헤더가 없는 클라이언트(부하 테스트 봇)는 기본 허용된다.
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
        // 한 세션 안에서 SUBSCRIBE -> SEND(join) 순서, move seq 순서를 보장한다.
        registry.setPreserveReceiveOrder(true);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        // /topic: 방 브로드캐스트, /queue: 세션 개인 메시지(user destination 변환 대상)
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(HEARTBEAT)
                .setTaskScheduler(heartbeatScheduler);
        // 한 세션으로 나가는 메시지 순서 보장 (입장 직후 전체 목록 -> 이후 SNAPSHOT)
        registry.setPreservePublishOrder(true);
    }
}
