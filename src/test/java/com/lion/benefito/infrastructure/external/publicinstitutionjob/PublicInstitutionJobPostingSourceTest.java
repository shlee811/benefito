package com.lion.benefito.infrastructure.external.publicinstitutionjob;

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

class PublicInstitutionJobPostingSourceTest {

    private PublicInstitutionJobClient client;
    private PublicInstitutionJobPostingSource source;

    @BeforeEach
    void setUp() {
        client = mock(PublicInstitutionJobClient.class);
        PublicInstitutionJobProperties properties = new PublicInstitutionJobProperties();
        properties.setNumOfRows(2);
        properties.setMaxPages(5);
        source = new PublicInstitutionJobPostingSource(client, properties);
    }

    private PublicInstitutionJobItem item(Long sn, String workRgnNmLst, String endYmd) {
        return new PublicInstitutionJobItem(sn, "제목" + sn, "기관" + sn, endYmd,
                "https://example.com/" + sn, workRgnNmLst, "정규직", "학력무관", "지원자격",
                "서류전형", "우대사항", "우대조건", "경영.회계.사무", "신입");
    }

    @Test
    void 단일_지역이면_원본_지역명을_그대로_ExternalPostingData_region에_담는다() {
        PublicInstitutionJobItem single = item(1L, "서울", "20260804");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 1, List.of(single)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).region()).isEqualTo("서울");
    }

    @Test
    void 콤마로_구분된_여러_지역이면_전국으로_정규화한다() {
        PublicInstitutionJobItem nationwide = item(1L, "서울,부산,대구", "20260804");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 1, List.of(nationwide)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).region()).isEqualTo("전국");
    }

    @Test
    void sourceType과_externalId를_올바르게_채운다() {
        PublicInstitutionJobItem single = item(302908L, "서울", "20260804");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 1, List.of(single)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).sourceType()).isEqualTo("PUBLIC_INSTITUTION_JOB");
        assertThat(result.get(0).externalId()).isEqualTo("302908");
    }

    @Test
    void 마감일은_yyyyMMdd_형식으로_파싱한다() {
        PublicInstitutionJobItem single = item(1L, "서울", "20260804");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 1, List.of(single)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).deadline()).isEqualTo(LocalDate.of(2026, 8, 4));
    }

    @Test
    void 마감일_형식이_잘못되면_해당_항목만_제외하고_계속_진행한다() {
        PublicInstitutionJobItem invalidDate = item(1L, "서울", "2012-12-01");
        PublicInstitutionJobItem validDate = item(2L, "서울", "20260804");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 2, List.of(invalidDate, validDate)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalId()).isEqualTo("2");
    }

    @Test
    void qualification은_acbgCondNmLst와_aplyQlfcCn을_조합한다() {
        PublicInstitutionJobItem single = new PublicInstitutionJobItem(1L, "제목", "기관", "20260804",
                "https://example.com/1", "서울", "정규직", "학력무관", "예비창업자 우대", "서류전형", "우대", "조건", "NCS", "신입");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 1, List.of(single)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).qualification()).isEqualTo("학력무관\n예비창업자 우대");
    }

    @Test
    void content는_scrnprcdrMthdExpln_prefCn_prefCondCn_ncsCdNmLst_recrutSeNm을_조합한다() {
        PublicInstitutionJobItem single = new PublicInstitutionJobItem(1L, "제목", "기관", "20260804",
                "https://example.com/1", "서울", "정규직", "학력무관", "지원자격",
                "서류전형", "취업지원 대상자", "장애인", "보건.의료", "신입");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 1, List.of(single)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).content())
                .isEqualTo("서류전형\n취업지원 대상자\n장애인\n보건.의료\n신입");
    }

    @Test
    void totalCount에_도달하면_다음_페이지를_요청하지_않는다() {
        PublicInstitutionJobItem a = item(1L, "서울", "20260804");
        PublicInstitutionJobItem b = item(2L, "서울", "20260804");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 2, List.of(a, b)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(2);
        verify(client, times(1)).fetchList(anyInt(), eq(2));
    }

    @Test
    void totalCount에_도달하지_않으면_다음_페이지를_계속_요청한다() {
        PublicInstitutionJobItem a = item(1L, "서울", "20260804");
        PublicInstitutionJobItem b = item(2L, "서울", "20260804");
        PublicInstitutionJobItem c = item(3L, "서울", "20260804");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 3, List.of(a, b)));
        when(client.fetchList(2, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 3, List.of(c)));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(3);
        verify(client, times(1)).fetchList(1, 2);
        verify(client, times(1)).fetchList(2, 2);
    }

    @Test
    void 특정_페이지_호출이_실패하면_그때까지_수집한_결과만_반환하고_중단한다() {
        PublicInstitutionJobItem a = item(1L, "서울", "20260804");
        when(client.fetchList(1, 2))
                .thenReturn(new PublicInstitutionJobListResponse(200, "성공했습니다.", 100, List.of(a)));
        when(client.fetchList(2, 2))
                .thenThrow(new RuntimeException("네트워크 오류"));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        verify(client, times(1)).fetchList(2, 2);
    }
}
