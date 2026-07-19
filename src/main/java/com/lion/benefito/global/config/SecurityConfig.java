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
import org.springframework.security.config.http.SessionCreationPolicy;
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
            "/chatbot/**",
            "/api/v1/scraps/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        CustomLoginFilter loginFilter = new CustomLoginFilter(authenticationManager(authenticationConfiguration), jwtUtil);
        loginFilter.setFilterProcessesUrl("/auth/login");

        http
                // 프론트엔드 연동을 위한 CORS 설정 추가
                .cors(cors -> cors.configurationSource(CorsConfig.apiConfigurationSource()))
                // JWT 구조에 맞게 서버 세션을 생성하지 않도록 무상태(Stateless) 강제
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

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
                        .logoutUrl("/auth/logout") // 로그아웃을 요청할 API 주소 설정
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
