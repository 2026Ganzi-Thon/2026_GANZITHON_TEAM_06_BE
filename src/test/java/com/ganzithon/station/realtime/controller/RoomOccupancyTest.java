package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.realtime.support.StompIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 방 선택 화면(ON-02)의 대기실별 현재 인원. 실제로 STOMP로 입장시킨 뒤 REST로 조회합니다 (좌석 수 2).
 */
class RoomOccupancyTest extends StompIntegrationTest {

    @LocalServerPort
    int serverPort;

    @Test
    void 토큰_없이_모든_대기실의_착석_입석_인원을_대기실_순서대로_준다() throws Exception {
        joinNewGuest("RESULT");
        joinNewGuest("RESULT");
        joinNewGuest("RESULT");
        joinNewGuest("GANZITHON_FINAL");

        List<Map<String, Object>> rooms = getOccupancy();

        assertThat(rooms).extracting(room -> room.get("roomCode"))
                .containsExactly("RESULT", "NO_REPLY", "GANZITHON_FINAL");
        assertThat(rooms.get(0)).containsEntry("count", 3).containsEntry("seatedCount", 2)
                .containsEntry("standingCount", 1).containsEntry("seatCount", 2);
        assertThat(rooms.get(1)).containsEntry("count", 0).containsEntry("seatedCount", 0)
                .containsEntry("standingCount", 0);
        assertThat(rooms.get(2)).containsEntry("count", 1).containsEntry("seatedCount", 1)
                .containsEntry("standingCount", 0);
    }

    @Test
    void 나간_승객은_세지_않는다() throws Exception {
        Client leaver = joinNewGuest("NO_REPLY");
        joinNewGuest("NO_REPLY");

        leaver.session.disconnect();

        long deadline = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000;
        Map<String, Object> noReply = getOccupancy().get(1);
        while (!Integer.valueOf(1).equals(noReply.get("count")) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            noReply = getOccupancy().get(1);
        }
        assertThat(noReply).containsEntry("count", 1).containsEntry("seatedCount", 1);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getOccupancy() {
        Map<String, Object> body = RestClient.create("http://localhost:" + serverPort)
                .get().uri("/api/v1/rooms/occupancy")
                .retrieve()
                .body(Map.class);
        assertThat(body).containsEntry("success", true);
        return (List<Map<String, Object>>) ((Map<String, Object>) body.get("data")).get("rooms");
    }
}
