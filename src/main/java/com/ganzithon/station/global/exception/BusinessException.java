package com.ganzithon.station.global.exception;

import lombok.Getter;

/**
 * 비즈니스 예외는 이것 하나만 씁니다. 새 예외 클래스를 만들지 않고 {@link ErrorCode}를 추가합니다.
 * <pre>{@code
 * throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
 * throw new BusinessException(ErrorCode.BROADCAST_COOLDOWN, remainingMs);   // 다시 시도할 수 있는 시각이 있을 때
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 쿨다운처럼 다시 시도할 수 있을 때까지 남은 시간(ms). 없으면 null (WebSocket ERROR의 retryAfterMs) */
    private final Long retryAfterMs;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, Long retryAfterMs) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.retryAfterMs = retryAfterMs;
    }
}
