package com.ganzithon.station.realtime.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ganzithon.station.global.exception.ErrorCode;

/**
 * 나에게만 (/user/queue/events): 요청 실패. code는 REST와 같은 ErrorCode입니다.
 *
 * @param retryAfterMs 쿨다운처럼 다시 시도할 수 있는 시각이 있을 때만 (없으면 필드 생략)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorMessage(
        MessageType type,
        String code,
        String message,
        Long retryAfterMs,
        long serverTs
) {
    public static ErrorMessage of(ErrorCode errorCode) {
        return new ErrorMessage(MessageType.ERROR, errorCode.getCode(), errorCode.getMessage(), null,
                System.currentTimeMillis());
    }
}
