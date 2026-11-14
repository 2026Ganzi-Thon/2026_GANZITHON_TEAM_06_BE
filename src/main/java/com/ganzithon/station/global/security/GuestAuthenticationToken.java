package com.ganzithon.station.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * X-Guest-Token만 있는 비로그인 승객의 인증 정보 (권한 ROLE_GUEST).
 * 토큰이 "어느 승차권의 것인지"는 여기서 확인하지 않습니다. TicketService가 승차권의 guestToken과 비교합니다.
 */
public class GuestAuthenticationToken extends AbstractAuthenticationToken {

    static final String ROLE_GUEST = "ROLE_GUEST";

    private final String guestToken;

    public GuestAuthenticationToken(String guestToken) {
        super(AuthorityUtils.createAuthorityList(ROLE_GUEST));
        this.guestToken = guestToken;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return guestToken;
    }

    @Override
    public Object getPrincipal() {
        return guestToken;
    }
}
