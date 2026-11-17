package com.ganzithon.station.ticket.controller;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.blanket.dto.request.BlanketPieceCreateRequest;
import com.ganzithon.station.blanket.service.BlanketService;
import com.ganzithon.station.global.security.AccountTokenProvider;
import com.ganzithon.station.global.security.GuestTokenHeader;
import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.service.UserService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 승차권 발급·기다림 종료·전광판 통계. 시드 데이터가 들어간 실제 Postgres로 확인합니다.
 * 아직 구현 전인 UserService.getUserEntity, BlanketService.createPiece(상진)는 이 테스트에서만 대신합니다.
 * 구현이 머지되면 두 스텁을 지워도 테스트가 그대로 통과해야 합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountTokenProvider accountTokenProvider;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @MockitoSpyBean
    UserService userService;

    @MockitoSpyBean
    BlanketService blanketService;

    @BeforeEach
    void stubOtherDomainTodos() {
        doAnswer(invocation -> entityManager.getReference(User.class, invocation.getArgument(0)))
                .when(userService).getUserEntity(anyLong());
        doReturn(null).when(blanketService).createPiece(any());
        clearInvocations(blanketService);
    }

    @Test
    void 비로그인_발급은_201과_게스트_토큰_호칭_도안을_준다() throws Exception {
        issue("{\"roomCode\": \"RESULT\", \"moodChip\": \"FLUTTERING\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticketId").isNumber())
                .andExpect(jsonPath("$.data.roomCode").value("RESULT"))
                .andExpect(jsonPath("$.data.moodChip").value("FLUTTERING"))
                .andExpect(jsonPath("$.data.displayName").value(matchesPattern("\\d+번 두근두근한 양")))
                .andExpect(jsonPath("$.data.patternCode").value(matchesPattern("PATTERN_0[1-3]")))
                .andExpect(jsonPath("$.data.guestToken").value(matchesPattern("[A-Za-z0-9_-]{43}")))
                .andExpect(jsonPath("$.data.startedAt").value(matchesPattern(".*\\+09:00")))
                .andExpect(jsonPath("$.data.serverTime").isString())
                .andExpect(jsonPath("$.data.trainIntervalMs").value(15000));
    }

    @Test
    void 상태_칩을_건너뛰면_호칭은_번호와_양만() throws Exception {
        issue("{\"roomCode\": \"NO_REPLY\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.moodChip").value(nullValue()))
                .andExpect(jsonPath("$.data.displayName").value(matchesPattern("\\d+번 양")));
    }

    @Test
    void 같은_대기실은_번호가_1씩_올라간다() throws Exception {
        int first = ticketNumber(issue("{\"roomCode\": \"GANZITHON_FINAL\"}"));
        int second = ticketNumber(issue("{\"roomCode\": \"GANZITHON_FINAL\"}"));

        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    void 이전에_받은_게스트_토큰을_보내면_다시_쓴다() throws Exception {
        String guestToken = guestToken(issue("{\"roomCode\": \"RESULT\"}"));

        issue("{\"roomCode\": \"NO_REPLY\"}", guestToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.guestToken").value(guestToken));
    }

    @Test
    void 처음_보는_게스트_토큰은_쓰지_않고_새로_만든다() throws Exception {
        String unknownToken = "made-up-by-client";

        String guestToken = guestToken(issue("{\"roomCode\": \"RESULT\"}", unknownToken));

        assertThat(guestToken).isNotEqualTo(unknownToken).hasSize(43);
    }

    @Test
    void 로그인_발급은_계정_소유이고_게스트_토큰이_없다() throws Exception {
        Long userId = createUser();

        String body = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + accountTokenProvider.issue(userId).accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomCode\": \"RESULT\", \"moodChip\": \"CALM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.guestToken").value(nullValue()))
                .andExpect(jsonPath("$.data.displayName").value(matchesPattern("\\d+번 고요한 양")))
                .andReturn().getResponse().getContentAsString();

        Long ticketId = ((Number) JsonPath.read(body, "$.data.ticketId")).longValue();
        assertThat(jdbcTemplate.queryForObject("select user_id from tickets where id = ?", Long.class, ticketId))
                .isEqualTo(userId);
    }

    @Test
    void 없는_대기실은_404_ROOM_NOT_FOUND() throws Exception {
        issue("{\"roomCode\": \"NOT_EXIST\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void 대기실_코드가_없으면_400() throws Exception {
        issue("{\"moodChip\": \"CALM\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("roomCode"));
    }

    @Test
    void 목록에_없는_상태_칩은_400() throws Exception {
        issue("{\"roomCode\": \"RESULT\", \"moodChip\": \"HAPPY\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
    }

    // ── 기다림 종료 ──

    @Test
    void 비로그인_종료는_조각_내용을_주고_계정에_저장하지_않는다() throws Exception {
        ResultActions issued = issue("{\"roomCode\": \"RESULT\"}");
        Long ticketId = ticketId(issued);
        // 46초 전에 시작한 기다림 → 15초 간격 열차가 3대 지나감
        jdbcTemplate.update("update tickets set started_at = now() - interval '46 seconds' where id = ?", ticketId);

        end(ticketId, guestToken(issued), "{\"endReason\": \"ANSWERED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketId").value(ticketId))
                .andExpect(jsonPath("$.data.patternCode").value(matchesPattern("PATTERN_0[1-3]")))
                .andExpect(jsonPath("$.data.ticketNumber").value(ticketNumber(issued)))
                .andExpect(jsonPath("$.data.waitingName").value("결과발표"))
                .andExpect(jsonPath("$.data.trainCount").value(3))
                .andExpect(jsonPath("$.data.endReason").value("ANSWERED"))
                .andExpect(jsonPath("$.data.endedAt").value(matchesPattern(".*\\+09:00")))
                .andExpect(jsonPath("$.data.savedToAccount").value(false));

        assertThat(jdbcTemplate.queryForMap("select status, end_reason, ended_at from tickets where id = ?", ticketId))
                .containsEntry("status", "ENDED")
                .containsEntry("end_reason", "ANSWERED")
                .extractingByKey("ended_at").isNotNull();
        verify(blanketService, never()).createPiece(any());
    }

    @Test
    void 방송한_적이_있으면_기다림_이름은_마지막_상황_칩() throws Exception {
        ResultActions issued = issue("{\"roomCode\": \"RESULT\"}");
        Long ticketId = ticketId(issued);
        jdbcTemplate.update("update tickets set last_situation_chip_id = "
                + "(select id from situation_chips where code = 'RESULT_EXAM') where id = ?", ticketId);

        end(ticketId, guestToken(issued), "{\"endReason\": \"LET_GO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waitingName").value("시험 결과 기다리는 중"));
    }

    @Test
    void 로그인_종료는_서버_기록으로_계정에_조각을_저장한다() throws Exception {
        Long userId = createUser();
        String accessToken = accountTokenProvider.issue(userId).accessToken();
        String body = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomCode\": \"NO_REPLY\"}"))
                .andReturn().getResponse().getContentAsString();
        Long ticketId = ((Number) JsonPath.read(body, "$.data.ticketId")).longValue();
        int ticketNumber = JsonPath.read(body, "$.data.ticketNumber");
        Long patternId = jdbcTemplate.queryForObject("select pattern_id from tickets where id = ?", Long.class, ticketId);
        String roomName = jdbcTemplate.queryForObject("select name from rooms where code = 'NO_REPLY'", String.class);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/end", ticketId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endReason\": \"UNEXPECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedToAccount").value(true));

        ArgumentCaptor<BlanketPieceCreateRequest> captor = ArgumentCaptor.forClass(BlanketPieceCreateRequest.class);
        verify(blanketService).createPiece(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new BlanketPieceCreateRequest(
                ticketId, userId, patternId, ticketNumber, roomName, 0, EndReason.UNEXPECTED));
    }

    @Test
    void 이미_끝난_기다림을_다시_종료하면_409() throws Exception {
        ResultActions issued = issue("{\"roomCode\": \"RESULT\"}");
        Long ticketId = ticketId(issued);
        String guestToken = guestToken(issued);
        end(ticketId, guestToken, "{\"endReason\": \"ANSWERED\"}").andExpect(status().isOk());

        end(ticketId, guestToken, "{\"endReason\": \"LET_GO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_ALREADY_ENDED"));
        assertThat(jdbcTemplate.queryForObject("select end_reason from tickets where id = ?", String.class, ticketId))
                .isEqualTo("ANSWERED");
    }

    @Test
    void 남의_승차권을_종료하면_403() throws Exception {
        Long ticketId = ticketId(issue("{\"roomCode\": \"RESULT\"}"));
        String otherGuestToken = guestToken(issue("{\"roomCode\": \"RESULT\"}"));

        end(ticketId, otherGuestToken, "{\"endReason\": \"ANSWERED\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("TICKET_FORBIDDEN"));
    }

    @Test
    void 없는_승차권을_종료하면_404() throws Exception {
        String guestToken = guestToken(issue("{\"roomCode\": \"RESULT\"}"));

        end(Long.MAX_VALUE, guestToken, "{\"endReason\": \"ANSWERED\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void 종료_사유가_없거나_목록에_없으면_400() throws Exception {
        ResultActions issued = issue("{\"roomCode\": \"RESULT\"}");
        Long ticketId = ticketId(issued);
        String guestToken = guestToken(issued);

        end(ticketId, guestToken, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
        end(ticketId, guestToken, "{\"endReason\": \"BORED\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
    }

    // ── 전광판 통계 ──

    @Test
    void 종료하면_그_사유의_통계가_1_오르고_토큰_없이_조회한다() throws Exception {
        String before = stats().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        ResultActions issued = issue("{\"roomCode\": \"RESULT\"}");

        end(ticketId(issued), guestToken(issued), "{\"endReason\": \"LET_GO\"}").andExpect(status().isOk());

        stats()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answeredCount").value(count(before, "answeredCount")))
                .andExpect(jsonPath("$.data.unexpectedCount").value(count(before, "unexpectedCount")))
                .andExpect(jsonPath("$.data.letGoCount").value(count(before, "letGoCount") + 1));
    }

    private ResultActions end(Long ticketId, String guestToken, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/tickets/{ticketId}/end", ticketId)
                .header(GuestTokenHeader.NAME, guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions stats() throws Exception {
        return mockMvc.perform(get("/api/v1/tickets/stats"));
    }

    private static int count(String statsBody, String field) {
        return JsonPath.read(statsBody, "$.data." + field);
    }

    private Long ticketId(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.ticketId")).longValue();
    }

    private ResultActions issue(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions issue(String json, String guestToken) throws Exception {
        return mockMvc.perform(post("/api/v1/tickets")
                .header(GuestTokenHeader.NAME, guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private int ticketNumber(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.ticketNumber");
    }

    private String guestToken(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.guestToken");
    }

    private Long createUser() {
        String nickname = UUID.randomUUID().toString().substring(0, 12);
        return jdbcTemplate.queryForObject(
                "insert into users (nickname, pin_hash) values (?, 'not-used') returning id", Long.class, nickname);
    }
}
