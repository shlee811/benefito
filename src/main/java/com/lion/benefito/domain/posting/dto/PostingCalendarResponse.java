package com.lion.benefito.domain.posting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "월별 공고 캘린더 응답")
public record PostingCalendarResponse(
        @Schema(description = "연도", example = "2026") int year,
        @Schema(description = "월", example = "7") int month,
        @Schema(description = "마감/시작 이벤트가 있는 날짜별 집계 목록") List<PostingCalendarDayResponse> days
) {

    public static PostingCalendarResponse from(int year, int month, List<PostingCalendarDayResponse> days) {
        return new PostingCalendarResponse(year, month, days);
    }
}
