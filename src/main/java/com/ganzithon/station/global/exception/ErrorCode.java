package com.ganzithon.station.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 모든 에러 코드. 형식은 {도메인}_{이유}. REST와 WebSocket이 같은 코드를 씁니다.
 * 새 코드가 필요하면 global 담당자(환희)에게 요청하고, docs/API_CONVENTION.md 4장 표도 함께 갱신합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 서버 오류가 발생했습니다."),

    // 인증
    AUTH_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "로그인 정보가 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "대기실을 찾을 수 없습니다."),
    SITUATION_CHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "이 대기실에 없는 상황 칩입니다."),

    // user
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."),

    // ticket
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "기다림을 찾을 수 없습니다."),
    TICKET_FORBIDDEN(HttpStatus.FORBIDDEN, "내 기다림이 아닙니다."),
    TICKET_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 종료된 기다림입니다."),
    TICKET_NOT_ENDED(HttpStatus.CONFLICT, "아직 끝나지 않은 기다림입니다."),
    TICKET_ALREADY_CLAIMED(HttpStatus.CONFLICT, "이미 계정으로 옮긴 기다림입니다."),
    TICKET_ROOM_MISMATCH(HttpStatus.BAD_REQUEST, "이 대기실의 승차권이 아닙니다."),

    // blanket
    BLANKET_PIECE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이 기다림의 담요 조각이 이미 있습니다."),

    // realtime
    BROADCAST_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "방송은 30초에 한 번만 할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}
