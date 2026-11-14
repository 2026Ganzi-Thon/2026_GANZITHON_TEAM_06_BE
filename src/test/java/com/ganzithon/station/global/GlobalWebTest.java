package com.ganzithon.station.global;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * global 공통 코드 확인: 응답 형식, 예외 처리, 시각 형식, CORS.
 */
@WebMvcTest(controllers = GlobalWebTest.TestController.class)
@Import(GlobalWebTest.TestController.class)
@TestPropertySource(properties = "station.cors.allowed-origin-patterns=https://station.example.com, http://localhost:*")
class GlobalWebTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 성공_응답은_success_data_error_형식이고_시각은_한국_오프셋() throws Exception {
        mockMvc.perform(get("/api/v1/test/time"))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("{\"success\":true,\"data\":")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startedAt").value("2026-11-11T21:30:00+09:00"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void BusinessException은_ErrorCode의_상태와_코드로_응답() throws Exception {
        mockMvc.perform(get("/api/v1/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("TICKET_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("기다림을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.error.fieldErrors").doesNotExist());
    }

    @Test
    void 검증_실패는_400과_fieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("nickname"));
    }

    @Test
    void 잘못된_JSON은_400() throws Exception {
        mockMvc.perform(post("/api/v1/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nickname"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
    }

    @Test
    void 없는_경로는_404() throws Exception {
        mockMvc.perform(get("/api/v1/nothing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMON_NOT_FOUND"));
    }

    @Test
    void 알_수_없는_예외는_500이고_내용을_숨김() throws Exception {
        mockMvc.perform(get("/api/v1/test/bug"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("COMMON_INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("알 수 없는 서버 오류가 발생했습니다."));
    }

    @Test
    void CORS_localhost_패턴은_어느_포트든_허용하고_credentials는_없음() throws Exception {
        mockMvc.perform(options("/api/v1/test/time")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void CORS_쉼표로_넣은_정확한_주소도_허용() throws Exception {
        mockMvc.perform(get("/api/v1/test/time").header("Origin", "https://station.example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://station.example.com"));
    }

    @Test
    void CORS_목록에_없는_Origin은_거부() throws Exception {
        mockMvc.perform(get("/api/v1/test/time").header("Origin", "https://evil.example.com"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class TestController {

        record TimeResponse(Instant startedAt) {
        }

        record ValidRequest(@NotBlank String nickname) {
        }

        @GetMapping("/api/v1/test/time")
        ResponseEntity<ApiResponse<TimeResponse>> time() {
            return ResponseEntity.ok(ApiResponse.success(new TimeResponse(Instant.parse("2026-11-11T12:30:00Z"))));
        }

        @GetMapping("/api/v1/test/business")
        ResponseEntity<ApiResponse<Void>> business() {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }

        @PostMapping("/api/v1/test/valid")
        ResponseEntity<ApiResponse<Void>> valid(@Valid @RequestBody ValidRequest request) {
            return ResponseEntity.ok(ApiResponse.success());
        }

        @GetMapping("/api/v1/test/bug")
        ResponseEntity<ApiResponse<Void>> bug() {
            throw new IllegalStateException("내부 정보");
        }
    }
}
