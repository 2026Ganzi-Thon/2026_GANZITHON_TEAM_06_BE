package hwan.milestone.dto;

public record JoinEvent(MessageType type, PlayerState player, int count, long serverTs) {

    public static JoinEvent of(PlayerState player, int count) {
        return new JoinEvent(MessageType.JOIN, player, count, System.currentTimeMillis());
    }
}
