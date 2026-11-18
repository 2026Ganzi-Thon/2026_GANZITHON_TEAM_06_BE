package com.ganzithon.station.global.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 열차 (TR-01, TR-02). 열차 1대 = 털실 1올. 계산은 Ticket.countPassedTrains.
 * 값이 없거나 숫자가 아니면 서버가 시작하지 않습니다.
 *
 * @param intervalSeconds 공용 열차 간격(초). 환경변수 TRAIN_INTERVAL_SECONDS: 실제 서비스 30분(1800), 시연 영상 촬영 20초
 * @param firstTrainDelay 입장 후 내 첫 열차까지 (TR-01 "첫 열차는 입장 후 10초 안에 통과", 10초)
 */
@Validated
@ConfigurationProperties(prefix = "station.train")
public record TrainProperties(
        @Positive int intervalSeconds,
        @NotNull Duration firstTrainDelay
) {
    public Duration interval() {
        return Duration.ofSeconds(intervalSeconds);
    }
}
