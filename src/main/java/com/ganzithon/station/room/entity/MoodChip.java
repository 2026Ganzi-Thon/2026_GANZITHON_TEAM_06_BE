package com.ganzithon.station.room.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 상태 칩. 모든 방 공통 12개 (docs/DOMAIN.md 4-2, 와이어프레임 S03).
 * 문구가 호칭형이라 대기실 호칭에 그대로 씁니다. 예: 104번 두근두근한 양
 * DB에는 enum 이름(문자열)으로 저장합니다 (tickets.mood_chip).
 */
@Getter
@RequiredArgsConstructor
public enum MoodChip {

    NERVOUS("조마조마한"),
    FLUTTERING("두근두근한"),
    RESTLESS("마음이 바쁜"),
    ANXIOUS("불안한"),
    BLANK("멍한"),
    TIRED("지친"),
    CALM("고요한"),
    RELAXED("느긋한"),
    COMPOSED("담담한"),
    RELIEVED("한숨 돌린"),
    EXCITED_ANXIOUS("설레고 불안한"),
    COMPLICATED("복잡한");

    private final String label;
}
