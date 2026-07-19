package com.lion.benefito.global.config;

import com.lion.benefito.global.security.exception.JwtAccessDeniedHandler;
import com.lion.benefito.global.security.exception.JwtAuthenticationEntryPoint;
import com.lion.benefito.global.security.filter.CustomLoginFilter;
import com.lion.benefito.global.security.filter.JwtAuthorizationFilter;
import com.lion.benefito.global.security.handler.CustomLogoutHandler;
import com.lion.benefito.global.security.handler.CustomLogoutSuccessHandler;
import com.lion.benefito.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomLogoutHandler customLogoutHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final RedisTemplate<String, Object> redisTemplate;


    //인증이 필요하지 않은 url
    private final String[] allowUrl = {
            "/auth/login", // 로그인은 인증이 필요하지 않음
            "/auth/signup", // 회원가입은 인증이 필요하지 않음
            "/auth/reissue", // 토큰 재발급은 인증이 필요하지 않음
            "/auth/**",
            "/swagger-ui/**", // swagger 관련 URL
            "/api-docs/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        CustomLoginFilter loginFilter = new CustomLoginFilter(authenticationManager(authenticationConfiguration), jwtUtil);
        loginFilter.setFilterProcessesUrl("/api/v1/auth/login");

        http
                .authorizeHttpRequests(request -> request
                        .requestMatchers(allowUrl).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthorizationFilter(jwtUtil, redisTemplate), UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(HttpBasicConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout") // 로그아웃을 요청할 API 주소 설정
                        .addLogoutHandler(customLogoutHandler) // 비즈니스 로직(DB 삭제 등)을 처리할 핸들러 등록
                        .logoutSuccessHandler(customLogoutSuccessHandler) // 로그아웃 성공 후 JSON 반환 핸들러 등록
                )
        ;

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
}
