package com.ganzithon.station.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 계정 토큰 발급. 가입(MY-01)·로그인(MY-09)에 성공했을 때 user 도메인이 호출합니다.
 * <pre>{@code
 * AccountToken token = accountTokenProvider.issue(user.getId());
 * }</pre>
 * 검증은 Spring Security가 요청마다 자동으로 합니다. 로그아웃은 프론트가 토큰을 지우는 것으로 처리합니다.
 */
@Component
@RequiredArgsConstructor
public class AccountTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AccountToken issue(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.expiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccountToken(accessToken, expiresAt);
    }
}
