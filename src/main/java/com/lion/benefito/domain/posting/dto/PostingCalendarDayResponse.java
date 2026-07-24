package com.lion.benefito.domain.posting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "캘린더 날짜별 집계 및 미리보기 응답")
public record PostingCalendarDayResponse(
        @Schema(description = "날짜", example = "2026-07-01") LocalDate date,
        @Schema(description = "해당 날짜 시작 공고 수", example = "211") long startCount,
        @Schema(description = "해당 날짜 마감 공고 수", example = "3") long deadlineCount,
        @Schema(description = "startCount + deadlineCount", example = "214") long totalCount,
        @Schema(description = "미리보기 항목(최대 5개, START 우선 후 DEADLINE)") List<PostingCalendarItemResponse> items,
        @Schema(description = "totalCount - items.size() (0 이상)", example = "209") long remainingCount
) {
}
