package com.lion.benefito.infrastructure.external.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SeedQNetScheduleSourceTest {

    private final SeedQNetScheduleSource source = new SeedQNetScheduleSource();

    @Test
    void sourceType은_SEED_QNET_SCHEDULE로_고정되어_실제_QNET_데이터와_분리된다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(data -> data.sourceType().equals("SEED_QNET_SCHEDULE"));
    }

    @Test
    void 시드는_20에서_30건_사이다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.size()).isBetween(20, 30);
    }

    @Test
    void externalId는_소스_내에서_모두_고유하다() {
        List<ExternalPostingData> result = source.fetchPostings();

        Set<String> externalIds = result.stream().map(ExternalPostingData::externalId).collect(java.util.stream.Collectors.toSet());

        assertThat(externalIds).hasSameSizeAs(result);
    }

    @Test
    void 모든_원서접수_마감일은_오늘_이후라_status가_ACTIVE로_판정된다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).allSatisfy(data -> {
            assertThat(data.deadline()).isNotNull();
            assertThat(data.deadline()).isAfterOrEqualTo(LocalDate.now());
        });
    }

    @Test
    void 지역은_항상_전국이다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).allMatch(data -> data.region().equals("전국"));
    }

    @Test
    void 원문_URL은_실제_시행기관_도메인이고_가짜_example_com이_아니다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).allSatisfy(data -> {
            assertThat(data.originalUrl()).isNotBlank();
            assertThat(data.originalUrl()).doesNotContain("example.com");
        });
    }

    @Test
    void content에_회차_시험구분_접수기간_시험일_발표일이_모두_포함된다() {
        List<ExternalPostingData> result = source.fetchPostings();

        ExternalPostingData first = result.stream()
                .filter(data -> data.externalId().startsWith("정보처리기사"))
                .findFirst()
                .orElseThrow();

        assertThat(first.content())
                .contains("종목명:")
                .contains("시험회차:")
                .contains("구분:")
                .contains("원서접수:")
                .contains("시험일:")
                .contains("합격자 발표일:");
    }

    @Test
    void 예시로_주어진_제목이_시드에_포함된다() {
        List<ExternalPostingData> result = source.fetchPostings();
        List<String> titles = result.stream().map(ExternalPostingData::title).toList();

        assertThat(titles).contains(
                "2026년 정보처리기사 제3회 필기시험",
                "2026년 정보처리기사 제3회 실기시험",
                "2026년 전기기사 제3회 필기시험",
                "2026년 산업안전기사 제2회 실기시험",
                "2026년 건축기사 제2회 필기시험",
                "2026년 소방설비기사(전기분야) 제3회 필기시험",
                "2026년 한식조리기능사 상시 필기시험",
                "2026년 컴퓨터활용능력 1급 필기시험",
                "2026년 SQLD 제1회 시험",
                "2026년 ADsP 제1회 시험",
                "2026년 빅데이터분석기사 제3회 필기시험");
    }
}
