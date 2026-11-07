package hwan.milestone.dto;

public record BroadcastEvent(
        MessageType type,
        String broadcastId,
        String senderId,
        String situationChipId,
        String situationLabel,
        String stateChipId,
        String stateLabel,
        long queuedAt,
        long serverTs
) {
}
