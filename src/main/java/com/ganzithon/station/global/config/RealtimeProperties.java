package com.ganzithon.station.global.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 실시간 대기실 설정 (application.yml station.realtime).
 *
 * @param seatCount          대기실 하나의 좌석 수 (환경변수 SEAT_COUNT, 기본 20). 다 차면 서서 기다립니다 (seatNumber = null)
 * @param broadcastCooldown  승객 한 명이 다시 방송할 수 있을 때까지 (BR-01, 30초)
 * @param broadcastDisplay   전광판에 방송 하나를 보여 주는 시간. 대기열의 다음 방송은 이 간격으로 나갑니다 (BR-02, 15초)
 * @param broadcastQueueSize 대기실 하나의 방송 대기열 상한. 쿨다운 30초 × 여러 명 > 15초당 1건이라 상한이 없으면 끝없이 늘어납니다
 */
@Validated
@ConfigurationProperties(prefix = "station.realtime")
public record RealtimeProperties(
        @Positive int seatCount,
        @NotNull Duration broadcastCooldown,
        @NotNull Duration broadcastDisplay,
        @Positive int broadcastQueueSize
) {
}
