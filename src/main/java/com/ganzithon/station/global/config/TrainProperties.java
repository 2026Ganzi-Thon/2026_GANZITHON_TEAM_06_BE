package com.ganzithon.station.global.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 열차 간격(초). 환경변수 TRAIN_INTERVAL_SECONDS: 실제 서비스 30분(1800), 시연 영상 촬영 20초.
 * 지나간 열차 수 = 경과 시간 ÷ interval. 털실 수도 같은 값으로 계산합니다.
 * 값이 없거나 숫자가 아니면 서버가 시작하지 않습니다.
 */
@Validated
@ConfigurationProperties(prefix = "station.train")
public record TrainProperties(
        @Positive int intervalSeconds
) {
    public Duration interval() {
        return Duration.ofSeconds(intervalSeconds);
    }
}
