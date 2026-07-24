package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;
import org.springframework.data.domain.Sort;

public enum PostingSort {

    // deadline이 null인 공고는 명시적으로 nullsLast()를 지정해 항상 맨 뒤로 보낸다(Postgres 기본
    // 동작도 NULLS LAST이지만, DB 기본값에 암묵적으로 의존하지 않기 위해 명시한다).
    // 마감일이 같으면 updatedAt DESC, 그마저 같으면 id DESC로 순서를 결정적으로 만든다.
    DEADLINE(Sort.by(
            Sort.Order.asc("endDate").nullsLast(),
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("id"))),
    // 실제 스크랩 수(Posting.scrapCount, scraps 테이블 기준 비정규화 값) 내림차순으로 정렬한다.
    // 스크랩 수가 같으면 updatedAt DESC, 그마저 같으면 id DESC를 보조 정렬 기준으로 사용한다.
    POPULAR(Sort.by(Sort.Direction.DESC, "scrapCount", "updatedAt", "id")),
    // "최근 업데이트된 순서"가 기준이라 updatedAt을 1순위로 사용한다(deadline은 최신순 기준으로 쓰지 않는다).
    // updatedAt이 같으면 createdAt DESC, 그마저 같으면 id DESC를 보조 정렬 기준으로 사용한다.
    LATEST(Sort.by(Sort.Direction.DESC, "updatedAt", "createdAt", "id"));

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
