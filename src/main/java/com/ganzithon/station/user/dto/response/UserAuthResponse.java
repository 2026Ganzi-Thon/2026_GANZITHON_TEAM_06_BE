package com.ganzithon.station.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 가입·로그인 성공 응답 (MY-01, MY-09). */
public record UserAuthResponse(
        @Schema(description = "닉네임", example = "몽실이")
        String nickname,

        @Schema(description = "계정 토큰 (JWT). Authorization: Bearer {accessToken}으로 보내세요.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
) {
    public static UserAuthResponse of(String nickname, String accessToken) {
        return new UserAuthResponse(nickname, accessToken);
    }
}
