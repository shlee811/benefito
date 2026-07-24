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
        @Schema(description = "시작일(원서접수 시작일 등, 없으면 null)", example = "2026-08-01") LocalDate startDate,
        @Schema(description = "마감일", example = "2026-08-31") LocalDate endDate,
        @Schema(description = "조회수", example = "10") long viewCount,
        @Schema(description = "추천 공고 여부(관심 카테고리 일치 또는 마감 7일 이내인 ACTIVE 공고)", example = "true")
        boolean isRecommended,
        @Schema(description = "수집 소스(QNET, KSTARTUP 등, 수동 등록 공고는 null)", example = "QNET") String source,
        @Schema(description = "원문 링크", example = "https://www.q-net.or.kr") String link
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
                posting.getStartDate(),
                posting.getEndDate(),
                posting.getViewCount(),
                posting.isRecommended(interestCategoryIds),
                posting.getSourceType(),
                posting.getHomepageUrl()
        );
    }
}
