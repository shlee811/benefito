package com.lion.benefito.global.security;

import org.springframework.stereotype.Component;

@Component
public class MockCurrentUserProvider implements CurrentUserProvider {

    private static final Long MOCK_USER_ID = 1L;

    @Override
    public Long getCurrentUserId() {
        // TODO: Security(JWT) 도입 후 SecurityContext 기반 구현체로 교체
        return MOCK_USER_ID;
    }
}
