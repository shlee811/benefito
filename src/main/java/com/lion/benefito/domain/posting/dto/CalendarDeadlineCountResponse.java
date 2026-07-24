package com.lion.benefito.domain.posting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "날짜별 마감 공고 개수 응답")
public record CalendarDeadlineCountResponse(
        @Schema(description = "마감일", example = "2026-07-10") LocalDate date,
        @Schema(description = "해당 날짜 마감 공고 개수", example = "3") long count
) {
}
