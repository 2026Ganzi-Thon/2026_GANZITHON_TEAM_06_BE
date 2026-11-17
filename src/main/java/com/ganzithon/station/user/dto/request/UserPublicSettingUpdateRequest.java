package com.ganzithon.station.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 공개 설정 변경 (MY-04). PATCH /api/v1/users/me */
public record UserPublicSettingUpdateRequest(
        @Schema(description = "담요 대기실 공개 여부", example = "true")
        @NotNull(message = "공개 여부를 입력해 주세요.")
        Boolean isPublic
) {
}
