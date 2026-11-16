package com.ganzithon.station.global.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 실시간 대기실 설정.
 *
 * @param seatCount 대기실 하나의 좌석 수 (환경변수 SEAT_COUNT, 기본 20). 다 차면 서서 기다립니다 (seatNumber = null)
 */
@Validated
@ConfigurationProperties(prefix = "station.realtime")
public record RealtimeProperties(
        @Positive int seatCount
) {
}
