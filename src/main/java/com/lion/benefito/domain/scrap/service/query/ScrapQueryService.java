package com.lion.benefito.domain.scrap.service.query;

import com.lion.benefito.domain.scrap.dto.response.ScrapResDTO;

public interface ScrapQueryService {

    ScrapResDTO.MyScrapList getMyScrapList(Long userId, String categoryCode);

    ScrapResDTO.MyScrapList getWeeklyDeadlineScrapList(Long userId, String categoryCode);

    ScrapResDTO.ScrapSummary getScrapSummary(Long userId);
}
