package com.lion.benefito.domain.posting.dto;

import com.lion.benefito.domain.posting.entity.CalendarEventType;
import com.lion.benefito.domain.posting.entity.Posting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "캘린더 날짜 셀 미리보기 항목")
public record PostingCalendarItemResponse(
        @Schema(description = "공고 ID", example = "1") Long postingId,
        @Schema(description = "이벤트 타입(START/DEADLINE)", example = "START") CalendarEventType type,
        @Schema(description = "제목", example = "A기업 인턴 모집") String title,
        @Schema(description = "기관명", example = "A기업") String organization,
        @Schema(description = "카테고리 코드", example = "INTERN") String categoryCode,
        @Schema(description = "지역 코드", example = "SEOUL") String region,
        @Schema(description = "시작일(임시로 createdAt 날짜 부분을 사용, PostingService 참고)", example = "2026-07-01")
        LocalDate startDate,
        @Schema(description = "마감일", example = "2026-07-31") LocalDate deadline
) {

    public static PostingCalendarItemResponse from(Posting posting, CalendarEventType type) {
        return new PostingCalendarItemResponse(
                posting.getId(),
                type,
                posting.getTitle(),
                posting.getCompany(),
                posting.getCategoryCode() != null ? posting.getCategoryCode().getCode() : null,
                posting.getRegionCode() != null ? posting.getRegionCode().getCode() : null,
                posting.getCreatedAt() != null ? posting.getCreatedAt().toLocalDate() : null,
                posting.getEndDate()
        );
    }
}
