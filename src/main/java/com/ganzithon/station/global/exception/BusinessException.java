package com.ganzithon.station.global.exception;

import lombok.Getter;

/**
 * 비즈니스 예외는 이것 하나만 씁니다. 새 예외 클래스를 만들지 않고 {@link ErrorCode}를 추가합니다.
 * <pre>{@code
 * throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
