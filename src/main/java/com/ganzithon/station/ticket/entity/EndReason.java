package com.ganzithon.station.ticket.entity;

/**
 * 종료 사유. 화면 문구는 프론트가 가집니다.
 */
public enum EndReason {
    /** 답이 왔어요 */
    ANSWERED,
    /** 기대와 달랐어요 */
    UNEXPECTED,
    /** 내려놓을래요 */
    LET_GO
}
