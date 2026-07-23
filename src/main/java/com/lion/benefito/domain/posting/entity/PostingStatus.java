package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;

public enum PostingStatus {
    ACTIVE,
    CLOSED;

    // 캘린더 조회의 status 파라미터 파싱 전용. 값이 없으면(기본 정책) null을 반환해 상태 필터를
    // 아예 적용하지 않는다(ACTIVE/CLOSED 모두 이력으로 표시). PostingFilter/PostingSort와 달리
    // "미지정 시 기본값으로 대체"가 아니라 "미지정 시 필터 없음"이 의도된 동작이다.
    public static PostingStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PostingStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PostingException(PostingErrorCode.INVALID_POSTING_STATUS);
        }
    }
}
