package com.lion.benefito.global.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.global.security.jwt.JwtUtil;
import com.lion.benefito.global.security.userdetails.CustomUserDetails;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// 인증이 필요한 요청이 401로 거부되는 세 가지 경우(헤더 없음/토큰 만료/블랙리스트 토큰) 모두
// 동일한 CustomResponse JSON 형식으로 응답하는지 검증한다. 수정 전에는 헤더가 없는 경우만
// JwtAuthenticationEntryPoint를 통해 JSON으로 응답했고, 토큰 만료/블랙리스트는
// JwtAuthorizationFilter가 순수 텍스트로 직접 응답해 프론트가 모든 401을 동일하게 파싱할 수 없었다.
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthorizationFilterIntegrationTest {

    private static final String PROTECTED_ENDPOINT = "/users/profile";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private CustomUserDetails userDetails(long id, String loginId) {
        User user = User.builder().id(id).loginId(loginId).build();
        return new CustomUserDetails(user);
    }

    @Test
    void Authorization_헤더가_없으면_JSON_형식의_401을_응답한다() throws Exception {
        MvcResult result = mockMvc.perform(get(PROTECTED_ENDPOINT))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"COMMON401\"");
    }

    @Test
    void 만료된_토큰이면_JSON_형식의_401을_응답한다() throws Exception {
        CustomUserDetails details = userDetails(1L, "jwtFilterExpiredUser");
        String expiredToken = jwtUtil.tokenProvider(details, Instant.now().minusSeconds(600));

        MvcResult result = mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + expiredToken))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"COMMON401_2\"");
    }

    @Test
    void 블랙리스트에_등록된_토큰이면_JSON_형식의_401을_응답한다() throws Exception {
        CustomUserDetails details = userDetails(2L, "jwtFilterBlacklistedUser");
        String validToken = jwtUtil.tokenProvider(details, Instant.now().plusSeconds(600));
        redisTemplate.opsForValue().set(validToken, "logout", 600, TimeUnit.SECONDS);

        try {
            MvcResult result = mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + validToken))
                    .andReturn();

            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"COMMON401_1\"");
        } finally {
            redisTemplate.delete(validToken);
        }
    }
}
