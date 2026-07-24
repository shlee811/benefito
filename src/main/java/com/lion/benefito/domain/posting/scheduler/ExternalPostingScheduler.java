package com.lion.benefito.domain.posting.scheduler;

import com.lion.benefito.domain.posting.dto.ExternalPostingImportResponse;
import com.lion.benefito.domain.posting.service.ExternalPostingIngestService;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import com.lion.benefito.infrastructure.external.kstartup.KStartupPostingSource;
import com.lion.benefito.infrastructure.external.publicinstitutionjob.PublicInstitutionJobPostingSource;
import com.lion.benefito.infrastructure.external.qnet.QNetPostingSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * API 기반 외부 공고 소스(K-Startup/공공기관 채용/Q-Net)만 매일 자동 수집한다.
 * CSV 기반 소스(장학금, 공모전/대외활동 시드)는 사람이 파일을 관리하므로 대상에서 제외하고
 * 기존처럼 관리자 Import API로만 수동 실행한다.
 *
 * Swagger Import API와 완전히 동일한 ExternalPostingIngestService.sync(sourceType, source)를 그대로
 * 호출한다 - Controller를 거치지 않고 두 진입점이 같은 서비스 로직(sync history 기록, 소스별 락,
 * initialSync/incrementalSync 자동 분기)을 공유하는 구조다.
 *
 * 작업들은 서로 독립된 @Scheduled 메서드라 트리거 자체가 분리돼 있고, 메서드 내부에서도
 * 예외를 잡아 로그만 남기므로 한 작업의 실패가 다른 작업 실행에 영향을 주지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "external.scheduler", name = "enabled", havingValue = "true")
public class ExternalPostingScheduler {

    private final ExternalPostingIngestService externalPostingIngestService;
    private final KStartupPostingSource kStartupPostingSource;
    private final PublicInstitutionJobPostingSource publicInstitutionJobPostingSource;
    private final QNetPostingSource qNetPostingSource;

    @Scheduled(cron = "${external.scheduler.kstartup-cron}")
    public void importKStartupPostings() {
        runSync("KSTARTUP", "K-Startup", kStartupPostingSource);
    }

    @Scheduled(cron = "${external.scheduler.public-institution-job-cron}")
    public void importPublicInstitutionJobPostings() {
        runSync("PUBLIC_INSTITUTION_JOB", "공공기관 채용정보", publicInstitutionJobPostingSource);
    }

    @Scheduled(cron = "${external.scheduler.qnet-cron}")
    public void importQNetPostings() {
        runSync("QNET", "Q-Net 자격시험 일정", qNetPostingSource);
    }

    // 소스 무관하게 마감일이 지난 공고를 CLOSED로 전환한다. contentHash가 그대로라 재수집 때
    // 건드려지지 않는 공고도 이 작업으로 상태가 갱신된다.
    @Scheduled(cron = "${external.scheduler.close-expired-cron}")
    public void closeExpiredPostings() {
        try {
            externalPostingIngestService.closeExpiredPostings();
        } catch (Exception e) {
            log.error("마감 공고 정리 작업 실패: {}", e.getMessage(), e);
        }
    }

    private void runSync(String sourceType, String label, ExternalPostingSource source) {
        log.info("[{}] 스케줄러 자동 수집 시작", label);
        try {
            ExternalPostingImportResponse result = externalPostingIngestService.sync(sourceType, source);
            log.info("[{}] 스케줄러 자동 수집 종료 - 생성 {}건, 수정 {}건, 종료전환 {}건, 실패 {}건",
                    label, result.createdCount(), result.updatedCount(), result.closedCount(), result.failedCount());
        } catch (Exception e) {
            log.error("[{}] 스케줄러 자동 수집 실패: {}", label, e.getMessage(), e);
        }
    }
}
