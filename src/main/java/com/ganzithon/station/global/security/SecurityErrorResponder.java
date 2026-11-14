package com.ganzithon.station.global.security;

import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security가 막은 요청을 ApiResponse 형식으로 응답합니다.
 * (Security 필터에서 막히면 GlobalExceptionHandler까지 오지 않아서 여기서 따로 만듭니다)
 * <ul>
 *     <li>토큰이 만료·위조됨 → 401 AUTH_INVALID_TOKEN</li>
 *     <li>계정 토큰도 게스트 토큰도 없음 → 401 AUTH_LOGIN_REQUIRED</li>
 *     <li>게스트가 로그인 전용 API 호출 → 401 AUTH_LOGIN_REQUIRED</li>
 *     <li>그 밖의 권한 부족 → 403 AUTH_FORBIDDEN</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponder {

    private final JsonMapper jsonMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> write(response, toErrorCode(exception));
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            boolean isGuest = SecurityContextHolder.getContext().getAuthentication() instanceof GuestAuthenticationToken;
            write(response, isGuest ? ErrorCode.AUTH_LOGIN_REQUIRED : ErrorCode.AUTH_FORBIDDEN);
        };
    }

    private ErrorCode toErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            return ErrorCode.AUTH_INVALID_TOKEN;
        }
        if (exception instanceof InsufficientAuthenticationException) {
            return ErrorCode.AUTH_LOGIN_REQUIRED;
        }
        return ErrorCode.AUTH_INVALID_TOKEN;
    }

    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), ApiResponse.fail(errorCode));
    }
}
