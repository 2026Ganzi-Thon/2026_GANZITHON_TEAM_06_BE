package com.ganzithon.station.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 계정 토큰(JWT)으로 인증되지 않았고 X-Guest-Token 헤더가 있으면 게스트로 인증합니다.
 * 로그인 사용자가 게스트 토큰도 같이 보내면(임시 조각 옮기기) 계정 인증이 우선이고, 게스트 토큰은 @CurrentUser로 따로 꺼냅니다.
 * 이 필터는 빈으로 등록하지 않고 SecurityConfig에서 직접 만듭니다 (빈으로 만들면 서블릿 필터로 한 번 더 등록됨).
 */
public class GuestTokenAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String guestToken = GuestTokenHeader.resolve(request);
        if (guestToken != null && contextHolderStrategy.getContext().getAuthentication() == null) {
            SecurityContext context = contextHolderStrategy.createEmptyContext();
            context.setAuthentication(new GuestAuthenticationToken(guestToken));
            contextHolderStrategy.setContext(context);
        }
        filterChain.doFilter(request, response);
    }
}
