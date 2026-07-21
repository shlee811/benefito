package com.lion.benefito.global.security;

import com.lion.benefito.domain.auth.exception.AuthErrorCode;
import com.lion.benefito.domain.auth.exception.AuthException;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.exception.UserException;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * JwtAuthorizationFilter는 SecurityContext에 principal=CustomUserDetails를 저장하고,
 * 그 안의 User는 JWT claim(id, loginId)만으로 구성한 가상 객체다(DB 조회 없이 토큰에서 바로 생성됨).
 * 따라서 여기서는 principal에서 userId만 뽑아 쓰지 않고, 그 사용자가 실제로 존재하고(탈퇴하지
 * 않았고) 유효한지 UserRepository로 한 번 더 확인한 뒤 반환한다.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {

    private static final String ANONYMOUS_PRINCIPAL = "anonymousUser";

    private final UserRepository userRepository;

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorCode._UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (ANONYMOUS_PRINCIPAL.equals(principal)) {
            throw new AuthException(AuthErrorCode._UNAUTHORIZED);
        }

        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        User principalUser = customUserDetails.getUser();
        if (principalUser == null || principalUser.getId() == null) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        return userRepository.findByIdAndDeletedAtIsNull(principalUser.getId())
                .map(User::getId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
