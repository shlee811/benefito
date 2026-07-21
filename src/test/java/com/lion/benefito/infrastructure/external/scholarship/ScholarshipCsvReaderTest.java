package com.lion.benefito.infrastructure.external.scholarship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class ScholarshipCsvReaderTest {

    private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

    private ScholarshipCsvReader reader(String csvPath) {
        ScholarshipProperties properties = new ScholarshipProperties();
        properties.setCsvPath(csvPath);
        return new ScholarshipCsvReader(resourceLoader, properties);
    }

    @Test
    void CP949로_인코딩된_CSV를_정상적으로_읽고_한글이_깨지지_않는다() {
        List<ScholarshipCsvRow> rows = reader("classpath:data/scholarship_test.csv").readAll();

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).orgName()).isEqualTo("테스트재단");
        assertThat(rows.get(0).productName()).isEqualTo("생활비 지원 장학금");
    }

    @Test
    void 따옴표로_묶인_콤마_포함_필드를_안전하게_파싱한다() {
        List<ScholarshipCsvRow> rows = reader("classpath:data/scholarship_test.csv").readAll();

        assertThat(rows.get(0).requiredDocuments()).isEqualTo("신청서, 성적증명서");
    }

    @Test
    void 모든_필요_컬럼을_올바르게_매핑한다() {
        List<ScholarshipCsvRow> rows = reader("classpath:data/scholarship_test.csv").readAll();
        ScholarshipCsvRow row = rows.get(1);

        assertThat(row.no()).isEqualTo("2");
        assertThat(row.orgName()).isEqualTo("테스트재단");
        assertThat(row.productName()).isEqualTo("우수인재 장학금");
        assertThat(row.gradeType()).isEqualTo("대학3학기");
        assertThat(row.majorType()).isEqualTo("공학계열");
        assertThat(row.academicCriteria()).isEqualTo("성적 평균 3.5 이상");
        assertThat(row.incomeCriteria()).isEqualTo("해당없음");
        assertThat(row.supportDetail()).isEqualTo("등록금 전액 지원");
        assertThat(row.regionCondition()).isEqualTo("서울 거주자");
        assertThat(row.homepageUrl()).isEqualTo("https://example.com/scholarship2");
        assertThat(row.recruitStartDate()).isEqualTo("2026-02-01");
        assertThat(row.recruitEndDate()).isEqualTo("2026-02-28");
    }

    @Test
    void 파일이_존재하지_않으면_API_CALL_FAILED_예외() {
        ScholarshipCsvReader reader = reader("classpath:data/does-not-exist.csv");

        assertThatThrownBy(reader::readAll)
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.API_CALL_FAILED));
    }
}
