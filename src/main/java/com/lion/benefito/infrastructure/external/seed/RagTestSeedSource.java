package com.lion.benefito.infrastructure.external.seed;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RAG(임베딩 유사도 검색) 및 추천공고 기능을 프론트/성능 테스트할 만큼 실데이터가 부족한 카테고리
 * (장학금/자격증/공모전/대외활동/채용/정부지원사업)를 보강하는 임시 시드 데이터.
 *
 * sourceType을 RAG_TEST_SEED로 고정해 KSTARTUP/PUBLIC_INSTITUTION_JOB/QNET/SCHOLARSHIP/SEED 등
 * 기존 소스와 절대 섞이지 않는다 - 실데이터가 충분히 쌓이면
 * `DELETE FROM postings WHERE source_type = 'RAG_TEST_SEED'` 한 줄로 이 시드만 안전하게 제거할 수
 * 있다. externalId도 "RAG-SEED-"로 시작해 테스트 데이터임을 바로 식별할 수 있게 한다.
 *
 * CSV 한 파일당 카테고리 하나(고정)를 담당한다(RagSeedCsvRow에는 category 컬럼이 없음) - SEED
 * 소스처럼 한 소스가 여러 카테고리를 가지므로 PostingMapper에서도 SEED와 동일한 패턴
 * ("RAG_TEST_SEED" -> rawCategory)으로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTestSeedSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "RAG_TEST_SEED";
    // "온라인"은 REGION 공통코드에 없는 값이라 그대로 넘기면 regionCode가 null로 빠진다.
    // "전국"(비대면/전국 어디서나 참여 가능)으로 대체하고, 본문에 온라인 진행 여부를 남긴다.
    private static final String ONLINE_RAW = "온라인";
    private static final String NATIONWIDE_REGION = "전국";

    // CSV 파일 하나당 고정 카테고리 하나. externalId 접두사에도 이 카테고리 라벨을 그대로 쓴다.
    private static final Map<String, String> CSV_PATH_TO_CATEGORY = new LinkedHashMap<>();

    static {
        CSV_PATH_TO_CATEGORY.put("classpath:data/rag_seed_scholarship.csv", "SCHOLARSHIP");
        CSV_PATH_TO_CATEGORY.put("classpath:data/rag_seed_certificate.csv", "CERTIFICATE");
        CSV_PATH_TO_CATEGORY.put("classpath:data/rag_seed_contest.csv", "CONTEST");
        CSV_PATH_TO_CATEGORY.put("classpath:data/rag_seed_extracurricular.csv", "EXTRACURRICULAR");
        CSV_PATH_TO_CATEGORY.put("classpath:data/rag_seed_job.csv", "JOB");
        CSV_PATH_TO_CATEGORY.put("classpath:data/rag_seed_government_support.csv", "GOVERNMENT_SUPPORT");
    }

    private final RagSeedCsvReader ragSeedCsvReader;

    @Override
    public List<ExternalPostingData> fetchPostings() {
        List<ExternalPostingData> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : CSV_PATH_TO_CATEGORY.entrySet()) {
            result.addAll(readCategoryFile(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private List<ExternalPostingData> readCategoryFile(String csvPath, String categoryCode) {
        List<RagSeedCsvRow> rows;
        try {
            rows = ragSeedCsvReader.readAll(csvPath);
        } catch (Exception e) {
            log.warn("RAG 테스트 시드 CSV 로드 실패(path={}): {}", csvPath, e.getMessage());
            return List.of();
        }

        List<ExternalPostingData> result = new ArrayList<>();
        int seq = 0;
        for (RagSeedCsvRow row : rows) {
            seq++;
            try {
                result.add(toExternalPostingData(row, categoryCode, seq));
            } catch (Exception e) {
                log.warn("RAG 테스트 시드 행 파싱 실패(category={}, title={}): {}", categoryCode, row.title(), e.getMessage());
            }
        }
        return result;
    }

    private ExternalPostingData toExternalPostingData(RagSeedCsvRow row, String categoryCode, int seq) {
        String title = requireNonBlank(row.title(), "title");
        String organization = requireNonBlank(row.organization(), "organization");

        String externalId = "RAG-SEED-%s-%04d".formatted(categoryCode, seq);
        String region = resolveRegion(row.region());
        LocalDate regStart = parseDateOrNull(row.regStart());
        LocalDate deadline = parseDateOrNull(row.deadline());

        return new ExternalPostingData(
                SOURCE_TYPE,
                externalId,
                title,
                requireNonBlank(row.content(), "content"),
                organization,
                null,
                categoryCode,
                region,
                buildQualification(row),
                regStart,
                deadline,
                blankToNull(row.salary()),
                blankToNull(row.employmentType()),
                blankToNull(row.enterpriseType()),
                blankToNull(row.career()),
                blankToNull(row.education()));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }

    // target(지원대상) + major(전공) + grade(학년/학적)를 지원자격 텍스트로 합친다.
    // "전공 무관"처럼 정보값이 없는 항목은 굳이 나열하지 않는다.
    private String buildQualification(RagSeedCsvRow row) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, row.target());
        addIfPresent(parts, row.major());
        addIfPresent(parts, row.grade());
        if (row.benefit() != null && !row.benefit().isBlank()) {
            parts.add("혜택: " + row.benefit().strip());
        }
        if (row.applicationMethod() != null && !row.applicationMethod().isBlank()) {
            parts.add("신청방법: " + row.applicationMethod().strip());
        }
        return String.join("\n", parts);
    }

    private void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.strip());
        }
    }

    private String resolveRegion(String rawRegion) {
        if (rawRegion == null || rawRegion.isBlank()) {
            return NATIONWIDE_REGION;
        }
        String trimmed = rawRegion.strip();
        return trimmed.equals(ONLINE_RAW) ? NATIONWIDE_REGION : trimmed;
    }

    // deadline이 비어 있으면(상시 모집) null을 반환해 PostingMapper.resolveStatus가 ACTIVE로 판정하게 둔다.
    private LocalDate parseDateOrNull(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(rawDate.strip());
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
        return value.strip();
    }
}
