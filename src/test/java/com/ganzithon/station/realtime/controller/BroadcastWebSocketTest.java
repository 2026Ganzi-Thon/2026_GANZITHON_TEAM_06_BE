package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.realtime.support.StompIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 조합형 방송(BR-01)과 전광판 송출(BR-02)을 실제 WebSocket(STOMP) 연결로 확인합니다.
 * 전광판 노출 시간은 300ms로 줄였고, 쿨다운은 실제 값(30초)입니다.
 * 이전 테스트의 방송이 늦게 도착할 수 있어서 BROADCAST는 broadcastId로 찾습니다.
 */
class BroadcastWebSocketTest extends StompIntegrationTest {

    private static final Map<String, Object> NO_REPLY_BROADCAST =
            Map.of("situationChipCode", "NO_REPLY_WAITING_REPLY", "moodChip", "FLUTTERING");

    @Test
    void 방송하면_나에게_ACCEPTED_방에_BROADCAST가_나가고_승차권에_기록된다() throws Exception {
        String guestToken = newGuestToken();
        long ticketId = insertTicket("NO_REPLY", guestToken, null, false);
        Client client = connectAsGuest(guestToken);
        BlockingQueue<Map<String, Object>> room = client.subscribe("/topic/rooms/NO_REPLY");
        client.join("NO_REPLY", ticketId);
        int myTicketNumber = (int) client.nextEvent().get("myTicketNumber");

        client.broadcast("NO_REPLY", NO_REPLY_BROADCAST);

        Map<String, Object> accepted = client.nextEvent();
        assertThat(accepted.get("type")).isEqualTo("BROADCAST_ACCEPTED");
        assertThat(accepted.get("cooldownMs")).isEqualTo(30000);
        assertThat(accepted.get("queuePosition")).isInstanceOf(Integer.class);
        assertThat(accepted.get("displayName")).isEqualTo(myTicketNumber + "번 두근두근한 양");

        Map<String, Object> broadcast = pollBroadcast(room, accepted.get("broadcastId"));
        assertThat(broadcast.get("ticketNumber")).isEqualTo(myTicketNumber);
        assertThat(broadcast.get("moodChip")).isEqualTo("FLUTTERING");
        assertThat(broadcast.get("situationChipCode")).isEqualTo("NO_REPLY_WAITING_REPLY");
        assertThat(broadcast.get("situationChipLabel")).isEqualTo("답장 기다리는 중");
        assertThat(broadcast.get("sentence"))
                .isEqualTo("안내 말씀 드립니다. " + myTicketNumber + "번 두근두근한 양님은 지금 '답장 기다리는 중'입니다.");
        assertThat(broadcast.get("displayMs")).isEqualTo(300);

        Map<String, Object> saved = jdbcTemplate.queryForMap("""
                select t.mood_chip, c.code as chip_code
                from tickets t join situation_chips c on c.id = t.last_situation_chip_id
                where t.id = ?""", ticketId);
        assertThat(saved).containsEntry("mood_chip", "FLUTTERING").containsEntry("chip_code", "NO_REPLY_WAITING_REPLY");
    }

    @Test
    void 쿨다운_안에_다시_방송하면_BROADCAST_COOLDOWN과_남은_시간() throws Exception {
        Client client = joinNewGuest("NO_REPLY");
        client.broadcast("NO_REPLY", NO_REPLY_BROADCAST);
        assertThat(client.nextEvent().get("type")).isEqualTo("BROADCAST_ACCEPTED");

        client.broadcast("NO_REPLY", NO_REPLY_BROADCAST);

        Map<String, Object> error = client.nextEvent();
        assertError(error, "BROADCAST_COOLDOWN");
        assertThat(((Number) error.get("retryAfterMs")).longValue()).isBetween(1L, 30_000L);
    }

    @Test
    void 다른_방의_칩이면_SITUATION_CHIP_NOT_FOUND이고_쿨다운은_되돌린다() throws Exception {
        Client client = joinNewGuest("NO_REPLY");

        client.broadcast("NO_REPLY", Map.of("situationChipCode", "RESULT_DOCUMENT", "moodChip", "CALM"));
        assertError(client.nextEvent(), "SITUATION_CHIP_NOT_FOUND");

        client.broadcast("NO_REPLY", NO_REPLY_BROADCAST);
        assertThat(client.nextEvent().get("type")).isEqualTo("BROADCAST_ACCEPTED");
    }

    @Test
    void 입장하지_않고_방송하면_REALTIME_NOT_JOINED() throws Exception {
        Client client = connectAsGuest(newGuestToken());

        client.broadcast("NO_REPLY", NO_REPLY_BROADCAST);

        assertError(client.nextEvent(), "REALTIME_NOT_JOINED");
    }

    @Test
    void 다른_방에서_방송하면_REALTIME_NOT_JOINED() throws Exception {
        Client client = joinNewGuest("RESULT");

        client.broadcast("NO_REPLY", NO_REPLY_BROADCAST);

        assertError(client.nextEvent(), "REALTIME_NOT_JOINED");
    }

    @Test
    void 상태_칩이_없으면_COMMON_INVALID_INPUT() throws Exception {
        Client client = joinNewGuest("NO_REPLY");

        client.broadcast("NO_REPLY", Map.of("situationChipCode", "NO_REPLY_WAITING_REPLY"));

        assertError(client.nextEvent(), "COMMON_INVALID_INPUT");
    }

    @Test
    void 연달아_방송하면_노출_시간_간격으로_순서대로_나간다() throws Exception {
        Client first = joinNewGuest("GANZITHON_FINAL");
        BlockingQueue<Map<String, Object>> room = first.subscribe("/topic/rooms/GANZITHON_FINAL");
        Client second = joinNewGuest("GANZITHON_FINAL");
        Map<String, Object> body = Map.of("situationChipCode", "FINAL_JUDGING", "moodChip", "NERVOUS");

        first.broadcast("GANZITHON_FINAL", body);
        Object firstId = first.nextEvent().get("broadcastId");
        second.broadcast("GANZITHON_FINAL", body);
        Object secondId = second.nextEvent().get("broadcastId");

        Map<String, Object> firstBroadcast = pollBroadcast(room, firstId);
        Map<String, Object> secondBroadcast = pollBroadcast(room, secondId);
        long gap = ((Number) secondBroadcast.get("serverTs")).longValue()
                - ((Number) firstBroadcast.get("serverTs")).longValue();
        assertThat(gap).isGreaterThanOrEqualTo(300);
    }

    @Test
    void 방송_뒤에_들어온_승객은_바뀐_호칭을_본다() throws Exception {
        Client broadcaster = joinNewGuest("RESULT");
        broadcaster.broadcast("RESULT", Map.of("situationChipCode", "RESULT_DOCUMENT", "moodChip", "TIRED"));
        String changedName = (String) broadcaster.nextEvent().get("displayName");

        String guestToken = newGuestToken();
        long ticketId = insertTicket("RESULT", guestToken, null, false);
        Client newcomer = connectAsGuest(guestToken);
        newcomer.join("RESULT", ticketId);

        List<Map<String, Object>> passengers = list(newcomer.nextEvent().get("passengers"));
        assertThat(passengers).anySatisfy(passenger -> {
            assertThat(passenger.get("displayName")).isEqualTo(changedName);
            assertThat(passenger.get("moodChip")).isEqualTo("TIRED");
        });
    }

    private static Map<String, Object> pollBroadcast(BlockingQueue<Map<String, Object>> room, Object broadcastId)
            throws InterruptedException {
        return pollUntil(room, event -> "BROADCAST".equals(event.get("type"))
                && broadcastId.equals(event.get("broadcastId")));
    }
}
