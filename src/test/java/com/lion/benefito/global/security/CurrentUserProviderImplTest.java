package com.lion.benefito.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.auth.exception.AuthErrorCode;
import com.lion.benefito.domain.auth.exception.AuthException;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.exception.UserException;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.global.security.userdetails.CustomUserDetails;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserProviderImplTest {

    private UserRepository userRepository;
    private CurrentUserProviderImpl currentUserProvider;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        currentUserProvider = new CurrentUserProviderImpl(userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String loginId) {
        User principalUser = User.builder().id(userId).loginId(loginId).build();
        CustomUserDetails userDetails = new CustomUserDetails(principalUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void 로그인한_사용자_A의_userId를_반환한다() {
        authenticateAs(1L, "userA");
        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(User.builder().id(1L).loginId("userA").build()));

        Long userId = currentUserProvider.getCurrentUserId();

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void 로그인한_사용자_B의_userId를_반환한다() {
        authenticateAs(2L, "userB");
        when(userRepository.findByIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.of(User.builder().id(2L).loginId("userB").build()));

        Long userId = currentUserProvider.getCurrentUserId();

        assertThat(userId).isEqualTo(2L);
    }

    @Test
    void Authentication이_없으면_예외() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> currentUserProvider.getCurrentUserId())
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getCode())
                        .isEqualTo(AuthErrorCode._UNAUTHORIZED));
    }

    @Test
    void anonymousUser이면_예외() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThatThrownBy(() -> currentUserProvider.getCurrentUserId())
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getCode())
                        .isEqualTo(AuthErrorCode._UNAUTHORIZED));
    }

    @Test
    void 인증되지_않은_Authentication이면_예외() {
        Authentication notAuthenticated = new UsernamePasswordAuthenticationToken("user", "password");
        SecurityContextHolder.getContext().setAuthentication(notAuthenticated);

        assertThatThrownBy(() -> currentUserProvider.getCurrentUserId())
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getCode())
                        .isEqualTo(AuthErrorCode._UNAUTHORIZED));
    }

    @Test
    void principal이_CustomUserDetails가_아니면_INVALID_TOKEN_예외() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "그냥문자열principal", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> currentUserProvider.getCurrentUserId())
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getCode())
                        .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    void JWT_사용자에_해당하는_User가_존재하지_않으면_USER_NOT_FOUND_예외() {
        authenticateAs(99L, "삭제된사용자");
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserProvider.getCurrentUserId())
                .isInstanceOf(UserException.class)
                .satisfies(ex -> assertThat(((UserException) ex).getCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }
}
