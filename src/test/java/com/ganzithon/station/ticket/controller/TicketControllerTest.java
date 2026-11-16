package com.ganzithon.station.ticket.controller;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.security.AccountTokenProvider;
import com.ganzithon.station.global.security.GuestTokenHeader;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.room.service.RoomService;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.service.UserService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 승차권 발급. 시드 데이터가 들어간 실제 Postgres로 확인합니다.
 * 아직 구현 전인 RoomService.getRandomPattern(하빈), UserService.getUserEntity(상진)는 이 테스트에서만 대신합니다.
 * 구현이 머지되면 두 doAnswer를 지워도 테스트가 그대로 통과해야 합니다.
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
    RoomService roomService;

    @MockitoSpyBean
    UserService userService;

    @BeforeEach
    void stubOtherDomainTodos() {
        Long patternId = jdbcTemplate.queryForObject("select id from patterns where code = 'PATTERN_01'", Long.class);
        doAnswer(invocation -> entityManager.getReference(Pattern.class, patternId))
                .when(roomService).getRandomPattern();
        doAnswer(invocation -> entityManager.getReference(User.class, invocation.getArgument(0)))
                .when(userService).getUserEntity(anyLong());
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
                .andExpect(jsonPath("$.data.patternCode").value("PATTERN_01"))
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
