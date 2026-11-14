package com.ganzithon.station.global.security;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @CurrentUser Requester} 파라미터를 채웁니다.
 * userId는 Spring Security가 검증한 계정 토큰에서, guestToken은 X-Guest-Token 헤더에서 꺼냅니다.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Requester.class.equals(parameter.getParameterType());
    }

    @Override
    public Requester resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                     NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String guestToken = request == null ? null : GuestTokenHeader.resolve(request);
        return new Requester(resolveUserId(), guestToken);
    }

    private Long resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return null;
        }
        try {
            return Long.valueOf(jwtAuthentication.getToken().getSubject());
        } catch (NumberFormatException e) {
            // 우리 서버가 발급한 토큰이면 subject는 항상 숫자입니다
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }
}
