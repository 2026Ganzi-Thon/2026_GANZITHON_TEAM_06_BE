package com.ganzithon.station.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 가입 (MY-01). POST /api/v1/users */
@PinConfirmMatch
public record UserSignupRequest(
        @Schema(description = "닉네임. 한글·영문·숫자 1~12자", example = "몽실이")
        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,12}$", message = "닉네임은 한글·영문·숫자 1~12자여야 합니다.")
        String nickname,

        @Schema(description = "PIN. 숫자 4자리", example = "1234")
        @NotBlank(message = "PIN을 입력해 주세요.")
        @Pattern(regexp = "^[0-9]{4}$", message = "PIN은 숫자 4자리여야 합니다.")
        String pin,

        @Schema(description = "PIN 확인. 숫자 4자리", example = "1234")
        @NotBlank(message = "PIN 확인을 입력해 주세요.")
        @Pattern(regexp = "^[0-9]{4}$", message = "PIN 확인은 숫자 4자리여야 합니다.")
        String pinConfirm
) {
    @Override
    public String toString() {
        return "UserSignupRequest[nickname=" + nickname + ", pin=****, pinConfirm=****]";
    }
}
