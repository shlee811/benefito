package com.lion.benefito.domain.scrap.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class ScrapResDTO {

    @Builder
    public record MyScrapList(
            List<MyScrap> scraps
    ) {
    }

    @Builder
    public record MyScrap(
            Long scrapId,
            Long postingId,
            String title,
            String company,
            LocalDate endDate,
            String categoryName,
            Long dDay
    ) {
    }

    @Builder
    public record ScrapSummary(
            Long totalCount,
            Long weeklyDeadlineCount
    ) {
    }
}
