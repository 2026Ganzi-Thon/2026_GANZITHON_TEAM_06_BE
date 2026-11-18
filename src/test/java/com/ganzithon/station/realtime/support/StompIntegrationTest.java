package com.ganzithon.station.realtime.support;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.security.GuestTokenHeader;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실시간 대기실 통합 테스트 공통: 실제 WebSocket(STOMP) 연결, 승차권 DB 삽입, 테스트 사이 정리.
 * 하위 클래스가 같은 설정을 써서 스프링 컨텍스트(와 Postgres 컨테이너)를 함께 씁니다.
 * 좌석 수는 2, 전광판 노출 시간은 300ms로 줄였습니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "station.realtime.seat-count=2",
        "station.realtime.broadcast-display=300ms"
})
public abstract class StompIntegrationTest {

    protected static final long TIMEOUT_SECONDS = 5;
    private static final List<String> ROOM_CODES = List.of("RESULT", "NO_REPLY", "GANZITHON_FINAL");

    @LocalServerPort
    int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    RoomPassengerStore roomPassengerStore;

    private final List<StompSession> sessions = new ArrayList<>();

    @AfterEach
    void disconnectAll() throws InterruptedException {
        sessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
        // 다음 테스트가 빈 대기실에서 시작하도록 퇴장 처리를 기다립니다
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (!allRoomsEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(allRoomsEmpty()).isTrue();
    }

    // ── 연결 ──

    protected Client joinNewGuest(String roomCode) throws Exception {
        String guestToken = newGuestToken();
        long ticketId = insertTicket(roomCode, guestToken, null, false);
        Client client = connectAsGuest(guestToken);
        client.join(roomCode, ticketId);
        assertThat(client.nextEvent().get("type")).isEqualTo("ROOM_STATE");
        return client;
    }

    protected Client connectAsGuest(String guestToken) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add(GuestTokenHeader.NAME, guestToken);
        return connect(headers);
    }

    protected Client connect(StompHeaders connectHeaders) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        StompSession session = stompClient.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                        connectHeaders, new StompSessionHandlerAdapter() {
                        })
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        sessions.add(session);
        Client client = new Client(session);
        client.events = client.subscribe("/user/queue/events");
        return client;
    }

    // ── DB ──

    protected long insertTicket(String roomCode, String guestToken, String moodChip, boolean ended) {
        return jdbcTemplate.queryForObject("""
                        insert into tickets (room_id, ticket_number, guest_token, mood_chip, pattern_id, status,
                                             end_reason, started_at, ended_at)
                        select r.id,
                               (select coalesce(max(t.ticket_number), 0) + 1 from tickets t where t.room_id = r.id),
                               ?, ?, (select id from patterns where code = 'PATTERN_HEART'), ?, ?, now(), ?
                        from rooms r
                        where r.code = ?
                        returning id
                        """, Long.class,
                guestToken, moodChip, ended ? "ENDED" : "WAITING", ended ? "LET_GO" : null,
                ended ? Timestamp.from(Instant.now()) : null, roomCode);
    }

    protected Long insertUser() {
        String nickname = UUID.randomUUID().toString().substring(0, 12);
        return jdbcTemplate.queryForObject(
                "insert into users (nickname, pin_hash) values (?, 'not-used') returning id", Long.class, nickname);
    }

    protected static String newGuestToken() {
        return UUID.randomUUID().toString();
    }

    // ── 메시지 확인 ──

    protected static void assertError(Map<String, Object> event, String code) {
        assertThat(event.get("type")).isEqualTo("ERROR");
        assertThat(event.get("code")).isEqualTo(code);
        assertThat(event).containsKeys("message", "serverTs");
    }

    protected static Map<String, Object> poll(BlockingQueue<Map<String, Object>> queue) throws InterruptedException {
        Map<String, Object> event = queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(event).as("%d초 안에 메시지가 와야 합니다", TIMEOUT_SECONDS).isNotNull();
        return event;
    }

    /** 조건에 맞는 메시지가 올 때까지 다른 메시지는 건너뜁니다 (이전 테스트의 방송 등) */
    protected static Map<String, Object> pollUntil(BlockingQueue<Map<String, Object>> queue,
                                                   Predicate<Map<String, Object>> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> event = queue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            if (event != null && condition.test(event)) {
                return event;
            }
        }
        throw new AssertionError("%d초 안에 조건에 맞는 메시지가 와야 합니다".formatted(TIMEOUT_SECONDS));
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    protected static List<Map<String, Object>> list(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private boolean allRoomsEmpty() {
        return ROOM_CODES.stream().allMatch(roomCode -> roomPassengerStore.findByRoomCode(roomCode).isEmpty());
    }

    /** STOMP 세션 하나 + 나에게 오는 메시지(/user/queue/events) */
    protected static final class Client {

        public final StompSession session;
        private BlockingQueue<Map<String, Object>> events;

        private Client(StompSession session) {
            this.session = session;
        }

        public BlockingQueue<Map<String, Object>> subscribe(String destination) {
            BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
            session.subscribe(destination, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                @SuppressWarnings("unchecked")
                public void handleFrame(StompHeaders headers, Object payload) {
                    queue.add((Map<String, Object>) payload);
                }
            });
            return queue;
        }

        public void join(String roomCode, long ticketId) {
            session.send("/app/rooms/" + roomCode + "/join", Map.of("ticketId", ticketId));
        }

        public void broadcast(String roomCode, Map<String, Object> body) {
            session.send("/app/rooms/" + roomCode + "/broadcast", body);
        }

        public void react(String roomCode, Map<String, Object> body) {
            session.send("/app/rooms/" + roomCode + "/reaction", body);
        }

        public Map<String, Object> nextEvent() throws InterruptedException {
            return poll(events);
        }
    }
}
