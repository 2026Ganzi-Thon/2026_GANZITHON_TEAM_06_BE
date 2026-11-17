package com.ganzithon.station.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 로그인 (MY-09). POST /api/v1/users/login */
public record UserLoginRequest(
        @Schema(description = "닉네임. 한글·영문·숫자 1~12자", example = "몽실이")
        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,12}$", message = "닉네임은 한글·영문·숫자 1~12자여야 합니다.")
        String nickname,

        @Schema(description = "PIN. 숫자 4자리", example = "1234")
        @NotBlank(message = "PIN을 입력해 주세요.")
        @Pattern(regexp = "^[0-9]{4}$", message = "PIN은 숫자 4자리여야 합니다.")
        String pin
) {
    @Override
    public String toString() {
        return "UserLoginRequest[nickname=" + nickname + ", pin=****]";
    }
}
