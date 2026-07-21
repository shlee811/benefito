package com.lion.benefito.infrastructure.external.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeedPostingSourceTest {

    private SeedCsvReader reader;
    private SeedPostingSource source;

    @BeforeEach
    void setUp() {
        reader = mock(SeedCsvReader.class);
        source = new SeedPostingSource(reader);
    }

    private SeedCsvRow row(String title, String org, String category, String target,
                            String qualification, String content, String benefit) {
        return new SeedCsvRow(title, org, category, target, qualification, content, benefit);
    }

    @Test
    void sourceType은_SEED로_고정된다() {
        when(reader.readAll()).thenReturn(List.of(
                row("공모전A", "재단A", "CONTEST", "대학생", "재학생", "내용", "상금")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).sourceType()).isEqualTo("SEED");
    }

    @Test
    void CONTEST는_그대로_CONTEST_카테고리로_ACTIVITY는_EXTRACURRICULAR로_변환된다() {
        when(reader.readAll()).thenReturn(List.of(
                row("공모전A", "재단A", "CONTEST", "대학생", "재학생", "내용", "상금"),
                row("활동A", "재단B", "ACTIVITY", "대학생", "재학생", "내용", "활동비")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).category()).isEqualTo("CONTEST");
        assertThat(result.get(1).category()).isEqualTo("EXTRACURRICULAR");
    }

    @Test
    void externalId는_카테고리별로_3자리_순번을_매긴다() {
        when(reader.readAll()).thenReturn(List.of(
                row("공모전A", "재단A", "CONTEST", "대학생", "재학생", "내용", "상금"),
                row("활동A", "재단B", "ACTIVITY", "대학생", "재학생", "내용", "활동비"),
                row("공모전B", "재단C", "CONTEST", "대학생", "재학생", "내용", "상금"),
                row("활동B", "재단D", "ACTIVITY", "대학생", "재학생", "내용", "활동비")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).externalId()).isEqualTo("SEED-CONTEST-001");
        assertThat(result.get(1).externalId()).isEqualTo("SEED-ACTIVITY-001");
        assertThat(result.get(2).externalId()).isEqualTo("SEED-CONTEST-002");
        assertThat(result.get(3).externalId()).isEqualTo("SEED-ACTIVITY-002");
    }

    @Test
    void qualification은_target과_qualification을_조합한다() {
        when(reader.readAll()).thenReturn(List.of(
                row("공모전A", "재단A", "CONTEST", "전국 대학(원)생", "재학생, 휴학생", "내용", "상금")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).qualification()).isEqualTo("전국 대학(원)생\n재학생, 휴학생");
    }

    @Test
    void content는_benefit을_혜택_레이블과_함께_덧붙인다() {
        when(reader.readAll()).thenReturn(List.of(
                row("공모전A", "재단A", "CONTEST", "대학생", "재학생", "공모전 설명이다.", "상금 100만원")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).content()).isEqualTo("공모전 설명이다.\n혜택: 상금 100만원");
    }

    @Test
    void company는_organization_필드를_사용한다() {
        when(reader.readAll()).thenReturn(List.of(
                row("공모전A", "테스트재단", "CONTEST", "대학생", "재학생", "내용", "상금")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).organization()).isEqualTo("테스트재단");
    }

    @Test
    void 알_수_없는_카테고리_값이면_해당_행만_제외하고_계속_진행한다() {
        when(reader.readAll()).thenReturn(List.of(
                row("공모전A", "재단A", "UNKNOWN", "대학생", "재학생", "내용", "상금"),
                row("공모전B", "재단B", "CONTEST", "대학생", "재학생", "내용", "상금")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalId()).isEqualTo("SEED-CONTEST-001");
    }

    @Test
    void CSV_로드_자체가_실패하면_빈_리스트를_반환한다() {
        when(reader.readAll()).thenThrow(new RuntimeException("파일을 찾을 수 없음"));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isEmpty();
    }
}
