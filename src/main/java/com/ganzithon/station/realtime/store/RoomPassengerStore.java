package com.ganzithon.station.realtime.store;

import com.ganzithon.station.global.config.RealtimeProperties;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.ticket.dto.response.PassengerTicketResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 대기실별 접속 승객과 좌석 (메모리만, DB 없음). 서버를 재시작하면 비고, 다시 접속하면 채워집니다.
 * 입장·퇴장은 여러 단계(이전 방 퇴장, 좌석 찾기, 등록)를 한 번에 해야 해서 메서드 단위로 잠급니다.
 * 입장·퇴장은 접속할 때만 일어나서 잠금 경합이 거의 없습니다.
 */
@Component
public class RoomPassengerStore {

    /** roomCode → (ticketId → 승객), 입장 순서 유지 */
    private final Map<String, Map<Long, Passenger>> rooms = new HashMap<>();
    /** sessionId → 승객 (연결이 끊겼을 때 찾기 위한 역인덱스) */
    private final Map<String, Passenger> bySession = new HashMap<>();
    private final int seatCount;

    public RoomPassengerStore(RealtimeProperties realtimeProperties) {
        this.seatCount = realtimeProperties.seatCount();
    }

    /**
     * 입장. 세 가지 경우가 있습니다.
     * <ul>
     *     <li>새 승객: 비어 있는 가장 앞 좌석에 앉힙니다. 다 찼으면 서서 기다립니다 (seatNumber = null)</li>
     *     <li>같은 승차권이 이미 방에 있음 (새로고침, 탭 두 개): 좌석은 그대로 두고 세션만 바꿉니다. 이전 세션은 더 이상 이 승객이 아닙니다</li>
     *     <li>이 세션이 다른 승객으로 있었음 (다른 방·다른 승차권으로 옮김): 이전 승객을 먼저 내보냅니다</li>
     * </ul>
     */
    public synchronized JoinResult join(PassengerTicketResponse ticket, String sessionId) {
        Optional<Departure> previous = Optional.empty();
        Passenger current = bySession.get(sessionId);
        if (current != null && !current.ticketId().equals(ticket.ticketId())) {
            previous = remove(current);
        }

        Map<Long, Passenger> members = rooms.computeIfAbsent(ticket.roomCode(), roomCode -> new LinkedHashMap<>());
        Passenger existing = members.get(ticket.ticketId());
        Integer seatNumber = existing != null ? existing.seatNumber() : findFreeSeat(members);
        Passenger passenger = new Passenger(ticket.ticketId(), ticket.roomCode(), ticket.ticketNumber(),
                ticket.displayName(), ticket.moodChip(), ticket.startedAt(), seatNumber, sessionId);

        if (existing != null) {
            bySession.remove(existing.sessionId());
        }
        members.put(passenger.ticketId(), passenger);
        bySession.put(sessionId, passenger);
        return new JoinResult(passenger, existing == null, members.size(), previous);
    }

    /** 연결이 끊긴 세션의 승객을 내보냅니다. 이미 다른 세션으로 옮겨 간 승객이면 아무것도 하지 않습니다. */
    public synchronized Optional<Departure> leave(String sessionId) {
        Passenger passenger = bySession.get(sessionId);
        return passenger == null ? Optional.empty() : remove(passenger);
    }

    /** 이 세션으로 접속해 있는 승객 */
    public synchronized Optional<Passenger> findBySessionId(String sessionId) {
        return Optional.ofNullable(bySession.get(sessionId));
    }

    /** 방송으로 상태 칩이 바뀐 승객의 호칭을 고칩니다. 그 사이 나갔으면 아무것도 하지 않습니다. */
    public synchronized void updateMood(Long ticketId, String roomCode, String displayName, MoodChip moodChip) {
        Map<Long, Passenger> members = rooms.get(roomCode);
        Passenger current = members == null ? null : members.get(ticketId);
        if (current == null) {
            return;
        }
        Passenger updated = new Passenger(current.ticketId(), current.roomCode(), current.ticketNumber(),
                displayName, moodChip, current.startedAt(), current.seatNumber(), current.sessionId());
        members.put(ticketId, updated);
        bySession.put(updated.sessionId(), updated);
    }

    public synchronized List<Passenger> findByRoomCode(String roomCode) {
        Map<Long, Passenger> members = rooms.get(roomCode);
        return members == null ? List.of() : List.copyOf(members.values());
    }

    public int getSeatCount() {
        return seatCount;
    }

    private Optional<Departure> remove(Passenger passenger) {
        bySession.remove(passenger.sessionId());
        Map<Long, Passenger> members = rooms.get(passenger.roomCode());
        if (members == null || members.remove(passenger.ticketId()) == null) {
            return Optional.empty();
        }
        int remaining = members.size();
        if (members.isEmpty()) {
            rooms.remove(passenger.roomCode());
        }
        return Optional.of(new Departure(passenger, remaining));
    }

    private Integer findFreeSeat(Map<Long, Passenger> members) {
        boolean[] taken = new boolean[seatCount + 1];
        for (Passenger member : members.values()) {
            if (member.isSeated()) {
                taken[member.seatNumber()] = true;
            }
        }
        for (int seat = 1; seat <= seatCount; seat++) {
            if (!taken[seat]) {
                return seat;
            }
        }
        return null;
    }

    /**
     * @param passenger    입장한 승객 (세션이 바뀐 경우 새 세션으로)
     * @param newlyJoined  새로 들어왔으면 true. 같은 승차권의 재접속이면 false (방에 JOIN을 다시 보내지 않음)
     * @param count        입장 후 방 인원
     * @param previous     이 세션이 다른 승객으로 있다가 옮겨 온 경우 그 퇴장
     */
    public record JoinResult(Passenger passenger, boolean newlyJoined, int count, Optional<Departure> previous) {
    }

    /**
     * @param passenger 나간 승객
     * @param remaining 퇴장 후 그 방 인원
     */
    public record Departure(Passenger passenger, int remaining) {
    }
}
