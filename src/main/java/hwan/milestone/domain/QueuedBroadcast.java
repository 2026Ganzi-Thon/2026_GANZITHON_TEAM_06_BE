package hwan.milestone.domain;

public record QueuedBroadcast(
        String broadcastId,
        String roomId,
        String senderId,
        String situationChipId,
        String stateChipId,
        long queuedAt
) {
}
