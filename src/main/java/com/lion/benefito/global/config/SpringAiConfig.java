package com.lion.benefito.global.config;

import com.lion.benefito.infrastructure.ai.embedding.OpenAiEmbeddingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAiEmbeddingProperties.class)
public class SpringAiConfig {

    // 이 프로젝트의 Spring Boot 4.1.0 설정에서는 RestClient.Builder가 자동 등록되지 않아 직접 빈으로 등록한다.
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
