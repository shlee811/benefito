package com.lion.benefito.infrastructure.external.publicinstitutionjob;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicInstitutionJobPostingSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "PUBLIC_INSTITUTION_JOB";
    private static final String NATIONWIDE_REGION_NAME = "전국";
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final PublicInstitutionJobClient publicInstitutionJobClient;
    private final PublicInstitutionJobProperties properties;

    @Override
    public List<ExternalPostingData> fetchPostings() {
        List<ExternalPostingData> result = new ArrayList<>();
        int numOfRows = properties.getNumOfRows();
        int maxPages = properties.getMaxPages();

        for (int page = 1; page <= maxPages; page++) {
            PublicInstitutionJobListResponse response;
            try {
                response = publicInstitutionJobClient.fetchList(page, numOfRows);
            } catch (Exception e) {
                log.warn("공공기관 채용정보 {}페이지 조회 실패, 지금까지 수집한 {}건으로 수집을 중단합니다: {}",
                        page, result.size(), e.getMessage());
                break;
            }

            if (response.result() == null || response.result().isEmpty()) {
                break;
            }

            for (PublicInstitutionJobItem item : response.result()) {
                try {
                    result.add(toExternalPostingData(item));
                } catch (Exception e) {
                    log.warn("공공기관 채용정보 파싱 실패(recrutPblntSn={}): {}", item.recrutPblntSn(), e.getMessage());
                }
            }

            boolean isLastPage = (long) page * numOfRows >= response.totalCount();
            if (isLastPage) {
                break;
            }
        }

        return result;
    }

    private ExternalPostingData toExternalPostingData(PublicInstitutionJobItem item) {
        if (item.recrutPblntSn() == null) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }

        return new ExternalPostingData(
                SOURCE_TYPE,
                String.valueOf(item.recrutPblntSn()),
                item.recrutPbancTtl(),
                buildContent(item),
                item.instNm(),
                item.srcUrl(),
                null,
                resolveRegionName(item.workRgnNmLst()),
                buildQualification(item),
                parseDeadline(item.pbancEndYmd())
        );
    }

    // 사용자 결정: workRgnNmLst가 콤마로 구분된 여러 지역이면 "전국"으로 정규화한다.
    // 이후 PostingMapper.resolveRegionCode()가 Code.name="전국"(REGION 코드 ALL)을 그대로 조회하므로
    // 별도 매핑 로직 없이 기존 미매핑 시 null+warning 정책이 그대로 적용된다.
    private String resolveRegionName(String workRgnNmLst) {
        if (workRgnNmLst == null || workRgnNmLst.isBlank()) {
            return null;
        }
        if (workRgnNmLst.contains(",")) {
            return NATIONWIDE_REGION_NAME;
        }
        return workRgnNmLst;
    }

    // 사용자 결정: qualification은 acbgCondNmLst + aplyQlfcCn 조합.
    private String buildQualification(PublicInstitutionJobItem item) {
        String acbgCond = item.acbgCondNmLst() != null ? item.acbgCondNmLst() : "";
        String aplyQlfc = item.aplyQlfcCn();
        if (aplyQlfc == null || aplyQlfc.isBlank()) {
            return acbgCond;
        }
        return acbgCond.isBlank() ? aplyQlfc : acbgCond + "\n" + aplyQlfc;
    }

    // 사용자 결정: content는 scrnprcdrMthdExpln, prefCn, prefCondCn, ncsCdNmLst, recrutSeNm을 조합.
    private String buildContent(PublicInstitutionJobItem item) {
        return List.of(
                        nullToEmpty(item.scrnprcdrMthdExpln()),
                        nullToEmpty(item.prefCn()),
                        nullToEmpty(item.prefCondCn()),
                        nullToEmpty(item.ncsCdNmLst()),
                        nullToEmpty(item.recrutSeNm()))
                .stream()
                .filter(value -> !value.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    // 실제 응답 확인 결과 pbancEndYmd는 "20260804"와 같은 yyyyMMdd 8자리 숫자 문자열이다(K-Startup과 동일 포맷).
    private LocalDate parseDeadline(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
        try {
            return LocalDate.parse(rawDate, DEADLINE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
    }
}
