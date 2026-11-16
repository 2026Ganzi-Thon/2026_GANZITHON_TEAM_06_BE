package com.ganzithon.station.global.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * X-Guest-Token 헤더 읽기. 앞뒤 공백을 지우고, 비었거나 64자(tickets.guest_token 길이)를 넘으면 없는 것으로 봅니다.
 */
public final class GuestTokenHeader {

    public static final String NAME = "X-Guest-Token";
    private static final int MAX_LENGTH = 64;

    private GuestTokenHeader() {
    }

    /** 헤더가 없거나 형식이 맞지 않으면 null */
    public static String resolve(HttpServletRequest request) {
        return normalize(request.getHeader(NAME));
    }

    /** 헤더 값 정리 (STOMP CONNECT 헤더에도 같은 규칙). 없거나 형식이 맞지 않으면 null */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            return null;
        }
        return trimmed;
    }
}
