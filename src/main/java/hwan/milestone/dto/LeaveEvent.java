package hwan.milestone.dto;

public record LeaveEvent(MessageType type, String playerId, int count, long serverTs) {

    public static LeaveEvent of(String playerId, int count) {
        return new LeaveEvent(MessageType.LEAVE, playerId, count, System.currentTimeMillis());
    }
}
