package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.global.security.AccountTokenProvider;
import com.ganzithon.station.realtime.support.StompIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 대기실 입장(ON-05)을 실제 WebSocket(STOMP) 연결로 확인합니다. 승차권은 DB에 직접 넣습니다.
 * 좌석 수는 2로 줄여서 "다 차면 서서 기다리기"를 확인합니다.
 */
class RoomJoinWebSocketTest extends StompIntegrationTest {

    @Autowired
    AccountTokenProvider accountTokenProvider;

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

        Map<String, Object> join = pollUntil(room, event -> "JOIN".equals(event.get("type")));
        assertThat(join.get("count")).isEqualTo(1);
        assertThat(map(join.get("passenger")).get("ticketNumber")).isEqualTo(roomState.get("myTicketNumber"));
    }

    @Test
    void 로그인_사용자는_계정_토큰으로_입장한다() throws Exception {
        Long userId = insertUser();
        long ticketId = insertTicket("NO_REPLY", newGuestToken(), null, false);
        jdbcTemplate.update("update tickets set user_id = ?, guest_token = null where id = ?", userId, ticketId);
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

        Map<String, Object> error = client.nextEvent();
        assertError(error, "TICKET_FORBIDDEN");
        assertThat(error).doesNotContainKey("retryAfterMs");
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
        assertThat(pollUntil(room, event -> "JOIN".equals(event.get("type"))))
                .as("내 JOIN을 받으면 방 구독이 된 것").isNotNull();
        Client leaver = joinNewGuest("GANZITHON_FINAL");
        Map<String, Object> leaverJoin = pollUntil(room, event -> "JOIN".equals(event.get("type")));
        int leaverTicketNumber = (int) map(leaverJoin.get("passenger")).get("ticketNumber");

        leaver.session.disconnect();

        Map<String, Object> leave = pollUntil(room, event -> "LEAVE".equals(event.get("type")));
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
}
