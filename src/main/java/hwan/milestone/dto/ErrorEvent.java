package hwan.milestone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorEvent(MessageType type, String code, String message, Long retryAfterMs, long serverTs) {

    public static ErrorEvent of(String code, String message, Long retryAfterMs) {
        return new ErrorEvent(MessageType.ERROR, code, message, retryAfterMs, System.currentTimeMillis());
    }
}
