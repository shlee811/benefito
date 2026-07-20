package com.lion.benefito.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

        @Bean
        public OpenAPI BenefitoAPI() {
                String securityJwtName = "JWT";

                // 1. API 요청 시 JWT 보안 검증 요구사항 등록
                SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityJwtName);

                // 2. Swagger UI 헤더에 주입할 Bearer Authentication Scheme 구성
                Components components = new Components()
                                .addSecuritySchemes(securityJwtName, new SecurityScheme()
                                                .name(securityJwtName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT"));

                return new OpenAPI()
                                .addServersItem(new Server().url("/"))
                                .info(new Info()
                                                .title("BENEFITO API 명세서")
                                                .description("BENEFITO 백엔드 API 명세서입니다.")
                                                .version("1.0.0"))
                                .addSecurityItem(securityRequirement)
                                .components(components);
        }
}
