package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;
import org.springframework.data.domain.Sort;

public enum PostingSort {

    DEADLINE(Sort.by(Sort.Direction.ASC, "endDate")),
    POPULAR(Sort.by(Sort.Direction.DESC, "viewCount")),
    LATEST(Sort.by(Sort.Direction.DESC, "createdAt"));

    private final Sort sort;

    PostingSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    public static PostingSort from(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }
        try {
            return PostingSort.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PostingException(PostingErrorCode.INVALID_POSTING_SORT);
        }
    }
}
