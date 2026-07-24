package com.lion.benefito.infrastructure.external.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SeedInternSourceTest {

    private final SeedInternSource source = new SeedInternSource();

    @Test
    void sourceType은_SEED_INTERN으로_고정된다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(data -> data.sourceType().equals("SEED_INTERN"));
    }

    @Test
    void externalId는_모두_고유하다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.stream().map(ExternalPostingData::externalId).collect(Collectors.toSet()))
                .hasSameSizeAs(result);
    }

    @Test
    void 모집_마감일은_모두_오늘_이후라_status가_ACTIVE로_판정된다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).allSatisfy(data -> {
            assertThat(data.deadline()).isNotNull();
            assertThat(data.deadline()).isAfterOrEqualTo(LocalDate.now());
        });
    }

    @Test
    void 실존하지_않는_가상_회사이므로_원문_URL을_지어내지_않고_null로_둔다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).allMatch(data -> data.originalUrl() == null);
    }

    @Test
    void 지역이_여러_지역에_걸쳐_다양하게_분포한다() {
        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.stream().map(ExternalPostingData::region).collect(Collectors.toSet()).size())
                .isGreaterThanOrEqualTo(5);
    }
}
