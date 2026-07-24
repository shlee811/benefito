package com.lion.benefito.domain.posting.dto;

import com.lion.benefito.domain.posting.entity.PostingFilter;

public record PostingSearchCondition(
        String keyword,
        String region,
        String category,
        PostingFilter filter
) {
}
