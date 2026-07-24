package com.lion.benefito.infrastructure.external.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagTestSeedSourceTest {

    private final RagSeedCsvReader reader = mock(RagSeedCsvReader.class);
    private final RagTestSeedSource source = new RagTestSeedSource(reader);

    private RagSeedCsvRow row(String title, String org, String region, String content, String deadline) {
        return new RagSeedCsvRow(title, org, region, "대학생", "전공 무관", "재학생", "혜택", "서류 전형",
                content, "2026-08-01", deadline, null, null, null, null, null);
    }

    @Test
    void sourceType은_RAG_TEST_SEED로_고정된다() {
        when(reader.readAll("classpath:data/rag_seed_scholarship.csv"))
                .thenReturn(List.of(row("장학금A", "재단A", "서울", "설명", "2026-09-01")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(data -> data.sourceType().equals("RAG_TEST_SEED"));
    }

    @Test
    void externalId는_RAG_SEED_카테고리_4자리_순번_형식이다() {
        when(reader.readAll("classpath:data/rag_seed_scholarship.csv")).thenReturn(List.of(
                row("장학금A", "재단A", "서울", "설명A", "2026-09-01"),
                row("장학금B", "재단B", "경기", "설명B", "2026-09-05")));

        List<ExternalPostingData> result = source.fetchPostings();

        List<ExternalPostingData> scholarshipData = result.stream()
                .filter(d -> d.externalId().startsWith("RAG-SEED-SCHOLARSHIP")).toList();
        assertThat(scholarshipData.get(0).externalId()).isEqualTo("RAG-SEED-SCHOLARSHIP-0001");
        assertThat(scholarshipData.get(1).externalId()).isEqualTo("RAG-SEED-SCHOLARSHIP-0002");
    }

    @Test
    void category는_CSV_파일별로_고정된다() {
        when(reader.readAll("classpath:data/rag_seed_certificate.csv"))
                .thenReturn(List.of(row("2026년 정보처리기사 필기시험", "Q-Net", "전국", "설명", "2026-09-01")));

        List<ExternalPostingData> result = source.fetchPostings();

        ExternalPostingData certData = result.stream()
                .filter(d -> d.externalId().startsWith("RAG-SEED-CERTIFICATE")).findFirst().orElseThrow();
        assertThat(certData.category()).isEqualTo("CERTIFICATE");
    }

    @Test
    void 온라인_지역은_전국으로_치환된다() {
        when(reader.readAll("classpath:data/rag_seed_extracurricular.csv"))
                .thenReturn(List.of(row("온라인 멘토링", "재단A", "온라인", "설명", "2026-09-01")));

        List<ExternalPostingData> result = source.fetchPostings();

        ExternalPostingData data = result.stream()
                .filter(d -> d.externalId().startsWith("RAG-SEED-EXTRACURRICULAR")).findFirst().orElseThrow();
        assertThat(data.region()).isEqualTo("전국");
    }

    @Test
    void originalUrl은_지어내지_않고_항상_null이다() {
        when(reader.readAll("classpath:data/rag_seed_job.csv"))
                .thenReturn(List.of(row("백엔드 인턴", "라이언테크", "서울", "설명", "2026-09-01")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).allMatch(data -> data.originalUrl() == null);
    }

    @Test
    void 제목이나_기관명이_비어있으면_해당_행만_건너뛰고_계속_진행한다() {
        when(reader.readAll("classpath:data/rag_seed_scholarship.csv")).thenReturn(List.of(
                row("", "재단A", "서울", "설명A", "2026-09-01"),
                row("장학금B", "재단B", "경기", "설명B", "2026-09-05")));

        List<ExternalPostingData> result = source.fetchPostings();

        List<ExternalPostingData> scholarshipData = result.stream()
                .filter(d -> d.externalId().startsWith("RAG-SEED-SCHOLARSHIP")).toList();
        assertThat(scholarshipData).hasSize(1);
        assertThat(scholarshipData.get(0).title()).isEqualTo("장학금B");
    }

    @Test
    void CSV_로드_자체가_실패해도_다른_카테고리_파일은_계속_처리한다() {
        when(reader.readAll("classpath:data/rag_seed_scholarship.csv"))
                .thenThrow(new RuntimeException("파일 없음"));
        when(reader.readAll("classpath:data/rag_seed_certificate.csv"))
                .thenReturn(List.of(row("2026년 SQLD 시험", "한국데이터산업진흥원", "전국", "설명", "2026-09-01")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.stream().anyMatch(d -> d.externalId().startsWith("RAG-SEED-CERTIFICATE"))).isTrue();
        assertThat(result.stream().anyMatch(d -> d.externalId().startsWith("RAG-SEED-SCHOLARSHIP"))).isFalse();
    }

    @Test
    void JOB_필드는_salary_employmentType_enterpriseType_career_education이_그대로_전달된다() {
        RagSeedCsvRow jobRow = new RagSeedCsvRow(
                "백엔드 인턴", "라이언테크", "서울", "취업준비생", "전공 무관", "졸업예정자", "활동비", "서류 전형",
                "설명", "2026-08-01", "2026-09-01",
                "월 250만원", "인턴", "스타트업", "신입", "대학 졸업예정자");
        when(reader.readAll("classpath:data/rag_seed_job.csv")).thenReturn(List.of(jobRow));

        List<ExternalPostingData> result = source.fetchPostings();

        ExternalPostingData data = result.stream()
                .filter(d -> d.externalId().startsWith("RAG-SEED-JOB")).findFirst().orElseThrow();
        assertThat(data.salary()).isEqualTo("월 250만원");
        assertThat(data.employmentType()).isEqualTo("인턴");
        assertThat(data.enterpriseType()).isEqualTo("스타트업");
        assertThat(data.career()).isEqualTo("신입");
        assertThat(data.education()).isEqualTo("대학 졸업예정자");
    }
}
