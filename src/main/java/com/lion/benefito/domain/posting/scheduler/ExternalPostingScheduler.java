package com.lion.benefito.domain.posting.scheduler;

import com.lion.benefito.domain.posting.dto.ExternalPostingImportResponse;
import com.lion.benefito.domain.posting.service.ExternalPostingIngestService;
import com.lion.benefito.infrastructure.external.kstartup.KStartupPostingSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 수동 실행(POST /api/v1/admin/external-postings/kstartup/import)으로 검증을 마친 뒤 켜는 정기 수집 작업.
 * external.kstartup.scheduler.enabled=true일 때만 빈이 등록되며, 기본값은 false다.
 * 스케줄러 자체에는 비즈니스 로직을 두지 않고 ExternalPostingIngestService만 호출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "external.kstartup.scheduler", name = "enabled", havingValue = "true")
public class ExternalPostingScheduler {

    private final ExternalPostingIngestService externalPostingIngestService;
    private final KStartupPostingSource kStartupPostingSource;

    @Scheduled(cron = "${external.kstartup.scheduler.cron}")
    public void importKStartupPostings() {
        log.info("K-Startup 공고 수집 스케줄러 실행");
        ExternalPostingImportResponse result = externalPostingIngestService.importFrom(kStartupPostingSource);
        log.info("K-Startup 공고 수집 결과: {}", result);
    }
}
