package com.ganzithon.station.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ganzithon.station.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 모든 REST 응답의 공통 형식: { success, data, error }.
 * <pre>{@code
 * return ResponseEntity.ok(ApiResponse.success(roomService.getRoom(roomId)));
 * }</pre>
 * 실패 응답은 GlobalExceptionHandler가 만듭니다. Controller에서 직접 fail을 만들지 않습니다.
 * (record로 만들면 success 필드와 success() 메서드 이름이 겹쳐서 클래스로 둡니다.)
 */
@Getter
@JsonPropertyOrder({"success", "data", "error"})
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 데이터. 실패하거나 돌려줄 데이터가 없으면 null")
    private final T data;

    @Schema(description = "에러 정보. 성공이면 null")
    private final ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 돌려줄 데이터가 없을 때: "data": null */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(ErrorResponse.of(errorCode));
    }
}
