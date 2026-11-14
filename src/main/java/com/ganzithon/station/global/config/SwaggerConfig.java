package com.ganzithon.station.global.config;

import com.ganzithon.station.global.security.CurrentUser;
import com.ganzithon.station.global.security.GuestTokenHeader;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 문서: {서버 주소}/swagger-ui/index.html
 * 오른쪽 위 Authorize에서 계정 토큰(Bearer)이나 게스트 토큰(X-Guest-Token)을 넣고 API를 호출해 볼 수 있습니다.
 * WebSocket 메시지는 Swagger에 나오지 않으므로 README의 메시지 명세 표를 봅니다.
 */
@Configuration
public class SwaggerConfig {

    private static final String ACCOUNT_TOKEN = "accountToken";
    private static final String GUEST_TOKEN = "guestToken";

    static {
        // @CurrentUser 파라미터는 서버가 채우는 값이라 문서에서 숨깁니다
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("잠시 정차합니다 API")
                        .description("2026 간지톤 6팀. 모든 응답은 { success, data, error } 형식입니다.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ACCOUNT_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("가입·로그인 응답의 accessToken"))
                        .addSecuritySchemes(GUEST_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(GuestTokenHeader.NAME)
                                .description("비로그인 승차권 발급 응답의 guestToken")))
                .addSecurityItem(new SecurityRequirement().addList(ACCOUNT_TOKEN))
                .addSecurityItem(new SecurityRequirement().addList(GUEST_TOKEN));
    }
}
