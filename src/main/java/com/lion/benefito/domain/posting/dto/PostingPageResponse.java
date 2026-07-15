package com.lion.benefito.domain.posting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "공고 목록 페이지 응답")
public record PostingPageResponse(
        @Schema(description = "조회된 공고 목록") List<PostingListResponse> content,
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "전체 요소 수", example = "42") long totalElements,
        @Schema(description = "전체 페이지 수", example = "3") int totalPages,
        @Schema(description = "첫 페이지 여부") boolean first,
        @Schema(description = "마지막 페이지 여부") boolean last
) {

    public static PostingPageResponse from(Page<PostingListResponse> page) {
        return new PostingPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
