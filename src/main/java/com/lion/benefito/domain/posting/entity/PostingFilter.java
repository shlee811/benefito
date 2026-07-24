package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;

public enum PostingFilter {

    ALL,
    // 마감이 임박한(Posting.RECOMMENDED_DEADLINE_WITHIN_DAYS 이내) ACTIVE 공고만 조회한다.
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
