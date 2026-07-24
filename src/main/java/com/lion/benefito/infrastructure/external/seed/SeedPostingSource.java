package com.lion.benefito.infrastructure.external.seed;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 공식 API/CSV를 확보하기 어려운 공모전·대외활동 카테고리를 위한 시연용 시드 데이터 소스.
 * CSV의 category 값("CONTEST"/"ACTIVITY")을 실제 CommonCode 값으로 변환하는 것 외에는
 * 다른 ExternalPostingSource와 동일한 흐름(읽기 -> 항목 단위 변환 -> 실패 격리)을 따른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedPostingSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "SEED";

    // CSV 원본 값 -> 실제 CommonCode.code 값. "ACTIVITY"는 실제 코드가 EXTRACURRICULAR라 변환이 필요하다.
    private static final String CONTEST_RAW = "CONTEST";
    private static final String ACTIVITY_RAW = "ACTIVITY";
    private static final String CONTEST_CATEGORY_CODE = "CONTEST";
    private static final String ACTIVITY_CATEGORY_CODE = "EXTRACURRICULAR";
    // CSV에 지역 컬럼이 없어 지역 조건은 항상 "전국"으로 채운다.
    private static final String NATIONWIDE_REGION = "전국";

    private final SeedCsvReader seedCsvReader;

    @Override
    public List<ExternalPostingData> fetchPostings() {
        List<SeedCsvRow> rows;
        try {
            rows = seedCsvReader.readAll();
        } catch (Exception e) {
            log.warn("시드 CSV 로드 실패: {}", e.getMessage());
            return List.of();
        }

        // 카테고리별로 SEED-CONTEST-001, SEED-ACTIVITY-001처럼 순번을 매긴다.
        // CSV 행 순서가 곧 번호가 되므로, 파일의 행 순서를 바꾸면 externalId도 바뀐다는 점에 유의해야 한다.
        Map<String, Integer> sequenceByRawCategory = new HashMap<>();
        List<ExternalPostingData> result = new ArrayList<>();
        for (SeedCsvRow row : rows) {
            try {
                result.add(toExternalPostingData(row, sequenceByRawCategory));
            } catch (Exception e) {
                log.warn("시드 CSV 행 파싱 실패(title={}): {}", row.title(), e.getMessage());
            }
        }
        return result;
    }

    private ExternalPostingData toExternalPostingData(SeedCsvRow row, Map<String, Integer> sequenceByRawCategory) {
        String rawCategory = trimToNull(row.category());
        if (rawCategory == null) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }

        String categoryCode = resolveCategoryCode(rawCategory);
        String externalId = buildExternalId(rawCategory, sequenceByRawCategory);

        return new ExternalPostingData(
                SOURCE_TYPE,
                externalId,
                trimToNull(row.title()),
                buildContent(row),
                trimToNull(row.organization()),
                null,
                categoryCode,
                NATIONWIDE_REGION,
                buildQualification(row),
                null,
                null
        );
    }

    private String resolveCategoryCode(String rawCategory) {
        return switch (rawCategory) {
            case CONTEST_RAW -> CONTEST_CATEGORY_CODE;
            case ACTIVITY_RAW -> ACTIVITY_CATEGORY_CODE;
            default -> throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        };
    }

    private String buildExternalId(String rawCategory, Map<String, Integer> sequenceByRawCategory) {
        int seq = sequenceByRawCategory.merge(rawCategory, 1, Integer::sum);
        return "SEED-%s-%03d".formatted(rawCategory, seq);
    }

    // target(참가대상)과 qualification(지원조건)을 함께 담아야 "누가 지원할 수 있는지"가 온전히 드러난다.
    private String buildQualification(SeedCsvRow row) {
        String target = trimToNull(row.target());
        String qualification = trimToNull(row.qualification());
        if (target == null) {
            return qualification;
        }
        if (qualification == null) {
            return target;
        }
        return target + "\n" + qualification;
    }

    // benefit(혜택)은 별도 Posting 필드가 없어 content 뒤에 덧붙인다.
    private String buildContent(SeedCsvRow row) {
        String content = trimToNull(row.content());
        String benefit = trimToNull(row.benefit());
        if (benefit == null) {
            return content;
        }
        String base = content != null ? content : "";
        return base + "\n혜택: " + benefit;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
