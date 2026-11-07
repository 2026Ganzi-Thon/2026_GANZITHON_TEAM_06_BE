package hwan.milestone.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 데모용 하드코딩 칩 목록: 방별 상황 칩 + 공통 상태 칩.
 * (기능명세 확정 후 교체 예정)
 */
@Component
public class ChipCatalog {

    private static final Map<String, Map<String, String>> SITUATION_CHIPS = Map.of(
            "room-1", Map.of(   // 지하철 승강장
                    "SUBWAY_DELAY", "열차가 지연되고 있어요",
                    "SUBWAY_CROWDED", "승강장이 붐벼요",
                    "SUBWAY_TRANSFER", "환승 기다리는 중"),
            "room-2", Map.of(   // 병원 대기실
                    "HOSPITAL_WAITING", "진료 순서를 기다려요",
                    "HOSPITAL_RESULT", "검사 결과 기다리는 중",
                    "HOSPITAL_PHARMACY", "약 타러 가는 중"),
            "room-3", Map.of(   // 버스 정류장
                    "BUS_LATE", "버스가 늦게 와요",
                    "BUS_RAIN", "비가 와요",
                    "BUS_LAST", "막차를 기다려요")
    );

    private static final Map<String, String> STATE_CHIPS = Map.of(
            "TIRED", "피곤해요",
            "BORED", "심심해요",
            "HAPPY", "기분 좋아요",
            "HUNGRY", "배고파요");

    public Optional<String> situationLabel(String roomId, String chipId) {
        return Optional.ofNullable(SITUATION_CHIPS.getOrDefault(roomId, Map.of()).get(chipId));
    }

    public Optional<String> stateLabel(String chipId) {
        return Optional.ofNullable(STATE_CHIPS.get(chipId));
    }
}
