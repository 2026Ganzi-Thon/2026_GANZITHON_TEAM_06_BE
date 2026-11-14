package com.ganzithon.station.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ganzithon.station.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * {@link ApiResponse}의 error 부분. fieldErrors는 검증 실패(400)일 때만 내려갑니다.
 */
@JsonPropertyOrder({"code", "message", "fieldErrors"})
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "TICKET_NOT_FOUND")
        String code,

        @Schema(description = "에러 메시지", example = "기다림을 찾을 수 없습니다.")
        String message,

        @Schema(description = "검증 실패한 필드 목록 (검증 실패 때만)")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<FieldError> fieldErrors
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), fieldErrors);
    }

    public record FieldError(
            @Schema(description = "필드 이름", example = "nickname")
            String field,

            @Schema(description = "실패 이유", example = "닉네임은 1~12자여야 합니다.")
            String reason
    ) {
    }
}
