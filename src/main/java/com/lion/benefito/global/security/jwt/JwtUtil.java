package com.lion.benefito.global.security.jwt;

import com.lion.benefito.global.security.jwt.dto.JwtDTO;
import com.lion.benefito.global.security.jwt.entity.Token;
import com.lion.benefito.global.security.jwt.repository.TokenRepository;
import com.lion.benefito.global.security.userdetails.CustomUserDetails;
import com.lion.benefito.global.security.userdetails.service.CustomUserDetailsService;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Long accessExpMs;
    private final Long refreshExpMs;
    private final TokenRepository tokenRepository;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtUtil(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.token.access-expiration-time}") Long access,
            @Value("${spring.jwt.token.refresh-expiration-time}") Long refresh,
            TokenRepository tokenRepo,
            CustomUserDetailsService customUserDetailsService) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
        this.accessExpMs = access;
        this.refreshExpMs = refresh;
        this.tokenRepository = tokenRepo;
        this.customUserDetailsService = customUserDetailsService;
    }

    // JWT 토큰을 입력으로 받아 토큰의 subject 로부터 사용자 LoginId 추출하는 메서드
    public String getLoginId(String token) {

        log.info("[ JwtUtil ] 토큰에서 LoginId 를 추출합니다.");

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            throw new SecurityException("잘못된 토큰입니다.");
        }
    }

    // JWT 토큰을 입력으로 받아 클레임으로부터 사용자 DB 식별자(PK) 추출하는 메서드
    public Long getUserId(String token) {

        log.info("[ JwtUtil ] 토큰에서 UserId 를 추출합니다.");

        try {
            Number idObj = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("id", Number.class);
            return idObj != null ? idObj.longValue() : null;
        } catch (Exception e) {
            throw new SecurityException("잘못된 토큰입니다.");
        }
    }

    // Token 발급하는 메서드
    public String tokenProvider(CustomUserDetails customUserDetails, Instant expiration) {

        log.info("[ JwtUtil ] 토큰을 새로 발급합니다.");
        // 현재 시간
        Instant issuedAt = Instant.now();

        // 토큰에 부여할 권한
        String authorities = customUserDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // 유저 고유 PK ID 추출 (안전한 방어 코드 적용)
        Long userId = (customUserDetails.getUser() != null) ? customUserDetails.getUser().getId() : null;

        return Jwts.builder()
                .header()
                .add("typ", "JWT") // JWT type
                .and()
                .subject(customUserDetails.getUsername())
                .claim("id", userId) // 유저 ID(PK) 추가
                .claim("role", authorities) // 권한 추가
                .issuedAt(Date.from(issuedAt)) // 현재 시간 추가
                .expiration(Date.from(expiration)) // 만료 시간 추가
                .signWith(secretKey) // signature 추가
                .compact();
    }

    // principalDetails 객체에 대해 새로운 JWT 액세스 토큰을 생성
    public String createJwtAccessToken(CustomUserDetails customUserDetails) {
        Instant expiration = Instant.now().plusMillis(accessExpMs);
        return tokenProvider(customUserDetails, expiration);
    }

    // principalDetails 객체에 대해 새로운 JWT 리프레시 토큰을 생성
    public String createJwtRefreshToken(CustomUserDetails customUserDetails) {
        Instant expiration = Instant.now().plusMillis(refreshExpMs);
        String refreshToken = tokenProvider(customUserDetails, expiration);

        // DB에 Refresh Token 저장
        tokenRepository.save(Token.builder()
                .loginId(customUserDetails.getUsername())
                .token(refreshToken)
                .build()
        );

        return refreshToken;
    }

    // 제공된 리프레시 토큰을 기반으로 JwtDto 쌍을 다시 발급
    public JwtDTO reissueToken(String refreshToken) {

        log.info("[ JwtUtil ] 새로운 토큰을 재발급 합니다.");

        // 1. refreshToken 에서 user 의 loginId를 추출
        String loginId = getLoginId(refreshToken);

        // 2. DB에 저장된 현재 활성 Refresh Token을 가져와 대조 검증 (RTR 적용)
        Token savedToken = tokenRepository.findByLoginId(loginId)
                .orElseThrow(() -> new SecurityException("로그아웃 되었거나 세션이 만료되었습니다. 다시 로그인해 주세요."));

        if (!savedToken.getToken().equals(refreshToken)) {
            throw new SecurityException("이미 사용되었거나 만료된 Refresh Token입니다. 다시 로그인해 주세요.");
        }

        // 3. 주입 받은 customUserDetailsService를 사용해 DB에서 최신 유저 엔티티가 담긴 UserDetails를 가져옵니다.
        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(loginId);

        // 4. 재발급
        return new JwtDTO(
                createJwtAccessToken(userDetails),
                createJwtRefreshToken(userDetails),
                userDetails.getUser().getName()
        );
    }

    // HTTP 요청의 'Authorization' 헤더에서 JWT 액세스 토큰을 검색
    public String resolveAccessToken(HttpServletRequest request) {

        log.info("[ JwtUtil ] 헤더에서 토큰을 추출합니다.");
        String tokenFromHeader = request.getHeader("Authorization");

        if (tokenFromHeader == null || !tokenFromHeader.startsWith("Bearer ")) {
            log.warn("[ JwtUtil ] Request Header 에 토큰이 존재하지 않습니다.");
            return null;
        }

        log.info("[ JwtUtil ] 헤더에 토큰이 존재합니다.");

        return tokenFromHeader.split(" ")[1]; // Bearer 와 분리
    }

    // 토큰 유효성 검증
    public void validateToken(String token) {

        log.info("[ JwtUtil ] 토큰의 유효성을 검증합니다.");

        try {
            // 구문 분석 시스템의 시계가 JWT를 생성한 시스템의 시계 오차 고려
            // 약 3분 허용.
            long seconds = 3 * 60;
            boolean isExpired = Jwts
                    .parser()
                    .clockSkewSeconds(seconds)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .before(new Date());
            if (isExpired) {
                log.info("만료된 JWT 토큰입니다.");
            }
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            // 원하는 Exception throw
            throw new SecurityException("잘못된 토큰입니다.");
        } catch (ExpiredJwtException e) {
            // 원하는 Exception throw
            throw new ExpiredJwtException(null, null, "만료된 JWT 토큰입니다.");
        }
    }

    // JWT 토큰을 입력으로 받아 토큰의 만료 시간을 가져오는 메서드
    public Date getExpirationDate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (Exception e) {
            throw new SecurityException("잘못된 토큰입니다.");
        }
    }

    // Jwt 토큰을 입력으로 받아 토큰의 남은 만료 시간(밀리초) 계산 메서드
    public Long getExpirationTimeMillis(String token) {
        Date expiration = getExpirationDate(token);
        long now = System.currentTimeMillis();
        return Math.max(0, expiration.getTime() - now);
    }
}
