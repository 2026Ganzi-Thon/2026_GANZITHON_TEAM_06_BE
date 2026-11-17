package com.ganzithon.station.room.entity;

import java.util.List;
import java.util.Map;

/**
 * 시연용 가상 승객 (RM-05). 방마다 2명, DB가 아니라 코드 상수입니다.
 * 승차권 번호는 1~99에서 정합니다. 실제 승차권은 100번부터 발급돼서 겹치지 않습니다 (V20261116_2347 마이그레이션).
 * 상태 칩은 호칭에 들어가서 고정합니다. 자동 방송 시점은 realtime이 정합니다.
 */
public record VirtualPassenger(String roomCode, int ticketNumber, MoodChip moodChip) {

    static final Map<String, List<VirtualPassenger>> BY_ROOM = Map.of(
            "RESULT", List.of(
                    new VirtualPassenger("RESULT", 7, MoodChip.NERVOUS),
                    new VirtualPassenger("RESULT", 38, MoodChip.TIRED)),
            "NO_REPLY", List.of(
                    new VirtualPassenger("NO_REPLY", 15, MoodChip.RESTLESS),
                    new VirtualPassenger("NO_REPLY", 62, MoodChip.BLANK)),
            "GANZITHON_FINAL", List.of(
                    new VirtualPassenger("GANZITHON_FINAL", 21, MoodChip.FLUTTERING),
                    new VirtualPassenger("GANZITHON_FINAL", 84, MoodChip.EXCITED_ANXIOUS)));

    public static List<VirtualPassenger> findByRoomCode(String roomCode) {
        return BY_ROOM.getOrDefault(roomCode, List.of());
    }

    /** 대기실 호칭. 규칙은 Ticket.getDisplayName과 같습니다. 예: 7번 조마조마한 양 */
    public String displayName() {
        return ticketNumber + "번 " + moodChip.getLabel() + " 양";
    }
}
