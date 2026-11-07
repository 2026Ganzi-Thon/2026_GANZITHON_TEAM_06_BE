package hwan.milestone.repository;

import hwan.milestone.domain.Player;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 방별 플레이어 저장소 (메모리). key = STOMP sessionId = playerId.
 */
@Repository
public class PlayerRepository {

    /** roomId -> (sessionId -> Player) */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Player>> rooms = new ConcurrentHashMap<>();
    /** sessionId -> Player (퇴장 시 방을 찾기 위한 역인덱스) */
    private final ConcurrentHashMap<String, Player> bySession = new ConcurrentHashMap<>();

    /** 플레이어를 등록하고 등록 후 방 인원 수를 반환한다. */
    public int save(Player player) {
        bySession.put(player.getId(), player);
        int[] size = new int[1];
        rooms.compute(player.getRoomId(), (roomId, members) -> {
            if (members == null) {
                members = new ConcurrentHashMap<>();
            }
            members.put(player.getId(), player);
            size[0] = members.size();
            return members;
        });
        return size[0];
    }

    public Optional<Player> findBySessionId(String sessionId) {
        return Optional.ofNullable(bySession.get(sessionId));
    }

    /** 플레이어를 제거한다. 방이 비면 방 자체도 메모리에서 지운다. */
    public Optional<Removal> deleteBySessionId(String sessionId) {
        Player player = bySession.remove(sessionId);
        if (player == null) {
            return Optional.empty();
        }
        int[] remaining = new int[1];
        rooms.computeIfPresent(player.getRoomId(), (roomId, members) -> {
            members.remove(sessionId);
            remaining[0] = members.size();
            return members.isEmpty() ? null : members;
        });
        return Optional.of(new Removal(player, remaining[0]));
    }

    public List<Player> findByRoomId(String roomId) {
        Map<String, Player> members = rooms.get(roomId);
        return members == null ? List.of() : List.copyOf(members.values());
    }

    public void forEachRoom(BiConsumer<String, Collection<Player>> action) {
        rooms.forEach((roomId, members) -> action.accept(roomId, members.values()));
    }

    public record Removal(Player player, int remaining) {
    }
}
