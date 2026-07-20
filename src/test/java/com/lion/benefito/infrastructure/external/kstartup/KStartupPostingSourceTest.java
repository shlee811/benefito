package com.lion.benefito.infrastructure.external.kstartup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KStartupPostingSourceTest {

    private KStartupClient kStartupClient;
    private KStartupPostingSource source;

    @BeforeEach
    void setUp() {
        kStartupClient = mock(KStartupClient.class);
        KStartupProperties properties = new KStartupProperties();
        properties.setPerPage(2);
        properties.setMaxPages(5);
        source = new KStartupPostingSource(kStartupClient, properties);
    }

    private KStartupAnnouncementItem item(Long pbancSn, String pbancNtrpNm, String sprvInst,
                                           String aplyTrgtCtnt, String aplyTrgt, String endDt) {
        return new KStartupAnnouncementItem(pbancSn, "제목" + pbancSn, "내용", pbancNtrpNm, sprvInst,
                "사업화", "서울", aplyTrgtCtnt, aplyTrgt, endDt, "https://k-startup.go.kr/" + pbancSn);
    }

    @Test
    void company는_pbanc_ntrp_nm을_우선_사용하고_없으면_sprv_inst를_사용한다() {
        KStartupAnnouncementItem withNtrpNm = item(1L, "BoxLadder", "민간", "누구나", null, "20260803");
        KStartupAnnouncementItem blankNtrpNm = item(2L, "", "공공기관", "누구나", null, "20260803");
        when(kStartupClient.fetchAnnouncements(1, 2))
                .thenReturn(new KStartupAnnouncementResponse(2, 2, 1, 2, 2, List.of(withNtrpNm, blankNtrpNm), null, null));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).extracting(ExternalPostingData::organization)
                .containsExactly("BoxLadder", "공공기관");
    }

    @Test
    void qualification은_aply_trgt_ctnt와_aply_trgt를_조합한다() {
        KStartupAnnouncementItem withBoth = item(1L, "회사", null, "예비창업자", "대학생,일반인", "20260803");
        when(kStartupClient.fetchAnnouncements(1, 2))
                .thenReturn(new KStartupAnnouncementResponse(1, 1, 1, 2, 1, List.of(withBoth), null, null));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).qualification()).isEqualTo("예비창업자\n신청 대상: 대학생,일반인");
    }

    @Test
    void 마감일은_yyyyMMdd_형식으로_파싱한다() {
        KStartupAnnouncementItem valid = item(1L, "회사", null, "누구나", null, "20260803");
        when(kStartupClient.fetchAnnouncements(1, 2))
                .thenReturn(new KStartupAnnouncementResponse(1, 1, 1, 2, 1, List.of(valid), null, null));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).deadline()).isEqualTo(LocalDate.of(2026, 8, 3));
    }

    @Test
    void 마감일_형식이_잘못되면_해당_항목만_제외하고_계속_진행한다() {
        KStartupAnnouncementItem invalidDate = item(1L, "회사", null, "누구나", null, "2012-12-01");
        KStartupAnnouncementItem validDate = item(2L, "회사", null, "누구나", null, "20260803");
        when(kStartupClient.fetchAnnouncements(1, 2))
                .thenReturn(new KStartupAnnouncementResponse(2, 2, 1, 2, 2, List.of(invalidDate, validDate), null, null));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalId()).isEqualTo("2");
    }

    @Test
    void totalCount에_도달하면_다음_페이지를_요청하지_않는다() {
        KStartupAnnouncementItem a = item(1L, "회사", null, "누구나", null, "20260803");
        KStartupAnnouncementItem b = item(2L, "회사", null, "누구나", null, "20260803");
        when(kStartupClient.fetchAnnouncements(1, 2))
                .thenReturn(new KStartupAnnouncementResponse(2, 2, 1, 2, 2, List.of(a, b), null, null));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(2);
        verify(kStartupClient, times(1)).fetchAnnouncements(anyInt(), eq(2));
    }

    @Test
    void totalCount에_도달하지_않으면_다음_페이지를_계속_요청한다() {
        KStartupAnnouncementItem a = item(1L, "회사", null, "누구나", null, "20260803");
        KStartupAnnouncementItem b = item(2L, "회사", null, "누구나", null, "20260803");
        KStartupAnnouncementItem c = item(3L, "회사", null, "누구나", null, "20260803");
        when(kStartupClient.fetchAnnouncements(1, 2))
                .thenReturn(new KStartupAnnouncementResponse(2, 3, 1, 2, 3, List.of(a, b), null, null));
        when(kStartupClient.fetchAnnouncements(2, 2))
                .thenReturn(new KStartupAnnouncementResponse(1, 3, 2, 2, 3, List.of(c), null, null));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(3);
        verify(kStartupClient, times(1)).fetchAnnouncements(1, 2);
        verify(kStartupClient, times(1)).fetchAnnouncements(2, 2);
    }

    @Test
    void 특정_페이지_호출이_실패하면_그때까지_수집한_결과만_반환하고_중단한다() {
        KStartupAnnouncementItem a = item(1L, "회사", null, "누구나", null, "20260803");
        when(kStartupClient.fetchAnnouncements(1, 2))
                .thenReturn(new KStartupAnnouncementResponse(1, 100, 1, 2, 100, List.of(a), null, null));
        when(kStartupClient.fetchAnnouncements(2, 2))
                .thenThrow(new RuntimeException("네트워크 오류"));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        verify(kStartupClient, times(1)).fetchAnnouncements(2, 2);
    }
}
