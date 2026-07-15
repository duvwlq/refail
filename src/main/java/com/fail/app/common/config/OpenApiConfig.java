package com.fail.app.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String JWT_SCHEME_NAME = "Bearer JWT";

    @Bean
    public OpenAPI failOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("실패 공유 서비스 API")
                        .description("실패를 기록하고 후속 시도와 성장을 공유하는 서비스의 백엔드 API")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(
                        JWT_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 응답의 accessToken을 입력합니다.")
                ));
    }
}
