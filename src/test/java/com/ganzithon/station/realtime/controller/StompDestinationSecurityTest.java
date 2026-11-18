package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.realtime.support.StompIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * STOMP 목적지 제한. 내장 브로커는 클라이언트가 /topic·/queue로 직접 보낸 메시지도 중계하므로,
 * SEND는 /app/**만, SUBSCRIBE는 /topic/rooms/**와 /user/queue/events만 받아야 합니다.
 */
class StompDestinationSecurityTest extends StompIntegrationTest {

    private static final long NOT_RECEIVED_WAIT_MS = 1_000;

    @Test
    void 방_토픽으로_직접_보낸_가짜_방송은_승객에게_가지_않는다() throws Exception {
        Client passenger = joinNewGuest("RESULT");
        BlockingQueue<Map<String, Object>> room = passenger.subscribe("/topic/rooms/RESULT");
        Client attacker = connect(new StompHeaders());

        attacker.session.send("/topic/rooms/RESULT",
                Map.of("type", "BROADCAST", "displayName", "운영자", "sentence", "<img src=x onerror=alert(1)>"));

        assertThat(room.poll(NOT_RECEIVED_WAIT_MS, TimeUnit.MILLISECONDS)).as("가짜 방송이 승객에게 도착하면 안 됨").isNull();
    }

    @Test
    void 브로커_큐는_직접_구독하거나_보낼_수_없다() throws Exception {
        Client attacker = connect(new StompHeaders());
        BlockingQueue<Map<String, Object>> queue = attacker.subscribe("/queue/anything");

        attacker.session.send("/queue/anything", Map.of("type", "BROADCAST"));

        assertThat(queue.poll(NOT_RECEIVED_WAIT_MS, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void 막힌_메시지를_보내도_연결은_유지되고_정상_경로는_그대로_동작한다() throws Exception {
        Client client = connect(new StompHeaders());
        client.session.send("/topic/rooms/RESULT", Map.of("type", "BROADCAST"));

        client.broadcast("RESULT", Map.of("situationChipCode", "RESULT_EXAM", "moodChip", "CALM"));

        assertError(client.nextEvent(), "REALTIME_NOT_JOINED");
    }
}
