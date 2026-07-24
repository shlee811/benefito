package com.lion.benefito.global.security;

import java.util.Optional;

public interface CurrentUserProvider {

    Long getCurrentUserId();

    // 공개 API(비로그인도 허용)에서 "로그인했다면 개인화, 아니면 기본 동작"을 구현할 때 사용한다.
    // getCurrentUserId()와 달리 인증 정보가 없어도 예외를 던지지 않고 빈 값을 반환한다.
    Optional<Long> getCurrentUserIdOrEmpty();
}
