package com.ganzithon.station.global.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 게스트 토큰 생성. 비로그인 상태에서 승차권을 발급할 때 ticket 도메인이 호출합니다.
 * JWT가 아니라 추측할 수 없는 랜덤 문자열(43자)입니다. 브라우저가 보관했다가 X-Guest-Token 헤더로 보냅니다.
 */
@Component
public class GuestTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
