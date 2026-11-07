package hwan.milestone.dto;

public record ReactionEvent(MessageType type, String broadcastId, String playerId, ReactionType reaction, long serverTs) {

    public static ReactionEvent of(String broadcastId, String playerId, ReactionType reaction) {
        return new ReactionEvent(MessageType.REACTION, broadcastId, playerId, reaction, System.currentTimeMillis());
    }
}
