package com.ganzithon.station.global.security;

import java.time.Instant;

/**
 * 발급한 계정 토큰. 프론트는 accessToken을 보관했다가 Authorization: Bearer {accessToken}으로 보냅니다.
 *
 * @param accessToken JWT 문자열
 * @param expiresAt   만료 시각 (발급 후 7일)
 */
public record AccountToken(
        String accessToken,
        Instant expiresAt
) {
}
