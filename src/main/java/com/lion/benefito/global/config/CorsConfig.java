package com.lion.benefito.global.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collections;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CorsConfig implements WebMvcConfigurer {

    public static CorsConfigurationSource apiConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        //데이터 교환이 가능한 URL 지정
        ArrayList<String> allowedOrigins = new ArrayList<>();
        allowedOrigins.add("http://localhost:8080");
        allowedOrigins.add("http://localhost:3000");
        allowedOrigins.add("http://127.0.0.1:8080");
        allowedOrigins.add("http://127.0.0.1:3000");
        allowedOrigins.add("https://benefito-frontend-deployment-eta.vercel.app");

        //허용하는 HTTP METHOD 지정
        ArrayList<String> allowedHttpMethods = new ArrayList<>();
        allowedHttpMethods.add("GET");
        allowedHttpMethods.add("POST");
        allowedHttpMethods.add("PUT");
        allowedHttpMethods.add("PATCH");
        allowedHttpMethods.add("DELETE");
        allowedHttpMethods.add("OPTIONS"); // 브라우저 사전 요청(Preflight) 허용용

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedHttpMethods);

        // 허용 헤더 설정
        configuration.setAllowedHeaders(Collections.singletonList("*"));

        // 쿠키 및 인증 정보(Authorization 헤더 등) 전송 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}