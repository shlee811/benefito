package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;

public enum PostingFilter {

    ALL,
    // TODO: ERD에 recommended 컬럼이 없어 추천 로직은 미구현 상태. 현재는 ALL과 동일하게 동작한다.
    RECOMMENDED;

    public static PostingFilter from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return PostingFilter.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PostingException(PostingErrorCode.INVALID_POSTING_FILTER);
        }
    }
}
