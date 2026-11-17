package com.ganzithon.station.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 내 정보 조회 응답. GET /api/v1/users/me */
public record UserMeResponse(
        @Schema(description = "닉네임", example = "몽실이")
        String nickname,

        @Schema(description = "담요 대기실 공개 여부", example = "false")
        boolean isPublic
) {
    public static UserMeResponse of(String nickname, boolean isPublic) {
        return new UserMeResponse(nickname, isPublic);
    }
}
