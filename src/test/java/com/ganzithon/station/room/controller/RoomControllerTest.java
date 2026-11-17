package com.ganzithon.station.room.controller;

import com.ganzithon.station.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 대기실 API. 실제 Postgres(시드 데이터 포함)와 보안 설정까지 거쳐서 확인합니다.
 * 새 조회 API 테스트도 이 모양으로 만들면 됩니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 대기실_목록은_토큰_없이_순서대로_3개() throws Exception {
        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rooms", hasSize(3)))
                .andExpect(jsonPath("$.data.rooms[0].roomCode").value("RESULT"))
                .andExpect(jsonPath("$.data.rooms[1].roomCode").value("NO_REPLY"))
                .andExpect(jsonPath("$.data.rooms[2].roomCode").value("GANZITHON_FINAL"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void 대기실_단건_조회() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomCode}", "NO_REPLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomCode").value("NO_REPLY"))
                .andExpect(jsonPath("$.data.name").value("읽씹 방"))
                .andExpect(jsonPath("$.data.slogan").value("서두르지 않아도 괜찮아요. 여기서는 마음의 속도를 낮춰도 돼요."))
                .andExpect(jsonPath("$.data.retrospectQuestion1").value(nullValue()))
                .andExpect(jsonPath("$.data.npcEmpathyCopy").value(nullValue()));
    }

    @Test
    void 상황_칩_목록은_그_방의_칩만_순서대로() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomCode}/situation-chips", "NO_REPLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.situationChips", hasSize(6)))
                .andExpect(jsonPath("$.data.situationChips[0].situationChipCode").value("NO_REPLY_READ_LONG_AGO"))
                .andExpect(jsonPath("$.data.situationChips[0].label").value("읽음 표시 뜬 지 한참째"))
                .andExpect(jsonPath("$.data.situationChips[*].situationChipCode",
                        everyItem(startsWith("NO_REPLY_"))));
    }

    @Test
    void 없는_대기실의_상황_칩_목록은_404_ROOM_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomCode}/situation-chips", "NOT_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void 없는_대기실은_404_ROOM_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomCode}", "NOT_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }
}
