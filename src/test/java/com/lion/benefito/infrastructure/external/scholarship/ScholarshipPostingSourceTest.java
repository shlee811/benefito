package com.lion.benefito.infrastructure.external.scholarship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScholarshipPostingSourceTest {

    private ScholarshipCsvReader reader;
    private ScholarshipPostingSource source;

    @BeforeEach
    void setUp() {
        reader = mock(ScholarshipCsvReader.class);
        source = new ScholarshipPostingSource(reader);
    }

    private ScholarshipCsvRow row(String no, String org, String product, String grade, String major,
                                   String academic, String income, String support, String region,
                                   String docs, String url, String startDate, String endDate) {
        return new ScholarshipCsvRow(no, org, product, grade, major, academic, income, support,
                region, docs, url, startDate, endDate);
    }

    @Test
    void sourceType과_externalId를_올바르게_채운다() {
        when(reader.readAll()).thenReturn(List.of(row("1", "테스트재단", "생활비 장학금", "대학2학기", "해당없음",
                "성적 3.0 이상", "중위소득 100% 이하", "1인당 100만원", "해당없음", "신청서",
                "https://example.com/1", "2026-01-01", "2026-01-31")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).sourceType()).isEqualTo("SCHOLARSHIP");
        assertThat(result.get(0).externalId()).isEqualTo("1");
    }

    @Test
    void title은_운영기관명과_상품명을_조합한다() {
        when(reader.readAll()).thenReturn(List.of(row("1", "테스트재단", "생활비 장학금", "대학2학기", "해당없음",
                "성적 3.0 이상", "중위소득 100% 이하", "1인당 100만원", "해당없음", "신청서",
                "https://example.com/1", "2026-01-01", "2026-01-31")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).title()).isEqualTo("테스트재단 생활비 장학금");
    }

    @Test
    void startDate와_endDate를_yyyy_MM_dd_형식으로_파싱한다() {
        when(reader.readAll()).thenReturn(List.of(row("1", "테스트재단", "생활비 장학금", "대학2학기", "해당없음",
                "성적 3.0 이상", "중위소득 100% 이하", "1인당 100만원", "해당없음", "신청서",
                "https://example.com/1", "2026-01-01", "2026-01-31")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.get(0).deadline()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void qualification은_해당없음을_제외하고_학년_학과_성적_소득_지역조건을_조합한다() {
        when(reader.readAll()).thenReturn(List.of(row("1", "테스트재단", "생활비 장학금", "대학2학기", "해당없음",
                "성적 3.0 이상", "중위소득 100% 이하", "1인당 100만원", "서울 거주자", "신청서",
                "https://example.com/1", "2026-01-01", "2026-01-31")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).qualification())
                .isEqualTo("대학2학기\n성적 3.0 이상\n중위소득 100% 이하\n서울 거주자");
    }

    @Test
    void content는_지원내역과_제출서류를_조합한다() {
        when(reader.readAll()).thenReturn(List.of(row("1", "테스트재단", "생활비 장학금", "대학2학기", "해당없음",
                "성적 3.0 이상", "중위소득 100% 이하", "1인당 100만원 생활비 지원", "해당없음", "신청서, 성적증명서",
                "https://example.com/1", "2026-01-01", "2026-01-31")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).content()).isEqualTo("1인당 100만원 생활비 지원\n신청서, 성적증명서");
    }

    @Test
    void startDate가_없거나_형식이_잘못돼도_항목_전체는_실패하지_않는다() {
        when(reader.readAll()).thenReturn(List.of(
                row("1", "테스트재단", "장학금A", "해당없음", "해당없음", "해당없음", "해당없음", "해당없음",
                        "해당없음", "해당없음", "https://example.com/1", "", "2026-01-31"),
                row("2", "테스트재단", "장학금B", "해당없음", "해당없음", "해당없음", "해당없음", "해당없음",
                        "해당없음", "해당없음", "https://example.com/2", "2026-13-99", "2026-02-28")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).startDate()).isNull();
        assertThat(result.get(1).startDate()).isNull();
    }

    @Test
    void 마감일_형식이_잘못되면_해당_항목만_제외하고_계속_진행한다() {
        when(reader.readAll()).thenReturn(List.of(
                row("1", "테스트재단", "장학금A", "해당없음", "해당없음", "해당없음", "해당없음", "해당없음",
                        "해당없음", "해당없음", "https://example.com/1", "2026-01-01", "20260131"),
                row("2", "테스트재단", "장학금B", "해당없음", "해당없음", "해당없음", "해당없음", "해당없음",
                        "해당없음", "해당없음", "https://example.com/2", "2026-02-01", "2026-02-28")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalId()).isEqualTo("2");
    }

    @Test
    void CSV_로드_자체가_실패하면_빈_리스트를_반환한다() {
        when(reader.readAll()).thenThrow(new RuntimeException("파일을 찾을 수 없음"));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isEmpty();
    }
}
