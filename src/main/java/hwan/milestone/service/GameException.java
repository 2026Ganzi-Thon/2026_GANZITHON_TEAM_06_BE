package hwan.milestone.service;

import lombok.Getter;

/** 요청한 세션에게만 ERROR 로 돌려줄 비즈니스 예외. */
@Getter
public class GameException extends RuntimeException {

    private final String code;
    private final Long retryAfterMs;

    public GameException(String code, String message) {
        this(code, message, null);
    }

    public GameException(String code, String message, Long retryAfterMs) {
        super(message);
        this.code = code;
        this.retryAfterMs = retryAfterMs;
    }
}
