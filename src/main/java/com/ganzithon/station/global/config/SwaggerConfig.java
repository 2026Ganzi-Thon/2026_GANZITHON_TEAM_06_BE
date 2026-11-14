package com.ganzithon.station.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 문서: {서버 주소}/swagger-ui/index.html
 * WebSocket 메시지는 Swagger에 나오지 않으므로 README의 메시지 명세 표를 봅니다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("잠시 정차합니다 API")
                        .description("2026 간지톤 6팀. 모든 응답은 { success, data, error } 형식입니다.")
                        .version("v1"));
    }
}
