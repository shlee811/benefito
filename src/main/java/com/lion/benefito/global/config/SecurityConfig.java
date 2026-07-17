package com.lion.benefito.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // POST/DELETE 등 상태 변경 요청이 기본 CSRF 보호로 인해 403이 발생하는 문제만 해결한다.
        // 다른 경로의 CSRF/인가 설정은 변경하지 않는다.
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/chatbot/**", "/api/v1/scraps/**"))
                .build();
    }
}
