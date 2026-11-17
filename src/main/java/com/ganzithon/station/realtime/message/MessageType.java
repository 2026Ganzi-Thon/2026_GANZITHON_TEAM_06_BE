package com.ganzithon.station.realtime.message;

/**
 * 서버 → 클라이언트 메시지 구분자. 서버가 보내는 모든 메시지에 type과 serverTs가 있습니다.
 * 추가·변경하면 README의 메시지 명세 표도 같은 PR에서 갱신합니다 (docs/API_CONVENTION.md 7장).
 */
public enum MessageType {
    /** 입장 직후 나에게만: 방에 있는 승객 전체 */
    ROOM_STATE,
    /** 방 전체: 새 승객 입장 */
    JOIN,
    /** 방 전체: 승객 퇴장 */
    LEAVE,
    /** 나에게만: 방송 요청이 대기열에 들어감 */
    BROADCAST_ACCEPTED,
    /** 방 전체: 전광판 방송 송출 */
    BROADCAST,
    /** 방 전체: 1초 리액션 */
    REACTION,
    /** 나에게만: 탭한 승객의 확대 카드 */
    PASSENGER_CARD,
    /** 나에게만: 요청 실패 */
    ERROR
}
