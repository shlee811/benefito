package com.lion.benefito.domain.posting.dto;

import com.lion.benefito.domain.posting.entity.Posting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "공고 상세 응답")
public record PostingDetailResponse(
        @Schema(description = "공고 ID") Long id,
        @Schema(description = "제목") String title,
        @Schema(description = "회사명") String company,
        @Schema(description = "상세 내용") String content,
        @Schema(description = "카테고리 코드") String category,
        @Schema(description = "지역 코드") String region,
        @Schema(description = "마감일") LocalDate endDate,
        @Schema(description = "기업 형태") String enterpriseType,
        @Schema(description = "고용 형태") String employmentType,
        @Schema(description = "급여") String salary,
        @Schema(description = "지원 자격") String qualification,
        @Schema(description = "홈페이지 URL") String homepageUrl,
        @Schema(description = "조회수") long viewCount,
        @Schema(description = "등록일시") LocalDateTime createdAt,
        @Schema(description = "수정일시") LocalDateTime updatedAt
) {

    public static PostingDetailResponse from(Posting posting) {
        return new PostingDetailResponse(
                posting.getId(),
                posting.getTitle(),
                posting.getCompany(),
                posting.getContent(),
                posting.getCategoryCode() != null ? posting.getCategoryCode().getCode() : null,
                posting.getRegionCode() != null ? posting.getRegionCode().getCode() : null,
                posting.getEndDate(),
                posting.getEnterpriseType(),
                posting.getEmploymentType(),
                posting.getSalary(),
                posting.getQualification(),
                posting.getHomepageUrl(),
                posting.getViewCount(),
                posting.getCreatedAt(),
                posting.getUpdatedAt()
        );
    }
}
