package com.lion.benefito.infrastructure.external.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class SeedCsvReaderTest {

    private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

    private SeedCsvReader reader(String csvPath) {
        SeedProperties properties = new SeedProperties();
        properties.setCsvPath(csvPath);
        return new SeedCsvReader(resourceLoader, properties);
    }

    @Test
    void CSV를_읽어_모든_컬럼을_올바르게_매핑한다() {
        List<SeedCsvRow> rows = reader("classpath:data/seed_test.csv").readAll();

        assertThat(rows).hasSize(2);
        SeedCsvRow first = rows.get(0);
        assertThat(first.title()).isEqualTo("테스트 공모전 A");
        assertThat(first.organization()).isEqualTo("테스트재단A");
        assertThat(first.category()).isEqualTo("CONTEST");
        assertThat(first.target()).isEqualTo("전국 대학생");
        assertThat(first.qualification()).isEqualTo("재학생, 휴학생");
        assertThat(first.content()).isEqualTo("[시연용 샘플 데이터] 테스트 공모전 설명이다.");
        assertThat(first.benefit()).isEqualTo("상금 100만원");
    }

    @Test
    void 두번째_행은_ACTIVITY_카테고리로_읽힌다() {
        List<SeedCsvRow> rows = reader("classpath:data/seed_test.csv").readAll();

        assertThat(rows.get(1).category()).isEqualTo("ACTIVITY");
        assertThat(rows.get(1).title()).isEqualTo("테스트 대외활동 A");
    }

    @Test
    void 파일이_존재하지_않으면_API_CALL_FAILED_예외() {
        SeedCsvReader reader = reader("classpath:data/does-not-exist.csv");

        assertThatThrownBy(reader::readAll)
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.API_CALL_FAILED));
    }
}
