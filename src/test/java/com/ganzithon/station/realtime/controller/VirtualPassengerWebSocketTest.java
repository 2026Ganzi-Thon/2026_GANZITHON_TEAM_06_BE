package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.realtime.support.StompIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가상 승객 자동 방송 (RM-05). 실제로 입장하면 가상 승객의 BROADCAST가 방에 나옵니다.
 * 이 테스트만 가상 승객을 켜고 첫 방송을 1초로 줄였습니다.
 */
@TestPropertySource(properties = {
        "station.virtual-passenger.enabled=true",
        "station.virtual-passenger.first-delay=1s"
})
class VirtualPassengerWebSocketTest extends StompIntegrationTest {

    @Test
    void 입장하면_잠시_뒤_그_방_가상_승객의_방송이_나온다() throws Exception {
        Client passenger = joinNewGuest("NO_REPLY");
        BlockingQueue<Map<String, Object>> room = passenger.subscribe("/topic/rooms/NO_REPLY");

        Map<String, Object> broadcast = pollUntil(room, event -> "BROADCAST".equals(event.get("type")));

        assertThat((int) broadcast.get("ticketNumber")).as("가상 승객 번호는 1~99").isBetween(1, 99);
        assertThat((String) broadcast.get("situationChipCode")).startsWith("NO_REPLY_");
        assertThat((String) broadcast.get("sentence"))
                .startsWith("안내 말씀 드립니다. " + broadcast.get("displayName") + "님은 지금 '");
        assertThat(broadcast.get("broadcastId")).isNotNull();
    }
}
