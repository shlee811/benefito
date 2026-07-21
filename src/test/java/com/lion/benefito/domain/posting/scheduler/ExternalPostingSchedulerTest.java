package com.lion.benefito.domain.posting.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
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
        return new ExternalPostingImportResponse(10, 5, 3, 2, 0, 8, 0);
    }

    @Test
    void K_Startup_스케줄러는_ExternalPostingIngestService에_KStartupPostingSource를_전달한다() {
        when(externalPostingIngestService.importFrom(kStartupPostingSource)).thenReturn(response());

        scheduler.importKStartupPostings();

        verify(externalPostingIngestService).importFrom(kStartupPostingSource);
    }

    @Test
    void 공공기관_채용정보_스케줄러는_PublicInstitutionJobPostingSource를_전달한다() {
        when(externalPostingIngestService.importFrom(publicInstitutionJobPostingSource)).thenReturn(response());

        scheduler.importPublicInstitutionJobPostings();

        verify(externalPostingIngestService).importFrom(publicInstitutionJobPostingSource);
    }

    @Test
    void Q_Net_스케줄러는_QNetPostingSource를_전달한다() {
        when(externalPostingIngestService.importFrom(qNetPostingSource)).thenReturn(response());

        scheduler.importQNetPostings();

        verify(externalPostingIngestService).importFrom(qNetPostingSource);
    }

    @Test
    void 한_작업이_실패해도_예외가_스케줄러_밖으로_전파되지_않는다() {
        when(externalPostingIngestService.importFrom(kStartupPostingSource))
                .thenThrow(new RuntimeException("API 호출 실패"));

        assertThatCode(() -> scheduler.importKStartupPostings()).doesNotThrowAnyException();
    }

    @Test
    void 한_작업의_실패는_다른_작업_실행에_영향을_주지_않는다() {
        when(externalPostingIngestService.importFrom(kStartupPostingSource))
                .thenThrow(new RuntimeException("K-Startup 실패"));
        when(externalPostingIngestService.importFrom(publicInstitutionJobPostingSource)).thenReturn(response());
        when(externalPostingIngestService.importFrom(qNetPostingSource)).thenReturn(response());

        scheduler.importKStartupPostings();
        scheduler.importPublicInstitutionJobPostings();
        scheduler.importQNetPostings();

        verify(externalPostingIngestService).importFrom(publicInstitutionJobPostingSource);
        verify(externalPostingIngestService).importFrom(qNetPostingSource);
    }
}
