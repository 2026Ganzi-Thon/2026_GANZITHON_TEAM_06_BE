package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.realtime.support.StompIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 1초 리액션 (BR-03): 전광판 방송에 끄덕임·짠함으로 반응합니다. 방송 도중 입장했을 때의 전광판(BR-02)도 여기서 확인합니다.
 * 방송이 나와 있는 동안 반응·입장해야 해서 이 테스트만 노출 시간을 3초로 늘렸습니다.
 * "보내지 않음"은 그다음 리액션이 먼저 도착하는지로 확인합니다 (같은 방 메시지는 순서대로 옴).
 */
@TestPropertySource(properties = "station.realtime.broadcast-display=3s")
class ReactionWebSocketTest extends StompIntegrationTest {

    @Test
    void 방송에_반응하면_방에_반응한_양과_방송의_리액션_수가_간다() throws Exception {
        Joined broadcaster = join("NO_REPLY");
        Joined reactor = join("NO_REPLY");
        BlockingQueue<Map<String, Object>> room = broadcaster.client().subscribe("/topic/rooms/NO_REPLY");
        broadcaster.client().broadcast("NO_REPLY",
                Map.of("situationChipCode", "NO_REPLY_WAITING_REPLY", "moodChip", "CALM"));
        String broadcastId = (String) pollUntil(room, event -> "BROADCAST".equals(event.get("type"))).get("broadcastId");

        reactor.client().react("NO_REPLY", Map.of("broadcastId", broadcastId, "reaction", "NOD"));

        Map<String, Object> reaction = nextReaction(room);
        assertThat(reaction.get("broadcastId")).isEqualTo(broadcastId);
        assertThat(reaction.get("ticketNumber")).isEqualTo(reactor.ticketNumber());
        assertThat(reaction.get("reaction")).isEqualTo("NOD");
        assertThat(reaction.get("nodCount")).isEqualTo(1);
        assertThat(reaction.get("awwCount")).isEqualTo(0);
        assertThat(reaction).containsKey("serverTs");
    }

    @Test
    void 일초_안의_연타는_버리고_숫자는_한_명당_종류마다_한_번만_센다() throws Exception {
        Joined broadcaster = join("GANZITHON_FINAL");
        Joined reactor = join("GANZITHON_FINAL");
        BlockingQueue<Map<String, Object>> room = broadcaster.client().subscribe("/topic/rooms/GANZITHON_FINAL");
        broadcaster.client().broadcast("GANZITHON_FINAL",
                Map.of("situationChipCode", "FINAL_JUDGING", "moodChip", "NERVOUS"));
        String broadcastId = (String) pollUntil(room, event -> "BROADCAST".equals(event.get("type"))).get("broadcastId");

        reactor.client().react("GANZITHON_FINAL", Map.of("broadcastId", broadcastId, "reaction", "NOD"));
        reactor.client().react("GANZITHON_FINAL", Map.of("broadcastId", broadcastId, "reaction", "AWW")); // 1초 안 → 버림
        // 같은 세션 메시지는 순서대로 처리되므로, 이 ERROR가 오면 앞의 연타 처리가 끝난 것 (다른 세션과는 순서 보장 없음)
        reactor.client().react("GANZITHON_FINAL", Map.of("reaction", "NOD"));
        assertError(reactor.client().nextEvent(), "COMMON_INVALID_INPUT");
        assertThat(nextReaction(room)).containsEntry("ticketNumber", reactor.ticketNumber())
                .containsEntry("nodCount", 1).containsEntry("awwCount", 0);

        broadcaster.client().react("GANZITHON_FINAL", Map.of("broadcastId", broadcastId, "reaction", "AWW"));

        assertThat(nextReaction(room)).as("버려진 연타 대신 다음 승객의 리액션이 옴 (내 방송에도 반응 가능)")
                .containsEntry("ticketNumber", broadcaster.ticketNumber())
                .containsEntry("nodCount", 1).containsEntry("awwCount", 1);

        Thread.sleep(1_100);
        reactor.client().react("GANZITHON_FINAL", Map.of("broadcastId", broadcastId, "reaction", "NOD"));

        assertThat(nextReaction(room)).as("1초 뒤에는 다시 보내지지만 같은 승객의 끄덕임은 다시 세지 않음")
                .containsEntry("ticketNumber", reactor.ticketNumber())
                .containsEntry("nodCount", 1).containsEntry("awwCount", 1);
    }

    @Test
    void 방송_도중에_입장하면_ROOM_STATE에_지금_전광판_방송과_리액션_수가_온다() throws Exception {
        Joined broadcaster = join("NO_REPLY");
        Joined reactor = join("NO_REPLY");
        BlockingQueue<Map<String, Object>> room = broadcaster.client().subscribe("/topic/rooms/NO_REPLY");
        broadcaster.client().broadcast("NO_REPLY",
                Map.of("situationChipCode", "NO_REPLY_WAITING_REPLY", "moodChip", "FLUTTERING"));
        Map<String, Object> broadcast = pollUntil(room, event -> "BROADCAST".equals(event.get("type")));
        reactor.client().react("NO_REPLY", Map.of("broadcastId", broadcast.get("broadcastId"), "reaction", "AWW"));
        nextReaction(room);

        String guestToken = newGuestToken();
        long ticketId = insertTicket("NO_REPLY", guestToken, null, false);
        Client latecomer = connectAsGuest(guestToken);
        latecomer.join("NO_REPLY", ticketId);
        Map<String, Object> roomState = latecomer.nextEvent();

        assertThat(roomState.get("type")).isEqualTo("ROOM_STATE");
        Map<String, Object> onAir = map(roomState.get("onAirBroadcast"));
        assertThat(onAir.get("broadcastId")).isEqualTo(broadcast.get("broadcastId"));
        assertThat(onAir.get("ticketNumber")).isEqualTo(broadcaster.ticketNumber());
        assertThat(onAir.get("displayName")).isEqualTo(broadcast.get("displayName"));
        assertThat(onAir.get("sentence")).isEqualTo(broadcast.get("sentence"));
        assertThat(onAir.get("situationChipCode")).isEqualTo("NO_REPLY_WAITING_REPLY");
        assertThat(((Number) onAir.get("remainingMs")).longValue()).isBetween(1L, 3_000L);
        assertThat(onAir.get("nodCount")).isEqualTo(0);
        assertThat(onAir.get("awwCount")).isEqualTo(1);
    }

    @Test
    void 지금_나오지_않는_방송에_한_리액션은_방에_가지_않는다() throws Exception {
        Joined reactor = join("RESULT");
        BlockingQueue<Map<String, Object>> room = reactor.client().subscribe("/topic/rooms/RESULT");

        reactor.client().react("RESULT", Map.of("broadcastId", "not-on-air", "reaction", "NOD"));
        reactor.client().broadcast("RESULT", Map.of("situationChipCode", "RESULT_EXAM", "moodChip", "CALM"));

        assertThat(pollUntil(room, event -> "REACTION".equals(event.get("type"))
                || "BROADCAST".equals(event.get("type"))).get("type"))
                .as("버려진 리액션보다 뒤에 보낸 방송이 먼저 옴").isEqualTo("BROADCAST");
    }

    @Test
    void 입장하지_않고_리액션하면_REALTIME_NOT_JOINED() throws Exception {
        Client client = connectAsGuest(newGuestToken());

        client.react("RESULT", Map.of("broadcastId", "x", "reaction", "NOD"));

        assertError(client.nextEvent(), "REALTIME_NOT_JOINED");
    }

    @Test
    void 방송_ID나_리액션_종류가_없거나_목록에_없으면_COMMON_INVALID_INPUT() throws Exception {
        Client client = joinNewGuest("RESULT");

        client.react("RESULT", Map.of("reaction", "NOD"));
        assertError(client.nextEvent(), "COMMON_INVALID_INPUT");

        client.react("RESULT", Map.of("broadcastId", "x"));
        assertError(client.nextEvent(), "COMMON_INVALID_INPUT");

        client.react("RESULT", Map.of("broadcastId", "x", "reaction", "HEART"));
        assertError(client.nextEvent(), "COMMON_INVALID_INPUT");
    }

    private Joined join(String roomCode) throws Exception {
        String guestToken = newGuestToken();
        long ticketId = insertTicket(roomCode, guestToken, null, false);
        Client client = connectAsGuest(guestToken);
        client.join(roomCode, ticketId);
        Map<String, Object> roomState = client.nextEvent();
        assertThat(roomState.get("type")).isEqualTo("ROOM_STATE");
        return new Joined(client, (int) roomState.get("myTicketNumber"));
    }

    private static Map<String, Object> nextReaction(BlockingQueue<Map<String, Object>> room) throws InterruptedException {
        return pollUntil(room, event -> "REACTION".equals(event.get("type")));
    }

    private record Joined(Client client, int ticketNumber) {
    }
}
