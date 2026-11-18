package com.ganzithon.station.global.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 시연용 가상 승객 자동 방송 (RM-05, application.yml station.virtual-passenger).
 *
 * @param enabled     켜기/끄기 (환경변수 VIRTUAL_PASSENGER_ENABLED, 기본 true). 문제가 생기면 서버 .env에서 끕니다
 * @param firstDelay  방에 실제 승객이 처음 들어오고 첫 가상 방송까지 (10초)
 * @param minInterval 다음 가상 방송까지 최소 (40초)
 * @param maxInterval 다음 가상 방송까지 최대 (90초). 그 사이에서 랜덤
 */
@Validated
@ConfigurationProperties(prefix = "station.virtual-passenger")
public record VirtualPassengerProperties(
        boolean enabled,
        @NotNull Duration firstDelay,
        @NotNull Duration minInterval,
        @NotNull Duration maxInterval
) {
}
