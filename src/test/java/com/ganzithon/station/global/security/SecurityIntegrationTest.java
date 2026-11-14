package com.ganzithon.station.global.security;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.response.ApiResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증 규칙 확인: 계정 토큰, 게스트 토큰, @CurrentUser, 열어 둔 경로, 실패 응답 형식, CORS.
 */
@Import({TestcontainersConfiguration.class, SecurityIntegrationTest.TestController.class})
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "station.cors.allowed-origin-patterns=https://station.example.com, http://localhost:*")
class SecurityIntegrationTest {

    private static final String GUEST_TOKEN = "guest-token-abc";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountTokenProvider accountTokenProvider;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    GuestTokenGenerator guestTokenGenerator;

    @Autowired
    PasswordEncoder passwordEncoder;

    // ── 보호 경로: 계정 토큰 또는 게스트 토큰 ──

    @Test
    void 토큰이_하나도_없으면_401_AUTH_LOGIN_REQUIRED() throws Exception {
        mockMvc.perform(get("/api/v1/test/requester"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("AUTH_LOGIN_REQUIRED"));
    }

    @Test
    void 게스트_토큰만_있으면_통과하고_userId는_null() throws Exception {
        mockMvc.perform(get("/api/v1/test/requester").header(GuestTokenHeader.NAME, GUEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(nullValue()))
                .andExpect(jsonPath("$.data.guestToken").value(GUEST_TOKEN));
    }

    @Test
    void 계정_토큰이면_userId를_꺼낸다() throws Exception {
        mockMvc.perform(get("/api/v1/test/requester").header("Authorization", bearer(42L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.guestToken").value(nullValue()));
    }

    @Test
    void 계정_토큰과_게스트_토큰을_같이_보내면_둘_다_꺼낸다() throws Exception {
        mockMvc.perform(get("/api/v1/test/requester")
                        .header("Authorization", bearer(42L))
                        .header(GuestTokenHeader.NAME, GUEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.guestToken").value(GUEST_TOKEN));
    }

    @Test
    void 만료된_계정_토큰은_401_AUTH_INVALID_TOKEN() throws Exception {
        Instant past = Instant.now().minus(Duration.ofDays(8));
        String expired = encode(jwtEncoder, past, past.plus(Duration.ofDays(7)));

        mockMvc.perform(get("/api/v1/test/requester").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void 다른_키로_서명한_토큰은_401_AUTH_INVALID_TOKEN() throws Exception {
        byte[] otherKey = "another-secret-another-secret-123456".getBytes(StandardCharsets.UTF_8);
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherKey, "HmacSHA256")));
        String forged = encode(otherEncoder, Instant.now(), Instant.now().plus(Duration.ofDays(7)));

        mockMvc.perform(get("/api/v1/test/requester").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void 열어_둔_경로라도_잘못된_계정_토큰을_보내면_401() throws Exception {
        mockMvc.perform(post("/api/v1/tickets").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void requireUserId는_게스트면_401() throws Exception {
        mockMvc.perform(get("/api/v1/test/require-user").header(GuestTokenHeader.NAME, GUEST_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_LOGIN_REQUIRED"));
    }

    // ── 로그인 전용 경로 ──

    @Test
    void users_me는_게스트면_401_계정이면_통과() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/test").header(GuestTokenHeader.NAME, GUEST_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_LOGIN_REQUIRED"));

        mockMvc.perform(get("/api/v1/users/me/test").header("Authorization", bearer(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7));
    }

    // ── 열어 둔 경로: 토큰 없이 보안을 통과해야 함 (API가 아직 없어서 404/405가 나오면 통과한 것) ──

    @Test
    void 열어_둔_경로는_토큰_없이_보안_통과() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/rooms/1")).andExpect(notBlockedBySecurity());
        mockMvc.perform(get("/api/v1/rooms/1/situation-chips")).andExpect(notBlockedBySecurity());
        mockMvc.perform(get("/api/v1/tickets/stats")).andExpect(notBlockedBySecurity());
        mockMvc.perform(post("/api/v1/users")).andExpect(notBlockedBySecurity());
        mockMvc.perform(post("/api/v1/users/login")).andExpect(notBlockedBySecurity());
        mockMvc.perform(post("/api/v1/tickets")).andExpect(notBlockedBySecurity());
        mockMvc.perform(get("/ws/info")).andExpect(notBlockedBySecurity());
    }

    @Test
    void 열지_않은_메서드는_막힌다() throws Exception {
        // 승차권 발급(POST)은 열려 있지만, 승차권 종료는 토큰이 필요
        mockMvc.perform(post("/api/v1/tickets/1/end"))
                .andExpect(status().isUnauthorized());
        // 대기실은 조회(GET)만 열려 있음
        mockMvc.perform(post("/api/v1/rooms"))
                .andExpect(status().isUnauthorized());
    }

    // ── CORS ──

    @Test
    void CORS_preflight는_토큰_없이도_통과하고_credentials는_없음() throws Exception {
        mockMvc.perform(options("/api/v1/tickets/1/end")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization, x-guest-token, content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void CORS_쉼표로_넣은_정확한_주소도_허용() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/1").header("Origin", "https://station.example.com"))
                .andExpect(header().string("Access-Control-Allow-Origin", "https://station.example.com"));
    }

    @Test
    void CORS_목록에_없는_Origin은_거부() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/1").header("Origin", "https://evil.example.com"))
                .andExpect(status().isForbidden());
    }

    // ── 그 밖의 빈 ──

    @Test
    void 계정_토큰은_7일_뒤_만료() {
        AccountToken token = accountTokenProvider.issue(1L);
        Duration remaining = Duration.between(Instant.now(), token.expiresAt());
        assertThat(remaining).isBetween(Duration.ofDays(7).minusMinutes(1), Duration.ofDays(7));
    }

    @Test
    void 게스트_토큰은_43자_URL_안전_문자열이고_겹치지_않는다() {
        Set<String> tokens = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> tokens.add(guestTokenGenerator.generate()));

        assertThat(tokens).hasSize(1000);
        assertThat(tokens).allSatisfy(token -> assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void PasswordEncoder는_PIN을_해시하고_비교한다() {
        String hash = passwordEncoder.encode("1234");

        assertThat(hash).isNotEqualTo("1234").startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches("1234", hash)).isTrue();
        assertThat(passwordEncoder.matches("0000", hash)).isFalse();
    }

    private String bearer(Long userId) {
        return "Bearer " + accountTokenProvider.issue(userId).accessToken();
    }

    private static String encode(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .subject("42")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static ResultMatcher notBlockedBySecurity() {
        return result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/test/requester")
        ResponseEntity<ApiResponse<Requester>> requester(@CurrentUser Requester requester) {
            return ResponseEntity.ok(ApiResponse.success(requester));
        }

        @GetMapping("/api/v1/test/require-user")
        ResponseEntity<ApiResponse<Long>> requireUser(@CurrentUser Requester requester) {
            return ResponseEntity.ok(ApiResponse.success(requester.requireUserId()));
        }

        @GetMapping("/api/v1/users/me/test")
        ResponseEntity<ApiResponse<Requester>> me(@CurrentUser Requester requester) {
            return ResponseEntity.ok(ApiResponse.success(requester));
        }
    }
}
