package com.lion.benefito.domain.posting.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.posting.dto.ExternalPostingImportResponse;
import com.lion.benefito.domain.posting.service.ExternalPostingIngestService;
import com.lion.benefito.infrastructure.external.kstartup.KStartupPostingSource;
import com.lion.benefito.infrastructure.external.publicinstitutionjob.PublicInstitutionJobPostingSource;
import com.lion.benefito.infrastructure.external.qnet.QNetPostingSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalPostingSchedulerTest {

    private ExternalPostingIngestService externalPostingIngestService;
    private KStartupPostingSource kStartupPostingSource;
    private PublicInstitutionJobPostingSource publicInstitutionJobPostingSource;
    private QNetPostingSource qNetPostingSource;
    private ExternalPostingScheduler scheduler;

    @BeforeEach
    void setUp() {
        externalPostingIngestService = mock(ExternalPostingIngestService.class);
        kStartupPostingSource = mock(KStartupPostingSource.class);
        publicInstitutionJobPostingSource = mock(PublicInstitutionJobPostingSource.class);
        qNetPostingSource = mock(QNetPostingSource.class);
        scheduler = new ExternalPostingScheduler(externalPostingIngestService, kStartupPostingSource,
                publicInstitutionJobPostingSource, qNetPostingSource);
    }

    private ExternalPostingImportResponse response() {
        return new ExternalPostingImportResponse(10, 5, 3, 2, 0, 8, 0, 0);
    }

    @Test
    void K_Startup_스케줄러는_ExternalPostingIngestService에_KSTARTUP_sourceType과_source를_전달한다() {
        when(externalPostingIngestService.sync("KSTARTUP", kStartupPostingSource)).thenReturn(response());

        scheduler.importKStartupPostings();

        verify(externalPostingIngestService).sync("KSTARTUP", kStartupPostingSource);
    }

    @Test
    void 공공기관_채용정보_스케줄러는_PUBLIC_INSTITUTION_JOB_sourceType과_source를_전달한다() {
        when(externalPostingIngestService.sync("PUBLIC_INSTITUTION_JOB", publicInstitutionJobPostingSource))
                .thenReturn(response());

        scheduler.importPublicInstitutionJobPostings();

        verify(externalPostingIngestService).sync("PUBLIC_INSTITUTION_JOB", publicInstitutionJobPostingSource);
    }

    @Test
    void Q_Net_스케줄러는_QNET_sourceType과_source를_전달한다() {
        when(externalPostingIngestService.sync("QNET", qNetPostingSource)).thenReturn(response());

        scheduler.importQNetPostings();

        verify(externalPostingIngestService).sync("QNET", qNetPostingSource);
    }

    @Test
    void 한_작업이_실패해도_예외가_스케줄러_밖으로_전파되지_않는다() {
        when(externalPostingIngestService.sync("KSTARTUP", kStartupPostingSource))
                .thenThrow(new RuntimeException("API 호출 실패"));

        assertThatCode(() -> scheduler.importKStartupPostings()).doesNotThrowAnyException();
    }

    @Test
    void 한_작업의_실패는_다른_작업_실행에_영향을_주지_않는다() {
        when(externalPostingIngestService.sync("KSTARTUP", kStartupPostingSource))
                .thenThrow(new RuntimeException("K-Startup 실패"));
        when(externalPostingIngestService.sync("PUBLIC_INSTITUTION_JOB", publicInstitutionJobPostingSource))
                .thenReturn(response());
        when(externalPostingIngestService.sync("QNET", qNetPostingSource)).thenReturn(response());

        scheduler.importKStartupPostings();
        scheduler.importPublicInstitutionJobPostings();
        scheduler.importQNetPostings();

        verify(externalPostingIngestService).sync("PUBLIC_INSTITUTION_JOB", publicInstitutionJobPostingSource);
        verify(externalPostingIngestService).sync("QNET", qNetPostingSource);
    }

    @Test
    void 마감_공고_정리_스케줄러는_closeExpiredPostings를_호출한다() {
        scheduler.closeExpiredPostings();

        verify(externalPostingIngestService).closeExpiredPostings();
    }

    @Test
    void 마감_공고_정리가_실패해도_예외가_스케줄러_밖으로_전파되지_않는다() {
        when(externalPostingIngestService.closeExpiredPostings()).thenThrow(new RuntimeException("DB 오류"));

        assertThatCode(() -> scheduler.closeExpiredPostings()).doesNotThrowAnyException();
    }

    @Test
    void K_Startup_실패가_마감_공고_정리_호출에는_영향을_주지_않는다() {
        when(externalPostingIngestService.sync("KSTARTUP", kStartupPostingSource))
                .thenThrow(new RuntimeException("K-Startup 실패"));

        scheduler.importKStartupPostings();
        scheduler.closeExpiredPostings();

        verify(externalPostingIngestService).closeExpiredPostings();
        verify(externalPostingIngestService, never()).sync("PUBLIC_INSTITUTION_JOB", publicInstitutionJobPostingSource);
    }
}
