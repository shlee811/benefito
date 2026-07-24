package com.lion.benefito.global.config;

import com.lion.benefito.infrastructure.ai.chat.OpenAiChatProperties;
import com.lion.benefito.infrastructure.ai.embedding.OpenAiEmbeddingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({OpenAiEmbeddingProperties.class, OpenAiChatProperties.class})
public class SpringAiConfig {

    // 이 프로젝트의 Spring Boot 4.1.0 설정에서는 RestClient.Builder가 자동 등록되지 않아 직접 빈으로 등록한다.
    // connect/read timeout을 명시하지 않으면 외부 API가 응답 없이 연결만 유지할 때 호출 스레드가 무한정
    // 블로킹된다(공공기관 채용정보 API 실제 호출 중 스레드 덤프로 확인된 실제 장애 상황). JdkClientHttpRequestFactory는
    // 이 프로젝트의 Spring 버전에서 스트리밍 요청 경로의 readTimeout을 실제로 적용하지 않는 것이 스레드 덤프로
    // 재현되어(readTimeout(30s)을 설정해도 CompletableFuture.get()이 인자 없이 무한 대기), 대신 HttpURLConnection
    // 기반의 SimpleClientHttpRequestFactory를 사용한다(connect/read timeout이 확실하게 적용됨).
    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
