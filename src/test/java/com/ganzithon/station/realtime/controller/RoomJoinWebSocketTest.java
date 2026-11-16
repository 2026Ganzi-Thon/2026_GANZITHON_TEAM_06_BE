package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.security.AccountTokenProvider;
import com.ganzithon.station.global.security.GuestTokenHeader;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 대기실 입장(ON-05)을 실제 WebSocket(STOMP) 연결로 확인합니다. 승차권은 DB에 직접 넣습니다.
 * 좌석 수는 2로 줄여서 "다 차면 서서 기다리기"를 확인합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "station.realtime.seat-count=2")
class RoomJoinWebSocketTest {

    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AccountTokenProvider accountTokenProvider;

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

    @Test
    void 내_승차권으로_입장하면_나에게_ROOM_STATE_방에_JOIN() throws Exception {
        String guestToken = newGuestToken();
        long ticketId = insertTicket("RESULT", guestToken, "FLUTTERING", false);
        Client client = connectAsGuest(guestToken);
        BlockingQueue<Map<String, Object>> room = client.subscribe("/topic/rooms/RESULT");

        client.join("RESULT", ticketId);

        Map<String, Object> roomState = client.nextEvent();
        assertThat(roomState.get("type")).isEqualTo("ROOM_STATE");
        assertThat(roomState.get("roomCode")).isEqualTo("RESULT");
        assertThat(roomState.get("seatCount")).isEqualTo(2);
        assertThat(roomState.get("serverTs")).isInstanceOf(Number.class);
        List<Map<String, Object>> passengers = list(roomState.get("passengers"));
        assertThat(passengers).hasSize(1);
        Map<String, Object> me = passengers.getFirst();
        assertThat(me.get("ticketNumber")).isEqualTo(roomState.get("myTicketNumber"));
        assertThat((String) me.get("displayName")).endsWith("번 두근두근한 양");
        assertThat(me.get("moodChip")).isEqualTo("FLUTTERING");
        assertThat(me.get("seatNumber")).isEqualTo(1);
        assertThat(me).doesNotContainKeys("ticketId", "nickname");

        Map<String, Object> join = poll(room);
        assertThat(join.get("type")).isEqualTo("JOIN");
        assertThat(join.get("count")).isEqualTo(1);
        assertThat(map(join.get("passenger")).get("ticketNumber")).isEqualTo(roomState.get("myTicketNumber"));
    }

    @Test
    void 로그인_사용자는_계정_토큰으로_입장한다() throws Exception {
        Long userId = insertUser();
        long ticketId = insertTicketOwnedByUser("NO_REPLY", userId);
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + accountTokenProvider.issue(userId).accessToken());
        Client client = connect(headers);

        client.join("NO_REPLY", ticketId);

        assertThat(client.nextEvent().get("type")).isEqualTo("ROOM_STATE");
    }

    @Test
    void 남의_승차권이면_ERROR_TICKET_FORBIDDEN() throws Exception {
        long ticketId = insertTicket("RESULT", newGuestToken(), null, false);
        Client client = connectAsGuest(newGuestToken());

        client.join("RESULT", ticketId);

        assertError(client.nextEvent(), "TICKET_FORBIDDEN");
    }

    @Test
    void 끝난_승차권이면_ERROR_TICKET_ALREADY_ENDED() throws Exception {
        String guestToken = newGuestToken();
        long ticketId = insertTicket("RESULT", guestToken, null, true);
        Client client = connectAsGuest(guestToken);

        client.join("RESULT", ticketId);

        assertError(client.nextEvent(), "TICKET_ALREADY_ENDED");
    }

    @Test
    void 다른_대기실의_승차권이면_ERROR_TICKET_ROOM_MISMATCH() throws Exception {
        String guestToken = newGuestToken();
        long ticketId = insertTicket("RESULT", guestToken, null, false);
        Client client = connectAsGuest(guestToken);

        client.join("NO_REPLY", ticketId);

        assertError(client.nextEvent(), "TICKET_ROOM_MISMATCH");
    }

    @Test
    void 없는_승차권이면_ERROR_TICKET_NOT_FOUND() throws Exception {
        Client client = connectAsGuest(newGuestToken());

        client.join("RESULT", Long.MAX_VALUE);

        assertError(client.nextEvent(), "TICKET_NOT_FOUND");
    }

    @Test
    void 연결이_끊기면_방에_LEAVE() throws Exception {
        String stayerToken = newGuestToken();
        long stayerTicketId = insertTicket("GANZITHON_FINAL", stayerToken, null, false);
        Client stayer = connectAsGuest(stayerToken);
        BlockingQueue<Map<String, Object>> room = stayer.subscribe("/topic/rooms/GANZITHON_FINAL");
        stayer.join("GANZITHON_FINAL", stayerTicketId);
        assertThat(poll(room).get("type")).as("내 JOIN을 받으면 방 구독이 된 것").isEqualTo("JOIN");
        Client leaver = joinNewGuest("GANZITHON_FINAL");
        Map<String, Object> leaverJoin = poll(room);
        int leaverTicketNumber = (int) map(leaverJoin.get("passenger")).get("ticketNumber");

        leaver.session.disconnect();

        Map<String, Object> leave = poll(room);
        assertThat(leave.get("type")).isEqualTo("LEAVE");
        assertThat(leave.get("ticketNumber")).isEqualTo(leaverTicketNumber);
        assertThat(leave.get("seatNumber")).isEqualTo(2);
        assertThat(leave.get("count")).isEqualTo(1);
    }

    @Test
    void 좌석이_다_차면_서서_기다린다() throws Exception {
        joinNewGuest("RESULT");
        joinNewGuest("RESULT");
        String guestToken = newGuestToken();
        long ticketId = insertTicket("RESULT", guestToken, null, false);
        Client third = connectAsGuest(guestToken);

        third.join("RESULT", ticketId);

        Map<String, Object> roomState = third.nextEvent();
        Map<String, Object> me = list(roomState.get("passengers")).stream()
                .filter(passenger -> passenger.get("ticketNumber").equals(roomState.get("myTicketNumber")))
                .findFirst().orElseThrow();
        assertThat(me.get("seatNumber")).isNull();
    }

    @Test
    void 잘못된_계정_토큰이면_연결을_거부한다() {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer not-a-jwt");

        assertThatThrownBy(() -> connect(headers)).isInstanceOf(Exception.class);
    }

    // ── 도우미 ──

    private Client joinNewGuest(String roomCode) throws Exception {
        String guestToken = newGuestToken();
        long ticketId = insertTicket(roomCode, guestToken, null, false);
        Client client = connectAsGuest(guestToken);
        client.join(roomCode, ticketId);
        assertThat(client.nextEvent().get("type")).isEqualTo("ROOM_STATE");
        return client;
    }

    private Client connectAsGuest(String guestToken) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add(GuestTokenHeader.NAME, guestToken);
        return connect(headers);
    }

    private Client connect(StompHeaders connectHeaders) throws Exception {
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

    private long insertTicket(String roomCode, String guestToken, String moodChip, boolean ended) {
        return jdbcTemplate.queryForObject("""
                        insert into tickets (room_id, ticket_number, guest_token, mood_chip, pattern_id, status,
                                             end_reason, started_at, ended_at)
                        select r.id,
                               (select coalesce(max(t.ticket_number), 0) + 1 from tickets t where t.room_id = r.id),
                               ?, ?, (select id from patterns where code = 'PATTERN_01'), ?, ?, now(), ?
                        from rooms r
                        where r.code = ?
                        returning id
                        """, Long.class,
                guestToken, moodChip, ended ? "ENDED" : "WAITING", ended ? "LET_GO" : null,
                ended ? java.sql.Timestamp.from(java.time.Instant.now()) : null, roomCode);
    }

    private long insertTicketOwnedByUser(String roomCode, Long userId) {
        long ticketId = insertTicket(roomCode, newGuestToken(), null, false);
        jdbcTemplate.update("update tickets set user_id = ?, guest_token = null where id = ?", userId, ticketId);
        return ticketId;
    }

    private Long insertUser() {
        String nickname = UUID.randomUUID().toString().substring(0, 12);
        return jdbcTemplate.queryForObject(
                "insert into users (nickname, pin_hash) values (?, 'not-used') returning id", Long.class, nickname);
    }

    private boolean allRoomsEmpty() {
        return List.of("RESULT", "NO_REPLY", "GANZITHON_FINAL").stream()
                .allMatch(roomCode -> roomPassengerStore.findByRoomCode(roomCode).isEmpty());
    }

    private static String newGuestToken() {
        return UUID.randomUUID().toString();
    }

    private static void assertError(Map<String, Object> event, String code) {
        assertThat(event.get("type")).isEqualTo("ERROR");
        assertThat(event.get("code")).isEqualTo(code);
        assertThat(event).containsKeys("message", "serverTs").doesNotContainKey("retryAfterMs");
    }

    private static Map<String, Object> poll(BlockingQueue<Map<String, Object>> queue) throws InterruptedException {
        Map<String, Object> event = queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(event).as("%d초 안에 메시지가 와야 합니다", TIMEOUT_SECONDS).isNotNull();
        return event;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object value) {
        return (List<Map<String, Object>>) value;
    }

    /** STOMP 세션 하나 + 나에게 오는 메시지(/user/queue/events) */
    private static final class Client {

        private final StompSession session;
        private BlockingQueue<Map<String, Object>> events;

        private Client(StompSession session) {
            this.session = session;
        }

        BlockingQueue<Map<String, Object>> subscribe(String destination) {
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

        void join(String roomCode, long ticketId) {
            session.send("/app/rooms/" + roomCode + "/join", Map.of("ticketId", ticketId));
        }

        Map<String, Object> nextEvent() throws InterruptedException {
            return poll(events);
        }
    }
}
