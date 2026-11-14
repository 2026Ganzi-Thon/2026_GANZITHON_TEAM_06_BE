package com.ganzithon.station.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증 규칙.
 * <ul>
 *     <li>계정 토큰: Authorization: Bearer {JWT} → 로그인 사용자 (ROLE_USER)</li>
 *     <li>게스트 토큰: X-Guest-Token → 비로그인 승객 (ROLE_GUEST)</li>
 * </ul>
 * 아래 "열어 둔 경로"는 아무것도 없어도 되고, /api/v1/users/me/**는 계정 토큰이 필요하며,
 * 나머지는 계정 토큰 또는 게스트 토큰 중 하나가 필요합니다.
 * 열어 둔 경로라도 만료·위조된 계정 토큰을 보내면 401입니다 (프론트는 토큰을 지우고 다시 요청).
 * 경로를 추가·변경할 때는 환희에게 요청합니다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_USER = "ROLE_USER";

    private final SecurityErrorResponder securityErrorResponder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 문서 · 배포 확인
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        // WebSocket: 연결 인증은 realtime 작업 때 STOMP CONNECT에서 처리
                        .requestMatchers("/ws/**").permitAll()
                        // 인증 없이 쓰는 API
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tickets/stats").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets").permitAll()
                        // 로그인 전용 (내 담요, 공개 설정 등)
                        .requestMatchers("/api/v1/users/me/**").hasAuthority(ROLE_USER)
                        // 나머지: 계정 토큰 또는 게스트 토큰
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(accountJwtConverter()))
                        .authenticationEntryPoint(securityErrorResponder.authenticationEntryPoint())
                        .accessDeniedHandler(securityErrorResponder.accessDeniedHandler()))
                .addFilterAfter(new GuestTokenAuthenticationFilter(), BearerTokenAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityErrorResponder.authenticationEntryPoint())
                        .accessDeniedHandler(securityErrorResponder.accessDeniedHandler()));
        return http.build();
    }

    /** PIN 해시용 (기본 bcrypt). user 도메인에서 encode / matches로 씁니다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** 계정 토큰이 검증되면 ROLE_USER 권한을 줍니다 (scope claim은 쓰지 않음). */
    private JwtAuthenticationConverter accountJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> AuthorityUtils.createAuthorityList(ROLE_USER));
        return converter;
    }
}
