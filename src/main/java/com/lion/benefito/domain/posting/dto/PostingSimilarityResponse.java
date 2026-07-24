package com.lion.benefito.domain.posting.dto;

import com.lion.benefito.domain.posting.repository.PostingSimilarityProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "공고 유사도 검색 결과")
public record PostingSimilarityResponse(
        @Schema(description = "공고 ID", example = "1") Long postingId,
        @Schema(description = "제목") String title,
        @Schema(description = "내용") String content,
        @Schema(description = "카테고리 코드", example = "JOB") String category,
        @Schema(description = "기관/회사명") String organization,
        @Schema(description = "지역 코드", example = "SEOUL") String region,
        @Schema(description = "마감일") LocalDate deadline,
        @Schema(description = "원문 URL") String sourceUrl,
        @Schema(description = "유사도 = 1.0 - cosine distance. 값이 클수록 유사하다는 의미로만 사용하고, "
                + "코사인 거리 이론 범위(0~2)상 엄밀한 [0,1] 정규화 값은 아니다.")
        double similarity
) {

    public static PostingSimilarityResponse from(PostingSimilarityProjection projection) {
        double distance = projection.getDistance();
        double similarity = 1.0 - distance;
        return new PostingSimilarityResponse(
                projection.getId(),
                projection.getTitle(),
                projection.getContent(),
                projection.getCategoryCode(),
                projection.getCompany(),
                projection.getRegionCode(),
                projection.getEndDate(),
                projection.getSourceUrl(),
                similarity
        );
    }
}
