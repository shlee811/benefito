package com.lion.benefito.infrastructure.external.scholarship;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScholarshipPostingSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "SCHOLARSHIP";
    private static final String NOT_APPLICABLE = "해당없음";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ScholarshipCsvReader scholarshipCsvReader;

    @Override
    public List<ExternalPostingData> fetchPostings() {
        List<ScholarshipCsvRow> rows;
        try {
            rows = scholarshipCsvReader.readAll();
        } catch (Exception e) {
            log.warn("장학금 CSV 로드 실패: {}", e.getMessage());
            return List.of();
        }

        List<ExternalPostingData> result = new ArrayList<>();
        for (ScholarshipCsvRow row : rows) {
            try {
                result.add(toExternalPostingData(row));
            } catch (Exception e) {
                log.warn("장학금 CSV 행 파싱 실패(번호={}): {}", row.no(), e.getMessage());
            }
        }
        return result;
    }

    private ExternalPostingData toExternalPostingData(ScholarshipCsvRow row) {
        String no = trimToNull(row.no());
        if (no == null) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }

        return new ExternalPostingData(
                SOURCE_TYPE,
                no,
                buildTitle(row),
                buildContent(row),
                trimToNull(row.orgName()),
                trimToNull(row.homepageUrl()),
                null,
                null,
                buildQualification(row),
                parseDate(row.recruitStartDate()),
                parseDeadline(row.recruitEndDate())
        );
    }

    private String buildTitle(ScholarshipCsvRow row) {
        String org = valueOrEmpty(row.orgName());
        String product = valueOrEmpty(row.productName());
        return (org + " " + product).strip();
    }

    // Q-Net처럼, 이 데이터 자체가 곧 "공고 내용"이라 지원내역/제출서류를 그대로 content로 구성한다.
    private String buildContent(ScholarshipCsvRow row) {
        return joinNonBlank(row.supportDetail(), row.requiredDocuments());
    }

    // 학년/학과/성적/소득/지역조건을 조합한다. "해당없음"은 정보가 없다는 뜻이라 필터링한다.
    private String buildQualification(ScholarshipCsvRow row) {
        return joinNonBlank(row.gradeType(), row.majorType(), row.academicCriteria(),
                row.incomeCriteria(), row.regionCondition());
    }

    private String joinNonBlank(String... values) {
        return Arrays.stream(values)
                .map(this::trimToNull)
                .filter(value -> value != null && !value.equals(NOT_APPLICABLE))
                .collect(Collectors.joining("\n"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String valueOrEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : "";
    }

    // 모집시작일은 Posting에 대응 컬럼이 없어 PostingMapper가 사용하지 않는다(ExternalPostingData에만 보존).
    // null/파싱 실패 시 항목 전체를 실패 처리하지 않고 시작일만 비워둔다(마감일과 달리 필수 정보가 아님).
    private LocalDate parseDate(String rawDate) {
        String trimmed = trimToNull(rawDate);
        if (trimmed == null) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("장학금 모집시작일 파싱 실패, null로 둡니다: {}", rawDate);
            return null;
        }
    }

    // 실제 CSV 확인 결과 모집종료일은 "2026-03-23"과 같은 yyyy-MM-dd 형식이다(K-Startup 등의 yyyyMMdd와 다름).
    private LocalDate parseDeadline(String rawDate) {
        String trimmed = trimToNull(rawDate);
        if (trimmed == null) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
        try {
            return LocalDate.parse(trimmed, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
    }
}
