package com.ganzithon.station.global.security;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;

/**
 * 요청한 사람. {@code @CurrentUser Requester requester}로 받습니다.
 * 로그인 사용자는 userId, 비로그인 승객은 guestToken이 있고, 임시 조각 옮기기처럼 둘 다 있을 수도 있습니다.
 *
 * @param userId     계정 토큰(JWT)으로 로그인한 사용자 ID. 없으면 null
 * @param guestToken X-Guest-Token 헤더 값. 없으면 null
 */
public record Requester(
        Long userId,
        String guestToken
) {
    public boolean isLoggedIn() {
        return userId != null;
    }

    public boolean hasGuestToken() {
        return guestToken != null;
    }

    /** 로그인한 사용자 ID. 로그인하지 않았으면 401 (AUTH_LOGIN_REQUIRED) */
    public Long requireUserId() {
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
        return userId;
    }
}
