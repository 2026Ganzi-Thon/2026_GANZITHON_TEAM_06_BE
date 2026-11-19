package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.global.security.AccountTokenProvider;
import com.ganzithon.station.realtime.support.StompIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 승객 확대 카드 (MY-05). 공개한 승객은 조각 도안·승차권 번호·지나간 열차 수, 비공개·비로그인은 머문 시간과 상태 칩만.
 * 닉네임과 종료 표식은 어떤 경우에도 보내지 않습니다.
 */
class PassengerCardWebSocketTest extends StompIntegrationTest {

    @Autowired
    AccountTokenProvider accountTokenProvider;

    @Test
    void 공개한_승객은_조각_도안_최신순과_열차_수가_오고_닉네임과_종료_표식은_없다() throws Exception {
        Long userId = insertUser();
        jdbcTemplate.update("update users set is_public = true where id = ?", userId);
        insertPiece(userId, "PATTERN_HEART", "2026-11-10T10:00:00Z");
        insertPiece(userId, "PATTERN_STAR", "2026-11-12T10:00:00Z");
        Joined target = joinAsUser("RESULT", userId, "CALM");
        Client viewer = joinNewGuest("RESULT");

        viewer.session.send("/app/rooms/RESULT/passenger-card", Map.of("ticketNumber", target.ticketNumber()));

        Map<String, Object> card = viewer.nextEvent();
        assertThat(card.get("type")).isEqualTo("PASSENGER_CARD");
        assertThat(card.get("ticketNumber")).isEqualTo(target.ticketNumber());
        assertThat(card.get("displayName")).isEqualTo(target.ticketNumber() + "번 고요한 양");
        assertThat(card.get("moodChip")).isEqualTo("CALM");
        assertThat(card.get("visibility")).isEqualTo("PUBLIC");
        assertThat(card.get("patternCodes")).isEqualTo(List.of("PATTERN_STAR", "PATTERN_HEART"));
        assertThat(card.get("trainCount")).isInstanceOf(Integer.class);
        assertThat(card).containsKeys("startedTs", "serverTs").doesNotContainKeys("nickname", "endReason", "message");
    }

    @Test
    void 비공개_승객은_머문_시간과_상태_칩만_온다() throws Exception {
        Long userId = insertUser();
        insertPiece(userId, "PATTERN_HEART", "2026-11-10T10:00:00Z");
        Joined target = joinAsUser("NO_REPLY", userId, "TIRED");
        Client viewer = joinNewGuest("NO_REPLY");

        Map<String, Object> card = requestCard(viewer, "NO_REPLY", target.ticketNumber());

        assertThat(card.get("visibility")).isEqualTo("PRIVATE");
        assertThat(card.get("moodChip")).isEqualTo("TIRED");
        assertThat(card.get("startedTs")).isNotNull();
        assertThat(card.get("patternCodes")).isNull();
        assertThat(card.get("trainCount")).isNull();
    }

    @Test
    void 공개했지만_조각이_없으면_NO_PIECE_비로그인_승객은_GUEST() throws Exception {
        Long userId = insertUser();
        jdbcTemplate.update("update users set is_public = true where id = ?", userId);
        Joined noPiece = joinAsUser("GANZITHON_FINAL", userId, null);
        Joined guest = joinAsGuest("GANZITHON_FINAL");

        assertThat(requestCard(guest.client(), "GANZITHON_FINAL", noPiece.ticketNumber()).get("visibility")).isEqualTo("NO_PIECE");

        Map<String, Object> guestCard = requestCard(noPiece.client(), "GANZITHON_FINAL", guest.ticketNumber());
        assertThat(guestCard.get("visibility")).isEqualTo("GUEST");
        assertThat(guestCard.get("patternCodes")).isNull();
        assertThat(guestCard.get("trainCount")).isNull();
    }

    @Test
    void 입장하지_않았으면_REALTIME_NOT_JOINED_방에_없는_번호면_REALTIME_PASSENGER_NOT_FOUND() throws Exception {
        Client outsider = connectAsGuest(newGuestToken());
        outsider.session.send("/app/rooms/RESULT/passenger-card", Map.of("ticketNumber", 100));
        assertError(outsider.nextEvent(), "REALTIME_NOT_JOINED");

        Client viewer = joinNewGuest("RESULT");
        viewer.session.send("/app/rooms/RESULT/passenger-card", Map.of("ticketNumber", 7)); // 가상 승객 번호
        assertError(viewer.nextEvent(), "REALTIME_PASSENGER_NOT_FOUND");
    }

    @Test
    void 승차권_번호가_없으면_COMMON_INVALID_INPUT() throws Exception {
        Client viewer = joinNewGuest("RESULT");

        viewer.session.send("/app/rooms/RESULT/passenger-card", Map.of());

        assertError(viewer.nextEvent(), "COMMON_INVALID_INPUT");
    }

    private static Map<String, Object> requestCard(Client viewer, String roomCode, int ticketNumber) throws InterruptedException {
        viewer.session.send("/app/rooms/" + roomCode + "/passenger-card", Map.of("ticketNumber", ticketNumber));
        Map<String, Object> card = viewer.nextEvent();
        assertThat(card.get("type")).isEqualTo("PASSENGER_CARD");
        return card;
    }

    private Joined joinAsGuest(String roomCode) throws Exception {
        String guestToken = newGuestToken();
        long ticketId = insertTicket(roomCode, guestToken, null, false);
        Client client = connectAsGuest(guestToken);
        client.join(roomCode, ticketId);
        Map<String, Object> roomState = client.nextEvent();
        assertThat(roomState.get("type")).isEqualTo("ROOM_STATE");
        return new Joined(client, (int) roomState.get("myTicketNumber"));
    }

    /** 계정 승차권으로 입장. moodChip이 null이면 칩 없음 */
    private Joined joinAsUser(String roomCode, Long userId, String moodChip) throws Exception {
        long ticketId = insertTicket(roomCode, newGuestToken(), moodChip, false);
        jdbcTemplate.update("update tickets set user_id = ?, guest_token = null where id = ?", userId, ticketId);
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + accountTokenProvider.issue(userId).accessToken());
        Client client = connect(headers);
        client.join(roomCode, ticketId);
        Map<String, Object> roomState = client.nextEvent();
        assertThat(roomState.get("type")).isEqualTo("ROOM_STATE");
        return new Joined(client, (int) roomState.get("myTicketNumber"));
    }

    /** 그 계정의 끝난 승차권 하나와 조각 하나 */
    private void insertPiece(Long userId, String patternCode, String createdAt) {
        long ticketId = insertTicket("RESULT", newGuestToken(), null, true);
        jdbcTemplate.update("update tickets set user_id = ? where id = ?", userId, ticketId);
        jdbcTemplate.update("""
                        insert into blanket_pieces (ticket_id, user_id, pattern_id, ticket_number, waiting_name, train_count,
                                                    end_reason, created_at)
                        select t.id, ?, p.id, t.ticket_number, '결과 발표 방', 3, 'LET_GO', ?
                        from tickets t, patterns p where t.id = ? and p.code = ?
                        """, userId, Timestamp.from(Instant.parse(createdAt)), ticketId, patternCode);
    }

    private record Joined(Client client, int ticketNumber) {
    }
}
