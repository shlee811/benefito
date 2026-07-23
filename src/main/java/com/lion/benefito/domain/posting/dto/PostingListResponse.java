package com.lion.benefito.domain.posting.dto;

import com.lion.benefito.domain.posting.entity.Posting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "공고 목록 응답")
public record PostingListResponse(
        @Schema(description = "공고 ID", example = "1") Long id,
        @Schema(description = "제목", example = "2026 상반기 신입 백엔드 개발자 채용") String title,
        @Schema(description = "회사명", example = "라이언") String company,
        @Schema(description = "카테고리 코드", example = "JOB") String category,
        @Schema(description = "지역 코드", example = "SEOUL") String region,
        @Schema(description = "마감일", example = "2026-08-31") LocalDate endDate,
        @Schema(description = "조회수", example = "10") long viewCount,
        @Schema(description = "추천 공고 여부(관심 카테고리 일치 또는 마감 7일 이내인 ACTIVE 공고)", example = "true")
        boolean isRecommended
) {

    public static PostingListResponse from(Posting posting) {
        return from(posting, List.of());
    }

    public static PostingListResponse from(Posting posting, List<Long> interestCategoryIds) {
        return new PostingListResponse(
                posting.getId(),
                posting.getTitle(),
                posting.getCompany(),
                posting.getCategoryCode() != null ? posting.getCategoryCode().getCode() : null,
                posting.getRegionCode() != null ? posting.getRegionCode().getCode() : null,
                posting.getEndDate(),
                posting.getViewCount(),
                posting.isRecommended(interestCategoryIds)
        );
    }
}
